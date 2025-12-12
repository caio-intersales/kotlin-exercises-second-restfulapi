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
}