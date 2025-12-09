package de.intersales.quickstep.orders.repository

import de.intersales.quickstep.orders.entity.OrdersEntity
import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import org.hibernate.validator.internal.util.Contracts.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Tests for the Orders repository
 */

@QuarkusTest
class OrdersRepositoryTest {

    // Inject the repository for testing
    @Inject
    lateinit var repository: OrdersRepository

    // Define fixed dates for testing predictability
    private val datePast: OffsetDateTime = OffsetDateTime.now().minus(5, ChronoUnit.DAYS)
    private val dateMiddle: OffsetDateTime = OffsetDateTime.now().minus(3, ChronoUnit.DAYS)
    private val dateFuture: OffsetDateTime = OffsetDateTime.now().minus(1, ChronoUnit.DAYS)

    // Helper to create and persist orders with a specific issue_date
    // for testing the function findByDates
    private fun createOrder(issueDate: OffsetDateTime): OrdersEntity {
        val order = OrdersEntity()
        order.orderOwner = 10L
        order.issueDate = issueDate
        return order
    }

    // Function to create and persist entity
    fun setupOrder(orderOwner: Long): Uni<OrdersEntity> {
        val order = OrdersEntity().apply {
            this.orderOwner = orderOwner
            this.setOrderProducts(listOf(1L, 5L, 10L))
        }
        return order.persistAndFlush<OrdersEntity>().map { order }
    }

    @BeforeEach
    fun cleanDb() {
        repository.deleteAll().await().indefinitely()

        repository.persist(createOrder(datePast)).await().indefinitely()
        repository.persist(createOrder(dateMiddle)).await().indefinitely()
        repository.persist(createOrder(dateFuture)).await().indefinitely()
    }

    // Test fun findByOwner
    @Test
    fun `findByOwner should return a list of orders with a specific owner`() {
        // Three orders are already inserted for owner 10L before each test
        val testOwner = 10L

        repository.findByOwner(testOwner)
            .invoke { result ->
                assertNotNull(result)
                assertEquals(3, result.size, "The query should return the three orders inserted")
                assertTrue(result.all { it.orderOwner == testOwner }, "All returned orders must have the expected owner ID: $testOwner" )
            }
            .await().indefinitely()
    }

    // Tests for findByDates
    @Test
    fun `findByDates should return the right amount of orders when given both start and end dates`() {
        // ACT: Look for orders between datePast (inclusive) and dateFuture (inclusive)
        val result = repository.findByDates(datePast, dateFuture)
            .await().indefinitely()

        // ASSERT: Should return all 3 orders
        assertEquals(3, result.size, "Should find all 3 orders within the widest range.")
    }

    @Test
    fun `findByDates should return the right amount of orders when given just start date`() {
        // ACT: Look for orders starting from dateMiddle (inclusive) onwards
        val result = repository.findByDates(dateMiddle, null)
            .await().indefinitely()

        // ASSERT: Should find the order from dateMiddle and dateFuture (2 orders)
        assertEquals(2, result.size, "Should find 2 orders (Middle and Future) from the start date.")
        assertTrue(result.none { it.issueDate.isBefore(dateMiddle) }, "No order should be before the start date.")
    }

    @Test
    fun `findByDates should return the right amount of orders when given just end date`() {
        // ACT: Look for orders up to dateMiddle (inclusive)
        val result = repository.findByDates(null, dateMiddle)
            .await().indefinitely()

        // ASSERT: Should find the order from datePast and dateMiddle (2 orders)
        assertEquals(2, result.size, "Should find 2 orders (Past and Middle) up to the end date.")
        assertTrue(result.none { it.issueDate.isAfter(dateMiddle) }, "No order should be after the end date.")
    }

    @Test
    fun `findByDates should return all orders when no date is provided`() {
        // ACT: Look for all orders
        val result = repository.findByDates(null, null)
            .await().indefinitely()

        // ASSERT: Should return all 3 orders
        assertEquals(3, result.size, "Should find all 3 orders when no dates are provided.")
    }

    @Test
    fun `findByDates should return an empty list if the dates provided are invalid`() {
        // ACT: Look for orders where the Start Date (dateFuture) is after the End Date (datePast)
        val result = repository.findByDates(dateFuture, datePast)
            .await().indefinitely()

        // ASSERT: Should return an empty list based on the check in your function
        assertTrue(result.isEmpty(), "Should return an empty list when startDate is chronologically after endDate.")
    }

    @Test
    fun `findByDates should return just one result if date range is too narrow`() {
        // ACT: Look for the single order at dateMiddle
        val result = repository.findByDates(dateMiddle, dateMiddle)
            .await().indefinitely()

        // ASSERT: Should return exactly 1 order
        assertEquals(1, result.size, "Should find only the order that exactly matches the start and end date.")
        assertEquals(dateMiddle.truncatedTo(ChronoUnit.SECONDS), result.first().issueDate.truncatedTo(ChronoUnit.SECONDS), "The found order should be the one at dateMiddle.")
    }
}