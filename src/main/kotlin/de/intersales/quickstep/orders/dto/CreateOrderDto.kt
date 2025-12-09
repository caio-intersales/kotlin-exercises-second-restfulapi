package de.intersales.quickstep.orders.dto

/**
 * DTO Class for adding new orders
 */

data class CreateOrderDto (
    // Field for owner (user id - Long)
    val orderOwner: Long,

    // Field for products
    val orderProducts: List<Long>
)