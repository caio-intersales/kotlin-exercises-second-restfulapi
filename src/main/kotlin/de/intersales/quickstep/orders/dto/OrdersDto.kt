package de.intersales.quickstep.orders.dto

import java.time.OffsetDateTime

/**
 * DTO Class for returning data from an order
 */

data class OrdersDto (
    val id: Long?,
    val orderOwner: Long?,
    val orderProducts: String,
    val issueDate: OffsetDateTime
)