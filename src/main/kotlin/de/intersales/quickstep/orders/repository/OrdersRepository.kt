package de.intersales.quickstep.orders.repository

import de.intersales.quickstep.orders.entity.OrdersEntity
import io.quarkus.hibernate.reactive.panache.PanacheRepository
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import java.time.OffsetDateTime
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
     * What does it do: Allows for searching orders that were created between two dates
     */
    fun findByDates(startDate: OffsetDateTime?, endDate: OffsetDateTime?): Uni<List<OrdersEntity>> {

        // Check for invalidity
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            // If the start date is after the end date, return an empty list immediately
            return Uni.createFrom().item(emptyList())
        }

        // Dynamic query
        var query = ""
        val parameters = Parameters()

        if(startDate != null){
            query += "issue_date >= :startDate"
            parameters.and("startDate", startDate)
        }

        if(endDate != null){
            if(query.isNotEmpty()){
                query += " and "
            }
            query += "issue_date <= :endDate"
            parameters.and("endDate", endDate)
        }

        // Execute query
        return if (query.isEmpty()){
            // if both dates are empty, return all
            findAll().list()
        }else{
            // One or both were passed
            find(query, parameters).list()
        }
    }

    /**
     * Function: findByDatesAndOwner
     * What does it do: Allows for searching orders that were created between two dates and belong to one owner
     */
    fun findByDatesAndOwner(orderOwner: Long, startDate: OffsetDateTime?, endDate: OffsetDateTime?): Uni<List<OrdersEntity>> {

        // Check for invalidity
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            // If the start date is after the end date, return an empty list immediately
            return Uni.createFrom().item(emptyList())
        }

        // Dynamic query
        var query = "order_owner = :owner"
        val parameters = Parameters.with("owner", orderOwner)

        if(startDate != null){
            query += " AND issue_date >= :startDate"
            parameters.and("startDate", startDate)
        }

        if(endDate != null){
            if(query.isNotEmpty()){
                query += " and "
            }
            query += " AND issue_date <= :endDate"
            parameters.and("endDate", endDate)
        }

        return find(query, parameters).list()
    }

    /**
     * Other possible functions:
     *
     * findOrdersByProduct - would allow a search for orders in which a specific product was ordered
     */
}