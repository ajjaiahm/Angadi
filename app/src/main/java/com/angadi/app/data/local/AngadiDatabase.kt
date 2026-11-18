package com.angadi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CartItemEntity::class, FavoriteEntity::class, CachedProductEntity::class, OrderEntity::class, OrderItemEntity::class,
        AddressEntity::class, CouponUsageEntity::class, ReviewEntity::class, RecentViewEntity::class,
        SearchHistoryEntity::class, ProfileEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AngadiDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun catalogDao(): CatalogDao
    abstract fun orderDao(): OrderDao
    abstract fun addressDao(): AddressDao
    abstract fun reviewDao(): ReviewDao
    abstract fun recentViewDao(): RecentViewDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val NAME = "angadi.db"

        fun create(context: Context): AngadiDatabase =
            Room.databaseBuilder(context.applicationContext, AngadiDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/**
 * Version 1 was a hand-written SQLiteOpenHelper schema (`cart`, `favorites`, `orders`). This migration
 * copies every existing row into the Room-managed tables before dropping the old ones, so baskets,
 * saved items and past orders survive the upgrade. Legacy orders have no line items, so they keep
 * their original total and item count and are shown as already delivered.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cart_items` (`product_id` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, PRIMARY KEY(`product_id`))")
        db.execSQL("INSERT OR REPLACE INTO cart_items (product_id, quantity) SELECT product_id, quantity FROM cart WHERE quantity > 0")
        db.execSQL("DROP TABLE cart")

        db.execSQL("ALTER TABLE favorites RENAME TO favorites_v1")
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`product_id` INTEGER NOT NULL, PRIMARY KEY(`product_id`))")
        db.execSQL("INSERT OR IGNORE INTO favorites (product_id) SELECT product_id FROM favorites_v1")
        db.execSQL("DROP TABLE favorites_v1")

        db.execSQL("ALTER TABLE orders RENAME TO orders_v1")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `orders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `created_at` INTEGER NOT NULL, " +
                "`status` TEXT NOT NULL, `status_updated_at` INTEGER NOT NULL, `subtotal` REAL NOT NULL, `delivery_fee` REAL NOT NULL, " +
                "`discount` REAL NOT NULL, `total` REAL NOT NULL, `coupon_code` TEXT, `item_count` INTEGER NOT NULL, " +
                "`payment_method` TEXT NOT NULL, `recipient_name` TEXT NOT NULL, `phone` TEXT NOT NULL, `address_label` TEXT NOT NULL, " +
                "`address_text` TEXT NOT NULL, `slot_label` TEXT NOT NULL, `slot_start` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO orders (id, created_at, status, status_updated_at, subtotal, delivery_fee, discount, total, coupon_code, item_count, " +
                "payment_method, recipient_name, phone, address_label, address_text, slot_label, slot_start) " +
                "SELECT id, created_at, 'DELIVERED', created_at, total, 0, 0, total, NULL, item_count, 'CASH_ON_DELIVERY', '', '', '', " +
                "'Address not recorded', 'Not recorded', created_at FROM orders_v1",
        )
        db.execSQL("DROP TABLE orders_v1")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `order_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `order_id` INTEGER NOT NULL, " +
                "`product_id` INTEGER NOT NULL, `title` TEXT NOT NULL, `unit` TEXT NOT NULL, `unit_price` REAL NOT NULL, `quantity` INTEGER NOT NULL, " +
                "`thumbnail` TEXT NOT NULL, `emoji` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                "FOREIGN KEY(`order_id`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_items_order_id` ON `order_items` (`order_id`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cached_products` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                "`category` TEXT NOT NULL, `unit_price` REAL NOT NULL, `discount_percent` REAL NOT NULL, `rating` REAL NOT NULL, `stock` INTEGER NOT NULL, " +
                "`thumbnail` TEXT NOT NULL, `unit` TEXT NOT NULL, `emoji` TEXT NOT NULL, `is_sample_api_price` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `addresses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, `recipient_name` TEXT NOT NULL, " +
                "`phone` TEXT NOT NULL, `house` TEXT NOT NULL, `street` TEXT NOT NULL, `area` TEXT NOT NULL, `city` TEXT NOT NULL, `state` TEXT NOT NULL, " +
                "`pin_code` TEXT NOT NULL, `is_default` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS `coupon_usage` (`code` TEXT NOT NULL, `used_at` INTEGER NOT NULL, `order_id` INTEGER NOT NULL, PRIMARY KEY(`code`))")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reviews` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `product_id` INTEGER NOT NULL, `author_name` TEXT NOT NULL, " +
                "`rating` INTEGER NOT NULL, `body` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reviews_product_id` ON `reviews` (`product_id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `recent_views` (`product_id` INTEGER NOT NULL, `viewed_at` INTEGER NOT NULL, PRIMARY KEY(`product_id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (`query` TEXT NOT NULL, `searched_at` INTEGER NOT NULL, PRIMARY KEY(`query`))")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                "`image_path` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
    }
}
