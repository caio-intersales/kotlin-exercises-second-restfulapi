package de.intersales.quickstep.orders.dto

import de.intersales.quickstep.users.dto.UsersDto

/**
 * DTO Class for adding new orders
 */

data class CreateOrderDto (
    // Field for owner (user id - Long)
    val orderOwner: Long?,

    // Field for products
    val orderProducts: List<Long>
)