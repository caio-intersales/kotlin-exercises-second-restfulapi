package de.intersales.quickstep.orders.service

import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.entity.OrdersEntity
import de.intersales.quickstep.orders.mapper.OrdersMapper
import de.intersales.quickstep.orders.repository.OrdersRepository
import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.products.entity.ProductsEntity
import de.intersales.quickstep.products.mapper.ProductsMapper
import de.intersales.quickstep.products.repository.ProductsRepository
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.Mockito.*
import java.time.OffsetDateTime
import javax.inject.Inject

@QuarkusTest
class OrdersServiceTest {

    @Inject
    lateinit var service: OrdersService

    // Dependencies to be mocked
    @InjectMock
    lateinit var ordersRepository: OrdersRepository
    @InjectMock
    lateinit var ordersMapper: OrdersMapper
    @InjectMock
    lateinit var productsRepository: ProductsRepository
    @InjectMock
    lateinit var productsMapper: ProductsMapper

    // --- MOCK DATA SETUP ---

    private val TEST_OWNER_ID: Long = 101L
    private val TEST_DATE: OffsetDateTime = OffsetDateTime.now().minusDays(1)
    private val PRODUCT_ID_A: Long = 201L
    private val PRODUCT_ID_B: Long = 202L

    // 1. Entity Setup
    private val mockOrderEntity = OrdersEntity().apply {
        id = 1L
        orderOwner = TEST_OWNER_ID
        issueDate = TEST_DATE
        // Simulating the underlying field being used to create the getter
        orderProductsJson = "[201, 202]"
    }
    // We access the getter here to mock the value used by the service logic
    private val orderProductIds = mockOrderEntity.orderProducts // [201, 202]

    private val mockProductEntityA = ProductsEntity().apply { id = PRODUCT_ID_A; productType = 1; productPrice = 10.00; productQnt = 10; productName = "Product A" }
    private val mockProductEntityB = ProductsEntity().apply { id = PRODUCT_ID_B; productType = 1; productPrice = 10.00; productQnt = 10; productName = "Product B" }
    private val mockProductList = listOf(mockProductEntityA, mockProductEntityB)

    // 2. DTO Setup
    private val mockProductDtoA = ProductsDto(id = PRODUCT_ID_A, productType = 1, productPrice = 10.00, productQnt = 10, productName = "Product A DTO")
    private val mockProductDtoB = ProductsDto(id = PRODUCT_ID_B, productType = 1, productPrice = 10.00, productQnt = 10, productName = "Product B DTO")
    private val mockProductDtoList = listOf(mockProductDtoA, mockProductDtoB)

    // The final DTO before product enrichment
    private val mockBaseOrderDto = OrdersDto(
        id = 1L,
        orderOwner = TEST_OWNER_ID,
        orderProducts = emptyList(),
        issueDate = TEST_DATE
    )

    // --- MOCKING HELPER ---

    /**
     * Mocks the complex data enrichment sequence required by findAll, findByOwner, etc.
     */
    private fun mockEnrichmentChain() {
        // Mock the product mapper calls
        mockWhen(productsMapper.entityToDto(mockProductEntityA)).thenReturn(mockProductDtoA)
        mockWhen(productsMapper.entityToDto(mockProductEntityB)).thenReturn(mockProductDtoB)

        // Mock the orders mapper call to return the base DTO
        mockWhen(ordersMapper.entityToDto(any(OrdersEntity::class.java))).thenReturn(mockBaseOrderDto)

        // Mock the products repository call, triggered by the collected IDs
        // Note: The service collects the IDs [201, 202] and calls this method
        mockWhen(productsRepository.findListOfProducts(orderProductIds.toSet()))
            .thenReturn(Uni.createFrom().item(mockProductList))
    }

    // ----------------------------------------------------------------------------------
    // 1. Tests for createNewOrder
    // ----------------------------------------------------------------------------------

    @Test
    fun `createNewOrder should map, persist, and return mapped DTO`() {
        // Arrange (Using a dedicated DTO for input)
        val inputDto = CreateOrderDto(TEST_OWNER_ID, listOf(1, 2))
        val returnedDto = OrdersDto(1L, TEST_OWNER_ID, emptyList(), TEST_DATE)

        mockWhen(ordersMapper.createDataToEntity(inputDto)).thenReturn(mockOrderEntity)
        mockWhen(ordersRepository.persistAndFlush(mockOrderEntity)).thenReturn(Uni.createFrom().item(mockOrderEntity))
        mockWhen(ordersMapper.entityToDto(mockOrderEntity)).thenReturn(returnedDto)

        // Act
        val result = service.createNewOrder(inputDto).await().indefinitely()

        // Assert
        assertNotNull(result)
        assertEquals(returnedDto.id, result.id)
        verify(ordersRepository, times(1)).persistAndFlush(mockOrderEntity)
    }

    // ----------------------------------------------------------------------------------
    // 2. Tests for findAllOrders (Full Enrichment Logic)
    // ----------------------------------------------------------------------------------

    @Test
    fun `findAllOrders should enrich orders with product DTOs via batch query`() {
        // Arrange
        val entityList = listOf(mockOrderEntity)
        val mockQuery = mock(PanacheQuery::class.java) as PanacheQuery<OrdersEntity>

        // 1. Setup the initial query result
        mockWhen(mockQuery.list<OrdersEntity>()).thenReturn(Uni.createFrom().item(entityList))
        mockWhen(ordersRepository.findAll()).thenReturn(mockQuery)

        // 2. Mock the complex enrichment chain
        mockEnrichmentChain()

        // Act
        val resultList: List<OrdersDto> = service.findAllOrders().await().indefinitely()
        val firstResult = resultList.first()

        // Assert
        assertEquals(1, resultList.size)
        // Verify the product data was attached
        assertEquals(2, firstResult.orderProducts.size, "The order should contain two enriched products.")
        assertEquals(PRODUCT_ID_A, firstResult.orderProducts.first().id)

        // Verify enrichment steps were executed
        verify(productsRepository, times(1)).findListOfProducts(orderProductIds.toSet())
        // Verify mapping was called 1 time for the order, and 2 times for the products
        verify(ordersMapper, times(1)).entityToDto(mockOrderEntity)
        verify(productsMapper, times(2)).entityToDto(any(ProductsEntity::class.java))
    }

    // ----------------------------------------------------------------------------------
    // 3. Tests for findAllOrdersByOwner (Full Enrichment Logic)
    // ----------------------------------------------------------------------------------

    @Test
    fun `findAllOrdersByOwner should filter by owner and enrich with products`() {
        // Arrange
        val entityList = listOf(mockOrderEntity)

        // 1. Setup the initial repository call
        mockWhen(ordersRepository.findByOwner(TEST_OWNER_ID)).thenReturn(Uni.createFrom().item(entityList))

        // 2. Mock the complex enrichment chain
        mockEnrichmentChain()

        // Act
        val resultList: List<OrdersDto> = service.findAllOrdersByOwner(TEST_OWNER_ID).await().indefinitely()
        val firstResult = resultList.first()

        // Assert
        assertEquals(1, resultList.size)
        assertEquals(TEST_OWNER_ID, firstResult.orderOwner)
        assertEquals(2, firstResult.orderProducts.size)

        // Verify that the repository was called with the correct ID and enrichment happened
        verify(ordersRepository, times(1)).findByOwner(TEST_OWNER_ID)
        verify(productsRepository, times(1)).findListOfProducts(orderProductIds.toSet())
    }

    // ----------------------------------------------------------------------------------
    // 4. Tests for findOrdersByDateAndOwner (Testing Query Parameters and Enrichment)
    // ----------------------------------------------------------------------------------

    @Test
    fun `findOrdersByDateAndOwner should pass query params and enrich the result`() {
        // Arrange
        val startDate = TEST_DATE.minusDays(5)
        val endDate = TEST_DATE.plusDays(5)
        val entityList = listOf(mockOrderEntity)

        // 1. Setup the repository call with all query parameters
        mockWhen(ordersRepository.findByDatesAndOwner(TEST_OWNER_ID, startDate, endDate))
            .thenReturn(Uni.createFrom().item(entityList))

        // 2. Mock the complex enrichment chain
        mockEnrichmentChain()

        // Act
        val result: List<OrdersDto> = service.findOrdersByDateAndOwner(TEST_OWNER_ID, startDate, endDate).await().indefinitely()

        // Assert
        assertEquals(1, result.size)
        assertEquals(TEST_OWNER_ID, result.first().orderOwner)

        // Verify the repository was called correctly with all arguments
        verify(ordersRepository, times(1)).findByDatesAndOwner(TEST_OWNER_ID, startDate, endDate)
        // Verify enrichment happened
        verify(productsRepository, times(1)).findListOfProducts(orderProductIds.toSet())
    }

    // The test for findOrdersByDate is structurally identical to findOrdersByDateAndOwner,
    // just using the two-parameter repository method.
}