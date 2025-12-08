package de.intersales.quickstep.products.entity

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for Products entity
 */

class ProductsEntityTest {
    // Function to create a populated entity for reuse
    private fun createFullEntity(): ProductsEntity {
        val entity = ProductsEntity().apply {
            this.id = 1L
            this.productName = "Laptop"
            this.productType = 1
            this.productPrice = 999.99
            this.productQnt = 10
        }
        return entity
    }

    // 1. Basic construction test
    @Test
    fun `new entity should initialise all properties to null`() {
        val entity = ProductsEntity()

        // Assert that all nullable properties are initialised to be null
        Assertions.assertNull(entity.id, "ID should be null.")
        Assertions.assertNull(entity.productName, "productName should be null.")
        Assertions.assertNull(entity.productType, "productType should be null.")
        Assertions.assertNull(entity.productPrice, "productPrice should be null.")
        Assertions.assertNull(entity.productQnt, "productQnt should be null.")
    }

    // 2. Property access & assignment test
    @Test
    fun `properties should be correctly assigned and retrieved`(){
        val entity = ProductsEntity()

        // Set values
        entity.id = 99L
        entity.productName = "Desktop"
        entity.productType = 1
        entity.productPrice = 500.00
        entity.productQnt = 5

        // Check assigned values
        Assertions.assertEquals(99L, entity.id, "ID was not set correctly")
        Assertions.assertEquals("Desktop", entity.productName, "productName not set correctly")
        Assertions.assertEquals(1, entity.productType, "productType not set correctly")
        Assertions.assertEquals(500.00, entity.productPrice, "productPrice not set correctly")
        Assertions.assertEquals(5, entity.productQnt, "productQnt not set correctly")
    }

    // 3. Identity test
    @Test
    fun `entity with the same non-null ID should be equal`() {
        val entity1 = createFullEntity().apply {
            id = 500L
            productName = "Product A"
        }
        val entity2 = createFullEntity().apply {
            id = 500L
            productName = "Product B"
        }

        Assertions.assertEquals(entity1, entity2, "Entities with the same ID must be equal if overridden.")
    }
}