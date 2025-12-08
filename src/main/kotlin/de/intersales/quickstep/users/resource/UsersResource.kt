package de.intersales.quickstep.users.resource

import de.intersales.quickstep.users.dto.CreateUserDto
import de.intersales.quickstep.users.dto.UpdateUserDto
import de.intersales.quickstep.users.dto.UsersDto
import de.intersales.quickstep.users.exception.ElementNotFoundException
import de.intersales.quickstep.users.service.UsersService
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

@Path("/api/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class UsersResource (
    private val usersService: UsersService
) {

    /**
     * Endpoint: Read all users
     * What does it do: This endpoint takes a GET request and returns all users saved in the database
     */
    @GET
    @Path("/list")
    fun showAllUsers(): Uni<List<UsersDto>> {
        // Calls the function from Service that reads all users
        return usersService.showAllUsers()
    }

    /**
     * Endpoint: Read a specific user
     * What does it do: This endpoint takes an ID and returns the user that has this ID
     */
    @GET
    @Path("/show/{id}")
    fun showSpecificUser(@PathParam("id") id: Long): Uni<UsersDto> {
        return usersService.findOneUser(id)
    }

    /**
     * Endpoint: Create new user
     * What does it do: This endpoint receives JSON data and creates a new user
     */
    @POST
    @ReactiveTransactional
    @Path("/add")
    fun addNewUser(@Valid dto: CreateUserDto): Uni<Response> {
        return usersService.createNewUser(dto)
            .onItem().transform { createdUserDto ->
                Response.created(URI.create("/api/users/show/${createdUserDto.id}")).entity(createdUserDto).build()
                // If it is successful, it will return a 201 Created and the location of the new user
            }
    }

    /**
     * Endpoint: Update a user
     * What does it do: This endpoint receives JSON data and update an existing user
     */
    @PUT
    @ReactiveTransactional
    @Path("/edit")
    fun updateExistingUser(@Valid dto: UpdateUserDto): Uni<UsersDto> {
        return usersService.updateOneUser(dto)
        // If it is successful, it will return a 200 OK and the updated DTO
        // Errors are handled by the Service
    }

    /**
     * Endpoint: Delete a user
     * What does it do: This endpoint deletes an existing user based on a given ID
     */
    @DELETE
    @ReactiveTransactional
    @Path("/delete/{id}")
    fun deleteExistingUser(@PathParam("id") id: Long): Uni<Response> {
        return usersService.deleteUser(id)
            .onItem().transform {
                // If the service confirms the deletion, return 204 No Content Found
                Response.noContent().build()
            }
            // To avoid the problem with the throwable parameter, use '_'
            .onFailure(ElementNotFoundException::class.java).recoverWithItem { _: Throwable ->
                // If the exception is thrown, then return a 404 Not Found
                Response.status(Response.Status.NOT_FOUND).build()
            }
    }

}