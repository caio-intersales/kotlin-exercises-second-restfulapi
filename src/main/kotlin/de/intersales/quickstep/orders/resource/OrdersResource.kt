package de.intersales.quickstep.orders.resource

import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.orders.dto.CreateOrderDto
import de.intersales.quickstep.orders.dto.OrdersDto
import de.intersales.quickstep.orders.dto.UpdateOrderDto
import de.intersales.quickstep.orders.service.OrdersService
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import java.net.URI
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/api/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class OrdersResource (
    private val ordersService: OrdersService
) {
    /**
     * Endpoint: Read all orders (independent of owner)
     * What does it do: This endpoint gets a GET request and returns all orders from the database
     */
    @GET
    @Path("/list")
    fun showAllOrders(): Uni<List<OrdersDto>> {
        return ordersService.findAllOrders()
    }

    /**
     * Endpoint: Read a specific order
     * What does it do: This endpoint takes an ID and returns the order that has this id
     */
    @GET
    @Path("/show/{id}")
    fun showSpecificOrder(@PathParam("id") id: Long): Uni<OrdersDto> {
        return ordersService.findOneOrder(id)
    }

    /**
     * Endpoint: Create a new order
     * What does it do: This endpoint receives JSON data and creates a new order
     */
    @POST
    @ReactiveTransactional
    @Path("/add")
    fun addNewOrder(@Valid dto: CreateOrderDto): Uni<Response> {
        return ordersService.createNewOrder(dto)
            .onItem().transform { createdOrderDto ->
                Response.created(URI.create("/api/orders/show/${createdOrderDto.id}")).entity(createdOrderDto).build()
            }
    }

    /**
     * Endpoint: Update an order
     * What does it do: This endpoint receives JSON data and update and existing order
     */
    @PUT
    @ReactiveTransactional
    @Path("/edit")
    fun updateExistingOrder(@Valid dto: UpdateOrderDto): Uni<OrdersDto> {
        return ordersService.updateOneOrder(dto)
        // If successful, it will return a 200 OK and the updated DTO
        // Errors are handled by the service
    }

    /**
     * Endpoint: Delete an order
     * What does it do: This endpoint deletes an existing order based on a given ID
     */
    @DELETE
    @ReactiveTransactional
    @Path("/delete/{id}")
    fun deleteExistingOrder(@PathParam("id") id: Long): Uni<Response> {
        return ordersService.deleteOrder(id)
            .onItem().transform {
                // If deletion is confirmed by the service, return 204 NO CONTENT FOUND
                Response.noContent().build()
            }
            // "_" used to prevent a problem with the throwable parameter
            .onFailure(ElementNotFoundException::class.java)
            .recoverWithItem { _: Throwable ->
                // If the exception is thrown, then return a 404 NOT FOUND
                Response.status(Response.Status.NOT_FOUND).build()
            }
    }
}