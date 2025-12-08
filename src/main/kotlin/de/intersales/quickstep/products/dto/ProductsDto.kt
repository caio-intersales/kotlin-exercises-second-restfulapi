package de.intersales.quickstep.products.dto

/**
 * DTO class for returning data from a product
 */

data class ProductsDto (
    val id: Long?,
    val productName: String,
    val productType: Int,
    val productPrice: Double,
    val productQnt: Int
)