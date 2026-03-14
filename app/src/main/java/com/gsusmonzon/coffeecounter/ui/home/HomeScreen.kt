package com.gsusmonzon.coffeecounter.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gsusmonzon.coffeecounter.R

data class HistorySummaryUiState(
    val title: String,
    val supportingText: String,
)

data class HomeUiState(
    val todayCount: Int = 0,
    val todaySupportingText: String = "Manual add arrives in Phase 3.",
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

class HomeViewModel : ViewModel() {
    var uiState by mutableStateOf(HomeUiState())
        private set
}

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    HomeScreen(
        uiState = viewModel.uiState,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
