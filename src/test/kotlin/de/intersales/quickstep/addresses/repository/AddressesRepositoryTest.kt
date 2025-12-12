package de.intersales.quickstep.addresses.repository

import de.intersales.quickstep.addresses.entity.AddressesEntity
import de.intersales.quickstep.users.entity.UsersEntity
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject

/**
 * Tests for the Addresses Repository
 */

@QuarkusTest
class AddressesRepositoryTest {

    // Inject the repository for testing
    @Inject
    lateinit var repository: AddressesRepository

    // Define Users ID
    private val USER_A = UsersEntity().apply {
        id = 10L
        firstName = "Max"
        lastName = "Mustermann"
        emailAddress = "max@mus.com"
        password = "Passwort"
        deliveryAddress = "AAAA" // Will have to be updated later
    }
    private val USER_B = UsersEntity().apply {
        id = 11L
        firstName = "Jane"
        lastName = "Doe"
        emailAddress = "jane@email.com"
        password = "Passwort"
        deliveryAddress = "AAAA" // Will have to be updated later
    }

    // Users ID Long
    // Exclude after integration
    private val USER_A_ID = 10L
    private val USER_B_ID = 11L

    // Helper function to create and persist addresses with a specific user
    // !!! TO UPDATE: userId has to be UsersEntity after the integration
    private fun createAddress(userId: Long, givenCountry: String): AddressesEntity {
        val address = AddressesEntity()
        address.user_id = userId
        address.country = givenCountry
        return address
    }

    @BeforeEach
    fun setup() {
        // Ensure the db is empty when running each test
        // repository.deleteAll().await().indefinitely()

        // !!! Update to USER_A / _B after integration

        repository.persist(createAddress(USER_A_ID, "US")).await().indefinitely()
        repository.persist(createAddress(USER_B_ID, "US")).await().indefinitely()
    }

    // Test fun findByCountry
    @Test
    fun `findByCountry should return a list of addresses with a specific country code`() {

        // Call the repository method
        val multi: Multi<AddressesEntity?> = repository.findByCountry("US")

        // Collect all elements into Uni<List<AddressesEntity?>>
        val listUni: Uni<List<AddressesEntity?>> = multi.collect().asList()

        // Block the Uni to get the final List result synchronously for the test
        val result: List<AddressesEntity?> = listUni.await().indefinitely()

        // Perform tests

        assertNotNull(result)
        assertEquals(2, result.size, "The query should return 2 for country_code = 'US'")
        assertTrue(result.all { it?.country == "US"}, "All returned addresses must have the country_code as US")
    }
}