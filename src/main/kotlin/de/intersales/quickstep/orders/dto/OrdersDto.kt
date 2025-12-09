package de.intersales.quickstep.orders.dto

import de.intersales.quickstep.products.dto.ProductsDto
import java.time.OffsetDateTime

/**
 * DTO Class for returning data from an order
 */

data class OrdersDto (
    val id: Long?,
    val orderOwner: Long?,
    var orderProducts: List<ProductsDto>,
    val issueDate: OffsetDateTime
)