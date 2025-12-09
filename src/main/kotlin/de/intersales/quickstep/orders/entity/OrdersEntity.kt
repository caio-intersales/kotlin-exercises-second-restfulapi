package de.intersales.quickstep.orders.entity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import com.fasterxml.jackson.core.type.TypeReference
import de.intersales.quickstep.users.dto.UsersDto
import java.time.OffsetDateTime

/**
 * "Orders" entity with all information from system's orders
 */

@Entity
@Table(name = "orders")
class OrdersEntity : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    // --- Override for tests
    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(javaClass != other?.javaClass) return false

        other as OrdersEntity

        // If the ID is null (meaning it's a new entity), then an entity is only equal to itself (not others)
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        // If the ID is not null (test) use its hash code
        // If it is a new entity, then use a fixed constant
        return id?.hashCode() ?: 31
    }
    // --- End of overrides

    @Column(name = "order_owner")
    var orderOwner: Long? = null

    // persisted column (jsonb)
    @Column(name = "order_products", columnDefinition = "jsonb")
    var orderProductsJson: String = "[]"

    val orderProducts: List<Long>
        get() = objectMapper.readValue(
            orderProductsJson,
            object : TypeReference<List<Long>>() {}
        )

    fun setOrderProducts(value: List<Long>) {
        orderProductsJson = objectMapper.writeValueAsString(value)
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
    }

    @Column(name = "issue_date")
    var issueDate: OffsetDateTime = OffsetDateTime.now()
}