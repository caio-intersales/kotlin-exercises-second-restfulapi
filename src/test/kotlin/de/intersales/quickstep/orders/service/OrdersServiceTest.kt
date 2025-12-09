package de.intersales.quickstep.orders.service

import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.entity.OrdersEntity
import de.intersales.quickstep.orders.mapper.OrdersMapper
import de.intersales.quickstep.orders.repository.OrdersRepository
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.Mockito.*
import io.quarkus.hibernate.reactive.panache.PanacheQuery
import java.time.OffsetDateTime
import javax.inject.Inject

@QuarkusTest
class OrdersServiceTest {

    @Inject
    lateinit var service: OrdersService

    // We mock the dependencies to isolate the service logic
    @InjectMock
    lateinit var ordersRepository: OrdersRepository

    @InjectMock
    lateinit var ordersMapper: OrdersMapper

    // --- Helper Objects for Mocking ---

    // Define a fixed time for predictability
    private val TEST_DATE: OffsetDateTime = OffsetDateTime.now().minusDays(1)
    private val TEST_OWNER_ID: Long = 101L

    // A simple DTO instance used for input
    private val mockCreateDto = CreateOrderDto(
        orderOwner = TEST_OWNER_ID,
        orderProducts = listOf(1, 2, 3)
    )

    // The Entity instance the mapper should create
    private val mockEntity = OrdersEntity().apply {
        id = 1L
        orderOwner = TEST_OWNER_ID
        issueDate = TEST_DATE
        // No need to set orderProductsJson here since the mapper is mocked
    }

    // The final DTO instance the service should return
    private val mockOrdersDto = OrdersDto(
        id = 1L,
        orderOwner = TEST_OWNER_ID,
        orderProducts = "[1, 2, 3]",
        issueDate = TEST_DATE
    )

    // --- Tests for createNewOrder ---

    @Test
    fun `createNewOrder should map DTO to Entity, persist it, and map back to DTO`() {
        // Arrange
        // 1. Mock the mapper to convert DTO to Entity
        mockWhen(ordersMapper.createDataToEntity(mockCreateDto)).thenReturn(mockEntity)

        // 2. Mock the repository's persistAndFlush to return the persisted Entity
        //    The repository should return a Uni completed with the same entity
        mockWhen(ordersRepository.persistAndFlush(mockEntity)).thenReturn(Uni.createFrom().item(mockEntity))

        // 3. Mock the mapper to convert the Entity back to the final DTO
        mockWhen(ordersMapper.entityToDto(mockEntity)).thenReturn(mockOrdersDto)

        // Act
        val resultUni: Uni<OrdersDto> = service.createNewOrder(mockCreateDto)
        val result: OrdersDto = resultUni.await().indefinitely()

        // Assert
        assertNotNull(result)
        assertEquals(mockOrdersDto.id, result.id)
        assertEquals(mockOrdersDto.orderOwner, result.orderOwner)

        // Verify that the key steps were executed once
        verify(ordersMapper, times(1)).createDataToEntity(mockCreateDto)
        verify(ordersRepository, times(1)).persistAndFlush(mockEntity)
        verify(ordersMapper, times(1)).entityToDto(mockEntity)
    }

    // --- Tests for findAllOrders ---

    @Test
    fun `findAllOrders should retrieve all entities and map them to DTOs`() {
        // Arrange
        val entityList = listOf(mockEntity, mockEntity)
        val dtoList = listOf(mockOrdersDto, mockOrdersDto)

        // 1. Mock the Panache Query result
        val mockQuery = mock(PanacheQuery::class.java) as PanacheQuery<OrdersEntity>
        mockWhen(mockQuery.list<OrdersEntity>()).thenReturn(Uni.createFrom().item(entityList))

        // 2. Mock the repository's findAll call
        mockWhen(ordersRepository.findAll()).thenReturn(mockQuery)

        // 3. Mock the mapper for the transformation step
        mockWhen(ordersMapper.entityToDto(any(OrdersEntity::class.java))).thenReturn(mockOrdersDto)

        // Act
        val resultUni: Uni<List<OrdersDto>> = service.findAllOrders()
        val result: List<OrdersDto> = resultUni.await().indefinitely()

        // Assert
        assertEquals(2, result.size)
        assertEquals(TEST_OWNER_ID, result.first().orderOwner)

        // Verify calls
        verify(ordersRepository, times(1)).findAll()
        verify(ordersMapper, times(2)).entityToDto(any(OrdersEntity::class.java))
    }

    // --- Tests for findAllOrdersByOwner ---

    @Test
    fun `findAllOrdersByOwner should use the repository to filter by owner ID`() {
        // Arrange
        val entityList = listOf(mockEntity)

        // 1. Mock the repository to return a list for the specific owner
        mockWhen(ordersRepository.findByOwner(TEST_OWNER_ID)).thenReturn(Uni.createFrom().item(entityList))

        // 2. Mock the mapper
        mockWhen(ordersMapper.entityToDto(mockEntity)).thenReturn(mockOrdersDto)

        // Act
        val result: List<OrdersDto> = service.findAllOrdersByOwner(TEST_OWNER_ID).await().indefinitely()

        // Assert
        assertEquals(1, result.size)
        assertEquals(TEST_OWNER_ID, result.first().orderOwner)

        // Verify that the repository was called with the correct ID
        verify(ordersRepository, times(1)).findByOwner(TEST_OWNER_ID)
    }

    // --- Tests for findOrdersByDate ---

    @Test
    fun `findOrdersByDate should pass date range parameters to the repository`() {
        // Arrange
        val startDate = TEST_DATE.minusDays(5)
        val endDate = TEST_DATE.plusDays(5)
        val entityList = listOf(mockEntity)

        // 1. Mock the repository call with the nullable parameters
        mockWhen(ordersRepository.findByDates(startDate, endDate)).thenReturn(Uni.createFrom().item(entityList))

        // 2. Mock the mapper
        mockWhen(ordersMapper.entityToDto(mockEntity)).thenReturn(mockOrdersDto)

        // Act
        val result: List<OrdersDto> = service.findOrdersByDate(startDate, endDate).await().indefinitely()

        // Assert
        assertEquals(1, result.size)

        // Verify that the repository was called with the correct parameters
        verify(ordersRepository, times(1)).findByDates(startDate, endDate)
    }

    // --- Tests for findOrdersByDateAndOwner ---

    @Test
    fun `findOrdersByDateAndOwner should pass all three parameters to the repository`() {
        // Arrange
        val startDate = TEST_DATE.minusDays(5)
        val endDate = TEST_DATE.plusDays(5)
        val entityList = listOf(mockEntity)

        // 1. Mock the repository call
        mockWhen(ordersRepository.findByDatesAndOwner(TEST_OWNER_ID, startDate, endDate)).thenReturn(Uni.createFrom().item(entityList))

        // 2. Mock the mapper
        mockWhen(ordersMapper.entityToDto(mockEntity)).thenReturn(mockOrdersDto)

        // Act
        val result: List<OrdersDto> = service.findOrdersByDateAndOwner(TEST_OWNER_ID, startDate, endDate).await().indefinitely()

        // Assert
        assertEquals(1, result.size)

        // Verify that the repository was called with all correct parameters
        verify(ordersRepository, times(1)).findByDatesAndOwner(TEST_OWNER_ID, startDate, endDate)
    }
}