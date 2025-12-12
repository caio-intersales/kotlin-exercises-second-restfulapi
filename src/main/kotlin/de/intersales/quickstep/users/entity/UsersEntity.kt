package de.intersales.quickstep.users.entity

import de.intersales.quickstep.addresses.entity.AddressesEntity
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

/**
 * "Users" entity with all information from system's users
 */

@Entity
@Table(name = "users")
class UsersEntity : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    // --- Override for tests
    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(javaClass != other?.javaClass) return false

        other as UsersEntity

        // If the ID is null (meaning it's a new entity), then an entity is only equal to itself (not others)
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        // If the ID is not null (test) use its hash code
        // If it is a new entity, then use a fixed constant
        return id?.hashCode() ?: 31
    }
    // --- End of overrides


    @Column(name = "firstname")
    var firstName: String?      = null

    @Column(name = "lastname")
    var lastName: String?       = null

    @Column(name = "emailaddress")
    var emailAddress: String?   = null

    @Column(name = "password")
    var password: String?       = null

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "deliveryaddress")
    var deliveryAddress: AddressesEntity? = null
}