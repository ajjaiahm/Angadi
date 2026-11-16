package com.angadi.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class OrderSummary(val id: Long, val total: Double, val itemCount: Int, val createdAt: Long)

class AngadiDatabase(context: Context) : SQLiteOpenHelper(context, "angadi.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE cart (product_id INTEGER PRIMARY KEY, quantity INTEGER NOT NULL CHECK(quantity > 0))")
        db.execSQL("CREATE TABLE favorites (product_id INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, total REAL NOT NULL, item_count INTEGER NOT NULL, created_at INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun loadCart(): Map<Int, Int> = readableDatabase.rawQuery("SELECT product_id, quantity FROM cart", null).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) put(cursor.getInt(0), cursor.getInt(1))
        }
    }

    fun saveCart(items: Map<Int, Int>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("cart", null, null)
            items.filterValues { it > 0 }.forEach { (id, quantity) ->
                db.insertWithOnConflict("cart", null, ContentValues().apply {
                    put("product_id", id)
                    put("quantity", quantity)
                }, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadFavorites(): Set<Int> = readableDatabase.rawQuery("SELECT product_id FROM favorites", null).use { cursor ->
        buildSet {
            while (cursor.moveToNext()) add(cursor.getInt(0))
        }
    }

    fun saveFavorites(ids: Set<Int>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("favorites", null, null)
            ids.forEach { id ->
                db.insertWithOnConflict("favorites", null, ContentValues().apply { put("product_id", id) }, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun createOrder(total: Double, itemCount: Int) {
        writableDatabase.insert("orders", null, ContentValues().apply {
            put("total", total)
            put("item_count", itemCount)
            put("created_at", System.currentTimeMillis())
        })
        saveCart(emptyMap())
    }

    fun loadOrders(): List<OrderSummary> = readableDatabase.rawQuery(
        "SELECT id, total, item_count, created_at FROM orders ORDER BY created_at DESC LIMIT 20", null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(OrderSummary(cursor.getLong(0), cursor.getDouble(1), cursor.getInt(2), cursor.getLong(3)))
        }
    }
}
