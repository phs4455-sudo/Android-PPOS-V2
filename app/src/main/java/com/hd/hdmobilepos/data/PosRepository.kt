package com.hd.hdmobilepos.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PosRepository(private val dao: PosDao) {
    fun observeAreas(): Flow<List<Area>> = dao.observeAreas()

    fun observeTables(areaId: Long): Flow<List<TableSummary>> = dao.observeTableSummaries(areaId)

    fun observeCurrentOrderItems(tableId: Long): Flow<List<OrderItemRow>> = dao.observeCurrentOrderItems(tableId)

    fun observeActiveOrderDetails(tableId: Long): Flow<ActiveOrderDetails?> {
        return dao.observeActiveOrderItemFlats(tableId).map { flats ->
            if (flats.isEmpty()) {
                null
            } else {
                val first = flats.first()
                val lines = flats.mapNotNull { flat ->
                    val name = flat.nameSnapshot
                    val price = flat.priceSnapshot
                    val qty = flat.qty
                    if (name == null || price == null || qty == null) {
                        null
                    } else {
                        ActiveOrderLine(
                            itemName = name,
                            qty = qty,
                            lineTotal = price * qty
                        )
                    }
                }
                ActiveOrderDetails(
                    orderId = first.orderId,
                    status = first.status,
                    createdAt = first.createdAt,
                    orderTotalAmount = first.orderTotalAmount,
                    derivedTotalAmount = lines.sumOf { it.lineTotal },
                    items = lines
                )
            }
        }
    }

    suspend fun getOrderItemsOnce(tableId: Long): List<OrderItemRow> = dao.getOrderItemsOnce(tableId)

    suspend fun mergeTables(fromTableId: Long, toTableId: Long) {
        dao.mergeTables(fromTableId = fromTableId, toTableId = toTableId)
    }

    suspend fun seedIfNeeded() {
        if (dao.getAreaCount() > 0) return

        dao.upsertArea(Area(id = 1, name = "식당가 1층 홀", sortOrder = 1))
        dao.upsertArea(Area(id = 2, name = "룸", sortOrder = 2))

        dao.upsertTable(DiningTable(id = 1, areaId = 1, name = "T1", status = "OCCUPIED", capacity = 4))
        dao.upsertTable(DiningTable(id = 2, areaId = 1, name = "T2", status = "EMPTY", capacity = 4))
        dao.upsertTable(DiningTable(id = 3, areaId = 2, name = "R1", status = "OCCUPIED", capacity = 6))

        val firstOrderId = dao.insertOrder(
            Order(
                tableId = 1,
                status = "CREATED",
                totalAmount = 26000,
                createdAt = System.currentTimeMillis() - (35 * 60 * 1000)
            )
        )
        dao.insertOrderItems(
            listOf(
                OrderItem(orderId = firstOrderId, nameSnapshot = "불고기정식", priceSnapshot = 12000, qty = 1),
                OrderItem(orderId = firstOrderId, nameSnapshot = "비빔밥", priceSnapshot = 7000, qty = 2)
            )
        )

        val secondOrderId = dao.insertOrder(
            Order(
                tableId = 3,
                status = "CREATED",
                totalAmount = 18000,
                createdAt = System.currentTimeMillis() - (12 * 60 * 1000)
            )
        )
        dao.insertOrderItems(
            listOf(
                OrderItem(orderId = secondOrderId, nameSnapshot = "돈까스", priceSnapshot = 9000, qty = 2)
            )
        )
    }
}
