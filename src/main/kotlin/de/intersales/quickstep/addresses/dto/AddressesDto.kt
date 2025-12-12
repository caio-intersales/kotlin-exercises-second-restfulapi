package de.intersales.quickstep.addresses.dto

/**
 * Basic DTO for returning addresses
 */

data class AddressesDto (
    val id: Long?,
    val userId: Long?, // Will have to be updated after integration
    val street: String?,
    val houseNumber: String?,
    val city: String?,
    val state: String?,
    val zip: String?,
    val country: String?
)