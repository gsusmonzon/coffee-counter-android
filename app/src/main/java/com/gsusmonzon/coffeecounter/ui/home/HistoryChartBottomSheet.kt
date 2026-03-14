package com.gsusmonzon.coffeecounter.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.ui.UiTestTags
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private val ChartHeight = 188.dp
private val BarTrackHeight = 152.dp
private val ChartColumnWidth = 30.dp
private val ChartBarWidth = 14.dp
private val ZeroBarHeight = 6.dp
private val ChartBarShape = RoundedCornerShape(999.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryChartBottomSheet(
    uiState: HistoryChartUiState,
    onDismissRequest: () -> Unit,
    onLoadOlderHistory: () -> Unit,
    onHistoryBarClick: (HistoryChartBarUiState) -> Unit,
) {
    val listState = rememberLazyListState()
    val loadOlderHistory by rememberUpdatedState(onLoadOlderHistory)
    val canLoadOlder by rememberUpdatedState(uiState.canLoadOlder)

    LaunchedEffect(Unit) {
        listState.scrollToItem(historyChartInitialScrollIndex(uiState.bars.size))
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { firstVisibleItemIndex ->
                canLoadOlder && firstVisibleItemIndex == 0
            }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                loadOlderHistory()
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.history_chart_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.history_chart_supporting_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ChartHeight)
                    .testTag(UiTestTags.HOME_HISTORY_CHART),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BarTrackHeight)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    repeat(3) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        )
                    }
                }

                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = uiState.bars,
                        key = { bar -> bar.date.toString() },
                    ) { bar ->
                        HistoryChartBar(
                            bar = bar,
                            maxCount = uiState.maxCount,
                            onClick = { onHistoryBarClick(bar) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryChartBar(
    bar: HistoryChartBarUiState,
    maxCount: Int,
    onClick: () -> Unit,
) {
    val maxBarCount = maxCount.coerceAtLeast(1)
    val barHeight = if (bar.count == 0) {
        ZeroBarHeight
    } else {
        (BarTrackHeight * (bar.count.toFloat() / maxBarCount.toFloat()))
            .coerceAtLeast(12.dp)
    }

    Column(
        modifier = Modifier
            .width(ChartColumnWidth)
            .clickable(onClick = onClick)
            .then(
                if (bar.isToday) {
                    Modifier.testTag(UiTestTags.HOME_HISTORY_CHART_TODAY_BAR)
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.height(BarTrackHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(ChartBarWidth)
                    .height(barHeight)
                    .clip(ChartBarShape)
                    .background(
                        if (bar.count == 0) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    ),
            )
        }

        Text(
            text = bar.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
