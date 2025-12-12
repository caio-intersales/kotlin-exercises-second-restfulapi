package de.intersales.quickstep.orders.repository

import de.intersales.quickstep.orders.entity.OrdersEntity
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
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
    private val datePast: OffsetDateTime = OffsetDateTime.now().minus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
    private val dateMiddle: OffsetDateTime = OffsetDateTime.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
    private val dateFuture: OffsetDateTime = OffsetDateTime.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)

    // Define the formatter to ensure the correct string output
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // New val for the updated ReceiveDatesDto which now receives Strings
    private val datePastStr: String = OffsetDateTime.now()
        .minus(5, ChronoUnit.DAYS)
        .toLocalDate() // Convert to LocalDate to remove time component
        .format(DATE_FORMATTER)

    private val dateMiddleStr: String = OffsetDateTime.now()
        .minus(3, ChronoUnit.DAYS)
        .toLocalDate()
        .format(DATE_FORMATTER)

    private val dateFutureStr: String = OffsetDateTime.now()
        .minus(1, ChronoUnit.DAYS)
        .toLocalDate()
        .format(DATE_FORMATTER)

    // Define Owner IDs
    private val OWNER_A = 10L
    private val OWNER_B = 20L

    // Helper to create and persist orders with a specific issue_date and owner
    private fun createOrder(ownerId: Long, issueDate: OffsetDateTime): OrdersEntity {
        val order = OrdersEntity()
        order.orderOwner = ownerId
        order.issueDate = issueDate
        order.setOrderProducts(listOf(1L))
        return order
    }

    @BeforeEach
    fun setup() {
        // Ensure truncation is also applied in the setup to match the comparison in assertions
        repository.deleteAll().await().indefinitely()

        // 3 Orders for OWNER_A: Past, Middle, Future
        repository.persist(createOrder(OWNER_A, datePast)).await().indefinitely()
        repository.persist(createOrder(OWNER_A, dateMiddle)).await().indefinitely()
        repository.persist(createOrder(OWNER_A, dateFuture)).await().indefinitely()

        // 1 Order for OWNER_B: Middle
        repository.persist(createOrder(OWNER_B, dateMiddle)).await().indefinitely()
    }

    // Test fun findByOwner
    @Test
    fun `findByOwner should return a list of orders with a specific owner`() {
        // There are 3 orders for OWNER_A
        repository.findByOwner(OWNER_A)
            .invoke { result ->
                assertNotNull(result)
                assertEquals(3, result.size, "The query should return 3 orders for OWNER_A.")
                assertTrue(result.all { it.orderOwner == OWNER_A }, "All returned orders must have the expected owner ID: $OWNER_A")
            }
            .await().indefinitely()

        // There is 1 order for OWNER_B
        repository.findByOwner(OWNER_B)
            .invoke { result ->
                assertNotNull(result)
                assertEquals(1, result.size, "The query should return 1 order for OWNER_B.")
                assertTrue(result.all { it.orderOwner == OWNER_B }, "All returned orders must have the expected owner ID: $OWNER_B")
            }
            .await().indefinitely()
    }

    // ===============================================
    // Tests for findByDates (combined functionality)
    // ===============================================

    // --- Date Filtering Only (orderOwner = null) ---

    @Test
    fun `findByDates should return the right amount of orders when given both start and end dates (All Owners)`() {
        // ACT: Look for orders between datePast and dateFuture for ALL owners (4 orders total)
        val result = repository.findByDates(null, datePastStr, dateFutureStr)
            .await().indefinitely()

        // ASSERT: Should return all 4 orders (3 from A, 1 from B)
        assertEquals(4, result.size, "Should find all 4 orders within the widest range for all owners.")
    }

    @Test
    fun `findByDates should return the right amount of orders when given just start date (All Owners)`() {
        // ACT: Look for orders starting from dateMiddle onwards for ALL owners
        val result = repository.findByDates(null, dateMiddleStr, null)
            .await().indefinitely()

        // ASSERT: Should find 3 orders: A(Middle), A(Future), B(Middle)
        assertEquals(3, result.size, "Should find 3 orders (Middle and Future dates) from the start date.")
        assertTrue(result.none { it.issueDate.isBefore(dateMiddle) }, "No order should be before the start date.")
    }

    @Test
    fun `findByDates should return all orders when no criteria is provided (null, null, null)`() {
        // ACT: Look for all orders
        val result = repository.findByDates(null, null, null)
            .await().indefinitely()

        // ASSERT: Should return all 4 orders
        assertEquals(4, result.size, "Should find all 4 orders when no dates or owner are provided.")
    }

    @Test
    fun `findByDates should return an empty list if the dates provided are invalid`() {
        // ACT: Look for orders where the Start Date (dateFuture) is after the End Date (datePast) for ALL owners
        val result = repository.findByDates(null, dateFutureStr, datePastStr)
            .await().indefinitely()

        // ASSERT: Should return an empty list based on the check in the function
        assertTrue(result.isEmpty(), "Should return an empty list when startDate is chronologically after endDate.")
    }

    // --- Owner Filtering Only (dates = null) ---

    @Test
    fun `findByDates should return orders only for the specified owner when only owner is provided`() {
        // ACT: Look for orders only for OWNER_A
        val result = repository.findByDates(OWNER_A, null, null)
            .await().indefinitely()

        // ASSERT: Should return 3 orders, all belonging to OWNER_A
        assertEquals(3, result.size, "Should find 3 orders for the specified owner (A) when no dates are given.")
        assertTrue(result.all { it.orderOwner == OWNER_A }, "All results must belong to OWNER_A.")
    }

    @Test
    fun `findByDates should return empty list if owner is specified but has no orders`() {
        // ACT: Look for orders only for a non-existent owner (99L)
        val result = repository.findByDates(99L, null, null)
            .await().indefinitely()

        // ASSERT: Should return an empty list
        assertTrue(result.isEmpty(), "Should return an empty list for a non-existent owner.")
    }

    // --- Combined Date and Owner Filtering ---

    @Test
    fun `findByDates should filter by date range and specific owner`() {
        // ACT: Look for orders between datePast and dateMiddle for OWNER_A
        val result = repository.findByDates(OWNER_A, datePastStr, dateMiddleStr)
            .await().indefinitely()

        // ASSERT: Should find 2 orders: A(Past), A(Middle)
        assertEquals(2, result.size, "Should find 2 orders filtered by date range and OWNER_A.")
        assertTrue(result.all { it.orderOwner == OWNER_A }, "All results must belong to OWNER_A.")
        assertTrue(result.none { it.issueDate.isAfter(dateMiddle) }, "No order should be after dateMiddle.")
    }

    @Test
    fun `findByDates should return one result for exact match (Owner B, dateMiddle)`() {
        // ACT: Look for the single order matching OWNER_B and dateMiddle
        val result = repository.findByDates(OWNER_B, dateMiddleStr, dateMiddleStr)
            .await().indefinitely()

        // ASSERT: Should return exactly 1 order
        assertEquals(1, result.size, "Should find only the single order matching OWNER_B at dateMiddle.")
        assertEquals(OWNER_B, result.first().orderOwner, "The found order should belong to OWNER_B.")
        assertEquals(dateMiddle, result.first().issueDate.truncatedTo(ChronoUnit.SECONDS), "The found order should be at dateMiddle.")
    }

    @Test
    fun `findByDates should return empty list when date range excludes owner's orders`() {
        // ACT: Look for orders for OWNER_B (only has an order at dateMiddle) between datePast and datePast
        val result = repository.findByDates(OWNER_B, datePastStr, datePastStr)
            .await().indefinitely()

        // ASSERT: Should return empty list
        assertTrue(result.isEmpty(), "Should return an empty list as OWNER_B's order is outside the narrow date range.")
    }
}