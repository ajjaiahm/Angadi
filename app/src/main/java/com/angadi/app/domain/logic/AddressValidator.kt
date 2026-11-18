package com.angadi.app.domain.logic

import com.angadi.app.domain.model.Address

enum class AddressField { RECIPIENT, PHONE, HOUSE, STREET, AREA, CITY, STATE, PIN_CODE }

object AddressValidator {
    private val phoneRegex = Regex("^[6-9][0-9]{9}$")
    private val pinRegex = Regex("^[1-9][0-9]{5}$")

    /** Strips spaces, dashes and a +91 / 0 prefix so "+91 98765-43210" validates as 10 digits. */
    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
            digits.length == 11 && digits.startsWith("0") -> digits.substring(1)
            else -> digits
        }
    }

    fun validate(address: Address): Map<AddressField, String> = buildMap {
        if (address.recipientName.trim().length < 2) put(AddressField.RECIPIENT, "Enter the recipient's name")
        if (!phoneRegex.matches(normalizePhone(address.phone))) put(AddressField.PHONE, "Enter a valid 10-digit mobile number")
        if (address.house.isBlank()) put(AddressField.HOUSE, "Enter a house or building")
        if (address.street.trim().length < 2) put(AddressField.STREET, "Enter a street")
        if (address.area.trim().length < 2) put(AddressField.AREA, "Enter an area or locality")
        if (address.city.trim().length < 2) put(AddressField.CITY, "Enter a city")
        if (address.state.trim().length < 2) put(AddressField.STATE, "Enter a state")
        if (!pinRegex.matches(address.pinCode.trim())) put(AddressField.PIN_CODE, "Enter a valid 6-digit PIN code")
    }

    fun clean(address: Address): Address = address.copy(
        recipientName = address.recipientName.trim(),
        phone = normalizePhone(address.phone),
        house = address.house.trim(),
        street = address.street.trim(),
        area = address.area.trim(),
        city = address.city.trim(),
        state = address.state.trim(),
        pinCode = address.pinCode.trim(),
    )
}
