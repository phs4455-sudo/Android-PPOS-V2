package com.hd.hdmobilepos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.derivedStateOf
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.hd.hdmobilepos.data.ActiveOrderDetails
import com.hd.hdmobilepos.data.Area
import com.hd.hdmobilepos.data.AppDatabase
import com.hd.hdmobilepos.data.PosRepository
import com.hd.hdmobilepos.data.TableSummary
import com.hd.hdmobilepos.ui.theme.PPOSTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ppos.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        val repo = PosRepository(db.posDao())

        setContent {
            PPOSTheme {
                val factory = remember {
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
                    }
                }
                val vm: MainViewModel = viewModel(factory = factory)
                MainNavHost(vm = vm)
            }
        }
    }
}

data class RightPanelItemUi(
    val orderItemId: Long,
    val itemName: String,
    val priceSnapshot: Int,
    val qty: Int,
    val lineTotal: Int
)

data class RightOrderPanelUi(
    val orderId: Long,
    val orderStatus: String,
    val elapsedLabel: String,
    val items: List<RightPanelItemUi>,
    val orderTotalAmount: Int,
    val derivedTotalAmount: Int,
    val isTotalMismatch: Boolean
)


data class PaymentOrderItemUi(
    val name: String,
    val qty: Int,
    val price: Int
)

data class PaymentOrderSnapshot(
    val tableId: Long?,
    val tableName: String,
    val items: List<PaymentOrderItemUi>,
    val totalAmount: Int,
    val receivedAmount: Int
)

enum class PaymentMethod(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CARD_MOBILE("카드/모바일", Icons.Filled.CreditCard),
    CASH("현금", Icons.Filled.AccountBalanceWallet),
    GIFT_CARD("상품권", Icons.Filled.CardGiftcard),
    H_POINT("H-POINT", Icons.Filled.Loyalty),
    PARTNER_CARD("제휴카드", Icons.Filled.CreditCard),
    SIMPLE_PAY("간편결제", Icons.Filled.PhoneAndroid)
}

data class PaymentUiState(
    val tableName: String = "",
    val items: List<PaymentOrderItemUi> = emptyList(),
    val totalAmount: Int = 0,
    val receivedAmount: Int = 0,
    val selectedMethod: PaymentMethod? = null,
    val keypadInput: String = "",
    val methodAmounts: Map<PaymentMethod, Int> = emptyMap()
)

class PaymentViewModel(initialSnapshot: PaymentOrderSnapshot) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PaymentUiState(
            tableName = initialSnapshot.tableName,
            items = initialSnapshot.items,
            totalAmount = initialSnapshot.totalAmount,
            receivedAmount = initialSnapshot.receivedAmount
        )
    )
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method, keypadInput = "") }
    }

    fun onKeypadPressed(key: String) {
        when (key) {
            "Clear" -> _uiState.update { it.copy(keypadInput = "") }
            "Backspace" -> _uiState.update { state ->
                state.copy(keypadInput = state.keypadInput.dropLast(1))
            }
            "만원" -> _uiState.update { state ->
                val next = if (state.keypadInput.isBlank()) "10000" else state.keypadInput + "0000"
                state.copy(keypadInput = next)
            }
            "Enter" -> applyEnteredAmount()
            else -> {
                if (key.all { it.isDigit() }) {
                    _uiState.update { state -> state.copy(keypadInput = state.keypadInput + key) }
                }
            }
        }
    }

    private fun applyEnteredAmount() {
        _uiState.update { state ->
            val method = state.selectedMethod ?: return@update state
            val amount = state.keypadInput.toIntOrNull() ?: 0
            val updatedMap = state.methodAmounts.toMutableMap().apply { this[method] = amount }
            state.copy(
                methodAmounts = updatedMap,
                receivedAmount = updatedMap.values.sum(),
                keypadInput = ""
            )
        }
    }

    fun removePaymentMethod(method: PaymentMethod) {
        _uiState.update { state ->
            val updatedMap = state.methodAmounts.toMutableMap().apply { remove(method) }
            state.copy(
                methodAmounts = updatedMap,
                receivedAmount = updatedMap.values.sum()
            )
        }
    }
}

enum class UiMode {
    NORMAL,
    SELECT_TARGET_FOR_MOVE,
    SELECT_TARGET_FOR_MERGE
}

enum class TableActionType {
    MOVE,
    MERGE
}

data class PendingTableAction(
    val type: TableActionType,
    val sourceTableId: Long,
    val targetTableId: Long
)

data class MainUiState(
    val areas: List<Area> = emptyList(),
    val selectedAreaId: Long? = null,
    val tables: List<TableSummary> = emptyList(),
    val selectedTableId: Long? = null,
    val selectedSourceTableId: Long? = null,
    val selectedTargetTableId: Long? = null,
    val uiMode: UiMode = UiMode.NORMAL,
    val pendingAction: PendingTableAction? = null,
    val snackbarMessage: String? = null,
    val rightPanel: RightOrderPanelUi? = null,
    val isReseeding: Boolean = false,
    val reseedMessage: String? = null
)

class MainViewModel(private val repository: PosRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var tableObserverJob: Job? = null
    private var rightPanelObserverJob: Job? = null
    private val canceledPriceMemory = mutableMapOf<Long, Int>()

    init {
        viewModelScope.launch {
            bootstrapData()
            repository.observeAreas().collectLatest { areas ->
                val selectedArea = _uiState.value.selectedAreaId ?: repository.findFirstAreaIdWithTables()
                _uiState.update { it.copy(areas = areas, selectedAreaId = selectedArea) }
                selectedArea?.let(::observeTables)
            }
        }
    }

    fun selectArea(areaId: Long) {
        _uiState.update {
            it.copy(
                selectedAreaId = areaId,
                selectedTableId = null,
                selectedSourceTableId = null,
                selectedTargetTableId = null,
                uiMode = UiMode.NORMAL,
                pendingAction = null,
                rightPanel = null
            )
        }
        observeTables(areaId)
    }

    fun selectTable(tableId: Long) {
        _uiState.update {
            it.copy(
                selectedTableId = tableId,
                selectedSourceTableId = tableId,
                selectedTargetTableId = null,
                uiMode = UiMode.NORMAL,
                pendingAction = null
            )
        }
        observeRightPanel(tableId)
    }


    fun buildPaymentSnapshot(tableId: Long?): PaymentOrderSnapshot {
        val state = _uiState.value
        val resolvedTableId = tableId ?: state.selectedTableId
        val table = state.tables.firstOrNull { it.tableId == resolvedTableId }
        val panel = state.rightPanel
        val items = panel?.items?.map { PaymentOrderItemUi(name = it.itemName, qty = it.qty, price = it.lineTotal) }.orEmpty()
        val total = panel?.derivedTotalAmount ?: 0
        return PaymentOrderSnapshot(
            tableId = resolvedTableId,
            tableName = table?.tableName ?: "선택된 테이블 없음",
            items = items,
            totalAmount = total,
            receivedAmount = total
        )
    }

    fun startMoveMode() {
        val sourceId = _uiState.value.selectedTableId
        if (sourceId == null) {
            pushSnackbar("먼저 원본 테이블을 선택하세요")
            return
        }
        if (_uiState.value.rightPanel == null) {
            pushSnackbar("이동할 활성 주문이 없습니다")
            return
        }
        _uiState.update {
            it.copy(
                uiMode = UiMode.SELECT_TARGET_FOR_MOVE,
                selectedSourceTableId = sourceId,
                selectedTargetTableId = null,
                pendingAction = null
            )
        }
    }

    fun startMergeMode() {
        val sourceId = _uiState.value.selectedTableId
        if (sourceId == null) {
            pushSnackbar("먼저 원본 테이블을 선택하세요")
            return
        }
        if (_uiState.value.rightPanel == null) {
            pushSnackbar("합석할 활성 주문이 없습니다")
            return
        }
        _uiState.update {
            it.copy(
                uiMode = UiMode.SELECT_TARGET_FOR_MERGE,
                selectedSourceTableId = sourceId,
                selectedTargetTableId = null,
                pendingAction = null
            )
        }
    }

    fun onTableTileClicked(tableId: Long, status: String) {
        val state = _uiState.value
        if (state.uiMode == UiMode.NORMAL) {
            selectTable(tableId)
            return
        }

        val sourceId = state.selectedSourceTableId
        if (sourceId == null) {
            _uiState.update { it.copy(uiMode = UiMode.NORMAL) }
            return
        }
        if (status == "DISABLED") {
            pushSnackbar("사용불가 테이블은 선택할 수 없습니다")
            return
        }
        if (sourceId == tableId) {
            pushSnackbar("다른 대상 테이블을 선택하세요")
            return
        }

        viewModelScope.launch {
            if (state.uiMode == UiMode.SELECT_TARGET_FOR_MERGE && !repository.hasActiveOrder(tableId)) {
                pushSnackbar("합석은 양쪽 테이블에 활성 주문이 있어야 합니다")
                return@launch
            }
            val type = if (state.uiMode == UiMode.SELECT_TARGET_FOR_MOVE) TableActionType.MOVE else TableActionType.MERGE
            _uiState.update {
                it.copy(
                    selectedTargetTableId = tableId,
                    pendingAction = PendingTableAction(
                        type = type,
                        sourceTableId = sourceId,
                        targetTableId = tableId
                    )
                )
            }
        }
    }

    fun dismissPendingAction() {
        _uiState.update { it.copy(pendingAction = null, selectedTargetTableId = null, uiMode = UiMode.NORMAL) }
    }

    fun confirmPendingAction() {
        val action = _uiState.value.pendingAction ?: return
        viewModelScope.launch {
            when (action.type) {
                TableActionType.MOVE -> {
                    val moved = repository.moveActiveOrder(action.sourceTableId, action.targetTableId)
                    if (!moved) pushSnackbar("이동할 활성 주문이 없습니다")
                }

                TableActionType.MERGE -> {
                    repository.mergeTables(action.sourceTableId, action.targetTableId)
                    pushSnackbar("합석 처리되었습니다")
                }
            }
            _uiState.update {
                it.copy(
                    pendingAction = null,
                    selectedTargetTableId = null,
                    uiMode = UiMode.NORMAL,
                    selectedTableId = action.targetTableId,
                    selectedSourceTableId = action.targetTableId
                )
            }
            observeRightPanel(action.targetTableId)
        }
    }

    fun consumeSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onTableDropped(sourceTableId: Long, targetTableId: Long) {
        if (sourceTableId == targetTableId) return
        val targetTable = _uiState.value.tables.firstOrNull { it.tableId == targetTableId } ?: return
        if (targetTable.status == "DISABLED") {
            pushSnackbar("사용불가 테이블은 대상이 될 수 없습니다")
            return
        }

        viewModelScope.launch {
            if (!repository.hasActiveOrder(sourceTableId)) {
                pushSnackbar("원본 테이블에 활성 주문이 없습니다")
                return@launch
            }
            val targetHasActiveOrder = repository.hasActiveOrder(targetTableId)
            if (targetHasActiveOrder) {
                _uiState.update {
                    it.copy(
                        selectedTargetTableId = targetTableId,
                        pendingAction = PendingTableAction(
                            type = TableActionType.MERGE,
                            sourceTableId = sourceTableId,
                            targetTableId = targetTableId
                        )
                    )
                }
                return@launch
            }

            val moved = repository.moveActiveOrder(sourceTableId, targetTableId)
            if (!moved) {
                pushSnackbar("이동할 활성 주문이 없습니다")
                return@launch
            }
            pushSnackbar("이동 처리되었습니다")

            _uiState.update {
                it.copy(
                    selectedTableId = targetTableId,
                    selectedSourceTableId = targetTableId,
                    selectedTargetTableId = null,
                    uiMode = UiMode.NORMAL,
                    pendingAction = null
                )
            }
            observeRightPanel(targetTableId)
        }
    }

    fun addMenuToSelectedTable(menuName: String, price: Int) {
        val tableId = _uiState.value.selectedTableId ?: return
        viewModelScope.launch {
            repository.addMenuToTable(tableId = tableId, menuName = menuName, price = price)
        }
    }

    fun increaseOrderItemQty(orderItemId: Long) {
        val orderId = _uiState.value.rightPanel?.orderId ?: return
        viewModelScope.launch {
            repository.changeOrderItemQty(orderId = orderId, orderItemId = orderItemId, delta = 1)
        }
    }

    fun decreaseOrderItemQty(orderItemId: Long) {
        val panel = _uiState.value.rightPanel ?: return
        val target = panel.items.firstOrNull { it.orderItemId == orderItemId } ?: return
        if (target.qty <= 1) {
            pushSnackbar("수량은 1 이상이어야 합니다")
            return
        }
        val orderId = panel.orderId
        viewModelScope.launch {
            repository.changeOrderItemQty(orderId = orderId, orderItemId = orderItemId, delta = -1)
        }
    }

    fun changeOrderItemUnitPrice(orderItemId: Long, newPrice: Int) {
        if (newPrice <= 0) {
            pushSnackbar("금액은 1원 이상이어야 합니다")
            return
        }
        val panel = _uiState.value.rightPanel ?: return
        val orderId = panel.orderId
        viewModelScope.launch {
            repository.changeOrderItemUnitPrice(orderId = orderId, orderItemId = orderItemId, newPrice = newPrice)
        }
    }

    fun toggleOrderItemCanceled(orderItemId: Long) {
        val panel = _uiState.value.rightPanel ?: return
        val item = panel.items.firstOrNull { it.orderItemId == orderItemId } ?: return
        val orderId = panel.orderId
        viewModelScope.launch {
            if (item.priceSnapshot > 0) {
                canceledPriceMemory[orderItemId] = item.priceSnapshot
                repository.changeOrderItemUnitPrice(orderId = orderId, orderItemId = orderItemId, newPrice = 0)
                pushSnackbar("상품 지정취소 처리되었습니다")
            } else {
                val restorePrice = canceledPriceMemory[orderItemId] ?: 8000
                repository.changeOrderItemUnitPrice(orderId = orderId, orderItemId = orderItemId, newPrice = restorePrice)
                pushSnackbar("상품 지정취소가 해제되었습니다")
            }
        }
    }

    fun cancelAllCurrentOrderItems() {
        val orderId = _uiState.value.rightPanel?.orderId ?: return
        viewModelScope.launch {
            repository.cancelAllOrderItems(orderId)
            pushSnackbar("주문내역이 전체 취소되었습니다")
        }
    }

    private fun pushSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun reseedDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReseeding = true, reseedMessage = null) }
            try {
                repository.forceReseedDemoData()
                applyOneShotState("재생성 완료")
            } catch (t: Throwable) {
                _uiState.update { it.copy(reseedMessage = "재생성 실패: ${t.message ?: "unknown"}") }
            } finally {
                _uiState.update { it.copy(isReseeding = false) }
            }
        }
    }

    private suspend fun bootstrapData() {
        try {
            repository.seedIfNeeded()
            if (repository.getTableCount() == 0) {
                repository.forceReseedDemoData()
            }
            applyOneShotState("초기 로드 완료")
        } catch (t: Throwable) {
            _uiState.update { it.copy(reseedMessage = "초기 로드 실패: ${t.message ?: "unknown"}") }
        }
    }

    private suspend fun applyOneShotState(prefix: String) {
        val areas = repository.getAreasOnce()
        val selectedArea = repository.findFirstAreaIdWithTables()
        val tables = selectedArea?.let { repository.getTablesOnce(it) }.orEmpty()
        val selectedTable = tables.firstOrNull()?.tableId
        val areaCount = repository.getAreaCount()
        val tableCount = repository.getTableCount()
        _uiState.update {
            it.copy(
                areas = areas,
                selectedAreaId = selectedArea,
                tables = tables,
                selectedTableId = selectedTable,
                reseedMessage = "${prefix}: 구역 ${areaCount}개 / 테이블 ${tableCount}개"
            )
        }
        selectedTable?.let(::observeRightPanel)
    }

    private fun observeTables(areaId: Long) {
        tableObserverJob?.cancel()
        tableObserverJob = viewModelScope.launch {
            repository.observeTables(areaId).collectLatest { tables ->
                val selected = _uiState.value.selectedTableId?.takeIf { id -> tables.any { it.tableId == id } }
                    ?: tables.firstOrNull()?.tableId
                _uiState.update {
                    it.copy(
                        tables = tables,
                        selectedTableId = selected,
                        selectedSourceTableId = it.selectedSourceTableId?.takeIf { id -> tables.any { t -> t.tableId == id } },
                        selectedTargetTableId = it.selectedTargetTableId?.takeIf { id -> tables.any { t -> t.tableId == id } }
                    )
                }
                selected?.let(::observeRightPanel)
            }
        }
    }

    private fun observeRightPanel(tableId: Long) {
        rightPanelObserverJob?.cancel()
        rightPanelObserverJob = viewModelScope.launch {
            repository.observeActiveOrderDetails(tableId).collectLatest { activeOrder ->
                _uiState.update { state ->
                    state.copy(
                        rightPanel = activeOrder?.toRightPanelUi()
                    )
                }
            }
        }
    }
}


private fun formatAmount(value: Int): String = String.format(Locale.KOREA, "%,d", value)

private fun formatElapsed(createdAt: Long): String {
    val elapsedMillis = (System.currentTimeMillis() - createdAt).coerceAtLeast(0)
    return "${TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)}분"
}

private fun ActiveOrderDetails.toRightPanelUi(): RightOrderPanelUi {
    return RightOrderPanelUi(
        orderId = orderId,
        orderStatus = status,
        elapsedLabel = formatElapsed(createdAt),
        items = items.map { line ->
            RightPanelItemUi(
                orderItemId = line.orderItemId,
                itemName = line.itemName,
                priceSnapshot = line.priceSnapshot,
                qty = line.qty,
                lineTotal = line.lineTotal
            )
        },
        orderTotalAmount = orderTotalAmount,
        derivedTotalAmount = derivedTotalAmount,
        isTotalMismatch = orderTotalAmount != derivedTotalAmount
    )
}

@Composable
fun MainNavHost(vm: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "restaurant") {
        composable("restaurant") { RestaurantScreen(navController, vm) }
        composable("food/{tableId}") { backStackEntry ->
            val tableId = backStackEntry.arguments?.getString("tableId")?.toLongOrNull()
            FoodCourtScreen(navController = navController, vm = vm, tableId = tableId)
        }
        composable("payment/{tableId}") { backStackEntry ->
            val tableId = backStackEntry.arguments?.getString("tableId")?.toLongOrNull()
            val snapshot = remember(tableId, vm.uiState.collectAsState().value.rightPanel) { vm.buildPaymentSnapshot(tableId) }
            val paymentVm: PaymentViewModel = viewModel(
                key = "payment_${tableId ?: -1}",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = PaymentViewModel(snapshot) as T
                }
            )
            PaymentScreen(navController = navController, paymentVm = paymentVm)
        }
    }
}

@Composable
private fun PosTopBar() {
    val now = rememberCurrentDateTime()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd (E)") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "THE HYUNDAI",
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF005645),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(now.format(dateFormatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(now.format(timeFormatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(6.dp))
        Surface(color = Color(0xFFE7E7E7), shape = MaterialTheme.shapes.small) {
            Text("포스: 5556", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
        Spacer(Modifier.width(4.dp))
        Surface(color = Color(0xFFE7E7E7), shape = MaterialTheme.shapes.small) {
            Text("거래: 0014", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
        Spacer(Modifier.weight(1f))
        PosTopActionButton("점검", Icons.Filled.CheckCircle)
        PosTopActionButton("조회", Icons.Filled.Search)
        PosTopActionButton("영수증 재출력", Icons.Filled.Print)
        PosTopActionButton("더보기", Icons.Filled.MoreVert)
    }
}

@Composable
private fun PosTopActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedButton(
        onClick = {},
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.padding(end = 4.dp), tint = Color.Black)
        Text(label, color = Color.Black)
    }
}

@Composable
private fun rememberCurrentDateTime(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }
    return now
}

@Composable
fun RestaurantScreen(navController: NavHostController, vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    val selectedTable = uiState.tables.firstOrNull { it.tableId == uiState.selectedTableId }
    val snackbarHostState = remember { SnackbarHostState() }
    val tableBounds = remember { mutableStateMapOf<Long, Rect>() }
    var draggingTableId by remember { mutableStateOf<Long?>(null) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.consumeSnackbarMessage()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { PosTopBar() },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (uiState.areas.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = uiState.areas.indexOfFirst { it.id == uiState.selectedAreaId }.coerceAtLeast(0)
                    ) {
                        uiState.areas.forEach { area ->
                            Tab(
                                selected = area.id == uiState.selectedAreaId,
                                onClick = { vm.selectArea(area.id) },
                                text = {
                                    val isSelected = area.id == uiState.selectedAreaId
                                    Text(
                                        area.name,
                                        style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) Color(0xFF005645) else Color(0xFF444444)
                                    )
                                }
                            )
                        }
                    }
                }

                if (uiState.uiMode != UiMode.NORMAL) {
                    val modeLabel = if (uiState.uiMode == UiMode.SELECT_TARGET_FOR_MOVE) "이동 대상 테이블 선택" else "합석 대상 테이블 선택"
                    Text(
                        text = modeLabel,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = Color(0xFF005645),
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (uiState.tables.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("테이블 데이터가 없습니다")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            gridItems(uiState.tables) { table ->
                                val selected = table.tableId == selectedTable?.tableId
                                val isTargetMode = uiState.uiMode != UiMode.NORMAL
                                val isTargetCandidate = isTargetMode &&
                                    table.tableId != uiState.selectedSourceTableId &&
                                    table.status != "DISABLED"
                                val isSelectedTarget = table.tableId == uiState.selectedTargetTableId
                                val isSelectedSource = table.tableId == uiState.selectedTableId
                                val isDragActiveSource = draggingTableId == table.tableId
                                val containerColor = when {
                                    table.status == "DISABLED" -> Color(0xFFE0E0E0)
                                    table.status == "MERGED" -> Color(0xFFC7A97E)
                                    selected -> Color(0xFF005645)
                                    else -> Color(0xFFFFFFFF)
                                }
                                val contentColor = if (selected || table.status == "MERGED") Color.White else Color(0xFF2E2E2E)
                                Card(
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = containerColor,
                                        contentColor = contentColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .onGloballyPositioned { coordinates ->
                                            tableBounds[table.tableId] = coordinates.boundsInWindow()
                                        }
                                        .offset {
                                            if (draggingTableId == table.tableId) {
                                                IntOffset(draggingOffset.x.roundToInt(), draggingOffset.y.roundToInt())
                                            } else {
                                                IntOffset.Zero
                                            }
                                        }
                                        .zIndex(if (draggingTableId == table.tableId) 10f else 0f)
                                        .border(
                                            width = when {
                                                isDragActiveSource -> 4.dp
                                                selected || isSelectedTarget -> 2.dp
                                                else -> 1.dp
                                            },
                                            color = when {
                                                isDragActiveSource -> Color(0xFFFFB300)
                                                isSelectedTarget -> Color(0xFF1E88E5)
                                                isSelectedSource -> Color(0xFF005645)
                                                isTargetCandidate -> Color(0xFF8BC34A)
                                                else -> Color(0xFFDDDDDD)
                                            },
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .pointerInput(table.tableId) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingTableId = table.tableId
                                                    draggingOffset = Offset.Zero
                                                },
                                                onDrag = { _, dragAmount ->
                                                    if (draggingTableId == table.tableId) {
                                                        draggingOffset += dragAmount
                                                    }
                                                },
                                                onDragEnd = {
                                                    val sourceId = draggingTableId
                                                    val sourceBounds = sourceId?.let { tableBounds[it] }
                                                    val dropPoint = sourceBounds?.center?.plus(draggingOffset)
                                                    if (sourceId != null && dropPoint != null) {
                                                        val targetId = tableBounds.entries
                                                            .firstOrNull { (id, rect) -> id != sourceId && rect.contains(dropPoint) }
                                                            ?.key
                                                        if (targetId != null) {
                                                            vm.onTableDropped(sourceId, targetId)
                                                        }
                                                    }
                                                    draggingTableId = null
                                                    draggingOffset = Offset.Zero
                                                },
                                                onDragCancel = {
                                                    draggingTableId = null
                                                    draggingOffset = Offset.Zero
                                                }
                                            )
                                        }
                                        .clickable {
                                            vm.onTableTileClicked(table.tableId, table.status)
                                            if (uiState.uiMode == UiMode.NORMAL && table.status == "EMPTY") {
                                                navController.navigate("food/${table.tableId}")
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(table.tableName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                                        Text("${formatAmount(table.totalAmount)}원", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Filled.AccessTime, contentDescription = "식사시간", modifier = Modifier.width(18.dp).height(18.dp), tint = contentColor)
                                            Text(formatElapsed(table.createdAt), style = MaterialTheme.typography.titleSmall)
                                            Icon(Icons.Filled.Person, contentDescription = "인원수", modifier = Modifier.width(18.dp).height(18.dp), tint = contentColor)
                                            Text("${table.capacity}명", style = MaterialTheme.typography.titleSmall)
                                        }
                                        Text(formatTableStatus(table.status), color = if (contentColor == Color.White) Color.White else Color.Gray)
                                        if (table.status == "MERGED") {
                                            Text("합석됨", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFF4F1EB))
                    .padding(14.dp)
            ) {
                if (selectedTable == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("테이블을 선택하세요")
                    }
                } else {
                    Surface(color = Color(0xFF005645), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val chipColors = statusChipColors(selectedTable.status)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(selectedTable.tableName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                                Surface(
                                    color = chipColors.first,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = formatTableStatus(selectedTable.status),
                                        color = chipColors.second,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.AccessTime, contentDescription = "식사시간", modifier = Modifier.width(18.dp).height(18.dp), tint = Color.White)
                                Text(uiState.rightPanel?.elapsedLabel ?: "0분", color = Color.White, style = MaterialTheme.typography.titleSmall)
                                Icon(Icons.Filled.Person, contentDescription = "인원수", modifier = Modifier.width(18.dp).height(18.dp), tint = Color.White)
                                Text("${selectedTable.capacity}명", color = Color.White, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val panel = uiState.rightPanel
                    val visiblePanelItems = panel?.items?.filter { it.priceSnapshot > 0 }.orEmpty()
                    if (panel == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("활성 주문이 없습니다")
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("상품명", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.50f))
                            Text("수량", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(0.20f))
                            Text("금액", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.30f))
                        }
                        Divider()
                        val listState = rememberLazyListState()
                        val showScrollHint by remember(visiblePanelItems, listState) {
                            derivedStateOf {
                                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                                lastVisible < visiblePanelItems.lastIndex
                            }
                        }
                        val bounceTransition = rememberInfiniteTransition(label = "scrollHint")
                        val bounceOffset by bounceTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scrollHintOffset"
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(visiblePanelItems) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.itemName, modifier = Modifier.weight(0.50f))
                                        Text("${item.qty}", textAlign = TextAlign.Center, modifier = Modifier.weight(0.20f))
                                        Text("${formatAmount(item.lineTotal)}원", textAlign = TextAlign.End, modifier = Modifier.weight(0.30f))
                                    }
                                    Divider()
                                }
                            }
                            if (showScrollHint) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "아래로 더보기",
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .width(34.dp)
                                        .height(34.dp)
                                        .align(Alignment.BottomCenter)
                                        .offset(y = (-bounceOffset).dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("주문합계", color = Color.Black, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${formatAmount(panel.orderTotalAmount)}원", color = Color(0xFFD63B3B), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate("food/${selectedTable.tableId}") },
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD8CCD2),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Filled.AddShoppingCart,
                                contentDescription = "추가 주문",
                                tint = Color.White,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                "추가 주문",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { navController.navigate("payment/${selectedTable.tableId}") },
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC1A57A))
                        ) {
                            Icon(Icons.Filled.Payment, contentDescription = "결제", modifier = Modifier.padding(end = 6.dp))
                            Text("결제", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val pending = uiState.pendingAction
    if (pending != null) {
        val sourceName = uiState.tables.firstOrNull { it.tableId == pending.sourceTableId }?.tableName ?: "T-${pending.sourceTableId}"
        val targetName = uiState.tables.firstOrNull { it.tableId == pending.targetTableId }?.tableName ?: "T-${pending.targetTableId}"
        val message = if (pending.type == TableActionType.MOVE) {
            "${sourceName}의 주문을 ${targetName}로 이동할까요?"
        } else {
            "${sourceName}과 ${targetName}를 합석할까요?"
        }
        AlertDialog(
            onDismissRequest = { vm.dismissPendingAction() },
            title = { Text(if (pending.type == TableActionType.MOVE) "이동 확인" else "합석 확인") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { vm.confirmPendingAction() }) { Text("확인") }
            },
            dismissButton = {
                OutlinedButton(onClick = { vm.dismissPendingAction() }) { Text("취소") }
            }
        )
    }
}


@Composable
fun FoodCourtScreen(navController: NavHostController, vm: MainViewModel, tableId: Long?) {
    val uiState by vm.uiState.collectAsState()
    var priceEditItem by remember { mutableStateOf<RightPanelItemUi?>(null) }
    var priceInput by remember { mutableStateOf("") }
    var showCancelAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(tableId) {
        tableId?.let(vm::selectTable)
    }

    val menusByCategory = remember {
        linkedMapOf(
            "오므&커리" to listOf("오므라이스", "비프카레", "치킨카레", "새우카레"),
            "본까스" to listOf("등심 돈까스", "치즈 돈까스", "매운 돈까스", "생선까스"),
            "해천죽" to listOf("전복죽", "소고기죽", "야채죽", "해물죽"),
            "이심사철기" to listOf("철판 불고기", "철판 제육", "철판 낙지", "철판 우동"),
            "미코" to listOf("열무보리비빔밥", "사리기름고기비빔밥", "한우 안심 스테이크", "두레 갈치조림")
        )
    }
    val categories = remember(menusByCategory) { menusByCategory.keys.toList() }
    val favoriteTabTitle = "즐겨찾기"
    val favoriteAddCardLabel = "__favorite_add_card__"
    val displayCategories = remember(categories) { listOf(favoriteTabTitle) + categories }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var showFavoritePickerDialog by remember { mutableStateOf(false) }
    var selectedFavoriteDialogCategoryIndex by remember { mutableIntStateOf(0) }
    val favoriteMenus = remember { mutableStateListOf<String>() }
    val selectedFavoriteCandidates = remember { mutableStateListOf<String>() }
    val allMenusWithCategory = remember(menusByCategory) {
        menusByCategory.flatMap { (category, menus) -> menus.map { menu -> "$category|$menu" } }
    }

    val currentCategory = displayCategories.getOrElse(selectedCategoryIndex) { favoriteTabTitle }
    val isFavoriteTab = currentCategory == favoriteTabTitle
    val currentMenus = if (isFavoriteTab) {
        listOf(favoriteAddCardLabel) + favoriteMenus
    } else {
        menusByCategory[currentCategory].orEmpty()
    }
    val selectedTable = uiState.tables.firstOrNull { it.tableId == uiState.selectedTableId }
    val panelItems = uiState.rightPanel?.items.orEmpty()
    val totalAmount = uiState.rightPanel?.derivedTotalAmount ?: 0

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { PosTopBar() }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .background(Color.White)
                    .padding(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        selectedTable?.tableName ?: "선택된 테이블 없음",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Icon(Icons.Filled.AccessTime, contentDescription = "식사시간", modifier = Modifier.width(18.dp).height(18.dp), tint = Color(0xFF6B4B2A))
                        Text(uiState.rightPanel?.elapsedLabel ?: "0분", style = MaterialTheme.typography.titleSmall, color = Color(0xFF6B4B2A))
                        Icon(Icons.Filled.Person, contentDescription = "인원수", modifier = Modifier.width(18.dp).height(18.dp), tint = Color(0xFF6B4B2A))
                        Text("${selectedTable?.capacity ?: 0}명", style = MaterialTheme.typography.titleSmall, color = Color(0xFF6B4B2A))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("상품명", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.42f))
                    Text("수량", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(0.30f))
                    Text("금액", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.20f))
                    Spacer(modifier = Modifier.width(32.dp))
                }
                Divider()
                val leftListState = rememberLazyListState()
                val showLeftScrollHint by remember(panelItems, leftListState) {
                    derivedStateOf {
                        val lastVisible = leftListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                        lastVisible < panelItems.lastIndex
                    }
                }
                val leftBounceTransition = rememberInfiniteTransition(label = "leftScrollHint")
                val leftBounceOffset by leftBounceTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "leftScrollHintOffset"
                )
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = leftListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(panelItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isCanceled = item.priceSnapshot == 0
                                Text(
                                    item.itemName,
                                    modifier = Modifier.weight(0.42f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (isCanceled) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isCanceled) Color(0xFFD63B3B) else Color(0xFF222222)
                                )
                                Row(
                                    modifier = Modifier.weight(0.30f).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { vm.decreaseOrderItemQty(item.orderItemId) },
                                        modifier = Modifier.height(28.dp).width(28.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White)
                                    ) { Icon(Icons.Filled.Remove, contentDescription = "감소") }
                                    Text("${item.qty}", modifier = Modifier.padding(horizontal = 6.dp), style = MaterialTheme.typography.titleSmall)
                                    FilledTonalIconButton(
                                        onClick = { vm.increaseOrderItemQty(item.orderItemId) },
                                        modifier = Modifier.height(28.dp).width(28.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White)
                                    ) { Icon(Icons.Filled.Add, contentDescription = "증가") }
                                }
                                Text(
                                    text = "${formatAmount(item.lineTotal)}원",
                                    modifier = Modifier
                                        .weight(0.20f)
                                        .clickable {
                                            priceEditItem = item
                                            priceInput = item.priceSnapshot.toString()
                                        },
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.End,
                                    color = if (isCanceled) Color(0xFFD63B3B) else Color(0xFF005645),
                                    textDecoration = if (isCanceled) TextDecoration.LineThrough else TextDecoration.None
                                )
                                FilledTonalIconButton(
                                    onClick = { vm.toggleOrderItemCanceled(item.orderItemId) },
                                    modifier = Modifier.width(32.dp).height(28.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White)
                                ) {
                                    Icon(
                                        imageVector = if (isCanceled) Icons.Filled.Undo else Icons.Filled.Close,
                                        contentDescription = if (isCanceled) "복원" else "지정취소"
                                    )
                                }
                            }
                        }
                    }
                    if (showLeftScrollHint) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "아래로 더보기",
                            tint = Color(0xFF6B4B2A),
                            modifier = Modifier
                                .width(45.dp)
                                .height(45.dp)
                                .align(Alignment.BottomCenter)
                                .offset(y = (-leftBounceOffset).dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("총 매출", color = Color(0xFF8A8A8A), style = MaterialTheme.typography.titleSmall, fontSize = 18.sp)
                    Text("${formatAmount(totalAmount)}", color = Color(0xFF8A8A8A), style = MaterialTheme.typography.titleSmall, fontSize = 18.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("할인금액", color = Color(0xFF7A7A7A), style = MaterialTheme.typography.titleSmall, fontSize = 18.sp)
                    Text("${formatAmount(0)}", color = Color(0xFF3A76D2), style = MaterialTheme.typography.titleSmall, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("받을 금액", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${formatAmount(totalAmount)}원",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFD63B3B),
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("행사적용", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("주문 보류", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { showCancelAllDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("전체취소", color = Color(0xFFD63B3B), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                }
            }

            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(Color(0xFFF8F5EE))
                    .padding(10.dp)
            ) {
                ScrollableTabRow(selectedTabIndex = selectedCategoryIndex) {
                    displayCategories.forEachIndexed { index, category ->
                        Tab(
                            selected = index == selectedCategoryIndex,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (index == 0) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "즐겨찾기",
                                            tint = Color(0xFFF2C94C),
                                            modifier = Modifier.width(24.dp).height(24.dp)
                                        )
                                    }
                                    Text(
                                        category,
                                        style = if (index == selectedCategoryIndex) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                        color = if (index == selectedCategoryIndex) Color(0xFF005645) else Color(0xFF444444)
                                    )
                                }
                            }
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridItems(currentMenus) { menuName ->
                        val isFavoriteAddCard = isFavoriteTab && menuName == favoriteAddCardLabel
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clickable {
                                    if (isFavoriteAddCard) {
                                        selectedFavoriteCandidates.clear()
                                        favoriteMenus.forEach { favoriteMenu ->
                                            allMenusWithCategory.firstOrNull { it.endsWith("|$favoriteMenu") }?.let { selectedFavoriteCandidates.add(it) }
                                        }
                                        selectedFavoriteDialogCategoryIndex = 0
                                        showFavoritePickerDialog = true
                                    } else {
                                        vm.addMenuToSelectedTable(menuName = menuName, price = 8000)
                                    }
                                },
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isFavoriteAddCard) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            selectedFavoriteCandidates.clear()
                                            favoriteMenus.forEach { favoriteMenu ->
                                                allMenusWithCategory.firstOrNull { it.endsWith("|$favoriteMenu") }?.let { selectedFavoriteCandidates.add(it) }
                                            }
                                            selectedFavoriteDialogCategoryIndex = 0
                                            showFavoritePickerDialog = true
                                        },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFFD8CCD2))
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "즐겨찾기 추가", tint = Color.White)
                                    }
                                } else {
                                    Text(menuName, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(6.dp))
                                    Text("${formatAmount(8000)}", color = Color(0xFF005645), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8EA1))
                        ) {
                            Icon(Icons.Filled.Replay, contentDescription = "반품/환불", modifier = Modifier.padding(end = 5.dp))
                            Text("반품/환불", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005645))
                        ) {
                            Icon(Icons.Filled.PauseCircle, contentDescription = "주문 보류", modifier = Modifier.padding(end = 5.dp))
                            Text("주문 보류", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { navController.navigate("payment/${selectedTable?.tableId ?: (tableId ?: -1)}") },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(72.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC1A57A))
                        ) {
                            Icon(Icons.Filled.Payment, contentDescription = "결제 진행", modifier = Modifier.padding(end = 5.dp))
                            Text("결제 진행", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    val tableBtnTransition = rememberInfiniteTransition(label = "tableScreenBtn")
                    val tableBtnOffset by tableBtnTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "tableScreenBtnOffset"
                    )
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = (-86 - tableBtnOffset).dp)
                            .width(120.dp)
                            .height(82.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8CCD2), contentColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "테이블 화면", tint = Color.White)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "테이블 화면",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFavoritePickerDialog) {
        val dialogCategory = categories.getOrElse(selectedFavoriteDialogCategoryIndex) { categories.firstOrNull().orEmpty() }
        val dialogMenus = menusByCategory[dialogCategory].orEmpty()
        AlertDialog(
            onDismissRequest = { showFavoritePickerDialog = false },
            title = { Text("즐겨찾기 상품 선택") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScrollableTabRow(selectedTabIndex = selectedFavoriteDialogCategoryIndex) {
                        categories.forEachIndexed { index, category ->
                            Tab(
                                selected = index == selectedFavoriteDialogCategoryIndex,
                                onClick = { selectedFavoriteDialogCategoryIndex = index },
                                text = { Text(category) }
                            )
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(dialogMenus) { menu ->
                            val entry = "$dialogCategory|$menu"
                            val checked = entry in selectedFavoriteCandidates
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedFavoriteCandidates.remove(entry) else selectedFavoriteCandidates.add(entry)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedFavoriteCandidates.add(entry) else selectedFavoriteCandidates.remove(entry)
                                    }
                                )
                                Text(menu)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val chosenMenus = selectedFavoriteCandidates
                        .map { it.substringAfter("|") }
                        .distinct()
                    favoriteMenus.clear()
                    favoriteMenus.addAll(chosenMenus)
                    showFavoritePickerDialog = false
                    selectedCategoryIndex = 0
                }) { Text("적용") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFavoritePickerDialog = false }) { Text("취소") }
            }
        )
    }

    priceEditItem?.let { target ->
        AlertDialog(
            onDismissRequest = { priceEditItem = null },
            title = { Text("금액 변경") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target.itemName)
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { input -> priceInput = input.filter { it.isDigit() } },
                        label = { Text("단가") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newPrice = priceInput.toIntOrNull()
                    if (newPrice != null) {
                        vm.changeOrderItemUnitPrice(target.orderItemId, newPrice)
                        priceEditItem = null
                    }
                }) { Text("적용") }
            },
            dismissButton = {
                OutlinedButton(onClick = { priceEditItem = null }) { Text("취소") }
            }
        )
    }

    if (showCancelAllDialog) {
        AlertDialog(
            onDismissRequest = { showCancelAllDialog = false },
            title = { Text("전체취소") },
            text = { Text("주문내역을 취소하시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    vm.cancelAllCurrentOrderItems()
                    showCancelAllDialog = false
                }) { Text("확인") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelAllDialog = false }) { Text("취소") }
            }
        )
    }
}


private fun statusChipColors(status: String): Pair<Color, Color> = when (status) {
    "OCCUPIED" -> Color(0xFFE2F3EC) to Color(0xFF005645)
    "EMPTY" -> Color(0xFFF0F0F0) to Color(0xFF5E5E5E)
    "BILLING" -> Color(0xFFFFEED1) to Color(0xFF9A6300)
    "DISABLED" -> Color(0xFFE6E6E6) to Color(0xFF8A8A8A)
    "MERGED" -> Color(0xFFEFE3D0) to Color(0xFF6B4B2A)
    else -> Color(0xFFE2F3EC) to Color(0xFF005645)
}



@Composable
fun PaymentScreen(navController: NavHostController, paymentVm: PaymentViewModel) {
    val uiState by paymentVm.uiState.collectAsState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { PosTopBar() }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val paymentLines = uiState.methodAmounts.entries
                .filter { it.value > 0 }
                .sortedBy { it.key.ordinal }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Text(uiState.tableName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("결제수단", modifier = Modifier.weight(0.45f), color = Color.Gray)
                    Text("결제금액", modifier = Modifier.weight(0.35f), textAlign = TextAlign.End, color = Color.Gray)
                    Spacer(modifier = Modifier.width(48.dp))
                }
                Divider()
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(paymentLines) { line ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(line.key.label, modifier = Modifier.weight(0.45f), fontWeight = FontWeight.SemiBold)
                            Text("${formatAmount(line.value)}원", modifier = Modifier.weight(0.35f), textAlign = TextAlign.End)
                            FilledTonalIconButton(
                                onClick = { paymentVm.removePaymentMethod(line.key) },
                                modifier = Modifier.width(40.dp).height(32.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "결제 취소", tint = Color(0xFFD63B3B))
                            }
                        }
                    }
                }
                Divider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("총 금액", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    Text("${formatAmount(uiState.totalAmount)}원", style = MaterialTheme.typography.titleLarge, color = Color(0xFFD63B3B), fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("받은 금액", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 23.sp)
                    Text("${formatAmount(uiState.receivedAmount)}원", style = MaterialTheme.typography.titleMedium, color = Color(0xFF005645), fontWeight = FontWeight.Bold, fontSize = 23.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .background(Color(0xFFF8F5EE))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PaymentMethodGrid(
                    selected = uiState.selectedMethod,
                    methodAmounts = uiState.methodAmounts,
                    onSelect = paymentVm::selectPaymentMethod
                )
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("선택 결제수단: ${uiState.selectedMethod?.label ?: "선택 없음"}", color = Color.Gray)
                        Text(
                            if (uiState.keypadInput.isBlank()) "입력 금액: 0원" else "입력 금액: ${formatAmount(uiState.keypadInput.toIntOrNull() ?: 0)}원",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                NumericKeypad(
                    modifier = Modifier.weight(1f),
                    onKeyPress = paymentVm::onKeypadPressed
                )
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("결제 화면 닫기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodGrid(
    selected: PaymentMethod?,
    methodAmounts: Map<PaymentMethod, Int>,
    onSelect: (PaymentMethod) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(280.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        gridItems(PaymentMethod.values().toList()) { method ->
            val isSelected = selected == method
            val amount = methodAmounts[method] ?: 0
            Button(
                onClick = { onSelect(method) },
                modifier = Modifier.fillMaxWidth().height(86.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF005645) else Color.White,
                    contentColor = if (isSelected) Color.White else Color.Black
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF005645) else Color(0xFFCCCCCC))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(method.icon, contentDescription = method.label)
                    Text(method.label, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (amount > 0) Text("${formatAmount(amount)}원", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun NumericKeypad(modifier: Modifier = Modifier, onKeyPress: (String) -> Unit) {
    val keys = listOf(
        listOf("7", "8", "9", "Clear"),
        listOf("4", "5", "6", "Backspace"),
        listOf("1", "2", "3", "Enter"),
        listOf("0", "00", "만원")
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEachIndexed { rowIndex, rowKeys ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowKeys.forEach { key ->
                    Button(
                        onClick = { onKeyPress(key) },
                        modifier = Modifier
                            .weight(if (rowIndex == 3 && key == "만원") 1.6f else 1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (key) {
                                "Enter" -> Color(0xFF005645)
                                "Clear", "Backspace" -> Color(0xFF6C8EA1)
                                else -> Color.White
                            },
                            contentColor = if (key == "Enter" || key == "Clear" || key == "Backspace") Color.White else Color.Black
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D0D0))
                    ) {
                        if (key == "Backspace") {
                            Icon(Icons.Filled.Backspace, contentDescription = key)
                        } else {
                            Text(key, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTableStatus(status: String): String = when (status) {
    "OCCUPIED" -> "식사중"
    "EMPTY" -> "빈자리"
    "BILLING" -> "결제대기"
    "DISABLED" -> "사용불가"
    "MERGED" -> "합석됨"
    else -> status
}

private fun formatElapsed(createdAt: Long?): String {
    if (createdAt == null) return "0분"
    val elapsedMillis = (System.currentTimeMillis() - createdAt).coerceAtLeast(0)
    return "${TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)}분"
}
