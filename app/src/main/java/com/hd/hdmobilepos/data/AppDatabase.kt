package com.hd.hdmobilepos.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Area::class, DiningTable::class, Order::class, OrderItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao
}
