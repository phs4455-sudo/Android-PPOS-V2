package com.hd.hdmobilepos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {
    @Query("SELECT * FROM areas ORDER BY sortOrder")
    fun observeAreas(): Flow<List<Area>>

    @Query(
        """
        SELECT t.id AS tableId,
               t.name AS tableName,
               t.status AS status,
               t.capacity AS capacity,
               COALESCE(o.totalAmount, 0) AS totalAmount,
               o.createdAt AS createdAt
        FROM tables t
        LEFT JOIN orders o ON o.id = (
            SELECT o2.id
            FROM orders o2
            WHERE o2.tableId = t.id
            ORDER BY o2.createdAt DESC, o2.id DESC
            LIMIT 1
        )
        WHERE t.areaId = :areaId
        ORDER BY t.id
        """
    )
    fun observeTableSummaries(areaId: Long): Flow<List<TableSummary>>

    @Query(
        """
        SELECT oi.nameSnapshot, oi.priceSnapshot, oi.qty
        FROM order_items oi
        INNER JOIN orders o ON o.id = oi.orderId
        WHERE o.id = (
            SELECT o2.id
            FROM orders o2
            WHERE o2.tableId = :tableId
            ORDER BY o2.createdAt DESC, o2.id DESC
            LIMIT 1
        )
        ORDER BY oi.id
        """
    )
    fun observeCurrentOrderItems(tableId: Long): Flow<List<OrderItemRow>>

    @Query(
        """
        SELECT o.id AS orderId,
               o.status AS status,
               o.totalAmount AS orderTotalAmount,
               o.createdAt AS createdAt,
               oi.nameSnapshot AS nameSnapshot,
               oi.priceSnapshot AS priceSnapshot,
               oi.qty AS qty
        FROM orders o
        LEFT JOIN order_items oi ON oi.orderId = o.id
        WHERE o.id = (
            SELECT o2.id
            FROM orders o2
            WHERE o2.tableId = :tableId AND o2.status IN ('CREATED', 'SENT')
            ORDER BY o2.createdAt DESC, o2.id DESC
            LIMIT 1
        )
        ORDER BY oi.id
        """
    )
    fun observeActiveOrderItemFlats(tableId: Long): Flow<List<ActiveOrderItemFlat>>

    @Query(
        """
        SELECT oi.nameSnapshot, oi.priceSnapshot, oi.qty
        FROM order_items oi
        INNER JOIN orders o ON o.id = oi.orderId
        WHERE o.id = (
            SELECT o2.id
            FROM orders o2
            WHERE o2.tableId = :tableId
            ORDER BY o2.createdAt DESC, o2.id DESC
            LIMIT 1
        )
        ORDER BY oi.id
        """
    )
    suspend fun getOrderItemsOnce(tableId: Long): List<OrderItemRow>

    @Query("SELECT COUNT(*) FROM areas")
    suspend fun getAreaCount(): Int

    @Query("SELECT COUNT(*) FROM tables")
    suspend fun getTableCount(): Int

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Query(
        """
        SELECT *
        FROM orders
        WHERE tableId = :tableId
        ORDER BY createdAt DESC, id DESC
        LIMIT 1
        """
    )
    suspend fun getLatestOrderForTable(tableId: Long): Order?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY id")
    suspend fun getOrderItemsByOrderId(orderId: Long): List<OrderItem>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItemsByOrderId(orderId: Long)

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Long)

    @Query("UPDATE orders SET tableId = :tableId, totalAmount = :totalAmount WHERE id = :orderId")
    suspend fun updateOrderTableAndTotal(orderId: Long, tableId: Long, totalAmount: Int)

    @Query("UPDATE tables SET status = :status WHERE id = :tableId")
    suspend fun updateTableStatus(tableId: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArea(area: Area)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTable(table: DiningTable)

    @Insert
    suspend fun insertOrder(order: Order): Long

    @Insert
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Transaction
    suspend fun mergeTables(fromTableId: Long, toTableId: Long) {
        if (fromTableId == toTableId) return

        val fromOrder = getLatestOrderForTable(fromTableId) ?: return
        val toOrder = getLatestOrderForTable(toTableId)
        val fromItems = getOrderItemsByOrderId(fromOrder.id)

        if (toOrder == null) {
            updateOrderTableAndTotal(
                orderId = fromOrder.id,
                tableId = toTableId,
                totalAmount = fromItems.sumOf { it.priceSnapshot * it.qty }
            )
        } else {
            val toItems = getOrderItemsByOrderId(toOrder.id)
            val merged = linkedMapOf<Pair<String, Int>, Int>()

            toItems.forEach { item ->
                val key = item.nameSnapshot to item.priceSnapshot
                merged[key] = (merged[key] ?: 0) + item.qty
            }
            fromItems.forEach { item ->
                val key = item.nameSnapshot to item.priceSnapshot
                merged[key] = (merged[key] ?: 0) + item.qty
            }

            val mergedItems = merged.map { (key, qty) ->
                OrderItem(
                    orderId = toOrder.id,
                    nameSnapshot = key.first,
                    priceSnapshot = key.second,
                    qty = qty
                )
            }

            deleteOrderItemsByOrderId(toOrder.id)
            if (mergedItems.isNotEmpty()) {
                insertOrderItems(mergedItems)
            }
            val mergedTotal = mergedItems.sumOf { it.priceSnapshot * it.qty }
            updateOrderTableAndTotal(toOrder.id, toTableId, mergedTotal)
            deleteOrderById(fromOrder.id)
        }

        updateTableStatus(fromTableId, "EMPTY")
        updateTableStatus(toTableId, "OCCUPIED")
    }
}
