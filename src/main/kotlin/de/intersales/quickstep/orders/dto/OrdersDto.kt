package de.intersales.quickstep.orders.dto

import de.intersales.quickstep.products.dto.ProductsDto
import de.intersales.quickstep.users.dto.UsersDto
import java.time.OffsetDateTime

/**
 * DTO Class for returning data from an order
 */

data class OrdersDto (
    val id: Long?,
    var orderOwner: UsersDto,
    var orderProducts: List<ProductsDto>,
    val issueDate: OffsetDateTime
)