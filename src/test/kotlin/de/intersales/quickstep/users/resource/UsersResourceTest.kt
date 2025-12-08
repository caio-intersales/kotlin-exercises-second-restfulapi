package de.intersales.quickstep.users.resource

import de.intersales.quickstep.users.dto.CreateUserDto
import de.intersales.quickstep.users.dto.UpdateUserDto
import de.intersales.quickstep.users.dto.UsersDto
import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.users.service.UsersService
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured.given
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Test
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import org.mockito.Mockito.`when` as mockWhen
import org.hamcrest.CoreMatchers.`is`

/**
 * Tests for Users Resource
 */

@QuarkusTest
class UsersResourceTest {

    // Mock data for testing in an isolated Resource Logic
    @InjectMock
    lateinit var usersService: UsersService

    private val USER_ID = 1L
    private val USER_DTO = UsersDto(
        id = USER_ID,
        firstName = "Max",
        lastName = "Mustermann",
        email = "max@email.com",
        deliveryAddress = "Musterstr 123"
    )
    private val CREATE_DTO = CreateUserDto(
        firstName = "Maxi",
        lastName = "Mustermanni",
        email = "maxi@email.com",
        rawPassword = "hashed_password",
        deliveryAddress = "Musterstr 124"
    )
    private val UPDATE_DTO = UpdateUserDto(
        id = USER_ID,
        firstName = "Jane",
        lastName = "Doe",
        email = "jane.doe@email.com",
        deliveryAddress = "Main St 123"
    )

    // Test @GET /list
    @Test
    fun `showAllUsers should return 200 OK with a list of users`() {
        // Mock service to return a list of DTOs
        mockWhen(usersService.findAllUsers()).thenReturn(Uni.createFrom().item(listOf(USER_DTO)))

        // Act & Assert
        given()
            .`when`().get("/api/users/list")
            .then()
            .statusCode(Response.Status.OK.statusCode)
            .contentType(MediaType.APPLICATION_JSON)
            .body("size()", `is`(1))
            .body("[0].id", `is`(USER_ID.toInt()))
    }

    // Test @GET /show/{id}
    @Test
    fun `showSpecificUser should return 200 OK when user is found`() {
        // Mock service to return the user DTO
        mockWhen(usersService.findOneUser(USER_ID)).thenReturn(Uni.createFrom().item(USER_DTO))

        // Act & Assert
        given()
            .`when`().get("api/users/show/$USER_ID")
            .then()
            .statusCode(Response.Status.OK.statusCode)
            .contentType(MediaType.APPLICATION_JSON)
            .body("firstName", `is`("Max"))
    }

    @Test
    fun `showSpecificUser should return 404 NOT FOUND when user does not exist`() {
        // Mock service to return an ElementNotFoundException
        mockWhen(usersService.findOneUser(USER_ID)).thenReturn(Uni.createFrom().failure(ElementNotFoundException("User not found.")))

        // Act & Assert
        given()
            .`when`().get("/api/users/show/$USER_ID")
            .then()
            .statusCode(Response.Status.NOT_FOUND.statusCode)
    }

    // Test @POST /add
    @Test
    fun `addNewUser should return 201 CREATED when user is successfully added`() {
        // Mock service to return a newly created DTO with an ID
        val createdDto = USER_DTO.copy(id = 5L)
        mockWhen(usersService.createNewUser(CREATE_DTO)).thenReturn(Uni.createFrom().item(createdDto))

        // Act & Assert
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(CREATE_DTO)
            .`when`().post("/api/users/add")
            .then()
            .statusCode(Response.Status.CREATED.statusCode)
            .header("Location", `is`("http://localhost:8081/api/users/show/5")) // Assert whether the Location header matches the endpoint's creation URI
            .body("id", `is`(5))
    }

    @Test
    fun `addNewUser should return 400 BAD REQUEST for invalid input`() {
        // Create an invalid DTO
        val invalidDto = CREATE_DTO.copy(email = "aa==com")
        // Service doesn't need to be mocked as validation happens before the service runs

        // Act & Assert
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(invalidDto)
            .`when`().post("/api/users/add")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    // Test @PUT /edit
    @Test
    fun `updateExistingUser should return 200 OK when user is updated`() {
        // Mock the service to return the updated DTO
        val updatedDto = USER_DTO.copy(firstName = "Updated")
        mockWhen(usersService.updateOneUser(UPDATE_DTO)).thenReturn(Uni.createFrom().item(updatedDto))

        // Act & Assert
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(UPDATE_DTO)
            .`when`().put("/api/users/edit")
            .then()
            .statusCode(Response.Status.OK.statusCode)
            .body("firstName", `is`("Updated"))
    }

    // Test @DELETE /delete/{id}
    @Test
    fun `deleteExistingUser should return 204 NO CONTENT on successful deletion`() {
        // Mock the service to complete successfully (return void/Unit)
        mockWhen(usersService.deleteUser(USER_ID)).thenReturn(Uni.createFrom().item(true))

        // Act & Assert
        given()
            .`when`().delete("/api/users/delete/$USER_ID")
            .then()
            .statusCode(Response.Status.NO_CONTENT.statusCode)
    }

    @Test
    fun `deleteExistingUser should return 404 NOT FOUND when user does not exist`() {
        // Mock the service to return an ElementNotFoundException
        mockWhen(usersService.deleteUser(USER_ID)).thenReturn(
            Uni.createFrom().failure(ElementNotFoundException("User not found"))
        )

        // Act & Assert
        given()
            .`when`().delete("/api/users/delete/$USER_ID")
            .then()
            .statusCode(Response.Status.NOT_FOUND.statusCode)
    }


}