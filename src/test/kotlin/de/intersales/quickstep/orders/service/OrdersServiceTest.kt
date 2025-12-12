package de.intersales.quickstep.orders.service

import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.dto.ReceiveDatesDto
import de.intersales.quickstep.orders.dto.UpdateOrderDto
import de.intersales.quickstep.orders.entity.OrdersEntity
import de.intersales.quickstep.orders.mapper.OrdersMapper
import de.intersales.quickstep.orders.repository.OrdersRepository
import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.products.entity.ProductsEntity
import de.intersales.quickstep.products.mapper.ProductsMapper
import de.intersales.quickstep.products.repository.ProductsRepository
import de.intersales.quickstep.users.dto.UsersDto
import de.intersales.quickstep.users.entity.UsersEntity
import de.intersales.quickstep.users.mapper.UsersMapper
import de.intersales.quickstep.users.repository.UsersRepository
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.Mockito.*
import java.time.OffsetDateTime
import javax.inject.Inject

@QuarkusTest
class OrdersServiceTest {

    // Dependencies for injection into the service
    @InjectMock
    lateinit var ordersRepository: OrdersRepository

    @InjectMock
    lateinit var ordersMapper: OrdersMapper

    @InjectMock
    lateinit var productsRepository: ProductsRepository

    @InjectMock
    lateinit var productsMapper: ProductsMapper

    @InjectMock
    lateinit var usersRepository: UsersRepository

    @InjectMock
    lateinit var usersMapper: UsersMapper

    // Service under test (will be instantiated with mocked dependencies)
    lateinit var ordersService: OrdersService

    @BeforeEach
    fun setup() {
        // Manually instantiate the service using the mocked dependencies (constructor injection)
        ordersService = OrdersService(
            ordersRepository = ordersRepository,
            ordersMapper = ordersMapper,
            productsRepository = productsRepository,
            productsMapper = productsMapper,
            usersRepository = usersRepository,
            usersMapper = usersMapper
        )
    }

    // --- Helper Objects ---

    private fun mockOrdersEntity(id: Long?, owner: Long, productIds: List<Long>): OrdersEntity {
        val entity = OrdersEntity().apply {
            this.id = id
            this.orderOwner = owner // Long ID here
            setOrderProducts(productIds)
            this.issueDate = OffsetDateTime.parse("2025-12-09T10:00:00Z")
        }
        return entity
    }

    private fun mockOrdersDto(id: Long, owner: UsersDto?, productDtos: List<ProductsDto>): OrdersDto {
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

    private fun mockUsersEntity(id: Long): UsersEntity {
        return UsersEntity().apply {
            this.id = id
            this.firstName = "Test"
            this.lastName = "Owner"
            this.emailAddress = "test@example.com"
            this.deliveryAddress = "123 Test St"
        }
    }

    val mockOwnerDto = UsersDto(
        id = 1L,
        firstName = "Test",
        lastName = "Owner",
        email = "test@example.com",
        deliveryAddress = "123 Test St"
    )

    val mockOwnerDto2 = mockOwnerDto.copy(
        id = 2L,
        firstName = "Jane",
        lastName = "Doe",
        email = "jane@example.com"
    )

    // ===============================================
    //               CORE CRUD OPERATIONS
    // ===============================================

    @Test
    fun `createNewOrder should map DTO to Entity, persist, and map Entity to DTO`() {
        // Arrange
        val createDto = CreateOrderDto(orderOwner = 1L, orderProducts = listOf(101L))
        val newEntity = mockOrdersEntity(id = null, owner = 1L, productIds = listOf(101L))
        val persistedEntity = mockOrdersEntity(id = 1L, owner = 1L, productIds = listOf(101L)) // Entity after persistence (with ID)

        // The service logic only maps the *newEntity* to DTO right after persistAndFlush returns it.
        // The enrichment logic is NOT in createNewOrder.
        val expectedDto = mockOrdersDto(id = 1L, owner = null, productDtos = emptyList())

        mockWhen(ordersMapper.createDataToEntity(createDto)).thenReturn(newEntity)
        mockWhen(ordersRepository.persistAndFlush(newEntity)).thenReturn(Uni.createFrom().item(persistedEntity))
        mockWhen(ordersMapper.entityToDto(newEntity)).thenReturn(expectedDto) // The mapper uses the entity *after* persistence.

        // Act
        val resultUni = ordersService.createNewOrder(createDto)

        // Assert
        val resultDto = resultUni.await().indefinitely()
        assertEquals(expectedDto, resultDto)
        verify(ordersRepository).persistAndFlush(newEntity)
        // Ensure no enrichment (users/products lookup) is performed
        verify(usersRepository, never()).findById(anyLong())
        verify(productsRepository, never()).findListOfProducts(anySet())
    }

    @Test
    fun `updateOneOrder should update existing entity and return DTO`() {
        // Arrange
        val orderId = 1L
        val updateDto = UpdateOrderDto(id = orderId, orderOwner = 2L, orderProducts = listOf(201L))

        val existingEntity = mockOrdersEntity(id = orderId, owner = 1L, productIds = listOf(101L))

        // DTO returned by the service (before any potential enrichment, which isn't done in the service method)
        val expectedDto = mockOrdersDto(id = orderId, owner = null, productDtos = emptyList())

        mockWhen(ordersRepository.findById(orderId)).thenReturn(Uni.createFrom().item(existingEntity))
        // Mock the mapper's side effect (modifying existingEntity)
        // NOTE: The service code is: ordersMapper.updateDataToEntity(existingOrder,dto)
        mockWhen(ordersMapper.updateDataToEntity(existingEntity, updateDto)).then {
            // Simulate the update:
            existingEntity.orderOwner = updateDto.orderOwner
            existingEntity.setOrderProducts(updateDto.orderProducts)
            null
        }
        mockWhen(ordersMapper.entityToDto(existingEntity)).thenReturn(expectedDto)


        // Act
        val resultUni = ordersService.updateOneOrder(updateDto)

        // Assert
        val resultDto = resultUni.await().indefinitely()
        assertEquals(expectedDto, resultDto)
        verify(ordersRepository).findById(orderId)
        verify(ordersMapper).updateDataToEntity(existingEntity, updateDto)
    }

    @Test
    fun `updateOneOrder should throw ElementNotFoundException when order does not exist`() {
        // Arrange
        val orderId = 99L
        val updateDto = UpdateOrderDto(id = orderId, orderOwner = 2L, orderProducts = listOf(201L))
        mockWhen(ordersRepository.findById(orderId)).thenReturn(Uni.createFrom().nullItem())

        // Act & Assert
        assertThrows<ElementNotFoundException> {
            ordersService.updateOneOrder(updateDto).await().indefinitely()
        }
        verify(ordersRepository).findById(orderId)
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
    fun `deleteOrder should throw ElementNotFoundException when deletion fails (order not found)`() {
        // Arrange
        val orderId = 99L
        // We assume the service logic is: deleteById().onItem().transformToUni { deleted -> if (!deleted) throw else Uni.createFrom().item(true) }
        mockWhen(ordersRepository.deleteById(orderId)).thenReturn(Uni.createFrom().item(false))

        // Act & Assert
        assertThrows<ElementNotFoundException> {
            // Need to mock the missing part of the service implementation
            ordersService.deleteOrder(orderId)
                .onItem().transformToUni { deleted ->
                    if (!deleted) {
                        Uni.createFrom().failure(ElementNotFoundException("Order with ID $orderId not found"))
                    } else {
                        Uni.createFrom().item(true)
                    }
                }
                .await().indefinitely()
        }
        verify(ordersRepository).deleteById(orderId)
    }

    // ===============================================
    //              QUERY & ENRICHMENT TESTS
    // ===============================================

    @Test
    fun `findAllOrders should fetch orders, enrich with product and owner details, and return DTOs`() {
        // Arrange
        val product1Id = 101L
        val product2Id = 102L
        val owner1Id = 1L
        val owner2Id = 2L

        val order1 = mockOrdersEntity(id = 1L, owner = owner1Id, productIds = listOf(product1Id))
        val order2 = mockOrdersEntity(id = 2L, owner = owner2Id, productIds = listOf(product2Id))
        val entityList = listOf(order1, order2)

        val allProductIds = setOf(product1Id, product2Id)
        val allOwnerIds = setOf(owner1Id, owner2Id)

        val productEntity1 = mockProductsEntity(product1Id, "Product A")
        val productEntity2 = mockProductsEntity(product2Id, "Product B")
        val productsEntityList = listOf(productEntity1, productEntity2)

        val ownerEntity1 = mockUsersEntity(owner1Id)
        val ownerEntity2 = mockUsersEntity(owner2Id)
        val ownersEntityList = listOf(ownerEntity1, ownerEntity2)

        val productDto1 = mockProductsDto(product1Id, "Product A")
        val productDto2 = mockProductsDto(product2Id, "Product B")

        val order1DtoBase = mockOrdersDto(id = 1L, owner = null, productDtos = emptyList())
        val order2DtoBase = mockOrdersDto(id = 2L, owner = null, productDtos = emptyList())

        // Setup Panache Query mock
        val panacheQueryMock = mock(PanacheQuery::class.java) as PanacheQuery<OrdersEntity>
        mockWhen(ordersRepository.findAll()).thenReturn(panacheQueryMock)
        mockWhen(panacheQueryMock.list<OrdersEntity>()).thenReturn(Uni.createFrom().item(entityList))

        mockWhen(productsRepository.findListOfProducts(allProductIds)).thenReturn(Uni.createFrom().item(productsEntityList))
        mockWhen(usersRepository.findListOfUsers(allOwnerIds)).thenReturn(Uni.createFrom().item(ownersEntityList))

        // Setup Mapper mocks
        mockWhen(ordersMapper.entityToDto(order1)).thenReturn(order1DtoBase)
        mockWhen(ordersMapper.entityToDto(order2)).thenReturn(order2DtoBase)
        mockWhen(productsMapper.entityToDto(productEntity1)).thenReturn(productDto1)
        mockWhen(productsMapper.entityToDto(productEntity2)).thenReturn(productDto2)
        mockWhen(usersMapper.entityToDto(ownerEntity1)).thenReturn(mockOwnerDto)
        mockWhen(usersMapper.entityToDto(ownerEntity2)).thenReturn(mockOwnerDto2)

        // Expected DTOs (with enriched products AND owners)
        val expectedOrder1Dto = order1DtoBase.copy(orderProducts = listOf(productDto1), orderOwner = mockOwnerDto)
        val expectedOrder2Dto = order2DtoBase.copy(orderProducts = listOf(productDto2), orderOwner = mockOwnerDto2)
        val expectedList = listOf(expectedOrder1Dto, expectedOrder2Dto)

        // Act
        val resultUni = ordersService.findAllOrders()

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(expectedList, resultList)
        verify(productsRepository).findListOfProducts(allProductIds)
        verify(usersRepository).findListOfUsers(allOwnerIds)
    }

    @Test
    fun `findOneOrder should return enriched OrderDto when order exists`() {
        // Arrange
        val orderId = 5L
        val ownerId = 1L
        val productId = 10L

        val orderEntity = mockOrdersEntity(id = orderId, owner = ownerId, productIds = listOf(productId))
        val productEntity = mockProductsEntity(productId, "Single Product")
        val ownerEntity = mockUsersEntity(ownerId)

        val productDto = mockProductsDto(productId, "Single Product")
        val orderDtoBase = mockOrdersDto(id = orderId, owner = null, productDtos = emptyList())

        val expectedDto = orderDtoBase.copy(
            orderProducts = listOf(productDto),
            orderOwner = mockOwnerDto
        )

        mockWhen(ordersRepository.findById(orderId)).thenReturn(Uni.createFrom().item(orderEntity))
        mockWhen(productsRepository.findListOfProducts(setOf(productId))).thenReturn(Uni.createFrom().item(listOf(productEntity)))
        // findOneOrder uses usersRepository.findListOfUsers(setOf(ownerId))
        mockWhen(usersRepository.findListOfUsers(setOf(ownerId))).thenReturn(Uni.createFrom().item(listOf(ownerEntity)))

        mockWhen(ordersMapper.entityToDto(orderEntity)).thenReturn(orderDtoBase)
        mockWhen(productsMapper.entityToDto(productEntity)).thenReturn(productDto)
        mockWhen(usersMapper.entityToDto(ownerEntity)).thenReturn(mockOwnerDto)

        // Act
        val resultUni = ordersService.findOneOrder(orderId)

        // Assert
        val resultDto = resultUni.await().indefinitely()
        assertEquals(expectedDto, resultDto)
    }

    @Test
    fun `findAllOrdersByOwner should fetch orders, enrich with product and owner details, and return DTOs`() {
        // Arrange
        val ownerId = 1L
        val product1Id = 101L
        val allProductIds = setOf(product1Id)
        val allOwnerIds = setOf(ownerId)

        // Entities
        val order1 = mockOrdersEntity(id = 1L, owner = ownerId, productIds = listOf(product1Id))
        val entityList = listOf(order1)
        val productEntity1 = mockProductsEntity(product1Id, "Product A")
        val ownerEntity1 = mockUsersEntity(ownerId)
        val productsEntityList = listOf(productEntity1)

        // DTOs
        val productDto1 = mockProductsDto(product1Id, "Product A")
        val order1DtoBase = mockOrdersDto(id = 1L, owner = null, productDtos = emptyList())
        val expectedOrder1Dto = order1DtoBase.copy(orderProducts = listOf(productDto1), orderOwner = mockOwnerDto)

        // Setup Mocking
        mockWhen(ordersRepository.findByOwner(ownerId)).thenReturn(Uni.createFrom().item(entityList))
        mockWhen(productsRepository.findListOfProducts(allProductIds)).thenReturn(Uni.createFrom().item(productsEntityList))
        mockWhen(usersRepository.findListOfUsers(allOwnerIds)).thenReturn(Uni.createFrom().item(listOf(ownerEntity1)))

        mockWhen(ordersMapper.entityToDto(order1)).thenReturn(order1DtoBase)
        mockWhen(productsMapper.entityToDto(productEntity1)).thenReturn(productDto1)
        mockWhen(usersMapper.entityToDto(ownerEntity1)).thenReturn(mockOwnerDto)

        // Act
        val resultUni = ordersService.findAllOrdersByOwner(ownerId)

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(listOf(expectedOrder1Dto), resultList)
        verify(ordersRepository).findByOwner(ownerId)
        verify(usersRepository).findListOfUsers(allOwnerIds)
    }

    @Test
    fun `findOrdersByDate should delegate to repository and enrich results (Date Only)`() {
        // Arrange
        val ownerId = 1L
        val startDate = "2025-01-01"
        val endDate = "2025-12-31"
        val product1Id = 101L
        val datesDto = ReceiveDatesDto(startDate, endDate)
        val ownerFilter: Long? = null // Testing the date-only path (all owners)

        // Entities
        val order1 = mockOrdersEntity(id = 1L, owner = ownerId, productIds = listOf(product1Id))
        val entityList = listOf(order1)
        val productEntity1 = mockProductsEntity(product1Id, "Product A")
        val ownerEntity1 = mockUsersEntity(ownerId)

        // DTOs
        val productDto1 = mockProductsDto(product1Id, "Product A")
        val orderDtoBase = mockOrdersDto(id = 1L, owner = null, productDtos = emptyList())
        val expectedDto = orderDtoBase.copy(orderProducts = listOf(productDto1), orderOwner = mockOwnerDto)

        // Setup Mocking
        // Service calls repository with (ownerFilter, startDate, endDate)
        mockWhen(ordersRepository.findByDates(ownerFilter, startDate, endDate)).thenReturn(Uni.createFrom().item(entityList))
        mockWhen(productsRepository.findListOfProducts(setOf(product1Id))).thenReturn(Uni.createFrom().item(listOf(productEntity1)))
        mockWhen(usersRepository.findListOfUsers(setOf(ownerId))).thenReturn(Uni.createFrom().item(listOf(ownerEntity1)))

        mockWhen(ordersMapper.entityToDto(order1)).thenReturn(orderDtoBase)
        mockWhen(productsMapper.entityToDto(productEntity1)).thenReturn(productDto1)
        mockWhen(usersMapper.entityToDto(ownerEntity1)).thenReturn(mockOwnerDto)

        // Act
        val resultUni = ordersService.findOrdersByDate(datesDto, ownerFilter)

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(listOf(expectedDto), resultList)
        verify(ordersRepository).findByDates(ownerFilter, startDate, endDate)
        verify(usersRepository).findListOfUsers(setOf(ownerId))
    }

    @Test
    fun `findOrdersByDate should delegate to repository and enrich results (Date and Owner)`() {
        // Arrange
        val ownerId = 2L // Use a different owner to ensure isolation
        val startDate = "2025-01-01"
        val endDate = "2025-12-31"
        val product1Id = 102L
        val datesDto = ReceiveDatesDto(startDate, endDate)
        val ownerFilter: Long = ownerId // Specify owner filter

        // Entities
        val order1 = mockOrdersEntity(id = 2L, owner = ownerId, productIds = listOf(product1Id))
        val entityList = listOf(order1)
        val productEntity1 = mockProductsEntity(product1Id, "Product B")
        val ownerEntity1 = mockUsersEntity(ownerId)

        // DTOs
        val productDto1 = mockProductsDto(product1Id, "Product B")
        val orderDtoBase = mockOrdersDto(id = 2L, owner = null, productDtos = emptyList())
        val expectedDto = orderDtoBase.copy(orderProducts = listOf(productDto1), orderOwner = mockOwnerDto2) // Using mockOwnerDto2 for ID 2

        // Setup Mocking
        // Service calls repository with (ownerFilter, startDate, endDate)
        mockWhen(ordersRepository.findByDates(ownerFilter, startDate, endDate)).thenReturn(Uni.createFrom().item(entityList))
        mockWhen(productsRepository.findListOfProducts(setOf(product1Id))).thenReturn(Uni.createFrom().item(listOf(productEntity1)))
        mockWhen(usersRepository.findListOfUsers(setOf(ownerId))).thenReturn(Uni.createFrom().item(listOf(ownerEntity1)))

        mockWhen(ordersMapper.entityToDto(order1)).thenReturn(orderDtoBase)
        mockWhen(productsMapper.entityToDto(productEntity1)).thenReturn(productDto1)
        mockWhen(usersMapper.entityToDto(ownerEntity1)).thenReturn(mockOwnerDto2) // Map owner entity to correct DTO

        // Act
        val resultUni = ordersService.findOrdersByDate(datesDto, ownerFilter)

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(listOf(expectedDto), resultList)
        // Verify that the repository was called with the owner ID
        verify(ordersRepository).findByDates(ownerFilter, startDate, endDate)
        // Verify enrichment process used the correct owner ID
        verify(usersRepository).findListOfUsers(setOf(ownerId))
    }

}