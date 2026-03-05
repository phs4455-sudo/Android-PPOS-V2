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
                // 개발용 스키마 정합성 보장을 위해 1->2에서 테이블을 최신 스키마로 재생성한다.
                db.execSQL("PRAGMA foreign_keys=OFF")

                db.execSQL("DROP TABLE IF EXISTS `order_items`")
                db.execSQL("DROP TABLE IF EXISTS `orders`")
                db.execSQL("DROP TABLE IF EXISTS `tables`")
                db.execSQL("DROP TABLE IF EXISTS `areas`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `areas` (
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tables` (
                        `id` INTEGER NOT NULL,
                        `areaId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `capacity` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`areaId`) REFERENCES `areas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tables_areaId` ON `tables` (`areaId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `orders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tableId` INTEGER,
                        `status` TEXT NOT NULL,
                        `totalAmount` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`tableId`) REFERENCES `tables`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_tableId` ON `orders` (`tableId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `order_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `orderId` INTEGER NOT NULL,
                        `nameSnapshot` TEXT NOT NULL,
                        `priceSnapshot` INTEGER NOT NULL,
                        `qty` INTEGER NOT NULL,
                        FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_items_orderId` ON `order_items` (`orderId`)")

                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }
    }
}
