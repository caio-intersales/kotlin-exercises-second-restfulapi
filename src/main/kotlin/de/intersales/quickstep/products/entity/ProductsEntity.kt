package de.intersales.quickstep.products.entity

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

/**
 * "Products" entity with all information from products saved in the system
 */

@Entity
@Table(name = "products")
class ProductsEntity : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    // --- Override for tests
    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(javaClass != other?.javaClass) return false

        other as ProductsEntity

        // If the ID is null (meaning it's a new entity), then an entity is only equal to itself (not others)
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        // If the ID is not null (test) use its hash code
        // If it is a new entity, then use a fixed constant
        return id?.hashCode() ?: 31
    }
    // --- End of overrides

    @Column(name = "name")
    var productName: String? = null

    @Column(name = "type")
    var productType: Int? = null

    @Column(name = "price")
    var productPrice: Double? = null

    @Column(name = "quantity")
    var productQnt: Int? = null
}