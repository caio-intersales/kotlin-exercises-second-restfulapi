package de.intersales.quickstep.products.dto

import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

/**
 * DTO Class for adding new products
 */

data class CreateProductDto(
    @field:NotBlank(message = "Product name cannot be blank.")
    val productName: String,

    @field:Min(value = 0, message = "Product type must be at least 0.")
    val productType: Int,

    @field:Min(value = 0, message = "Product price must be at least 0.")
    val productPrice: Double,

    @field:Min(value = 0, message = "Product quantity must be at least 0.")
    val productQnt: Int
)