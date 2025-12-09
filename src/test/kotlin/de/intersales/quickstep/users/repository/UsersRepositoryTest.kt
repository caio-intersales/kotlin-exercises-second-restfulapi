package de.intersales.quickstep.users.repository

import de.intersales.quickstep.users.entity.UsersEntity
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject
import kotlin.apply

/**
 * Tests for the Users repository
 */

@QuarkusTest
class UsersRepositoryTest {
    // The repository instance must be injected for testing
    @Inject
    lateinit var repository: UsersRepository

    // Function to create and persist entity
    fun setupUser(email: String): Uni<UsersEntity> {
        val user = UsersEntity().apply {
            this.firstName = "Test"
            this.lastName = "User"
            this.emailAddress = email
            this.password = "secret"
            this.deliveryAddress = "blah blah Str."
        }
        return user.persistAndFlush<UsersEntity>().map { user }
    }

    @BeforeEach
    fun cleanDb() {
        repository.deleteAll().await().indefinitely()
    }

    // Test fun findByEmail
    @Test
    fun `findByEmail should return the correct user`() {
        val testEmail = "testuser@email.com"
        // 1. Add user to the database
        // 2. Find the added user
        // 3. Run assertions
        setupUser(testEmail)
            .flatMap { repository.findByEmail(testEmail) }
            .invoke { result ->
                assertNotNull(result)
                assertEquals(testEmail, result?.emailAddress)
            }
            .await().indefinitely()
    }

    // Test fun findByEmail when user doesn't exist
    @Test
    fun `findByEmail should return null if user does not exist`() {
        // setupUser is not called so that the table is empty

        // Call the repository function
        val uniResult = repository.findByEmail("nonexistent@email.com")

        // Subscribe and wait for the Uni result
        val result = uniResult.await().indefinitely()

        // Assertions
        assertNull(result, "No user should be found.")
    }

    // Test fun findUserById
    @Test
    fun `findUserById should return the correct user when ID is present`() {
        // 1. Add user
        // 2. Find the added user
        // 3. Assert whether it is null
        setupUser("email@email.com")
            .flatMap { created ->
                    val id = created.id ?: throw IllegalStateException("User is null")
                    repository.findUserById(id)
                }
            .invoke { result ->
                assertNotNull(result)
            }
            .await().indefinitely()
    }

    // Test fun showAllUsers
    @Test
    fun `showAllUsers should return all persisted users`() {
        // 1. Create user 1
        // 2. Create user 2
        // 3. Look for users
        // 4. Check whether they've been found
        setupUser("email1@email.com")
            .flatMap { setupUser("email2@email.com") }
            .flatMap { repository.showAllUsers().list<UsersEntity>() }
            .invoke { list ->
                assertEquals(2, list.size)
            }
            .await().indefinitely()
    }
}