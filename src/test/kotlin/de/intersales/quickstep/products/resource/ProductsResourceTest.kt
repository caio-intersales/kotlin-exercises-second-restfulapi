package de.intersales.quickstep.products.resource

import de.intersales.quickstep.exceptions.ElementNotFoundException
import de.intersales.quickstep.products.dto.CreateProductDto
import de.intersales.quickstep.products.dto.UpdateProductDto
import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.products.service.ProductsService
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured.given
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Test
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.hamcrest.CoreMatchers.`is`
import org.mockito.Mockito.`when` as mockWhen

/**
 * Tests for Products Resource
 */

@QuarkusTest
class ProductsResourceTest {

    // Mock data for testing in an isolated Resource Logic
    @InjectMock
    lateinit var productsService: ProductsService

    private val PRODUCT_ID = 10L
    private val PRODUCT_DTO = ProductsDto(
        id = PRODUCT_ID,
        productName = "Product A",
        productType = 1,
        productPrice = 100.00,
        productQnt = 100
    )

    private val CREATE_DTO = CreateProductDto(
        productName = "Product B",
        productType = 1,
        productPrice = 200.00,
        productQnt = 50
    )

    private val UPDATE_DTO = UpdateProductDto(
        id = PRODUCT_ID,
        productName = "Product Z",
        productType = 1,
        productPrice = 99.99,
        productQnt = 10
    )

    // ─────────────────────────────────────────────
    // @GET /list
    // ─────────────────────────────────────────────
    @Test
    fun `showAllProducts should return 200 OK with a list of products`() {
        mockWhen(productsService.findAllProducts()).thenReturn(Uni.createFrom().item(listOf(PRODUCT_DTO)))

        given()
            .`when`().get("/api/products/list")
            .then()
            .statusCode(Response.Status.OK.statusCode)
            .contentType(MediaType.APPLICATION_JSON)
            .body("size()", `is`(1))
            .body("[0].id", `is`(PRODUCT_ID.toInt()))
    }

    // ─────────────────────────────────────────────
    // @GET /show/{id}
    // ─────────────────────────────────────────────
    @Test
    fun `showSpecificProduct should return 200 OK when product is found`() {
        mockWhen(productsService.findOneProduct(PRODUCT_ID)).thenReturn(Uni.createFrom().item(PRODUCT_DTO))

        given()
            .`when`().get("/api/products/show/$PRODUCT_ID")
            .then()
            .statusCode(Response.Status.OK.statusCode)
            .contentType(MediaType.APPLICATION_JSON)
            .body("productName", `is`("Product A"))
    }

    @Test
    fun `showSpecificProduct should return 404 NOT FOUND when product does not exist`() {
        mockWhen(productsService.findOneProduct(PRODUCT_ID))
            .thenReturn(Uni.createFrom().failure(ElementNotFoundException("Product not found.")))

        given()
            .`when`().get("/api/products/show/$PRODUCT_ID")
            .then()
            .statusCode(Response.Status.NOT_FOUND.statusCode)
    }

    // ─────────────────────────────────────────────
    // @POST /add
    // ─────────────────────────────────────────────
    @Test
    fun `addNewProduct should return 201 CREATED when product is successfully added`() {
        val createdDto = PRODUCT_DTO.copy(id = 99L)
        mockWhen(productsService.createNewProduct(CREATE_DTO)).thenReturn(Uni.createFrom().item(createdDto))

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(CREATE_DTO)
            .`when`().post("/api/products/add")
            .then()
            .statusCode(Response.Status.CREATED.statusCode)
            .header("Location", `is`("http://localhost:8081/api/products/show/99"))
            .body("id", `is`(99))
    }

    @Test
    fun `addNewProduct should return 400 BAD REQUEST for invalid input`() {
        val invalidDto = CREATE_DTO.copy(productName = "") // Name cannot be empty normally

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(invalidDto)
            .`when`().post("/api/products/add")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.statusCode)
    }

    // ─────────────────────────────────────────────
    // @PUT /edit
    // ─────────────────────────────────────────────
    @Test
    fun `updateExistingProduct should return 200 OK when product is updated`() {
        val updatedDto = PRODUCT_DTO.copy(productName = "Updated Product")
        mockWhen(productsService.updateOneProduct(UPDATE_DTO)).thenReturn(Uni.createFrom().item(updatedDto))

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(UPDATE_DTO)
            .`when`().put("/api/products/edit")
            .then()
            .statusCode(Response.Status.OK.statusCode)
            .body("productName", `is`("Updated Product"))
    }

    // ─────────────────────────────────────────────
    // @DELETE /delete/{id}
    // ─────────────────────────────────────────────
    @Test
    fun `deleteExistingProduct should return 204 NO CONTENT on successful deletion`() {
        mockWhen(productsService.deleteProduct(PRODUCT_ID))
            .thenReturn(Uni.createFrom().item(true))

        given()
            .`when`().delete("/api/products/delete/$PRODUCT_ID")
            .then()
            .statusCode(Response.Status.NO_CONTENT.statusCode)
    }

    @Test
    fun `deleteExistingProduct should return 404 NOT FOUND when product does not exist`() {
        mockWhen(productsService.deleteProduct(PRODUCT_ID))
            .thenReturn(Uni.createFrom().failure(ElementNotFoundException("Product not found")))

        given()
            .`when`().delete("/api/products/delete/$PRODUCT_ID")
            .then()
            .statusCode(Response.Status.NOT_FOUND.statusCode)
    }
}
