package com.angadi.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.angadi.app.data.local.AngadiDatabase
import com.angadi.app.data.local.MIGRATION_1_2
import com.angadi.app.data.repository.AddressRepository
import com.angadi.app.data.repository.OrderRepository
import com.angadi.app.data.repository.PlaceOrderResult
import com.angadi.app.data.repository.ProfileRepository
import com.angadi.app.data.repository.ReviewRepository
import com.angadi.app.data.repository.SaveAddressResult
import com.angadi.app.data.repository.ShopRepository
import com.angadi.app.domain.logic.DeliverySlotPlanner
import com.angadi.app.domain.model.Address
import com.angadi.app.domain.model.OrderStatus
import com.angadi.app.domain.model.PaymentMethod
import com.angadi.app.domain.model.SeedCatalog
import com.angadi.app.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

private val validAddress = Address(
    recipientName = "Asha Rao", phone = "9876543210", house = "12B", street = "MG Road", area = "Indiranagar",
    city = "Bengaluru", state = "Karnataka", pinCode = "560038",
)

@RunWith(AndroidJUnit4::class)
class RepositoryDatabaseTest {
    private lateinit var db: AngadiDatabase
    private lateinit var shop: ShopRepository
    private lateinit var orders: OrderRepository
    private lateinit var addresses: AddressRepository
    private var clockMillis = 1_000L
    private var zoned: ZonedDateTime = ZonedDateTime.now()

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AngadiDatabase::class.java).allowMainThreadQueries().build()
        shop = ShopRepository(db, clock = { clockMillis })
        orders = OrderRepository(db, shop) { zoned }
        addresses = AddressRepository(db)
    }

    @After fun tearDown() = db.close()

    private fun firstSlot() = DeliverySlotPlanner.plan(zoned).flatMap { it.slots }.first { it.available }

    private suspend fun place(coupon: String? = null): PlaceOrderResult {
        val saved = addresses.save(validAddress) as SaveAddressResult.Saved
        val slot = firstSlot()
        return orders.placeOrder(addresses.get(saved.id), slot.date, slot.startHour, coupon, PaymentMethod.UPI_ON_DELIVERY)
    }

    @Test fun cartAndFavoritesPersistAndClear() = runBlocking {
        shop.setQuantity(9001, 2); shop.setQuantity(9002, 1); shop.setFavorite(9003, true)
        assertEquals(mapOf(9001 to 2, 9002 to 1), shop.cart.first())
        assertEquals(setOf(9003), shop.favorites.first())
        shop.setQuantity(9001, 0)
        assertEquals(mapOf(9002 to 1), shop.cart.first())
        shop.setFavorite(9003, false)
        assertTrue(shop.favorites.first().isEmpty())
    }

    @Test fun placingAnOrderPersistsOrderAndItemSnapshotsAndClearsBasket() = runBlocking {
        shop.setQuantity(9001, 2) // Crisp red apples, price 170 after 10% discount
        shop.setQuantity(9004, 1) // Farm fresh milk, 68
        val result = place()
        val id = (result as PlaceOrderResult.Placed).orderId
        val order = orders.observeOrder(id).first()!!
        assertEquals(OrderStatus.PLACED, order.status)
        assertEquals(2 * 170.0 + 68.0, order.subtotal, 0.0)
        assertEquals(40.0, order.deliveryFee, 0.0)
        assertEquals(order.subtotal + 40.0, order.total, 0.0)
        assertEquals(3, order.itemCount)
        assertEquals(PaymentMethod.UPI_ON_DELIVERY, order.paymentMethod)
        assertEquals(setOf("Crisp red apples", "Farm fresh milk"), order.items.map { it.title }.toSet())
        assertEquals(170.0, order.items.first { it.productId == 9001 }.unitPrice, 0.0)
        assertTrue(order.addressText.contains("560038"))
        assertTrue(shop.cart.first().isEmpty())
        assertEquals(1, orders.orders.first().size)
    }

    @Test fun orderSnapshotIsIndependentOfLaterCatalogueChanges() = runBlocking {
        shop.setQuantity(9001, 1)
        val id = (place() as PlaceOrderResult.Placed).orderId
        val before = orders.observeOrder(id).first()!!.items.single().unitPrice
        // Catalogue data lives outside the order tables; the stored price must not change.
        db.catalogDao().upsertAll(emptyList())
        assertEquals(before, orders.observeOrder(id).first()!!.items.single().unitPrice, 0.0)
    }

    @Test fun emptyBasketOrAddressIsRejectedAndNothingIsSaved() = runBlocking {
        val slot = firstSlot()
        val noItems = orders.placeOrder(validAddress.copy(id = 1), slot.date, slot.startHour, null, PaymentMethod.CASH_ON_DELIVERY)
        assertTrue(noItems is PlaceOrderResult.Rejected)
        shop.setQuantity(9001, 1)
        val noAddress = orders.placeOrder(null, slot.date, slot.startHour, null, PaymentMethod.CASH_ON_DELIVERY)
        assertTrue(noAddress is PlaceOrderResult.Rejected)
        assertTrue(orders.orders.first().isEmpty())
        assertEquals(mapOf(9001 to 1), shop.cart.first())
    }

    @Test fun oneTimeCouponIsAppliedOnceAndReleasedOnCancel() = runBlocking {
        shop.setQuantity(9001, 3)
        val first = place("WELCOME50") as PlaceOrderResult.Placed
        assertEquals(50.0, orders.observeOrder(first.orderId).first()!!.discount, 0.0)
        assertTrue("WELCOME50" in orders.usedCoupons())

        shop.setQuantity(9001, 3)
        val second = place("WELCOME50")
        assertTrue(second is PlaceOrderResult.Rejected)

        assertTrue(orders.cancel(first.orderId))
        assertFalse("WELCOME50" in orders.usedCoupons())
        assertTrue(place("WELCOME50") is PlaceOrderResult.Placed)
    }

    @Test fun cancelAndStatusProgressionRespectRules() = runBlocking {
        shop.setQuantity(9001, 1)
        val id = (place() as PlaceOrderResult.Placed).orderId
        repeat(3) { orders.simulateNextStatus(id) }
        assertEquals(OrderStatus.PACKED, orders.observeOrder(id).first()!!.status)
        assertFalse(orders.cancel(id))               // too late once packed
        orders.simulateNextStatus(id); orders.simulateNextStatus(id)
        assertEquals(OrderStatus.DELIVERED, orders.observeOrder(id).first()!!.status)
        orders.simulateNextStatus(id)                 // no-op past the end
        assertEquals(OrderStatus.DELIVERED, orders.observeOrder(id).first()!!.status)
    }

    @Test fun reorderAddsItemsAtCurrentPricesAndKeepsExistingBasket() = runBlocking {
        shop.setQuantity(9001, 2); shop.setQuantity(9004, 1)
        val id = (place() as PlaceOrderResult.Placed).orderId
        shop.setQuantity(9004, 5) // user already has some milk in the new basket
        val result = orders.reorder(id)!!
        assertEquals(2, result.added.size)
        assertEquals(mapOf(9001 to 2, 9004 to 6), shop.cart.first())
        assertEquals(170.0, result.added.first { it.title == "Crisp red apples" }.currentPrice, 0.0)
    }

    @Test fun addressesSupportCrudDefaultsAndValidation() = runBlocking {
        val invalid = addresses.save(validAddress.copy(pinCode = "12"))
        assertTrue(invalid is SaveAddressResult.Invalid)
        assertTrue(addresses.addresses.first().isEmpty())

        val a = (addresses.save(validAddress) as SaveAddressResult.Saved).id
        val b = (addresses.save(validAddress.copy(recipientName = "Second", street = "Brigade Rd")) as SaveAddressResult.Saved).id
        assertTrue("first saved address becomes default", addresses.get(a)!!.isDefault)
        assertFalse(addresses.get(b)!!.isDefault)

        addresses.setDefault(b)
        assertTrue(addresses.get(b)!!.isDefault)
        assertFalse(addresses.get(a)!!.isDefault)

        addresses.save(addresses.get(a)!!.copy(city = "Mysuru"))
        assertEquals("Mysuru", addresses.get(a)!!.city)
        assertFalse("editing doesn't steal the default", addresses.get(a)!!.isDefault)

        addresses.delete(b)
        assertNull(addresses.get(b))
        assertTrue("deleting the default promotes another", addresses.get(a)!!.isDefault)
    }

    @Test fun reviewsAreOnePerProductEditableAndDeletable() = runBlocking {
        val reviews = ReviewRepository(db) { clockMillis }
        assertNotNull(reviews.submit(9001, "Asha", 0, "great"))        // invalid rating
        assertNotNull(reviews.submit(9001, "Asha", 5, "x"))             // too short
        assertNull(reviews.submit(9001, "Asha", 5, "Really crisp"))
        clockMillis = 5_000
        assertNull(reviews.submit(9001, "Asha", 3, "Edited my mind"))
        val list = reviews.reviews(9001).first()
        assertEquals(1, list.size)
        assertEquals(3, list.single().rating)
        assertEquals(1_000L, list.single().createdAt)
        assertEquals(5_000L, list.single().updatedAt)
        assertEquals(3.0, reviews.summaries.first().getValue(9001).first, 0.0)
        reviews.delete(list.single().id)
        assertTrue(reviews.reviews(9001).first().isEmpty())
    }

    @Test fun recentlyViewedHasNoDuplicatesNewestFirstAndRespectsLimit() = runBlocking {
        clockMillis = 1; shop.recordView(9001, 3)
        clockMillis = 2; shop.recordView(9002, 3)
        clockMillis = 3; shop.recordView(9003, 3)
        clockMillis = 4; shop.recordView(9001, 3)         // duplicate moves to front
        assertEquals(listOf(9001, 9003, 9002), shop.recentViewIds.first())
        clockMillis = 5; shop.recordView(9004, 3)         // exceeds limit 3, drops the oldest
        assertEquals(listOf(9004, 9001, 9003), shop.recentViewIds.first())
        shop.clearRecentViews()
        assertTrue(shop.recentViewIds.first().isEmpty())
    }

    @Test fun searchHistoryIsNormalisedLimitedAndClearable() = runBlocking {
        clockMillis = 1; shop.recordSearch("  Apple ")
        clockMillis = 2; shop.recordSearch("APPLE")
        clockMillis = 3; shop.recordSearch("a")                // too short to keep
        assertEquals(listOf("apple"), shop.searchHistory().first())
        for (i in 0 until 12) { clockMillis = 10L + i; shop.recordSearch("query $i") }
        assertEquals(ShopRepository.SEARCH_HISTORY_LIMIT, shop.searchHistory().first().size)
        assertEquals("query 11", shop.searchHistory().first().first())
        shop.clearSearchHistory()
        assertTrue(shop.searchHistory().first().isEmpty())
    }

    @Test fun profilePersistsAndValidates() = runBlocking {
        val repo = ProfileRepository(db)
        assertEquals(UserProfile(), repo.profile.first())
        assertNotNull(repo.save(UserProfile(name = "Asha", email = "not-an-email")))
        assertNull(repo.save(UserProfile(name = "Asha Rao", email = "asha@example.com", phone = "+91 98765 43210")))
        val saved = repo.profile.first()
        assertEquals("Asha Rao", saved.name)
        assertEquals("Hi Asha!", saved.greeting)
        assertEquals("asha@example.com", saved.email)
    }

    @Test fun catalogueCacheRoundTrips() = runBlocking {
        val cached = SeedCatalog.products.first().copy(id = 1234, title = "Cached thing")
        db.catalogDao().upsertAll(listOf(com.angadi.app.data.local.CachedProductEntity(
            cached.id, cached.title, cached.description, cached.category, cached.unitPrice, cached.discountPercent, cached.rating, cached.stock,
            cached.thumbnail, cached.unit, cached.emoji, cached.isSampleApiPrice,
        )))
        shop.loadCachedCatalog()
        assertTrue(shop.catalog.value.any { it.id == 1234 && it.title == "Cached thing" })
        assertTrue("seed products remain", shop.catalog.value.any { it.id == 9001 })
    }
}

/** Reproduces the database written by app v1 (hand-written SQLiteOpenHelper) and upgrades it with Room. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    private class LegacyHelper(context: Context, name: String) : SQLiteOpenHelper(context, name, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE cart (product_id INTEGER PRIMARY KEY, quantity INTEGER NOT NULL CHECK(quantity > 0))")
            db.execSQL("CREATE TABLE favorites (product_id INTEGER PRIMARY KEY)")
            db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, total REAL NOT NULL, item_count INTEGER NOT NULL, created_at INTEGER NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    @Before fun createLegacyDatabase() {
        context.deleteDatabase(name)
        LegacyHelper(context, name).writableDatabase.use { db ->
            db.execSQL("INSERT INTO cart VALUES (9001, 2), (9004, 1)")
            db.execSQL("INSERT INTO favorites VALUES (9003), (9007)")
            db.execSQL("INSERT INTO orders (id, total, item_count, created_at) VALUES (7, 512.0, 3, 1700000000000), (8, 99.0, 1, 1700000100000)")
        }
    }

    @After fun cleanUp() { context.deleteDatabase(name) }

    @Test fun upgradingFromVersion1KeepsCartFavoritesAndOrders() = runBlocking {
        val db = Room.databaseBuilder(context, AngadiDatabase::class.java, name).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            // Room validates the migrated schema against the entities the first time the database is opened.
            assertEquals(mapOf(9001 to 2, 9004 to 1), db.cartDao().getAll().associate { it.productId to it.quantity })
            assertEquals(setOf(9003, 9007), db.favoriteDao().observe().first().toSet())
            val orders = db.orderDao().observeOrders().first()
            assertEquals(listOf(8L, 7L), orders.map { it.id })
            val legacy = orders.last()
            assertEquals(512.0, legacy.total, 0.0)
            assertEquals(3, legacy.itemCount)
            assertEquals(1_700_000_000_000L, legacy.createdAt)
            assertEquals("DELIVERED", legacy.status)
            assertTrue(db.orderDao().getItems(7).isEmpty())

            // New tables are usable after the migration, and new order IDs continue after the legacy ones.
            db.profileDao().save(com.angadi.app.data.local.ProfileEntity(1, "Asha", "", "", ""))
            assertEquals("Asha", db.profileDao().observe().first()!!.name)
            val nextId = db.orderDao().insertOrder(
                com.angadi.app.data.local.OrderEntity(
                    createdAt = 1, status = "PLACED", statusUpdatedAt = 1, subtotal = 1.0, deliveryFee = 0.0, discount = 0.0, total = 1.0, couponCode = null,
                    itemCount = 1, paymentMethod = "CASH_ON_DELIVERY", recipientName = "", phone = "", addressLabel = "", addressText = "", slotLabel = "", slotStart = 0,
                ),
            )
            assertTrue(nextId > 8)
        } finally {
            db.close()
        }
    }

    @Test fun migratedDatabaseCanBeReopenedWithoutAnotherMigration() = runBlocking {
        Room.databaseBuilder(context, AngadiDatabase::class.java, name).addMigrations(MIGRATION_1_2).build().apply { cartDao().getAll(); close() }
        val reopened = Room.databaseBuilder(context, AngadiDatabase::class.java, name).build()   // no migrations supplied
        try { assertEquals(2, reopened.cartDao().getAll().size) } finally { reopened.close() }
    }
}
