package de.intersales.quickstep.orders.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

/**
 * Tests for the Orders entity
 */

class OrdersEntityTest {
    // Function to create a populated entity for reuse
    private fun createFullEntity(): OrdersEntity {
        val entity = OrdersEntity().apply {
            this.id = 1L
            this.orderOwner = 10L
            this.setOrderProducts(listOf(1L, 2L, 5L))
        }
        return entity
    }

    // 1. Basic construction test
    @Test
    fun `new OrdersEntity should initialise all properties correctly`() {
        val entity = OrdersEntity()

        // ID and orderOwner are nullable, so should be null
        assertNull(entity.id, "ID should be null.")
        assertNull(entity.orderOwner, "orderOwner should be null.")

        // orderProductsJson should default to "[]"
        assertEquals("[]", entity.orderProductsJson, "orderProductsJson should default to empty JSON array.")

        // orderProducts getter should return empty list
        assertTrue(entity.orderProducts.isEmpty(), "orderProducts should be empty list.")

        // issueDate should be initialized to a non-null OffsetDateTime
        assertNotNull(entity.issueDate, "issueDate should be initialized to current timestamp.")
        assertTrue(entity.issueDate is OffsetDateTime, "issueDate should be of type OffsetDateTime.")
    }

    // 2. Property access & assignment test
    @Test
    fun `properties should be correctly assigned and retrieved`() {
        val entity = OrdersEntity()

        // Set values
        entity.id = 99L
        entity.orderOwner = 2L
        entity.setOrderProducts(listOf(1L, 2L, 5L))

        // Check assigned values
        assertEquals(99L, entity.id, "ID was not set correctly")
        assertEquals(2L, entity.orderOwner, "orderOwner was not set correctly")
        assertEquals(listOf(1L, 2L, 5L), entity.orderProducts, "orderProducts was not set correctly")
    }

    // 3. Identity test
    @Test
    fun `entity with the same non-null ID should be equal`() {
        val entity1 = createFullEntity().apply {
            id = 500L
            orderOwner = 10L
        }
        val entity2 = createFullEntity().apply {
            id = 500L
            orderOwner = 11L
        }

        assertEquals(entity1, entity2, "Entities with the same ID must be equal if overridden.")
    }
}