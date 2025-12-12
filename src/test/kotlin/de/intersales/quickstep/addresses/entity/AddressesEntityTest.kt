package de.intersales.quickstep.addresses.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the Addresses entity
 */

class AddressesEntityTest {
    // Function to create a populated entity for reuse
    private fun createFullEntity(): AddressesEntity {
        val entity = AddressesEntity().apply {
            this.id = 1L
            this.street = "Main St"
            this.houseNumber = "12"
            this.city = "Springfield"
            this.state = "NY"
            this.zip = "5sa898"
            this.country = "US"
        }
        return entity
    }

    // Basic construction test
    @Test
    fun `new AddressesEntity should initialise all properties correctly`() {
        val entity = AddressesEntity()

        assertNull(entity.id, "ID should be null.")
        assertNull(entity.street, "entityStreet should be null.")
        assertNull(entity.houseNumber, "houseNumber should be null.")
        assertNull(entity.city, "city should be null.")
        assertNull(entity.state, "state should be null.")
        assertNull(entity.zip, "zip should be null.")
        assertNull(entity.country, "country should be null.")
    }

    // Property access & assignment test
    @Test
    fun `properties should be correctly assigned and retrieved`() {
        val entity = AddressesEntity()

        // Set values
        entity.id       = 1L
        entity.street   = "Main St"
        entity.houseNumber = "12"
        entity.city     = "Springfield"
        entity.state    = "NY"
        entity.zip      = "5sa898"
        entity.country  = "US"

        // Check assigned values
        assertEquals(1L, entity.id, "ID was not set correctly")
        assertEquals("Main St", entity.street, "street was not set correctly")
        assertEquals("12", entity.houseNumber, "houseNumber was not set correctly")
        assertEquals("Springfield", entity.city, "city was not set correctly")
        assertEquals("NY", entity.state, "state was not set correctly")
        assertEquals("5sa898", entity.zip, "zip was not set correctly")
        assertEquals("US", entity.country, "country was not set correctly")

    }

    // Identity test
    @Test
    fun `entity with the same non-null ID should be equal`() {
        val entity1 = createFullEntity().apply {
            id = 500L
            street = "Main St"
        }

        val entity2 = createFullEntity().apply {
            id = 500L
            street = "Hauptstr."
        }

        assertEquals(entity1, entity2, "Entities with the same ID must be equal if overridden.")
    }
}