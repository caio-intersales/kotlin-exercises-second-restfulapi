package de.intersales.quickstep.users.dto

/**
 * DTO Class for updating users
 */

data class UpdateUserDto (
    val id: Long,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val deliveryAddress: String?
)