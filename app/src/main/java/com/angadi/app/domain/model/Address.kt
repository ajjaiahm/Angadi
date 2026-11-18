package com.angadi.app.domain.model

enum class AddressLabel(val display: String) { HOME("Home"), WORK("Work"), OTHER("Other") }

data class Address(
    val id: Long = 0,
    val label: AddressLabel = AddressLabel.HOME,
    val recipientName: String = "",
    val phone: String = "",
    val house: String = "",
    val street: String = "",
    val area: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = "",
    val isDefault: Boolean = false,
) {
    /** One-line text used on cards, checkout and the order snapshot. */
    val singleLine: String
        get() = listOf(house, street, area, city, state).filter { it.isNotBlank() }.joinToString(", ") + " – $pinCode"
}
