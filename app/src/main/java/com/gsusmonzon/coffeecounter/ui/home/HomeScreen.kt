package com.gsusmonzon.coffeecounter.ui.home

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
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
import com.gsusmonzon.coffeecounter.domain.buildHistorySummary
import com.gsusmonzon.coffeecounter.domain.buildHistoryTimeline
import com.gsusmonzon.coffeecounter.ui.UiTestTags
import java.text.DecimalFormat
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val HISTORY_DAYS_7 = 7
private const val HISTORY_DAYS_30 = 30

data class HistorySectionUiState(
    @param:StringRes val titleRes: Int,
    val totalCount: Int,
    val averagePerDay: Double,
    val totalCountTag: String,
)

data class HomeUiState(
    val todayCount: Int = 0,
    val historySections: List<HistorySectionUiState> = listOf(
        HistorySectionUiState(
            titleRes = R.string.history_last_7_days_title,
            totalCount = 0,
            averagePerDay = 0.0,
            totalCountTag = UiTestTags.HOME_7_DAY_TOTAL,
        ),
        HistorySectionUiState(
            titleRes = R.string.history_last_30_days_title,
            totalCount = 0,
            averagePerDay = 0.0,
            totalCountTag = UiTestTags.HOME_30_DAY_TOTAL,
        ),
    ),
)

class HomeViewModel(
    private val coffeeRepository: CoffeeRepository,
    private val localDateProvider: LocalDateProvider,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = localDateProvider.observeToday()
        .flatMapLatest { today ->
            combine(
                coffeeRepository.observeTodayCount(),
                coffeeRepository.observeDailyCounts(
                    startDate = today.minusDays(HISTORY_DAYS_30.toLong() - 1),
                    endDate = today,
                ),
            ) { todayCount, storedCounts ->
                // Rebuild history windows from the current local day so the UI shifts forward
                // automatically at midnight instead of waiting for a manual refresh.
                buildHomeUiState(
                    today = today,
                    todayCount = todayCount,
                    storedCounts = storedCounts,
                )
            }
        }
        .stateIn(
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
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddCoffeeClick: () -> Unit,
    onRemoveCoffeeClick: () -> Unit,
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
            Card(
                modifier = Modifier.fillMaxWidth(),
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
): HomeUiState {
    val last30Days = buildHistoryTimeline(
        endDate = today,
        days = HISTORY_DAYS_30,
        storedCounts = storedCounts,
    )
    val last7Days = last30Days.take(HISTORY_DAYS_7)
    val last7DaySummary = buildHistorySummary(last7Days)
    val last30DaySummary = buildHistorySummary(last30Days)

    // Product decision: averages stay based on active coffee days, not full calendar windows.
    return HomeUiState(
        todayCount = todayCount,
        historySections = listOf(
            HistorySectionUiState(
                titleRes = R.string.history_last_7_days_title,
                totalCount = last7DaySummary.totalCount,
                averagePerDay = last7DaySummary.averagePerActiveDay,
                totalCountTag = UiTestTags.HOME_7_DAY_TOTAL,
            ),
            HistorySectionUiState(
                titleRes = R.string.history_last_30_days_title,
                totalCount = last30DaySummary.totalCount,
                averagePerDay = last30DaySummary.averagePerActiveDay,
                totalCountTag = UiTestTags.HOME_30_DAY_TOTAL,
            ),
        ),
    )
}
