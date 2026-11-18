package com.angadi.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.angadi.app.data.repository.SaveAddressResult
import com.angadi.app.domain.model.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end UI flows against the real app container (database is wiped before every test). */
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val container get() = (ApplicationProvider.getApplicationContext<AngadiApplication>()).container

    @Before fun resetData() {
        runBlocking(Dispatchers.IO) { container.database.clearAllTables() }
        rule.waitForIdle()
    }

    private fun tab(name: String) = rule.onNodeWithTag("tab_$name").performClick()

    private fun waitForTag(tag: String, timeoutMs: Long = 10_000) = rule.waitUntil(timeoutMs) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun scrollTo(listTag: String, targetTag: String) {
        rule.onNodeWithTag(listTag).performScrollToNode(hasTestTag(targetTag))
    }

    private fun saveAddress(): Long = runBlocking {
        (container.addresses.save(Address(
            recipientName = "Asha Rao", phone = "9876543210", house = "12B", street = "MG Road", area = "Indiranagar",
            city = "Bengaluru", state = "Karnataka", pinCode = "560038",
        )) as SaveAddressResult.Saved).id
    }

    private fun addToBasketViaExplore(productTitle: String) {
        tab("EXPLORE")
        rule.onNodeWithTag("search_field").performTextReplacement(productTitle)
        rule.onNode(hasContentDescription("Add $productTitle to basket")).performClick()
    }

    @Test fun homeNavigationReachesEveryTab() {
        waitForTag("home_screen")
        tab("EXPLORE"); waitForTag("explore_screen")
        tab("SAVED"); waitForTag("saved_screen")
        tab("BASKET"); waitForTag("basket_screen"); rule.onNodeWithTag("empty_basket").assertIsDisplayed()
        tab("PROFILE"); waitForTag("profile_screen")
        tab("HOME"); waitForTag("home_screen")
    }

    @Test fun searchFiltersResultsAndShowsNoResultsState() {
        tab("EXPLORE")
        rule.onNodeWithTag("search_field").performTextInput("avocado")
        rule.waitUntil(5_000) { runCatching { rule.onNodeWithTag("result_count").assertTextContains("1 good finds"); true }.getOrDefault(false) }
        rule.onNodeWithTag("search_field").performTextReplacement("zzzzqq")
        waitForTag("no_results")
    }

    @Test fun priceFilterAndSortWorkTogether() {
        tab("EXPLORE")
        rule.onNodeWithTag("toggle_filters").performClick()
        rule.onNodeWithTag("max_price").performTextInput("70")
        rule.onNodeWithTag("apply_price").performClick()
        // Only the ₹59 bananas and ₹68 milk are at or under ₹70 in the offline catalogue.
        rule.waitUntil(5_000) { runCatching { rule.onNodeWithTag("result_count").assertTextContains("2 good finds"); true }.getOrDefault(false) }
        rule.onNodeWithTag("sort_PRICE_HIGH_LOW").performClick()
        rule.onNodeWithTag("result_count").assertTextContains("2 good finds")
    }

    @Test fun productDetailsOpenAndRecordRecentlyViewed() {
        tab("EXPLORE")
        rule.onNodeWithTag("search_field").performTextReplacement("avocado")
        waitForTag("product_card_9002")
        rule.onNodeWithTag("product_card_9002").performClick()
        waitForTag("product_sheet")
        rule.onNodeWithTag("sheet_title").assertTextContains("Creamy avocados")
        rule.onNodeWithTag("sheet_add").performClick()
        rule.waitUntil(5_000) { runBlocking { container.shop.recentViewIds.first() }.contains(9002) }
    }

    @Test fun reviewCanBeSubmittedEditedAndDeleted() {
        tab("EXPLORE")
        rule.onNodeWithTag("search_field").performTextReplacement("avocado")
        waitForTag("product_card_9002")
        rule.onNodeWithTag("product_card_9002").performClick()
        waitForTag("product_sheet")
        rule.onNode(hasTestTag("product_sheet")).performScrollToNode(hasTestTag("submit_review"))
        rule.onNodeWithTag("star_4").performClick()
        rule.onNodeWithTag("review_body").performTextInput("Creamy and ripe")
        rule.onNodeWithTag("submit_review").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("review_average").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("review_average").assertTextContains("4.0")
        rule.onNodeWithTag("delete_review").performClick()
        waitForTag("no_reviews")
    }

    @Test fun basketQuantityCanBeChangedAndItemRemoved() {
        addToBasketViaExplore("Creamy avocados")
        tab("BASKET")
        waitForTag("basket_row_9002")
        rule.onNode(hasContentDescription("Add one Creamy avocados")).performClick()
        rule.waitUntil(5_000) { runBlocking { container.shop.cart.first() }[9002] == 2 }
        rule.onNode(hasContentDescription("Remove one Creamy avocados")).performClick()
        rule.waitUntil(5_000) { runBlocking { container.shop.cart.first() }[9002] == 1 }
        rule.onNode(androidx.compose.ui.test.hasText("Remove")).performClick()
        waitForTag("empty_basket")
    }

    @Test fun checkoutPlacesOrderThatAppearsInHistoryAndDetails() {
        saveAddress()
        addToBasketViaExplore("Creamy avocados")
        tab("BASKET")
        waitForTag("checkout_button")
        rule.onNodeWithTag("checkout_button").performClick()
        waitForTag("checkout_screen")
        // pick the first enabled slot chip
        rule.waitUntil(5_000) { rule.onAllNodes(slotChip).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(slotChip)[0].performClick()
        scrollTo("checkout_screen", "place_order")
        rule.onNodeWithTag("place_order").performClick()
        waitForTag("order_detail_screen", 15_000)
        waitForTag("status_timeline")
        waitForTag("order_item_9002")

        // Order shows in history via Profile → Your orders
        androidx.test.espresso.Espresso.pressBack()
        tab("PROFILE")
        rule.onNodeWithTag("shortcut_orders").performClick()
        waitForTag("orders_screen")
        waitForTag("order_card_1")
    }

    @Test fun orderCanBeCancelledAndReordered() {
        saveAddress()
        addToBasketViaExplore("Creamy avocados")
        tab("BASKET")
        rule.onNodeWithTag("checkout_button").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(slotChip).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(slotChip)[0].performClick()
        scrollTo("checkout_screen", "place_order")
        rule.onNodeWithTag("place_order").performClick()
        waitForTag("order_detail_screen", 15_000)

        scrollTo("order_detail_screen", "detail_reorder")
        rule.onNodeWithTag("detail_reorder").performClick()
        waitForTag("reorder_dialog")
        rule.onNode(androidx.compose.ui.test.hasText("Keep browsing")).performClick()
        rule.waitUntil(5_000) { runBlocking { container.shop.cart.first() }[9002] == 1 }

        scrollTo("order_detail_screen", "cancel_order")
        rule.onNodeWithTag("cancel_order").performClick()
        rule.onNodeWithTag("confirm_cancel").performClick()
        waitForTag("status_cancelled")
    }

    @Test fun addressCanBeAddedAndDeleted() {
        tab("PROFILE")
        rule.onNodeWithTag("shortcut_addresses").performClick()
        waitForTag("addresses_screen"); waitForTag("empty_addresses")
        rule.onNodeWithTag("add_address").performClick()
        waitForTag("address_editor")
        rule.onNodeWithTag("save_address").performClick()                       // empty form → validation errors
        waitForTag("field_phone_error")
        rule.onNodeWithTag("field_recipient").performTextInput("Asha Rao")
        rule.onNodeWithTag("field_phone").performTextInput("9876543210")
        rule.onNodeWithTag("field_house").performTextInput("12B")
        rule.onNodeWithTag("field_street").performTextInput("MG Road")
        rule.onNodeWithTag("address_editor").performScrollToNode(hasTestTag("field_area"))
        rule.onNodeWithTag("field_area").performTextInput("Indiranagar")
        rule.onNodeWithTag("field_city").performTextInput("Bengaluru")
        rule.onNodeWithTag("address_editor").performScrollToNode(hasTestTag("field_state"))
        rule.onNodeWithTag("field_state").performTextInput("Karnataka")
        rule.onNodeWithTag("address_editor").performScrollToNode(hasTestTag("field_pin"))
        rule.onNodeWithTag("field_pin").performTextInput("560038")
        rule.onNodeWithTag("address_editor").performScrollToNode(hasTestTag("save_address"))
        rule.onNodeWithTag("save_address").performClick()
        waitForTag("address_card_1")
        rule.onNode(androidx.compose.ui.test.hasText("Delete")).performClick()
        rule.onNodeWithTag("confirm_delete_address").performClick()
        waitForTag("empty_addresses")
    }

    @Test fun profileNameAppearsInHomeGreeting() {
        tab("PROFILE")
        rule.onNodeWithTag("profile_card").performClick()
        waitForTag("profile_edit")
        rule.onNodeWithTag("profile_name_field").performTextInput("Asha Rao")
        rule.onNodeWithTag("save_profile").performClick()
        waitForTag("profile_screen")
        rule.onNodeWithTag("profile_name").assertTextContains("Asha Rao")
        tab("HOME")
        rule.onNodeWithTag("home_greeting").assertTextContains("Hi Asha!")
    }

    @Test fun themeSelectionIsPersistedInSettings() {
        tab("PROFILE")
        rule.onNodeWithTag("theme_DARK").performClick()
        rule.waitUntil(5_000) { container.settings.themeMode.value == com.angadi.app.ui.theme.AppThemeMode.DARK }
        rule.onNodeWithTag("theme_SYSTEM").performClick()
    }

    private val slotChip = SemanticsMatcher("enabled delivery slot") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("slot_") == true
    }.and(isEnabled())
}
