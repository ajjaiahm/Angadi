package com.angadi.app

import com.angadi.app.domain.logic.CartLine
import com.angadi.app.domain.logic.CartRules
import com.angadi.app.domain.logic.CouponCheck
import com.angadi.app.domain.logic.PricingEngine
import com.angadi.app.domain.model.Coupon
import com.angadi.app.domain.model.CouponCatalog
import com.angadi.app.domain.model.DiscountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingEngineTest {
    private fun lines(vararg pairs: Pair<Double, Int>) = pairs.mapIndexed { i, (price, qty) -> CartLine(product(i, price = price), qty) }

    @Test fun cartSubtotalMultipliesPriceByQuantity() {
        assertEquals(2 * 100.0 + 3 * 50.0, PricingEngine.subtotal(lines(100.0 to 2, 50.0 to 3)), 0.0)
    }

    @Test fun cartUsesDiscountedUnitPrice() {
        val discounted = CartLine(product(1, price = 200.0, discount = 10.0), 2)
        assertEquals(360.0, discounted.lineTotal, 0.0)
    }

    @Test fun deliveryFeeAppliesBelowThresholdAndIsFreeAtOrAbove() {
        assertEquals(40.0, PricingEngine.deliveryFee(698.0), 0.0)
        assertEquals(0.0, PricingEngine.deliveryFee(699.0), 0.0)
        assertEquals(0.0, PricingEngine.deliveryFee(0.0), 0.0)
    }

    @Test fun totalWithoutCouponIsSubtotalPlusDelivery() {
        val b = PricingEngine.calculate(lines(100.0 to 3), nowMillis = TEST_NOW_MILLIS)
        assertEquals(300.0, b.subtotal, 0.0)
        assertEquals(40.0, b.deliveryFee, 0.0)
        assertEquals(340.0, b.total, 0.0)
        assertEquals(3, b.itemCount)
    }

    @Test fun percentageCouponRespectsMaximumDiscount() {
        val b = PricingEngine.calculate(lines(1000.0 to 1), CouponCatalog.find("FRESH10"), TEST_NOW_MILLIS)
        assertEquals(100.0, b.discount, 0.0)            // 10% would be 100 -> exactly the cap
        val big = PricingEngine.calculate(lines(1000.0 to 3), CouponCatalog.find("FRESH10"), TEST_NOW_MILLIS)
        assertEquals(100.0, big.discount, 0.0)           // 10% of 3000 = 300, capped at 100
        assertEquals(3000.0 - 100.0, big.total, 0.0)     // free delivery above threshold
    }

    @Test fun percentageCouponBelowCapUsesPercentage() {
        val b = PricingEngine.calculate(lines(600.0 to 1), CouponCatalog.find("FRESH10"), TEST_NOW_MILLIS)
        assertEquals(60.0, b.discount, 0.0)
        assertEquals("FRESH10", b.couponCode)
    }

    @Test fun fixedCouponSubtractsFlatAmount() {
        val b = PricingEngine.calculate(lines(400.0 to 1), CouponCatalog.find("WELCOME50"), TEST_NOW_MILLIS)
        assertEquals(50.0, b.discount, 0.0)
        assertEquals(400.0 - 50.0 + 40.0, b.total, 0.0)
    }

    @Test fun couponBelowMinimumOrderValueIsRejectedWithGuidance() {
        val b = PricingEngine.calculate(lines(200.0 to 1), CouponCatalog.find("WELCOME50"), TEST_NOW_MILLIS)
        assertEquals(0.0, b.discount, 0.0)
        assertNull(b.couponCode)
        assertTrue(b.couponError!!.contains("99"))
    }

    @Test fun expiredCouponIsRejected() {
        val check = PricingEngine.validateCoupon(CouponCatalog.find("FESTIVE20")!!, 2000.0, TEST_NOW_MILLIS, emptySet())
        assertTrue(check is CouponCheck.Invalid)
        assertTrue((check as CouponCheck.Invalid).reason.contains("expired"))
    }

    @Test fun oneTimeCouponCannotBeReused() {
        val coupon = CouponCatalog.find("welcome50")!!
        val used = PricingEngine.validateCoupon(coupon, 500.0, TEST_NOW_MILLIS, setOf("WELCOME50"))
        assertTrue(used is CouponCheck.Invalid)
        assertTrue(PricingEngine.validateCoupon(coupon, 500.0, TEST_NOW_MILLIS, emptySet()) is CouponCheck.Valid)
    }

    @Test fun reusableCouponIgnoresUsageHistory() {
        val coupon = CouponCatalog.find("FRESH10")!!
        assertTrue(PricingEngine.validateCoupon(coupon, 800.0, TEST_NOW_MILLIS, setOf("FRESH10")) is CouponCheck.Valid)
    }

    @Test fun discountNeverExceedsSubtotal() {
        val huge = Coupon("HUGE", "t", "t", DiscountType.FIXED, 5000.0)
        val b = PricingEngine.calculate(lines(100.0 to 1), huge, TEST_NOW_MILLIS)
        assertEquals(100.0, b.discount, 0.0)
        assertEquals(40.0, b.total, 0.0)
    }

    @Test fun unknownCodeIsNotInCatalog() {
        assertNull(CouponCatalog.find("NOPE"))
        assertEquals("BIG150", CouponCatalog.find("  big150 ")?.code)
    }

    @Test fun deliveryIsJudgedBeforeDiscount() {
        // 720 subtotal qualifies for free delivery; a 100 discount must not bring the fee back.
        val b = PricingEngine.calculate(lines(720.0 to 1), CouponCatalog.find("FRESH10"), TEST_NOW_MILLIS)
        assertEquals(0.0, b.deliveryFee, 0.0)
        assertEquals(720.0 - 72.0, b.total, 0.0)
    }

    @Test fun maxQuantityIsLimitedByStockAndPerItemCap() {
        assertEquals(5, CartRules.maxQuantity(product(1, stock = 5)))
        assertEquals(CartRules.MAX_PER_ITEM, CartRules.maxQuantity(product(2, stock = 500)))
        assertEquals(0, CartRules.maxQuantity(product(3, stock = 0)))
    }
}
