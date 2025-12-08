package de.intersales.quickstep.users.dto

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

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