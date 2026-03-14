package com.gsusmonzon.coffeecounter.ui.home

import android.content.Context
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.data.repository.LocalDateProvider
import com.gsusmonzon.coffeecounter.data.repository.SystemLocalDateProvider
import com.gsusmonzon.coffeecounter.domain.HistoryTimelineEntry
import com.gsusmonzon.coffeecounter.domain.buildHistoryTimeline
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryDayUiState(
    val date: java.time.LocalDate,
    val count: Int,
)

data class HistorySectionUiState(
    val title: String,
    val entries: List<HistoryDayUiState>,
)

data class HomeUiState(
    val todayCount: Int = 0,
    val todaySupportingText: String = "One tap logs one coffee.",
    val historySections: List<HistorySectionUiState> = listOf(
        HistorySectionUiState(
            title = "Last 7 days",
            entries = emptyList(),
        ),
        HistorySectionUiState(
            title = "Last 30 days",
            entries = emptyList(),
        ),
    ),
)

class HomeViewModel(
    private val coffeeRepository: CoffeeRepository,
    private val localDateProvider: LocalDateProvider = SystemLocalDateProvider,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        coffeeRepository.observeTodayCount(),
        coffeeRepository.observeDailyCounts(
            startDate = localDateProvider.today().minusDays(HISTORY_DAYS_30.toLong() - 1),
            endDate = localDateProvider.today(),
        ),
    ) { todayCount, storedCounts ->
        val today = localDateProvider.today()
        val last30Days = buildHistoryTimeline(
            endDate = today,
            days = HISTORY_DAYS_30,
            storedCounts = storedCounts,
        )

        HomeUiState(
            todayCount = todayCount,
            historySections = listOf(
                HistorySectionUiState(
                    title = "Last 7 days",
                    entries = last30Days.take(HISTORY_DAYS_7).toHistoryEntries(),
                ),
                HistorySectionUiState(
                    title = "Last 30 days",
                    entries = last30Days.toHistoryEntries(),
                ),
            ),
        )
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

    companion object {
        private const val HISTORY_DAYS_7 = 7
        private const val HISTORY_DAYS_30 = 30

        fun factory(
            coffeeRepository: CoffeeRepository,
            localDateProvider: LocalDateProvider = SystemLocalDateProvider,
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
        factory = HomeViewModel.factory(appContainer.coffeeRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onAddCoffeeClick = viewModel::onAddCoffeeClick,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddCoffeeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle(title = stringResource(R.string.today_title))
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.todays_coffee_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = uiState.todaySupportingText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = uiState.todayCount.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item {
            Button(
                onClick = onAddCoffeeClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.add_coffee_label))
            }
        }

        item {
            SectionTitle(title = stringResource(R.string.history_title))
        }

        items(uiState.historySections) { section ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (section.entries.isEmpty()) {
                        Text(
                            text = stringResource(R.string.history_empty_label),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        section.entries.forEach { entry ->
                            HistoryRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun HistoryRow(entry: HistoryDayUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = entry.date.toDisplayLabel(),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = entry.count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun Context.appContainer() = (applicationContext as CoffeeCounterApplication).appContainer

private fun List<HistoryTimelineEntry>.toHistoryEntries(): List<HistoryDayUiState> {
    return map { entry ->
        HistoryDayUiState(
            date = entry.date,
            count = entry.count,
        )
    }
}

private fun java.time.LocalDate.toDisplayLabel(): String {
    return format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
}
