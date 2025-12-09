package de.intersales.quickstep.orders.service

import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.dto.UpdateOrderDto
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.Mockito.*
import java.time.OffsetDateTime
import javax.inject.Inject

@QuarkusTest
class OrdersServiceTest {

    @Inject
    lateinit var ordersService: OrdersService

    // Mocked Dependencies
    @InjectMock
    lateinit var ordersRepository: OrdersRepository

    @InjectMock
    lateinit var ordersMapper: OrdersMapper

    @InjectMock
    lateinit var productsRepository: ProductsRepository

    @InjectMock
    lateinit var productsMapper: ProductsMapper

    // --- Helper Objects ---

    private fun mockOrdersEntity(id: Long?, owner: Long, productIds: List<Long>): OrdersEntity {
        val entity = OrdersEntity().apply {
            this.id = id
            this.orderOwner = owner
            setOrderProducts(productIds)
            this.issueDate = OffsetDateTime.parse("2025-12-09T10:00:00Z")
        }
        return entity
    }

    private fun mockOrdersDto(id: Long, owner: Long, productDtos: List<ProductsDto>): OrdersDto {
        return OrdersDto(
            id = id,
            orderOwner = owner,
            orderProducts = productDtos,
            issueDate = OffsetDateTime.parse("2025-12-09T10:00:00Z")
        )
    }

    private fun mockProductsEntity(id: Long, name: String): ProductsEntity {
        return ProductsEntity().apply {
            this.id = id
            this.productName = name
        }
    }

    private fun mockProductsDto(id: Long, name: String): ProductsDto {
        return ProductsDto(id = id, productName = name, productType = 1, productPrice = 10.00, productQnt = 10)
    }

    // ===============================================
    //               CORE CRUD OPERATIONS
    // ===============================================

    @Test
    fun `createNewOrder should map DTO to Entity, persist, and map Entity to DTO`() {
        // Arrange
        val createDto = CreateOrderDto(orderOwner = 1L, orderProducts = listOf(101L))
        val newEntity = mockOrdersEntity(id = null, owner = 1L, productIds = listOf(101L))
        val persistedEntity = mockOrdersEntity(id = 1L, owner = 1L, productIds = listOf(101L))
        val expectedDto = mockOrdersDto(id = 1L, owner = 1L, productDtos = emptyList())

        mockWhen(ordersMapper.createDataToEntity(createDto)).thenReturn(newEntity)
        mockWhen(ordersRepository.persistAndFlush(newEntity)).thenReturn(Uni.createFrom().item(persistedEntity))
        mockWhen(ordersMapper.entityToDto(newEntity)).thenReturn(expectedDto)

        // Act
        val resultUni = ordersService.createNewOrder(createDto)

        // Assert
        val resultDto = resultUni.await().indefinitely()
        assertEquals(expectedDto, resultDto)
        verify(ordersRepository).persistAndFlush(newEntity)
    }

    @Test
    fun `updateOneOrder should update existing entity and return DTO`() {
        // Arrange
        val orderId = 1L
        val updateDto = UpdateOrderDto(id = orderId, orderOwner = 2L, orderProducts = listOf(201L))
        val existingEntity = mockOrdersEntity(id = orderId, owner = 1L, productIds = listOf(101L))
        val updatedDto = mockOrdersDto(id = orderId, owner = 2L, productDtos = emptyList())

        mockWhen(ordersRepository.findById(orderId)).thenReturn(Uni.createFrom().item(existingEntity))
        // Mock the mapper's side effect (modifying existingEntity)
        mockWhen(ordersMapper.updateDataToEntity(existingEntity, updateDto)).then {
            existingEntity.orderOwner = updateDto.orderOwner
            existingEntity.setOrderProducts(updateDto.orderProducts)
            null
        }
        mockWhen(ordersMapper.entityToDto(existingEntity)).thenReturn(updatedDto)

        // Act
        val resultUni = ordersService.updateOneOrder(updateDto)

        // Assert
        val resultDto = resultUni.await().indefinitely()
        assertEquals(updatedDto, resultDto)
        verify(ordersRepository).findById(orderId)
        verify(ordersMapper).updateDataToEntity(existingEntity, updateDto)
    }

    @Test
    fun `updateOneOrder should throw ElementNotFoundException if order does not exist`() {
        // Arrange
        val orderId = 99L
        val updateDto = UpdateOrderDto(id = orderId, orderOwner = 2L, orderProducts = listOf(201L))
        mockWhen(ordersRepository.findById(orderId)).thenReturn(Uni.createFrom().nullItem())

        // Act & Assert
        assertThrows<ElementNotFoundException> {
            ordersService.updateOneOrder(updateDto).await().indefinitely()
        }
        verify(ordersRepository).findById(orderId)
        verifyNoInteractions(ordersMapper)
    }

    @Test
    fun `deleteOrder should return true when deletion is successful`() {
        // Arrange
        val orderId = 1L
        mockWhen(ordersRepository.deleteById(orderId)).thenReturn(Uni.createFrom().item(true))

        // Act
        val resultUni = ordersService.deleteOrder(orderId)

        // Assert
        val result = resultUni.await().indefinitely()
        assertTrue(result)
        verify(ordersRepository).deleteById(orderId)
    }

    @Test
    fun `deleteOrder should throw ElementNotFoundException when order does not exist`() {
        // Arrange
        val orderId = 99L
        mockWhen(ordersRepository.deleteById(orderId)).thenReturn(Uni.createFrom().item(false))

        // Act & Assert
        assertThrows<ElementNotFoundException> {
            ordersService.deleteOrder(orderId).await().indefinitely()
        }
        verify(ordersRepository).deleteById(orderId)
    }

    // ===============================================
    //              QUERY & ENRICHMENT TESTS
    // ===============================================

    @Test
    fun `findAllOrders should fetch orders, enrich with product details, and return DTOs`() {
        // Arrange
        val product1Id = 101L
        val product2Id = 102L
        val order1 = mockOrdersEntity(id = 1L, owner = 1L, productIds = listOf(product1Id))
        val order2 = mockOrdersEntity(id = 2L, owner = 2L, productIds = listOf(product2Id))
        val entityList = listOf(order1, order2)
        val allProductIds = setOf(product1Id, product2Id)

        val productEntity1 = mockProductsEntity(product1Id, "Product A")
        val productEntity2 = mockProductsEntity(product2Id, "Product B")
        val productsEntityList = listOf(productEntity1, productEntity2)

        val productDto1 = mockProductsDto(product1Id, "Product A")
        val productDto2 = mockProductsDto(product2Id, "Product B")
        val order1DtoBase = mockOrdersDto(id = 1L, owner = 1L, productDtos = emptyList())
        val order2DtoBase = mockOrdersDto(id = 2L, owner = 2L, productDtos = emptyList())

        // Setup Panache Query mock
        val panacheQueryMock = mock(PanacheQuery::class.java) as PanacheQuery<OrdersEntity>
        mockWhen(ordersRepository.findAll()).thenReturn(panacheQueryMock)
        mockWhen(panacheQueryMock.list<OrdersEntity>()).thenReturn(Uni.createFrom().item(entityList))
        mockWhen(productsRepository.findListOfProducts(allProductIds)).thenReturn(Uni.createFrom().item(productsEntityList))

        // Setup Mapper mocks
        mockWhen(ordersMapper.entityToDto(order1)).thenReturn(order1DtoBase)
        mockWhen(ordersMapper.entityToDto(order2)).thenReturn(order2DtoBase)
        mockWhen(productsMapper.entityToDto(productEntity1)).thenReturn(productDto1)
        mockWhen(productsMapper.entityToDto(productEntity2)).thenReturn(productDto2)

        // Expected DTOs (with enriched products)
        val expectedOrder1Dto = order1DtoBase.copy(orderProducts = listOf(productDto1))
        val expectedOrder2Dto = order2DtoBase.copy(orderProducts = listOf(productDto2))
        val expectedList = listOf(expectedOrder1Dto, expectedOrder2Dto)

        // Act
        val resultUni = ordersService.findAllOrders()

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(expectedList, resultList)
        verify(productsRepository).findListOfProducts(allProductIds)
    }

    @Test
    fun `findOneOrder should return enriched OrderDto when order exists`() {
        // Arrange
        val orderId = 5L
        val productId = 10L
        val orderEntity = mockOrdersEntity(id = orderId, owner = 1L, productIds = listOf(productId))
        val productEntity = mockProductsEntity(productId, "Single Product")
        val productDto = mockProductsDto(productId, "Single Product")
        val orderDtoBase = mockOrdersDto(id = orderId, owner = 1L, productDtos = emptyList())
        val expectedDto = orderDtoBase.copy(orderProducts = listOf(productDto))

        mockWhen(ordersRepository.findById(orderId)).thenReturn(Uni.createFrom().item(orderEntity))
        mockWhen(productsRepository.findListOfProducts(setOf(productId))).thenReturn(Uni.createFrom().item(listOf(productEntity)))
        mockWhen(ordersMapper.entityToDto(orderEntity)).thenReturn(orderDtoBase)
        mockWhen(productsMapper.entityToDto(productEntity)).thenReturn(productDto)

        // Act
        val resultUni = ordersService.findOneOrder(orderId)

        // Assert
        val resultDto = resultUni.await().indefinitely()
        assertEquals(expectedDto, resultDto)
    }

    @Test
    fun `findOneOrder should return null when order does not exist`() {
        // Arrange
        val orderId = 99L
        mockWhen(ordersRepository.findById(orderId)).thenReturn(Uni.createFrom().nullItem())

        // Act
        val resultUni = ordersService.findOneOrder(orderId)

        // Assert
        val resultDto = resultUni.await().indefinitely()
        assertEquals(null, resultDto)
        verifyNoInteractions(productsRepository, ordersMapper, productsMapper)
    }

    @Test
    fun `findAllOrdersByOwner should fetch orders, enrich with product details, and return DTOs`() {
        // Arrange
        val ownerId = 1L
        val product1Id = 101L
        val order1 = mockOrdersEntity(id = 1L, owner = ownerId, productIds = listOf(product1Id))
        val entityList = listOf(order1)
        val allProductIds = setOf(product1Id)

        val productEntity1 = mockProductsEntity(product1Id, "Product A")
        val productsEntityList = listOf(productEntity1)
        val productDto1 = mockProductsDto(product1Id, "Product A")
        val order1DtoBase = mockOrdersDto(id = 1L, owner = ownerId, productDtos = emptyList())
        val expectedOrder1Dto = order1DtoBase.copy(orderProducts = listOf(productDto1))

        mockWhen(ordersRepository.findByOwner(ownerId)).thenReturn(Uni.createFrom().item(entityList))
        mockWhen(productsRepository.findListOfProducts(allProductIds)).thenReturn(Uni.createFrom().item(productsEntityList))
        mockWhen(ordersMapper.entityToDto(order1)).thenReturn(order1DtoBase)
        mockWhen(productsMapper.entityToDto(productEntity1)).thenReturn(productDto1)

        // Act
        val resultUni = ordersService.findAllOrdersByOwner(ownerId)

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(listOf(expectedOrder1Dto), resultList)
    }

    @Test
    fun `findOrdersByDate should delegate to repository and enrich results`() {
        // Arrange
        val startDate = OffsetDateTime.parse("2025-01-01T00:00:00Z")
        val endDate = OffsetDateTime.parse("2025-12-31T23:59:59Z")
        val product1Id = 101L
        val order1 = mockOrdersEntity(id = 1L, owner = 1L, productIds = listOf(product1Id))
        val entityList = listOf(order1)
        val productEntity1 = mockProductsEntity(product1Id, "Product A")
        val productDto1 = mockProductsDto(product1Id, "Product A")
        val orderDtoBase = mockOrdersDto(id = 1L, owner = 1L, productDtos = emptyList())
        val expectedDto = orderDtoBase.copy(orderProducts = listOf(productDto1))

        mockWhen(ordersRepository.findByDates(startDate, endDate)).thenReturn(Uni.createFrom().item(entityList))
        mockWhen(productsRepository.findListOfProducts(setOf(product1Id))).thenReturn(Uni.createFrom().item(listOf(productEntity1)))
        mockWhen(ordersMapper.entityToDto(order1)).thenReturn(orderDtoBase)
        mockWhen(productsMapper.entityToDto(productEntity1)).thenReturn(productDto1)

        // Act
        val resultUni = ordersService.findOrdersByDate(startDate, endDate)

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(listOf(expectedDto), resultList)
        verify(ordersRepository).findByDates(startDate, endDate)
    }

    @Test
    fun `findOrdersByDateAndOwner should delegate to repository and enrich results`() {
        // Arrange
        val ownerId = 1L
        val startDate = OffsetDateTime.parse("2025-01-01T00:00:00Z")
        val endDate = OffsetDateTime.parse("2025-12-31T23:59:59Z")
        val product1Id = 101L
        val order1 = mockOrdersEntity(id = 1L, owner = ownerId, productIds = listOf(product1Id))
        val entityList = listOf(order1)
        val productEntity1 = mockProductsEntity(product1Id, "Product A")
        val productDto1 = mockProductsDto(product1Id, "Product A")
        val orderDtoBase = mockOrdersDto(id = 1L, owner = ownerId, productDtos = emptyList())
        val expectedDto = orderDtoBase.copy(orderProducts = listOf(productDto1))

        mockWhen(ordersRepository.findByDatesAndOwner(ownerId, startDate, endDate)).thenReturn(Uni.createFrom().item(entityList))
        mockWhen(productsRepository.findListOfProducts(setOf(product1Id))).thenReturn(Uni.createFrom().item(listOf(productEntity1)))
        mockWhen(ordersMapper.entityToDto(order1)).thenReturn(orderDtoBase)
        mockWhen(productsMapper.entityToDto(productEntity1)).thenReturn(productDto1)

        // Act
        val resultUni = ordersService.findOrdersByDateAndOwner(ownerId, startDate, endDate)

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(listOf(expectedDto), resultList)
        verify(ordersRepository).findByDatesAndOwner(ownerId, startDate, endDate)
    }
}