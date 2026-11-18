package com.angadi.app.domain.model

import java.time.LocalDate

data class DeliverySlot(
    val date: LocalDate,
    val startHour: Int,
    val endHour: Int,
    val slotsLeft: Int,
    /** Epoch millis of the slot start in the planner's zone. */
    val startMillis: Long,
    val available: Boolean,
) {
    val timeLabel: String get() = "${hourLabel(startHour)} – ${hourLabel(endHour)}"
    val key: String get() = "$date-$startHour"

    private fun hourLabel(hour: Int): String {
        val h = if (hour % 12 == 0) 12 else hour % 12
        return "$h ${if (hour < 12) "AM" else "PM"}"
    }
}

data class DeliveryDay(val date: LocalDate, val label: String, val slots: List<DeliverySlot>)
