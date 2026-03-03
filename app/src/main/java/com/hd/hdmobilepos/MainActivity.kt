package com.hd.hdmobilepos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.hd.hdmobilepos.data.AppDatabase
import com.hd.hdmobilepos.data.Area
import com.hd.hdmobilepos.data.OrderItemRow
import com.hd.hdmobilepos.data.PosRepository
import com.hd.hdmobilepos.data.TableSummary
import com.hd.hdmobilepos.ui.theme.PPOSTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ppos.db"
        ).build()
        val repo = PosRepository(db.posDao())

        setContent {
            PPOSTheme {
                val factory = remember {
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MainViewModel(repo) as T
                        }
                    }
                }
                val vm: MainViewModel = viewModel(factory = factory)
                MainNavHost(vm = vm)
            }
        }
    }
}

data class MainUiState(
    val areas: List<Area> = emptyList(),
    val selectedAreaId: Long? = null,
    val tables: List<TableSummary> = emptyList(),
    val selectedTableId: Long? = null,
    val selectedTableItems: List<OrderItemRow> = emptyList()
)

class MainViewModel(private val repository: PosRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var tableObserverJob: Job? = null
    private var selectedTableItemsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
            repository.observeAreas().collectLatest { areas ->
                val selectedArea = _uiState.value.selectedAreaId ?: areas.firstOrNull()?.id
                _uiState.update { it.copy(areas = areas, selectedAreaId = selectedArea) }
                selectedArea?.let { observeTables(it) }
            }
        }
    }

    fun selectArea(areaId: Long) {
        _uiState.update { it.copy(selectedAreaId = areaId, selectedTableId = null, selectedTableItems = emptyList()) }
        observeTables(areaId)
    }

    fun selectTable(tableId: Long) {
        _uiState.update { it.copy(selectedTableId = tableId) }
        selectedTableItemsJob?.cancel()
        selectedTableItemsJob = viewModelScope.launch {
            repository.observeCurrentOrderItems(tableId).collectLatest { items ->
                _uiState.update { state -> state.copy(selectedTableItems = items) }
            }
        }
    }

    private fun observeTables(areaId: Long) {
        tableObserverJob?.cancel()
        tableObserverJob = viewModelScope.launch {
            repository.observeTables(areaId).collectLatest { tables ->
                val selectedTableId = _uiState.value.selectedTableId?.takeIf { id -> tables.any { it.tableId == id } }
                    ?: tables.firstOrNull()?.tableId
                _uiState.update { it.copy(tables = tables, selectedTableId = selectedTableId) }
                selectedTableId?.let { id -> selectTable(id) }
            }
        }
    }
}

@Composable
fun MainNavHost(vm: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "restaurant") {
        composable("restaurant") {
            RestaurantScreen(navController = navController, vm = vm)
        }
        composable("food") {
            FoodCourtScreen(navController = navController)
        }
    }
}

@Composable
fun RestaurantScreen(navController: NavHostController, vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    val selectedTable = uiState.tables.firstOrNull { it.tableId == uiState.selectedTableId }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.3f)
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.tables) { table ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.selectTable(table.tableId) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(table.tableName, fontWeight = FontWeight.Bold)
                                Text("상태: ${table.status}")
                                Text("총금액: ${table.totalAmount}원")
                                Text("경과: ${formatElapsed(table.createdAt)}")
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("선택 테이블", style = MaterialTheme.typography.titleMedium)
                if (selectedTable == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("테이블을 선택하세요")
                    }
                } else {
                    Text("테이블: ${selectedTable.tableName}")
                    Text("상태: ${selectedTable.status}")
                    Text("경과: ${formatElapsed(selectedTable.createdAt)}")
                    Text("주문 목록", fontWeight = FontWeight.SemiBold)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.selectedTableItems) { item ->
                            val amount = item.priceSnapshot * item.qty
                            Text("${item.nameSnapshot} × ${item.qty} × ${amount}원")
                        }
                    }

                    Text("총액: ${selectedTable.totalAmount}원", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { navController.navigate("food") }) {
                            Text("주문")
                        }
                        Button(onClick = {
                            // TODO: 결제 처리 로직 연결
                        }) {
                            Text("결제")
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
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)
    return "${minutes}분"
}

@Composable
fun FoodCourtScreen(navController: NavHostController) {
    val categories = listOf("한식", "중식", "분식", "카페")
    val menuNames = listOf(
        "불고기덮밥", "김치찌개", "짜장면", "짬뽕", "떡볶이", "순대", "아메리카노", "라떼"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { }) {
                    Text("반품/환불")
                }
                Button(onClick = { }) {
                    Text("주문 보류")
                }
                Button(onClick = { }) {
                    Text("결제 진행")
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("주문 리스트", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(listOf("불고기덮밥", "아메리카노")) { itemName ->
                        Text("$itemName / 1개 / 9,000원")
                    }
                }
                Text("받는금액: 18,000원", fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { navController.popBackStack() }) {
                    Text("레스토랑 화면으로")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight()
            ) {
                ScrollableTabRow(selectedTabIndex = 0) {
                    categories.forEach { category ->
                        Tab(selected = category == categories.first(), onClick = { }, text = { Text(category) })
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(menuNames) { menuName ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(menuName, fontWeight = FontWeight.SemiBold)
                                Text("9,000원")
                            }
                        }
                    }
                }
            }
        }
    }
}
