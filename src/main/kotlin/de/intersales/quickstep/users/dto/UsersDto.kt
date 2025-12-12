package de.intersales.quickstep.users.dto

/**
 * DTO Class for returning data from user
 */

data class UsersDto(
    val id: Long?,
    val firstName: String,
    val lastName: String,
    val email: String,
    val deliveryAddress: String?
)