package de.intersales.quickstep.products.dto

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

/**
 * DTO Class for adding new products
 */

data class CreateProductDto(
    @field:NotBlank(message = "Product name cannot be blank.")
    val productName: String,

    @field:NotBlank(message = "Product type cannot be blank.")
    val productType: Int,

    @field:NotBlank(message = "Product price cannot be blank.")
    @field:Size(min = 0, message = "Product price must be at least 0.")
    val productPrice: Double,

    @field:NotBlank(message = "Product quantity cannot be blank.")
    @field:Size(min = 0, message = "Product quantity must be at least 0.")
    val productQnt: Int
)