package de.intersales.quickstep.addresses.dto

data class UpdateAddressDto (
    val id: Long,
    val userId: Long,
    val street: String?,
    val houseNumber: String?,
    val city: String?,
    val state: String?,
    val zip: String?,
    val country: String?
)