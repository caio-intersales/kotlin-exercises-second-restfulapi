package de.intersales.quickstep.orders.dto

import java.time.OffsetDateTime

/**
 * DTO Class for receiving dates to look for orders inside a time range
 */

data class ReceiveDatesDto (
    val startDate: String? = null,
    val endDate: String? = null
)