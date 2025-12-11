package de.intersales.quickstep.orders.dto

import de.intersales.quickstep.users.dto.UsersDto

/**
 * DTO Class for updating orders
 */

data class UpdateOrderDto (
    val id: Long,
    val orderOwner: Long,
    val orderProducts: List<Long>
)