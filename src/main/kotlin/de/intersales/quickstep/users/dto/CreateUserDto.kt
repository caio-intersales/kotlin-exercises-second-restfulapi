package de.intersales.quickstep.users.dto

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

/**
 * DTO Class for adding new users
 */

data class CreateUserDto (

    // Field for first name must not be blank
    @field:NotBlank(message = "First name is required.")
    val firstName: String,

    // Field for last name must not be blank
    @field:NotBlank(message = "Last name is required.")
    val lastName: String,

    // Field for email must not be blank & valid
    @field:NotBlank(message = "Email address is required.")
    @field:Email(message = "Invalid email format.")
    val email: String,

    // Field for password must not be blank & respect the size constraint
    @field:NotBlank(message = "Password is required.")
    @field:Size(min = 8, message = "Passwords must be at least 8 characters long.")
    val rawPassword: String,

    // Field for delivery address can be blank as it can be added later by the user
    val deliveryAddress: String?
)