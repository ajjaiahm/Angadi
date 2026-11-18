package com.angadi.app.domain.logic

import com.angadi.app.domain.model.DeliveryDay
import com.angadi.app.domain.model.DeliverySlot
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Builds *simulated* delivery slots. There is no logistics backend, so availability is derived
 * deterministically from the date and hour. The UI must label these as demo availability.
 */
object DeliverySlotPlanner {
    const val DAYS_AHEAD = 4
    const val LEAD_HOURS = 2
    private val windows = listOf(8 to 10, 10 to 12, 12 to 14, 16 to 18, 18 to 20)

    fun plan(now: ZonedDateTime = ZonedDateTime.now()): List<DeliveryDay> {
        val earliestStart = now.plusHours(LEAD_HOURS.toLong())
        return (0 until DAYS_AHEAD).map { offset ->
            val date = now.toLocalDate().plusDays(offset.toLong())
            DeliveryDay(date, dayLabel(date, now.toLocalDate()), windows.map { (start, end) -> slot(date, start, end, now.zone, earliestStart) })
        }
    }

    /** Re-validates a chosen slot at order time. */
    fun isBookable(date: LocalDate, startHour: Int, now: ZonedDateTime = ZonedDateTime.now()): Boolean =
        plan(now).firstOrNull { it.date == date }?.slots?.firstOrNull { it.startHour == startHour }?.available == true

    fun find(date: LocalDate, startHour: Int, now: ZonedDateTime = ZonedDateTime.now()): DeliverySlot? =
        plan(now).firstOrNull { it.date == date }?.slots?.firstOrNull { it.startHour == startHour }

    private fun slot(date: LocalDate, start: Int, end: Int, zone: ZoneId, earliest: ZonedDateTime): DeliverySlot {
        val startTime = date.atTime(start, 0).atZone(zone)
        val pastLead = startTime.isBefore(earliest)
        // Deterministic pseudo-capacity 0..6; some slots are always "full" to exercise the UI.
        val capacity = ((date.toEpochDay() * 31 + start * 7) % 7).toInt().let { if (it < 0) it + 7 else it }
        val left = if (pastLead) 0 else capacity
        return DeliverySlot(date, start, end, left, startTime.toInstant().toEpochMilli(), available = left > 0)
    }

    private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + date.dayOfMonth
    }
}
