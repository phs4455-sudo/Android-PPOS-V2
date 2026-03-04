package com.hd.hdmobilepos.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PosRepository(private val dao: PosDao) {
    fun observeAreas(): Flow<List<Area>> = dao.observeAreas()

    suspend fun getAreasOnce(): List<Area> = dao.getAreasOnce()

    fun observeTables(areaId: Long): Flow<List<TableSummary>> = dao.observeTableSummaries(areaId)

    suspend fun getTablesOnce(areaId: Long): List<TableSummary> = dao.getTableSummariesOnce(areaId)

    suspend fun getTableCount(): Int = dao.getTableCount()

    suspend fun getAreaCount(): Int = dao.getAreaCount()

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

    suspend fun forceReseedDemoData() {
        dao.resetAllData()
        seedIfNeeded()
    }

    suspend fun seedIfNeeded() {
        dao.upsertArea(Area(id = 1, name = "식당가 1층 홀", sortOrder = 1))
        dao.upsertArea(Area(id = 2, name = "식당가 1층 룸", sortOrder = 2))
        dao.upsertArea(Area(id = 3, name = "식당가 2층 홀", sortOrder = 3))
        dao.upsertArea(Area(id = 4, name = "야외 테라스", sortOrder = 4))

        // 테스트 가능한 테이블 수를 충분히 확보
        val defaultTables = listOf(
            DiningTable(id = 1, areaId = 1, name = "T-1", status = "OCCUPIED", capacity = 4),
            DiningTable(id = 2, areaId = 1, name = "T-2", status = "EMPTY", capacity = 4),
            DiningTable(id = 3, areaId = 1, name = "T-3", status = "EMPTY", capacity = 4),
            DiningTable(id = 4, areaId = 1, name = "T-4", status = "OCCUPIED", capacity = 2),
            DiningTable(id = 5, areaId = 2, name = "R-1", status = "EMPTY", capacity = 6),
            DiningTable(id = 6, areaId = 2, name = "R-2", status = "OCCUPIED", capacity = 8),
            DiningTable(id = 7, areaId = 3, name = "2F-1", status = "EMPTY", capacity = 4),
            DiningTable(id = 8, areaId = 3, name = "2F-2", status = "EMPTY", capacity = 4),
            DiningTable(id = 9, areaId = 4, name = "TERR-1", status = "EMPTY", capacity = 4),
            DiningTable(id = 10, areaId = 4, name = "TERR-2", status = "DISABLED", capacity = 4)
        )
        defaultTables.forEach { dao.upsertTable(it) }

        if (dao.getOrderCount() > 0) return

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
                tableId = 4,
                status = "SENT",
                totalAmount = 18000,
                createdAt = System.currentTimeMillis() - (12 * 60 * 1000)
            )
        )
        dao.insertOrderItems(
            listOf(
                OrderItem(orderId = secondOrderId, nameSnapshot = "돈까스", priceSnapshot = 9000, qty = 2)
            )
        )

        val thirdOrderId = dao.insertOrder(
            Order(
                tableId = 6,
                status = "CREATED",
                totalAmount = 54000,
                createdAt = System.currentTimeMillis() - (22 * 60 * 1000)
            )
        )
        dao.insertOrderItems(
            listOf(
                OrderItem(orderId = thirdOrderId, nameSnapshot = "한우 안심 스테이크", priceSnapshot = 45000, qty = 1),
                OrderItem(orderId = thirdOrderId, nameSnapshot = "콜라", priceSnapshot = 3000, qty = 3)
            )
        )
    }

}
