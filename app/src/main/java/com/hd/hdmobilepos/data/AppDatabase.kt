package com.hd.hdmobilepos.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Area::class, DiningTable::class, Order::class, OrderItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1 -> v2 스키마 변경이 없더라도 Room 버전 업그레이드 경로를 명시한다.
            }
        }
    }
}
