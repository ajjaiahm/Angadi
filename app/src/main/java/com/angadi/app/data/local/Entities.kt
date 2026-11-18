package com.angadi.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: Int,
    val quantity: Int,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey @ColumnInfo(name = "product_id") val productId: Int)

@Entity(tableName = "cached_products")
data class CachedProductEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val category: String,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    @ColumnInfo(name = "discount_percent") val discountPercent: Double,
    val rating: Double,
    val stock: Int,
    val thumbnail: String,
    val unit: String,
    val emoji: String,
    @ColumnInfo(name = "is_sample_api_price") val isSampleApiPrice: Boolean,
)

/** Columns `total`, `item_count`, `created_at` are inherited from database version 1. */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val status: String,
    @ColumnInfo(name = "status_updated_at") val statusUpdatedAt: Long,
    val subtotal: Double,
    @ColumnInfo(name = "delivery_fee") val deliveryFee: Double,
    val discount: Double,
    val total: Double,
    @ColumnInfo(name = "coupon_code") val couponCode: String?,
    @ColumnInfo(name = "item_count") val itemCount: Int,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "recipient_name") val recipientName: String,
    val phone: String,
    @ColumnInfo(name = "address_label") val addressLabel: String,
    @ColumnInfo(name = "address_text") val addressText: String,
    @ColumnInfo(name = "slot_label") val slotLabel: String,
    @ColumnInfo(name = "slot_start") val slotStart: Long,
)

@Entity(
    tableName = "order_items",
    foreignKeys = [ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["order_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("order_id")],
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "order_id") val orderId: Long,
    @ColumnInfo(name = "product_id") val productId: Int,
    val title: String,
    val unit: String,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    val quantity: Int,
    val thumbnail: String,
    val emoji: String,
    val category: String,
)

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    @ColumnInfo(name = "recipient_name") val recipientName: String,
    val phone: String,
    val house: String,
    val street: String,
    val area: String,
    val city: String,
    val state: String,
    @ColumnInfo(name = "pin_code") val pinCode: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
)

@Entity(tableName = "coupon_usage")
data class CouponUsageEntity(
    @PrimaryKey val code: String,
    @ColumnInfo(name = "used_at") val usedAt: Long,
    @ColumnInfo(name = "order_id") val orderId: Long,
)

@Entity(tableName = "reviews", indices = [Index(value = ["product_id"], unique = true)])
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "author_name") val authorName: String,
    val rating: Int,
    val body: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "recent_views")
data class RecentViewEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "viewed_at") val viewedAt: Long,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    @ColumnInfo(name = "searched_at") val searchedAt: Long,
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val email: String,
    val phone: String,
    @ColumnInfo(name = "image_path") val imagePath: String,
)
