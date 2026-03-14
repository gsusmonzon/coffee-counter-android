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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistorySummaryUiState(
    val title: String,
    val supportingText: String,
)

data class HomeUiState(
    val todayCount: Int = 0,
    val todaySupportingText: String = "One tap logs one coffee.",
    val historySummaries: List<HistorySummaryUiState> = listOf(
        HistorySummaryUiState(
            title = "Last 7 days",
            supportingText = "History rendering arrives in Phase 4.",
        ),
        HistorySummaryUiState(
            title = "Last 30 days",
            supportingText = "History rendering arrives in Phase 4.",
        ),
    ),
)

class HomeViewModel(
    private val coffeeRepository: CoffeeRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = coffeeRepository.observeTodayCount()
        .map { todayCount -> HomeUiState(todayCount = todayCount) }
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
        fun factory(
            coffeeRepository: CoffeeRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(coffeeRepository) as T
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

        items(uiState.historySummaries) { summary ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = summary.supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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

private fun Context.appContainer() = (applicationContext as CoffeeCounterApplication).appContainer
