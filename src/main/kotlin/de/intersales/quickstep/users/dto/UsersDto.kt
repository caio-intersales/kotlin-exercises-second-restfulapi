package de.intersales.quickstep.users.dto

import de.intersales.quickstep.addresses.entity.AddressesEntity

/**
 * DTO Class for returning data from user
 */

data class UsersDto(
    val id: Long?,
    val firstName: String,
    val lastName: String,
    val email: String,
    val deliveryAddress: AddressesEntity?
)