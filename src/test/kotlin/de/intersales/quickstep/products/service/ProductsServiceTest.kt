package de.intersales.quickstep.products.service

import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.products.dto.CreateProductDto
import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.products.dto.UpdateProductDto
import de.intersales.quickstep.products.entity.ProductsEntity
import de.intersales.quickstep.products.mapper.ProductsMapper
import de.intersales.quickstep.products.repository.ProductsRepository
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.Mockito.*
import io.quarkus.hibernate.reactive.panache.PanacheQuery

// Helper function to Mockito's any()
fun <T> anyNotNull(): T {
    @Suppress("UNCHECKED_CAST")
    return any<T>() as T
}

@QuarkusTest
class ProductsServiceTest {
    private lateinit var productsService: ProductsService

    // Mocked Dependencies
    @InjectMock
    lateinit var productsRepository: ProductsRepository

    @InjectMock
    lateinit var productsMapper: ProductsMapper

    private val PRODUCT_ID = 10L

    // lateinit set to nullable var to avoid TestInstantiationException
    private var PRODUCT_ENTITY: ProductsEntity? = null
    private var PRODUCT_DTO: ProductsDto? = null
    private var CREATE_DTO: CreateProductDto? = null
    private var UPDATE_DTO: UpdateProductDto? = null

    @BeforeEach
    fun setup() {
        // Reset mocks to assure stability
        reset(productsRepository, productsMapper)

        // Initialise service and test data
        productsService = ProductsService(productsRepository, productsMapper)

        CREATE_DTO = CreateProductDto(
            productName = "Generic Electronic 1",
            productType = 1,
            productPrice = 100.00,
            productQnt = 100
        )

        UPDATE_DTO = UpdateProductDto(
            id = PRODUCT_ID,
            productName = "Generic Electronic 100",
            productType = 1,
            productPrice = 99.99,
            productQnt = 5
        )

        PRODUCT_ENTITY = ProductsEntity().apply {
            id = PRODUCT_ID
        }

        PRODUCT_DTO = ProductsDto(
            id = PRODUCT_ID,
            productName = "Generic Electronic 2",
            productType = 1,
            productPrice = 100.00,
            productQnt = 10
        )
    }

    @Test
    fun `createNewProduct should persist and return DTO`() {
        mockWhen(productsMapper.createDataToEntity(anyNotNull())).thenReturn(PRODUCT_ENTITY!!)
        mockWhen(productsRepository.persistAndFlush(PRODUCT_ENTITY!!)).thenReturn(Uni.createFrom().item(PRODUCT_ENTITY!!))
        mockWhen(productsMapper.entityToDto(PRODUCT_ENTITY!!)).thenReturn(PRODUCT_DTO!!)

        val result = productsService.createNewProduct(CREATE_DTO!!).await().indefinitely()

        assertEquals(PRODUCT_DTO, result)
        verify(productsRepository).persistAndFlush(PRODUCT_ENTITY!!)
        verify(productsMapper).entityToDto(PRODUCT_ENTITY!!)
    }

    @Test
    fun `findAllProducts should return list of DTOs`() {
        val entityList = listOf(PRODUCT_ENTITY!!)

        // Mock PanacheQuery
        val mockQuery = mock(PanacheQuery::class.java) as PanacheQuery<ProductsEntity>

        // When findAll() is called, return the mock query
        mockWhen(productsRepository.findAll()).thenReturn(mockQuery)

        // When list() is called on the mock query, return the Uni with our entities
        mockWhen(mockQuery.list<ProductsEntity>()).thenReturn(Uni.createFrom().item(entityList))

        // Map entities to DTOs
        mockWhen(productsMapper.entityToDto(PRODUCT_ENTITY!!)).thenReturn(PRODUCT_DTO!!)

        val result = productsService.findAllProducts().await().indefinitely()

        assertEquals(1, result.size)
        assertEquals(PRODUCT_DTO, result[0])
        verify(productsMapper).entityToDto(PRODUCT_ENTITY!!)
    }

    @Test
    fun `findOneProduct should return DTO when found`() {
        mockWhen(productsRepository.findById(PRODUCT_ID)).thenReturn(Uni.createFrom().item(PRODUCT_ENTITY!!))
        mockWhen(productsMapper.entityToDto(PRODUCT_ENTITY!!)).thenReturn(PRODUCT_DTO!!)

        val result = productsService.findOneProduct(PRODUCT_ID).await().indefinitely()

        assertEquals(PRODUCT_DTO, result)
    }

    @Test
    fun `findOneProduct should throw exception when not found`() {
        mockWhen(productsRepository.findById(PRODUCT_ID)).thenReturn(Uni.createFrom().nullItem())

        val exception = assertThrows(ElementNotFoundException::class.java) {
            productsService.findOneProduct(PRODUCT_ID).await().indefinitely()
        }

        assertEquals("Product with ID $PRODUCT_ID not found.", exception.message)
    }

    @Test
    fun `updateOneProduct should update and return DTO`() {
        mockWhen(productsRepository.findById(PRODUCT_ID)).thenReturn(Uni.createFrom().item(PRODUCT_ENTITY!!))
        doNothing().`when`(productsMapper).updateDataToEntity(PRODUCT_ENTITY!!, UPDATE_DTO!!)
        mockWhen(productsMapper.entityToDto(PRODUCT_ENTITY!!)).thenReturn(PRODUCT_DTO!!)

        val result = productsService.updateOneProduct(UPDATE_DTO!!).await().indefinitely()

        assertEquals(PRODUCT_DTO, result)
        verify(productsMapper).updateDataToEntity(PRODUCT_ENTITY!!, UPDATE_DTO!!)
    }

    @Test
    fun `updateOneProduct should throw exception when not found`() {
        mockWhen(productsRepository.findById(PRODUCT_ID)).thenReturn(Uni.createFrom().nullItem())

        val exception = assertThrows(ElementNotFoundException::class.java) {
            productsService.updateOneProduct(UPDATE_DTO!!).await().indefinitely()
        }

        assertTrue(exception.message!!.contains("Product with ID"))
    }

    @Test
    fun `deleteProduct should return true when deleted`() {
        mockWhen(productsRepository.deleteById(PRODUCT_ID)).thenReturn(Uni.createFrom().item(true))

        val result = productsService.deleteProduct(PRODUCT_ID).await().indefinitely()

        assertTrue(result)
    }

    @Test
    fun `deleteProduct should throw exception when not found`() {
        mockWhen(productsRepository.deleteById(PRODUCT_ID)).thenReturn(Uni.createFrom().item(false))

        val exception = assertThrows(ElementNotFoundException::class.java) {
            productsService.deleteProduct(PRODUCT_ID).await().indefinitely()
        }

        assertEquals("Product with ID $PRODUCT_ID not found.", exception.message)
    }
}