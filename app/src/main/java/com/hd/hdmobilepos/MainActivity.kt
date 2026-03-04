package com.hd.hdmobilepos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ppos.db"
        )
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
    val itemName: String,
    val qty: Int,
    val lineTotal: Int
)

data class RightOrderPanelUi(
    val orderStatus: String,
    val elapsedLabel: String,
    val items: List<RightPanelItemUi>,
    val orderTotalAmount: Int,
    val derivedTotalAmount: Int,
    val isTotalMismatch: Boolean
)

data class MainUiState(
    val areas: List<Area> = emptyList(),
    val selectedAreaId: Long? = null,
    val tables: List<TableSummary> = emptyList(),
    val selectedTableId: Long? = null,
    val rightPanel: RightOrderPanelUi? = null,
    val isReseeding: Boolean = false,
    val reseedMessage: String? = null
)

class MainViewModel(private val repository: PosRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var tableObserverJob: Job? = null
    private var rightPanelObserverJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
            if (repository.getTableCount() == 0) {
                repository.forceReseedDemoData()
            }
            repository.observeAreas().collectLatest { areas ->
                val selectedArea = _uiState.value.selectedAreaId ?: areas.firstOrNull()?.id
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
                rightPanel = null
            )
        }
        observeTables(areaId)
    }

    fun selectTable(tableId: Long) {
        _uiState.update { it.copy(selectedTableId = tableId) }
        observeRightPanel(tableId)
    }

    fun reseedDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReseeding = true, reseedMessage = null) }
            try {
                repository.forceReseedDemoData()
                val areas = repository.getAreasOnce()
                val selectedArea = areas.firstOrNull()?.id
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
                        reseedMessage = "재생성 완료: 구역 ${areaCount}개 / 테이블 ${tableCount}개"
                    )
                }
                selectedTable?.let(::observeRightPanel)
            } catch (t: Throwable) {
                _uiState.update { it.copy(reseedMessage = "재생성 실패: ${t.message ?: "unknown"}") }
            } finally {
                _uiState.update { it.copy(isReseeding = false) }
            }
        }
    }

    private fun observeTables(areaId: Long) {
        tableObserverJob?.cancel()
        tableObserverJob = viewModelScope.launch {
            repository.observeTables(areaId).collectLatest { tables ->
                val selected = _uiState.value.selectedTableId?.takeIf { id -> tables.any { it.tableId == id } }
                    ?: tables.firstOrNull()?.tableId
                _uiState.update { it.copy(tables = tables, selectedTableId = selected) }
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

private fun ActiveOrderDetails.toRightPanelUi(): RightOrderPanelUi {
    return RightOrderPanelUi(
        orderStatus = status,
        elapsedLabel = formatElapsed(createdAt),
        items = items.map { line ->
            RightPanelItemUi(
                itemName = line.itemName,
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
        composable("food") { FoodCourtScreen(navController) }
    }
}

@Composable
private fun PosTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F4F4))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("THE HYUNDAI", fontWeight = FontWeight.ExtraBold, color = Color(0xFF005645))
        Text("2026-03-03\n10:53:25", style = MaterialTheme.typography.bodySmall)
        Surface(color = Color(0xFFE7E7E7), shape = MaterialTheme.shapes.small) {
            Text("포스: 5556", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
        Surface(color = Color(0xFFE7E7E7), shape = MaterialTheme.shapes.small) {
            Text("거래: 0014", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
        Spacer(Modifier.weight(1f))
        listOf("점검", "조회", "영수증 재출력", "더보기").forEach { label ->
            OutlinedButton(onClick = {}) { Text(label) }
        }
    }
}

@Composable
fun RestaurantScreen(navController: NavHostController, vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    val selectedTable = uiState.tables.firstOrNull { it.tableId == uiState.selectedTableId }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = { PosTopBar() }) { paddingValues ->
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
                                text = { Text(area.name) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = { vm.reseedDemoData() }, enabled = !uiState.isReseeding) {
                        Text(if (uiState.isReseeding) "재생성 중..." else "샘플데이터 재생성")
                    }
                }
                uiState.reseedMessage?.let { msg ->
                    Text(
                        text = msg,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = Color(0xFF005645)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (uiState.tables.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("테이블 데이터가 없습니다. 상단 버튼으로 재생성 해주세요.")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            gridItems(uiState.tables) { table ->
                                val selected = table.tableId == selectedTable?.tableId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) Color(0xFF005645) else Color(0xFFDDDDDD),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .clickable { vm.selectTable(table.tableId) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(table.tableName, fontWeight = FontWeight.Bold)
                                        Text("${table.totalAmount}원", fontWeight = FontWeight.SemiBold)
                                        Text("${formatElapsed(table.createdAt)} · ${table.capacity}명")
                                        Text(table.status, color = Color.Gray)
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
                    .background(Color(0xFFF7F7F7))
                    .padding(14.dp)
            ) {
                if (selectedTable == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("테이블을 선택하세요")
                    }
                } else {
                    Surface(color = Color(0xFF005645), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(selectedTable.tableName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                "${selectedTable.status} | ${uiState.rightPanel?.elapsedLabel ?: "0분"} | ${selectedTable.capacity}명",
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val panel = uiState.rightPanel
                    if (panel == null) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("활성 주문이 없습니다")
                        }
                    } else {
                        Text("주문상태: ${panel.orderStatus}", color = Color.Gray)
                        Spacer(Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(panel.items) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.itemName)
                                    Text("${item.qty}")
                                    Text("${item.lineTotal}원")
                                }
                                Divider()
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("주문합계(계산)", color = Color.Gray)
                            Text("${panel.derivedTotalAmount}원", fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("주문합계(DB)", color = Color.Gray)
                            Text("${panel.orderTotalAmount}원", color = Color(0xFFD63B3B), fontWeight = FontWeight.Bold)
                        }
                        if (panel.isTotalMismatch) {
                            Text("합계 불일치: 계산값과 DB 총액이 다릅니다", color = Color(0xFFD63B3B))
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { navController.navigate("food") }, modifier = Modifier.weight(1f)) {
                            Text("주문")
                        }
                        Button(
                            onClick = { /* TODO: 결제 처리 로직 연결 */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC1A57A))
                        ) {
                            Text("결제")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodCourtScreen(navController: NavHostController) {
    val categories = listOf("오므&커리", "본까스", "해천죽", "이심사철기", "미코")
    val menuNames = listOf("열무보리비빔밥", "사리기름고기비빔밥", "한우 안심 스테이크", "두레 갈치조림")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { PosTopBar() },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("반품/환불") }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005645))
                ) {
                    Text("주문 보류")
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1.6f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC1A57A))
                ) {
                    Text("결제 진행")
                }
            }
        }
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
                    .background(Color(0xFFFAFAFA))
                    .padding(10.dp)
            ) {
                Text("Table 1", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("상품명", color = Color.Gray)
                    Text("수량", color = Color.Gray)
                    Text("금액", color = Color.Gray)
                }
                Divider()
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(menuNames) { name ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name)
                            Text("1")
                            Text("8,000")
                        }
                    }
                }
                Text("받는 금액", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "71,000원",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFD63B3B),
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                    Text("레스토랑 화면으로")
                }
            }

            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                ScrollableTabRow(selectedTabIndex = 0) {
                    categories.forEachIndexed { index, category ->
                        Tab(selected = index == 0, onClick = {}, text = { Text(category) })
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridItems(menuNames) { menuName ->
                        Card(modifier = Modifier.fillMaxWidth().height(92.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(menuName, fontWeight = FontWeight.SemiBold)
                                Text("8,000", color = Color(0xFF005645), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatElapsed(createdAt: Long?): String {
    if (createdAt == null) return "0분"
    val elapsedMillis = (System.currentTimeMillis() - createdAt).coerceAtLeast(0)
    return "${TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)}분"
}
