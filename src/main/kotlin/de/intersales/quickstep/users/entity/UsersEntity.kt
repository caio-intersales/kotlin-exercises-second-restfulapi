package de.intersales.quickstep.users.entity

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
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

    @Column(name = "firstname")
    var firstName: String?      = null

    @Column(name = "lastname")
    var lastName: String?       = null

    @Column(name = "emailaddress")
    var emailAddress: String?   = null

    @Column(name = "password")
    var password: String?       = null

    @Column(name = "deliveryaddress")
    var deliveryAddress: String? = null
}