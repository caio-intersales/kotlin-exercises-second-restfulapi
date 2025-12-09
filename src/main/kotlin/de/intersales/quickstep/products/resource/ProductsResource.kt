package de.intersales.quickstep.products.resource

import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.products.dto.CreateProductDto
import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.products.dto.UpdateProductDto
import de.intersales.quickstep.products.service.ProductsService
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.net.URI
import javax.ws.rs.DELETE
import javax.ws.rs.PUT

@Path("/api/products")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class ProductsResource (
    private val productsService: ProductsService
) {
    /**
     * Endpoint: Read all products
     * What does it do: This endpoint takes a GET request and returns all products from the database
     */
    @GET
    @Path("/list")
    fun showAllProducts(): Uni<List<ProductsDto>>{
        // Calls the function from Service
        return productsService.findAllProducts()
    }

    /**
     * Endpoint: Read a specific product
     * What does it do: This endpoint takes an ID and returns the product that has this ID
     */
    @GET
    @Path("/show/{id}")
    fun showSpecificProduct(@PathParam("id") id: Long): Uni<ProductsDto> {
        return productsService.findOneProduct(id)
    }

    /**
     * Endpoint: Create a new product
     * What does it do: This endpoint receives JSON data and creates a new product
     */
    @POST
    @ReactiveTransactional
    @Path("/add")
    fun addNewProduct(@Valid dto: CreateProductDto): Uni<Response> {
        return productsService.createNewProduct(dto)
            .onItem().transform { createdProductDto ->
                Response.created(URI.create("/api/products/show/${createdProductDto.id}")).entity(createdProductDto).build()
                // If successful, it will return a 201 CREATED and the URI with the location of the new product
            }
    }

    /**
     * Endpoint: Update a product
     * What does it do: This endpoint receives JSON data and update and existing product
     */
    @PUT
    @ReactiveTransactional
    @Path("/edit")
    fun updateExistingProduct(@Valid dto: UpdateProductDto): Uni<ProductsDto> {
        return productsService.updateOneProduct(dto)
        // If successful, it will return a 200 OK and the updated DTO
        // Errors are handled by the service
    }

    /**
     * Endpoint: Delete a user
     * What does it do: This endpoint deletes an existing product based on a given ID
     */
    @DELETE
    @ReactiveTransactional
    @Path("/delete/{id}")
    fun deleteExistingProduct(@PathParam("id") id: Long): Uni<Response> {
        return productsService.deleteProduct(id)
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