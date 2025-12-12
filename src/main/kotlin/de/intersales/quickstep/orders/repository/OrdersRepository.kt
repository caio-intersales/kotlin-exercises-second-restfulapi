package de.intersales.quickstep.orders.repository

import de.intersales.quickstep.orders.entity.OrdersEntity
import io.quarkus.hibernate.reactive.panache.PanacheRepository
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class OrdersRepository : PanacheRepository<OrdersEntity> {
    /**
     * Function: findByOwner
     * What does it do: Allows for searching orders using a user's ID (orderOwner)
     */
    fun findByOwner(id: Long): Uni<List<OrdersEntity>> {
        return find("order_owner = :owner", Parameters.with("owner", id))
            .list()

        // For bigger db's, ".stream()" with Multi<> can be used for better performance (with @Transactional)
    }

    /**
     * Function: findByDates
     * What does it do: Allows for searching orders that were created between two dates, optionally filtering by Owner.
     */
    fun findByDates(orderOwner: Long?, startDateStr: String?, endDateStr: String?): Uni<List<OrdersEntity>> {

        // 0. Convert dates
        val localStartDate: LocalDate? = startDateStr?.let { LocalDate.parse(it) }
        val localEndDate: LocalDate? = endDateStr?.let { LocalDate.parse(it) }

        // Uses 00:00:00 for Start date
        val startDate: OffsetDateTime? = localStartDate?.atStartOfDay(ZoneOffset.UTC)?.toOffsetDateTime()

        // Uses 23:59:59 for the End date
        val endDate: OffsetDateTime? = localEndDate?.atTime(23, 59, 59, 999999999)?.atOffset(ZoneOffset.UTC)

        // 1. Check for invalid date range
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            // If the start date is after the end date, return an empty list immediately
            return Uni.createFrom().item(emptyList())
        }

        // 2. Initialize Dynamic Query and Parameters
        var query = ""
        val parameters = Parameters()

        // 3. Conditionally filter by Owner ID
        if (orderOwner != null) {
            query += "order_owner = :owner"
            parameters.and("owner", orderOwner)
        }

        // 4. Conditionally filter by Start Date
        if (startDate != null) {
            if (query.isNotEmpty()) {
                query += " and "
            }
            query += "issue_date >= :startDate"
            parameters.and("startDate", startDate)
        }

        // 5. Conditionally filter by End Date
        if (endDate != null) {
            if (query.isNotEmpty()) {
                query += " and "
            }
            query += "issue_date <= :endDate"
            parameters.and("endDate", endDate)
        }

        // 6. Execute Query
        return if (query.isEmpty()) {
            // If no criteria (owner, start date, or end date) are provided, return all
            findAll().list()
        } else {
            // Execute query with one or more passed criteria
            find(query, parameters).list()
        }
    }

    /**
     * Other possible functions:
     *
     * findOrdersByProduct - would allow a search for orders in which a specific product was ordered
     */
}