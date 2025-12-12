package de.intersales.quickstep.addresses.entity

import de.intersales.quickstep.users.entity.UsersEntity
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "addresses")
class AddressesEntity : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    // --- Necessary override for tests
    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(javaClass != other?.javaClass) return false

        other as AddressesEntity

        // If the ID is null (new entity), then the entity is only equal to itself
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 31
    }
    // --- End of overrides

    @OneToOne(mappedBy = "deliveryaddress")
    var user: UsersEntity? = null

    @Column(name = "street")
    var street: String? = null

    @Column(name = "house_number")
    var houseNumber: String? = null

    @Column(name = "city")
    var city: String? = null

    @Column(name = "state")
    var state: String? = null

    @Column(name = "zip_code")
    var zip: String? = null

    @Column(name = "country")
    var country: String? = null
}