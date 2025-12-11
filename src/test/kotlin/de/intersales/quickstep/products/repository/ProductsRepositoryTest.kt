package de.intersales.quickstep.products.repository

import de.intersales.quickstep.products.entity.ProductsEntity
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject

/**
 * Tests for the Products repository
 */

@QuarkusTest
class ProductsRepositoryTest {
    // The repository instance must be injected for testing
    @Inject
    lateinit var repository: ProductsRepository

    // Function to create and persist entity
    fun setupProduct(type: Int? = 1, price: Double? = 100.00): Uni<ProductsEntity> {
        val product = ProductsEntity().apply {
            this.productName = "Electronic"
            this.productType = type
            this.productPrice = price
            this.productQnt = 100
        }
        return product.persistAndFlush<ProductsEntity>().map { product }
    }

    @BeforeEach
    fun cleanDb() {
        repository.deleteAll().await().indefinitely()
    }

    // Test fun findByType
    @Test
    fun `findByType should return a list of all products of a given type`() {
        val givenType = 1

        setupProduct(givenType)
            .flatMap { setupProduct(givenType) }
            .flatMap { repository.findByType(givenType).collect().asList() }
            .invoke { list ->
                assertEquals(2, list.size)
            }
            .await().indefinitely()

        cleanDb() // Added because BeforeEach doesn't run as there is just this one test
    }
}