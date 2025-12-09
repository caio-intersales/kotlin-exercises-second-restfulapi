package de.intersales.quickstep.orders.dto

/**
 * DTO Class for updating orders
 */

data class UpdateOrderDto (
    val id: Long,
    val orderOwner: Long,
    val orderProducts: List<Long>
)