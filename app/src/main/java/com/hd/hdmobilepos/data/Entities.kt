package com.hd.hdmobilepos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "areas")
data class Area(
    @PrimaryKey val id: Long,
    val name: String,
    val sortOrder: Int
)

@Entity(
    tableName = "tables",
    foreignKeys = [
        ForeignKey(
            entity = Area::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("areaId")]
)
data class DiningTable(
    @PrimaryKey val id: Long,
    val areaId: Long,
    val name: String,
    val status: String,
    val capacity: Int
)

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = DiningTable::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tableId")]
)
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long?,
    val status: String,
    val totalAmount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val nameSnapshot: String,
    val priceSnapshot: Int,
    val qty: Int
)

data class TableSummary(
    val tableId: Long,
    val tableName: String,
    val status: String,
    val capacity: Int,
    val totalAmount: Int,
    val createdAt: Long?
)

data class OrderItemRow(
    val nameSnapshot: String,
    val priceSnapshot: Int,
    val qty: Int
)

data class ActiveOrderItemFlat(
    val orderId: Long,
    val status: String,
    val orderTotalAmount: Int,
    val createdAt: Long,
    val nameSnapshot: String?,
    val priceSnapshot: Int?,
    val qty: Int?
)

data class ActiveOrderLine(
    val itemName: String,
    val qty: Int,
    val lineTotal: Int
)

data class ActiveOrderDetails(
    val orderId: Long,
    val status: String,
    val createdAt: Long,
    val orderTotalAmount: Int,
    val derivedTotalAmount: Int,
    val items: List<ActiveOrderLine>
)
