package de.intersales.quickstep.orders.resource

import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.dto.ReceiveDatesDto
import de.intersales.quickstep.orders.dto.UpdateOrderDto
import de.intersales.quickstep.orders.service.OrdersService
import de.intersales.quickstep.users.dto.UsersDto
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.net.URI
import java.time.OffsetDateTime
import javax.ws.rs.core.Response

class OrdersResourceTest {

    private lateinit var ordersService: OrdersService
    private lateinit var ordersResource: OrdersResource

    @BeforeEach
    fun setup() {
        ordersService = mock(OrdersService::class.java)
        ordersResource = OrdersResource(ordersService)
    }

    val mockOwnerDto = UsersDto(
        id = 1L,
        firstName = "Test",
        lastName = "Owner",
        email = "test@example.com",
        deliveryAddress = "123 Test St"
    )

    val mockOwnerDto2 = mockOwnerDto.copy(
        id = 2L
    )

    // ─────────────────────────────────────────────
    // @GET /list
    // ─────────────────────────────────────────────
    @Test
    fun `showAllOrders should return list of OrdersDto`() {
        val orderDto1 = OrdersDto(1L, mockOwnerDto, emptyList(), OffsetDateTime.now())
        val orderDto2 = OrdersDto(2L, mockOwnerDto2, emptyList(), OffsetDateTime.now())
        `when`(ordersService.findAllOrders()).thenReturn(Uni.createFrom().item(listOf(orderDto1, orderDto2)))

        val result = ordersResource.showAllOrders().await().indefinitely()

        assertEquals(2, result.size)
        assertEquals(orderDto1, result[0])
        assertEquals(orderDto2, result[1])
        verify(ordersService).findAllOrders()
    }

    // ─────────────────────────────────────────────
    // @GET /show/{id}
    // ─────────────────────────────────────────────
    @Test
    fun `showSpecificOrder should return specific order`() {
        val orderDto = OrdersDto(1L, mockOwnerDto, emptyList(), OffsetDateTime.now())
        `when`(ordersService.findOneOrder(1L)).thenReturn(Uni.createFrom().item(orderDto))

        val result = ordersResource.showSpecificOrder(1L).await().indefinitely()

        assertEquals(orderDto, result)
        verify(ordersService).findOneOrder(1L)
    }

    // ─────────────────────────────────────────────
    // @POST /add
    // ─────────────────────────────────────────────
    @Test
    fun `addNewOrder should return created response`() {
        val createDto = CreateOrderDto(orderOwner = 1L, orderProducts = listOf(10L, 20L))
        val createdDto = OrdersDto(5L, mockOwnerDto, emptyList(), OffsetDateTime.now())
        `when`(ordersService.createNewOrder(createDto)).thenReturn(Uni.createFrom().item(createdDto))

        val response: Response = ordersResource.addNewOrder(createDto).await().indefinitely()

        assertEquals(Response.Status.CREATED.statusCode, response.status)
        assertEquals(createdDto, response.entity)
        assertEquals(URI.create("/api/orders/show/5"), response.location)
        verify(ordersService).createNewOrder(createDto)
    }

    // ─────────────────────────────────────────────
    // @PUT /edit/{id}
    // ─────────────────────────────────────────────
    @Test
    fun `updateExistingOrder should return updated order`() {
        val updateDto = UpdateOrderDto(1L, 1L, listOf(10L, 20L))
        val updatedOrder = OrdersDto(1L, mockOwnerDto, emptyList(), OffsetDateTime.now())
        `when`(ordersService.updateOneOrder(updateDto)).thenReturn(Uni.createFrom().item(updatedOrder))

        val result = ordersResource.updateExistingOrder(updateDto).await().indefinitely()

        assertEquals(updatedOrder, result)
        verify(ordersService).updateOneOrder(updateDto)
    }

    // ─────────────────────────────────────────────
    // @DELETE /delete/{id}
    // ─────────────────────────────────────────────

    @Test
    fun `deleteExistingOrder should return no content when deletion succeeds`() {
        `when`(ordersService.deleteOrder(1L)).thenReturn(Uni.createFrom().item(true))

        val response: Response = ordersResource.deleteExistingOrder(1L).await().indefinitely()

        assertEquals(Response.Status.NO_CONTENT.statusCode, response.status)
        verify(ordersService).deleteOrder(1L)
    }

    @Test
    fun `deleteExistingOrder should return not found when ElementNotFoundException is thrown`() {
        `when`(ordersService.deleteOrder(1L)).thenReturn(
            Uni.createFrom().failure(ElementNotFoundException("Not found"))
        )

        val response: Response = ordersResource.deleteExistingOrder(1L).await().indefinitely()

        assertEquals(Response.Status.NOT_FOUND.statusCode, response.status)
        verify(ordersService).deleteOrder(1L)
    }

    // ─────────────────────────────────────────────
    // @POST /show/time_range
    // ─────────────────────────────────────────────

    @Test
    fun `showTimeRange should call service with date DTO and null owner`() {
        // Arrange
        val startDate = "2025-01-01"
        val endDate = "2025-12-31"
        val datesDto = ReceiveDatesDto(startDate, endDate)
        val expectedResult = listOf(OrdersDto(1L, mockOwnerDto, emptyList(), OffsetDateTime.now()))

        // The service is expected to be called with the DTO and null for owner
        `when`(ordersService.findOrdersByDate(datesDto, null)).thenReturn(Uni.createFrom().item(expectedResult))

        // Act
        val resultUni = ordersResource.showTimeRange(datesDto)

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(expectedResult, resultList)
        verify(ordersService).findOrdersByDate(datesDto, null)
    }

    // ─────────────────────────────────────────────
    // @POST /show/owner/{id}
    // ─────────────────────────────────────────────

    @Test
    fun `showTimeRangeByOwner should call service with date DTO and specific owner ID`() {
        // Arrange
        val ownerId = 5L
        val startDate = "2025-01-01"
        val endDate = "2025-12-31"
        val datesDto = ReceiveDatesDto(startDate, endDate)
        val expectedResult = listOf(OrdersDto(10L, mockOwnerDto, emptyList(), OffsetDateTime.now()))

        // The service is expected to be called with the DTO and the specific owner ID
        `when`(ordersService.findOrdersByDate(datesDto, ownerId)).thenReturn(Uni.createFrom().item(expectedResult))

        // Act
        val resultUni = ordersResource.showTimeRangeByOwner(datesDto, ownerId)

        // Assert
        val resultList = resultUni.await().indefinitely()
        assertEquals(expectedResult, resultList)
        verify(ordersService).findOrdersByDate(datesDto, ownerId)
    }
}
