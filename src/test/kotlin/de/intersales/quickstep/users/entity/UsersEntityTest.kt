package de.intersales.quickstep.users.entity

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.apply

/**
 * Tests for the Users entity
 */

class UsersEntityTest {
    // Function to create a populated entity for reuse
    private fun createFullEntity(): UsersEntity {
        val entity = UsersEntity().apply {
            this.id = 1L
            this.firstName = "Max"
            this.lastName = "Mustermann"
            this.emailAddress = "max.mustermann@email.com"
            this.password = "hashed_password"
            this.deliveryAddress = "Musterstr. 123"
        }
        return entity
    }

    // 1. Basic construction test
    @Test
    fun `new entity should initialise all properties to null`() {
        val entity = UsersEntity()

        // Assert that all nullable properties are initialised to be null
        Assertions.assertNull(entity.id, "ID should be null.")
        Assertions.assertNull(entity.firstName, "firstName should be null.")
        Assertions.assertNull(entity.lastName, "lastName should be null.")
        Assertions.assertNull(entity.emailAddress, "emailAddress should be null.")
        Assertions.assertNull(entity.password, "password should be null.")
        Assertions.assertNull(entity.deliveryAddress, "deliveryAddress should be null.")
    }

    // 2. Property access & assignment test
    @Test
    fun `properties should be correctly assigned and retrieved`(){
        val entity = UsersEntity()

        // Set values
        entity.id = 99L
        entity.firstName = "Maxi"
        entity.lastName = "Mustermanni"
        entity.emailAddress = "max.mustermanni@email.com"
        entity.password = "hashed_passwordi"
        entity.deliveryAddress = "Musterstr. 125"

        // Check assigned values
        Assertions.assertEquals(99L, entity.id, "ID was not set correctly")
        Assertions.assertEquals("Maxi", entity.firstName, "firstName not set correctly")
        Assertions.assertEquals("Mustermanni", entity.lastName, "lastName not set correctly")
        Assertions.assertEquals("max.mustermanni@email.com", entity.emailAddress, "emailAddress not set correctly")
        Assertions.assertEquals("hashed_passwordi", entity.password, "password not set correctly")
        Assertions.assertEquals("Musterstr. 125", entity.deliveryAddress, "deliveryAddress not set correctly")
    }

    // 3. Identity test
    @Test
    fun `entity with the same non-null ID should be equal`() {
        val entity1 = createFullEntity().apply {
            id = 500L
            firstName = "Ignored A"
        }
        val entity2 = createFullEntity().apply {
            id = 500L
            firstName = "Ignored B"
        }

        Assertions.assertEquals(entity1, entity2, "Entities with the same ID must be equal if overridden.")
    }
}