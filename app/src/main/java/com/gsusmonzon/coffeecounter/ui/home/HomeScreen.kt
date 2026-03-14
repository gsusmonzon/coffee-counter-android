package com.gsusmonzon.coffeecounter.ui.home

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gsusmonzon.coffeecounter.CoffeeCounterApplication
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.data.model.DailyCount
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.data.repository.LocalDateProvider
import com.gsusmonzon.coffeecounter.domain.HistoryTimelineEntry
import com.gsusmonzon.coffeecounter.domain.buildHistorySummary
import com.gsusmonzon.coffeecounter.domain.buildHistoryTimeline
import com.gsusmonzon.coffeecounter.ui.UiTestTags
import java.text.DecimalFormat
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HISTORY_DAYS_7 = 7
private const val HISTORY_DAYS_30 = 30
private const val HISTORY_CHART_INITIAL_DAYS = 32
private const val HISTORY_CHART_PAGE_DAYS = 30
private const val HISTORY_CHART_VISIBLE_BARS = 8

data class HistorySectionUiState(
    @param:StringRes val titleRes: Int,
    val totalCount: Int,
    val averagePerDay: Double,
    val totalCountTag: String,
    val cardTag: String,
)

data class HistoryChartBarUiState(
    val date: LocalDate,
    val count: Int,
    val label: String,
)

data class HistoryChartUiState(
    val bars: List<HistoryChartBarUiState> = emptyList(),
    val maxCount: Int = 0,
    val canLoadOlder: Boolean = false,
)

data class HomeUiState(
    val todayCount: Int = 0,
    val historySections: List<HistorySectionUiState> = listOf(
        HistorySectionUiState(
            titleRes = R.string.history_last_7_days_title,
            totalCount = 0,
            averagePerDay = 0.0,
            totalCountTag = UiTestTags.HOME_7_DAY_TOTAL,
            cardTag = UiTestTags.HOME_7_DAY_CARD,
        ),
        HistorySectionUiState(
            titleRes = R.string.history_last_30_days_title,
            totalCount = 0,
            averagePerDay = 0.0,
            totalCountTag = UiTestTags.HOME_30_DAY_TOTAL,
            cardTag = UiTestTags.HOME_30_DAY_CARD,
        ),
    ),
    val isHistoryChartVisible: Boolean = false,
    val historyChart: HistoryChartUiState = HistoryChartUiState(),
)

class HomeViewModel(
    private val coffeeRepository: CoffeeRepository,
    private val localDateProvider: LocalDateProvider,
) : ViewModel() {
    private val isHistoryChartVisible = MutableStateFlow(false)
    private val historyChartDays = MutableStateFlow(HISTORY_CHART_INITIAL_DAYS)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        localDateProvider.observeToday(),
        historyChartDays,
    ) { today, chartDays ->
        today to chartDays
    }.flatMapLatest { (today, chartDays) ->
        val observedDays = maxOf(HISTORY_DAYS_30, chartDays)

        combine(
            coffeeRepository.observeTodayCount(),
            coffeeRepository.observeDailyCounts(
                startDate = today.minusDays(observedDays.toLong() - 1),
                endDate = today,
            ),
            coffeeRepository.observeOldestLoggedDate(),
            isHistoryChartVisible,
        ) { todayCount, storedCounts, oldestLoggedDate, isChartVisible ->
            buildHomeUiState(
                today = today,
                todayCount = todayCount,
                storedCounts = storedCounts,
                oldestLoggedDate = oldestLoggedDate,
                chartDays = chartDays,
                isHistoryChartVisible = isChartVisible,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(),
    )

    fun onAddCoffeeClick() {
        viewModelScope.launch {
            coffeeRepository.incrementToday()
        }
    }

    fun onRemoveCoffeeClick() {
        viewModelScope.launch {
            coffeeRepository.decrementToday()
        }
    }

    fun onHistoryChartOpen() {
        isHistoryChartVisible.value = true
    }

    fun onHistoryChartDismiss() {
        isHistoryChartVisible.value = false
        historyChartDays.value = HISTORY_CHART_INITIAL_DAYS
    }

    fun onLoadOlderHistory() {
        val currentState = uiState.value
        if (!currentState.isHistoryChartVisible || !currentState.historyChart.canLoadOlder) {
            return
        }

        historyChartDays.update { currentDays ->
            currentDays + HISTORY_CHART_PAGE_DAYS
        }
    }

    companion object {
        fun factory(
            coffeeRepository: CoffeeRepository,
            localDateProvider: LocalDateProvider,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    coffeeRepository = coffeeRepository,
                    localDateProvider = localDateProvider,
                ) as T
            }
        }
    }
}

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
) {
    val appContainer = LocalContext.current.appContainer()
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            coffeeRepository = appContainer.coffeeRepository,
            localDateProvider = appContainer.localDateProvider,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onAddCoffeeClick = viewModel::onAddCoffeeClick,
        onRemoveCoffeeClick = viewModel::onRemoveCoffeeClick,
        onHistoryChartOpen = viewModel::onHistoryChartOpen,
        onHistoryChartDismiss = viewModel::onHistoryChartDismiss,
        onLoadOlderHistory = viewModel::onLoadOlderHistory,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddCoffeeClick: () -> Unit,
    onRemoveCoffeeClick: () -> Unit,
    onHistoryChartOpen: () -> Unit,
    onHistoryChartDismiss: () -> Unit,
    onLoadOlderHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            TodayCard(
                todayCount = uiState.todayCount,
                onAddCoffeeClick = onAddCoffeeClick,
                onRemoveCoffeeClick = onRemoveCoffeeClick,
            )
        }

        item {
            SectionTitle(
                title = stringResource(R.string.history_title),
            )
        }

        if (uiState.historySections.all { it.totalCount == 0 }) {
            item {
                EmptyHistoryCard()
            }
        }

        items(uiState.historySections) { section ->
            HistorySummaryCard(
                section = section,
                onClick = onHistoryChartOpen,
            )
        }
    }

    if (uiState.isHistoryChartVisible) {
        HistoryChartBottomSheet(
            uiState = uiState.historyChart,
            onDismissRequest = onHistoryChartDismiss,
            onLoadOlderHistory = onLoadOlderHistory,
        )
    }
}

@Composable
private fun TodayCard(
    todayCount: Int,
    onAddCoffeeClick: () -> Unit,
    onRemoveCoffeeClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.today_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.today_supporting_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.todays_coffee_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = todayCount.toString(),
                    modifier = Modifier.testTag(UiTestTags.HOME_TODAY_COUNT),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRemoveCoffeeClick) {
                        Text(text = stringResource(R.string.remove_coffee_label))
                    }
                    Button(onClick = onAddCoffeeClick) {
                        Text(text = stringResource(R.string.add_coffee_label))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.history_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.history_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistorySummaryCard(
    section: HistorySectionUiState,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(section.cardTag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(section.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.history_average_per_active_day_label,
                            section.averagePerDay.toDisplayLabel(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = section.totalCount.toString(),
                    modifier = Modifier.testTag(section.totalCountTag),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun Context.appContainer() = (applicationContext as CoffeeCounterApplication).appContainer

private fun Double.toDisplayLabel(): String {
    val formatter = DecimalFormat("0.#")
    return formatter.format(this)
}

private fun buildHomeUiState(
    today: LocalDate,
    todayCount: Int,
    storedCounts: List<DailyCount>,
    oldestLoggedDate: LocalDate?,
    chartDays: Int,
    isHistoryChartVisible: Boolean,
): HomeUiState {
    val last30Days = buildHistoryTimeline(
        endDate = today,
        days = HISTORY_DAYS_30,
        storedCounts = storedCounts.takeLast(HISTORY_DAYS_30),
    )
    val last7Days = last30Days.take(HISTORY_DAYS_7)
    val last7DaySummary = buildHistorySummary(last7Days)
    val last30DaySummary = buildHistorySummary(last30Days)
    val chartTimeline = buildHistoryTimeline(
        endDate = today,
        days = chartDays,
        storedCounts = storedCounts,
    ).asReversed()
    val chartStartDate = today.minusDays(chartDays.toLong() - 1)

    // Product decision: averages stay based on active coffee days, not full calendar windows.
    return HomeUiState(
        todayCount = todayCount,
        historySections = listOf(
            HistorySectionUiState(
                titleRes = R.string.history_last_7_days_title,
                totalCount = last7DaySummary.totalCount,
                averagePerDay = last7DaySummary.averagePerActiveDay,
                totalCountTag = UiTestTags.HOME_7_DAY_TOTAL,
                cardTag = UiTestTags.HOME_7_DAY_CARD,
            ),
            HistorySectionUiState(
                titleRes = R.string.history_last_30_days_title,
                totalCount = last30DaySummary.totalCount,
                averagePerDay = last30DaySummary.averagePerActiveDay,
                totalCountTag = UiTestTags.HOME_30_DAY_TOTAL,
                cardTag = UiTestTags.HOME_30_DAY_CARD,
            ),
        ),
        isHistoryChartVisible = isHistoryChartVisible,
        historyChart = HistoryChartUiState(
            bars = chartTimeline.map(::toHistoryChartBarUiState),
            maxCount = chartTimeline.maxOfOrNull(HistoryTimelineEntry::count)?.coerceAtLeast(1) ?: 1,
            canLoadOlder = oldestLoggedDate != null && chartStartDate > oldestLoggedDate,
        ),
    )
}

private fun toHistoryChartBarUiState(entry: HistoryTimelineEntry): HistoryChartBarUiState {
    return HistoryChartBarUiState(
        date = entry.date,
        count = entry.count,
        label = entry.date.dayOfMonth.toString(),
    )
}

internal fun historyChartInitialScrollIndex(totalBars: Int): Int {
    return (totalBars - HISTORY_CHART_VISIBLE_BARS).coerceAtLeast(0)
}
