package de.intersales.quickstep.products.dto

/**
 * DTO Class for updating products
 */

data class UpdateProductDto(
    val id: Long,
    val productName: String?,
    val productType: Int?,
    val productPrice: Double?,
    val productQnt: Int?
)