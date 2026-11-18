package com.angadi.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun observe(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items")
    suspend fun getAll(): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE product_id = :productId")
    suspend fun remove(productId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CartItemEntity>)

    @Transaction
    suspend fun replaceAll(items: List<CartItemEntity>) {
        clear()
        upsertAll(items)
    }
}

@Dao
interface FavoriteDao {
    @Query("SELECT product_id FROM favorites")
    fun observe(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE product_id = :productId")
    suspend fun remove(productId: Int)
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM cached_products")
    suspend fun getAll(): List<CachedProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<CachedProductEntity>)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY created_at DESC, id DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun observeOrder(id: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrder(id: Long): OrderEntity?

    @Query("SELECT * FROM order_items WHERE order_id = :orderId ORDER BY id")
    suspend fun getItems(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE order_id = :orderId ORDER BY id")
    fun observeItems(orderId: Long): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items")
    fun observeAllItems(): Flow<List<OrderItemEntity>>

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Query("UPDATE orders SET status = :status, status_updated_at = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long)

    @Query("SELECT code FROM coupon_usage")
    suspend fun usedCoupons(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordCouponUse(usage: CouponUsageEntity)

    @Query("DELETE FROM coupon_usage WHERE order_id = :orderId")
    suspend fun releaseCoupon(orderId: Long)
}

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses ORDER BY is_default DESC, id DESC")
    fun observe(): Flow<List<AddressEntity>>

    @Query("SELECT * FROM addresses WHERE id = :id")
    suspend fun get(id: Long): AddressEntity?

    @Query("SELECT COUNT(*) FROM addresses")
    suspend fun count(): Int

    @Insert
    suspend fun insert(address: AddressEntity): Long

    @Update
    suspend fun update(address: AddressEntity)

    @Query("DELETE FROM addresses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE addresses SET is_default = (id = :id)")
    suspend fun setDefault(id: Long)

    @Query("SELECT id FROM addresses ORDER BY id DESC LIMIT 1")
    suspend fun newestId(): Long?
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE product_id = :productId ORDER BY updated_at DESC")
    fun observe(productId: Int): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE product_id = :productId LIMIT 1")
    suspend fun forProduct(productId: Int): ReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: ReviewEntity)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT product_id, AVG(rating) AS average, COUNT(*) AS total FROM reviews GROUP BY product_id")
    fun observeSummaries(): Flow<List<ReviewAggregate>>
}

data class ReviewAggregate(
    @androidx.room.ColumnInfo(name = "product_id") val productId: Int,
    val average: Double,
    val total: Int,
)

@Dao
interface RecentViewDao {
    @Query("SELECT product_id FROM recent_views ORDER BY viewed_at DESC")
    fun observeIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(view: RecentViewEntity)

    @Query("DELETE FROM recent_views WHERE product_id NOT IN (SELECT product_id FROM recent_views ORDER BY viewed_at DESC LIMIT :limit)")
    suspend fun trim(limit: Int)

    @Query("DELETE FROM recent_views")
    suspend fun clear()

    @Transaction
    suspend fun record(productId: Int, now: Long, limit: Int) {
        upsert(RecentViewEntity(productId, now))
        trim(limit)
    }
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT `query` FROM search_history ORDER BY searched_at DESC LIMIT :limit")
    fun observe(limit: Int): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` NOT IN (SELECT `query` FROM search_history ORDER BY searched_at DESC LIMIT :limit)")
    suspend fun trim(limit: Int)

    @Query("DELETE FROM search_history")
    suspend fun clear()

    @Transaction
    suspend fun record(query: String, now: Long, limit: Int) {
        upsert(SearchHistoryEntity(query, now))
        trim(limit)
    }
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: ProfileEntity)
}
