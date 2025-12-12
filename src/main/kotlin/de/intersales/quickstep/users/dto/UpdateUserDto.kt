package de.intersales.quickstep.users.dto

import de.intersales.quickstep.addresses.entity.AddressesEntity

/**
 * DTO Class for updating users
 */

data class UpdateUserDto (
    val id: Long,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val deliveryAddress: AddressesEntity?
)