package com.angadi.app

import com.angadi.app.domain.logic.AddressField
import com.angadi.app.domain.logic.AddressValidator
import com.angadi.app.domain.logic.CartLine
import com.angadi.app.domain.logic.DeliverySlotPlanner
import com.angadi.app.domain.logic.OrderPlanner
import com.angadi.app.domain.logic.PlanResult
import com.angadi.app.domain.logic.ReorderPlanner
import com.angadi.app.domain.model.Address
import com.angadi.app.domain.model.AddressLabel
import com.angadi.app.domain.model.OrderItem
import com.angadi.app.domain.model.OrderStatus
import com.angadi.app.domain.model.PaymentMethod
import com.angadi.app.domain.model.RatingSummary
import com.angadi.app.domain.model.Review
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AddressValidatorTest {
    private val valid = Address(
        label = AddressLabel.HOME, recipientName = "Asha Rao", phone = "98765 43210", house = "12B", street = "MG Road",
        area = "Indiranagar", city = "Bengaluru", state = "Karnataka", pinCode = "560038",
    )

    @Test fun completeAddressHasNoErrors() = assertTrue(AddressValidator.validate(valid).isEmpty())

    @Test fun emptyAddressFlagsEveryField() {
        assertEquals(AddressField.entries.toSet(), AddressValidator.validate(Address()).keys)
    }

    @Test fun phoneAcceptsCountryCodeAndRejectsBadNumbers() {
        assertEquals("9876543210", AddressValidator.normalizePhone("+91 98765-43210"))
        assertTrue(AddressValidator.validate(valid.copy(phone = "+919876543210")).isEmpty())
        assertTrue(AddressField.PHONE in AddressValidator.validate(valid.copy(phone = "12345")).keys)
        assertTrue(AddressField.PHONE in AddressValidator.validate(valid.copy(phone = "5876543210")).keys)  // must start 6-9
    }

    @Test fun pinCodeMustBeSixDigitsNotStartingWithZero() {
        assertTrue(AddressField.PIN_CODE in AddressValidator.validate(valid.copy(pinCode = "56003")).keys)
        assertTrue(AddressField.PIN_CODE in AddressValidator.validate(valid.copy(pinCode = "060038")).keys)
        assertTrue(AddressField.PIN_CODE in AddressValidator.validate(valid.copy(pinCode = "56003A")).keys)
    }

    @Test fun cleanTrimsAndNormalises() {
        val cleaned = AddressValidator.clean(valid.copy(recipientName = "  Asha Rao ", phone = "+91 98765 43210", city = " Bengaluru "))
        assertEquals("Asha Rao", cleaned.recipientName)
        assertEquals("9876543210", cleaned.phone)
        assertEquals("Bengaluru", cleaned.city)
    }

    @Test fun singleLineJoinsNonBlankPartsWithPin() {
        assertEquals("12B, MG Road, Indiranagar, Bengaluru, Karnataka – 560038", valid.singleLine)
    }
}

class DeliverySlotPlannerTest {
    @Test fun plansFourDaysWithFiveWindows() {
        val days = DeliverySlotPlanner.plan(TEST_NOW)
        assertEquals(DeliverySlotPlanner.DAYS_AHEAD, days.size)
        assertTrue(days.all { it.slots.size == 5 })
        assertEquals("Today", days[0].label)
        assertEquals("Tomorrow", days[1].label)
    }

    @Test fun slotsInsideLeadTimeAreUnavailable() {
        val today = DeliverySlotPlanner.plan(TEST_NOW).first()
        // Now is 09:00 with a 2h lead time: the 8-10 and 10-12 windows start before 11:00.
        assertFalse(today.slots.first { it.startHour == 8 }.available)
        assertFalse(today.slots.first { it.startHour == 10 }.available)
    }

    @Test fun availabilityIsDeterministic() {
        assertEquals(DeliverySlotPlanner.plan(TEST_NOW), DeliverySlotPlanner.plan(TEST_NOW))
    }

    @Test fun bookableMatchesPlan() {
        val slot = DeliverySlotPlanner.plan(TEST_NOW).flatMap { it.slots }.first { it.available }
        assertTrue(DeliverySlotPlanner.isBookable(slot.date, slot.startHour, TEST_NOW))
        assertFalse(DeliverySlotPlanner.isBookable(LocalDate.of(2020, 1, 1), 10, TEST_NOW))
    }

    @Test fun timeLabelsUseTwelveHourClock() {
        val slot = DeliverySlotPlanner.plan(TEST_NOW)[1].slots.first { it.startHour == 12 }
        assertEquals("12 PM – 2 PM", slot.timeLabel)
    }
}

class OrderPlannerTest {
    private val address = Address(
        id = 1, recipientName = "Asha Rao", phone = "9876543210", house = "12B", street = "MG Road", area = "Indiranagar",
        city = "Bengaluru", state = "Karnataka", pinCode = "560038", isDefault = true,
    )
    private val lines = listOf(CartLine(product(1, "Apples", 200.0), 2), CartLine(product(2, "Milk", 70.0, discount = 0.0), 1))
    private val slot = DeliverySlotPlanner.plan(TEST_NOW).flatMap { it.slots }.first { it.available }

    private fun plan(
        lines: List<CartLine> = this.lines, address: Address? = this.address, coupon: String? = null, used: Set<String> = emptySet(),
        date: LocalDate? = slot.date, hour: Int? = slot.startHour,
    ) = OrderPlanner.plan(lines, address, date, hour, coupon, PaymentMethod.CASH_ON_DELIVERY, used, TEST_NOW)

    @Test fun readyOrderSnapshotsProductsAndTotals() {
        val ready = plan(coupon = "WELCOME50") as PlanResult.Ready
        val d = ready.draft
        assertEquals(470.0, d.breakdown.subtotal, 0.0)
        assertEquals(50.0, d.breakdown.discount, 0.0)
        assertEquals(40.0, d.breakdown.deliveryFee, 0.0)
        assertEquals(460.0, d.breakdown.total, 0.0)
        assertEquals(listOf("Apples" to 200.0, "Milk" to 70.0), d.items.map { it.title to it.unitPrice })
        assertEquals(slot.startMillis, d.slotStart)
    }

    @Test fun emptyBasketIsRejected() = assertTrue(plan(lines = emptyList()) is PlanResult.Rejected)
    @Test fun missingAddressIsRejected() = assertTrue((plan(address = null) as PlanResult.Rejected).reason.contains("address"))
    @Test fun invalidAddressIsRejected() = assertTrue(plan(address = address.copy(pinCode = "1")) is PlanResult.Rejected)
    @Test fun missingSlotIsRejected() = assertTrue((plan(date = null, hour = null) as PlanResult.Rejected).reason.contains("slot"))

    @Test fun unavailableSlotIsRejected() {
        val today = DeliverySlotPlanner.plan(TEST_NOW).first()
        val blocked = today.slots.first { !it.available }
        assertTrue((plan(date = blocked.date, hour = blocked.startHour) as PlanResult.Rejected).reason.contains("slot"))
    }

    @Test fun unknownAndInvalidCouponsAreRejectedNotIgnored() {
        assertTrue((plan(coupon = "NOPE") as PlanResult.Rejected).reason.contains("exist"))
        assertTrue((plan(coupon = "FESTIVE20") as PlanResult.Rejected).reason.contains("expired"))
        assertTrue(plan(coupon = "WELCOME50", used = setOf("WELCOME50")) is PlanResult.Rejected)
    }

    @Test fun quantityAboveStockIsRejected() {
        val tooMany = listOf(CartLine(product(9, stock = 2), 3))
        assertTrue((plan(lines = tooMany) as PlanResult.Rejected).reason.contains("Only 2"))
    }
}

class ReorderPlannerTest {
    private fun item(id: Int, qty: Int, title: String = "Item $id") = OrderItem(productId = id, title = title, unit = "1", unitPrice = 1.0, quantity = qty)

    @Test fun usesCurrentCatalogPricesNotPurchasePrices() {
        val result = ReorderPlanner.plan(listOf(item(1, 2)), listOf(product(1, price = 250.0)), emptyMap())
        assertEquals(mapOf(1 to 2), result.newCart)
        assertEquals(250.0, result.added.single().currentPrice, 0.0)
    }

    @Test fun mergesWithExistingBasket() {
        val result = ReorderPlanner.plan(listOf(item(1, 2)), listOf(product(1)), mapOf(1 to 3, 7 to 1))
        assertEquals(mapOf(1 to 5, 7 to 1), result.newCart)
    }

    @Test fun missingAndOutOfStockProductsAreReported() {
        val result = ReorderPlanner.plan(listOf(item(1, 1, "Gone"), item(2, 1, "Empty"), item(3, 1, "Fine")), listOf(product(2, stock = 0), product(3)), emptyMap())
        assertEquals(mapOf(3 to 1), result.newCart)
        assertEquals(listOf("Gone", "Empty"), result.unavailable.map { it.title })
        assertEquals("Out of stock", result.unavailable.last().reason)
    }

    @Test fun quantityIsCappedByStockAndExplained() {
        val result = ReorderPlanner.plan(listOf(item(1, 8)), listOf(product(1, stock = 5)), mapOf(1 to 2))
        assertEquals(mapOf(1 to 5), result.newCart)
        val line = result.added.single()
        assertEquals(3, line.added)
        assertEquals(8, line.requested)
        assertEquals("Only 3 available", line.reason)
    }

    @Test fun itemAlreadyAtLimitIsUnavailable() {
        val result = ReorderPlanner.plan(listOf(item(1, 2)), listOf(product(1, stock = 4)), mapOf(1 to 4))
        assertFalse(result.hasAnything)
        assertEquals(1, result.unavailable.size)
    }
}

class OrderStatusTest {
    @Test fun progressionFollowsTheSixStepTimeline() {
        var status: OrderStatus? = OrderStatus.PLACED
        val seen = mutableListOf<OrderStatus>()
        while (status != null) { seen += status; status = status.next() }
        assertEquals(OrderStatus.timeline, seen)
    }

    @Test fun cancellationOnlyBeforePacking() {
        assertTrue(OrderStatus.PLACED.canCancel && OrderStatus.CONFIRMED.canCancel && OrderStatus.PREPARING.canCancel)
        assertFalse(OrderStatus.PACKED.canCancel || OrderStatus.OUT_FOR_DELIVERY.canCancel || OrderStatus.DELIVERED.canCancel || OrderStatus.CANCELLED.canCancel)
        assertNull(OrderStatus.CANCELLED.next())
    }

    @Test fun unknownStoredStatusFallsBackToPlaced() = assertEquals(OrderStatus.PLACED, OrderStatus.fromName("???"))
}

class RatingSummaryTest {
    private fun review(rating: Int) = Review(0, 1, "You", rating, "ok", 0, 0)

    @Test fun averageAndDistribution() {
        val s = RatingSummary.of(listOf(review(5), review(4), review(4), review(1)))
        assertEquals(3.5, s.average, 0.0)
        assertEquals(4, s.count)
        assertEquals(mapOf(5 to 1, 4 to 2, 3 to 0, 2 to 0, 1 to 1), s.distribution)
    }

    @Test fun emptySummaryIsZero() {
        val s = RatingSummary.of(emptyList())
        assertEquals(0.0, s.average, 0.0)
        assertEquals(0, s.count)
    }
}
