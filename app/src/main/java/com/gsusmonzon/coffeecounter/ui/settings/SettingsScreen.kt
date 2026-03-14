package com.gsusmonzon.coffeecounter.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gsusmonzon.coffeecounter.CoffeeCounterApplication
import com.gsusmonzon.coffeecounter.BuildConfig
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.ui.UiTestTags
import com.gsusmonzon.coffeecounter.widget.CoffeeWidgetUpdater
import com.gsusmonzon.coffeecounter.widget.GlanceCoffeeWidgetUpdater
import kotlinx.coroutines.launch

data class SettingsUiState(
    val versionName: String,
    val isAddWidgetVisible: Boolean,
    val isWidgetPinFallbackVisible: Boolean = false,
    val isDeleteHistoryConfirmationVisible: Boolean = false,
)

class SettingsViewModel(
    versionName: String,
    private val coffeeRepository: CoffeeRepository,
    private val widgetPinRequester: WidgetPinRequester,
    private val widgetUpdater: CoffeeWidgetUpdater,
) : ViewModel() {
    var uiState by mutableStateOf(
        SettingsUiState(
            versionName = versionName,
            isAddWidgetVisible = !widgetPinRequester.hasActiveWidget(),
        )
    )
        private set

    fun refreshAddWidgetVisibility() {
        uiState = uiState.copy(isAddWidgetVisible = !widgetPinRequester.hasActiveWidget())
    }

    fun onAddWidgetClick() {
        val pinRequested = widgetPinRequester.requestPin()
        uiState = uiState.copy(isWidgetPinFallbackVisible = !pinRequested)
    }

    fun onDeleteHistoryClick() {
        uiState = uiState.copy(isDeleteHistoryConfirmationVisible = true)
    }

    fun onDismissDeleteHistoryConfirmation() {
        uiState = uiState.copy(isDeleteHistoryConfirmationVisible = false)
    }

    fun onConfirmDeleteHistory() {
        uiState = uiState.copy(isDeleteHistoryConfirmationVisible = false)

        viewModelScope.launch {
            coffeeRepository.resetAll()
            widgetUpdater.refresh()
        }
    }

    companion object {
        fun factory(
            versionName: String,
            coffeeRepository: CoffeeRepository,
            widgetPinRequester: WidgetPinRequester,
            widgetUpdater: CoffeeWidgetUpdater,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    versionName = versionName,
                    coffeeRepository = coffeeRepository,
                    widgetPinRequester = widgetPinRequester,
                    widgetUpdater = widgetUpdater,
                ) as T
            }
        }
    }
}

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appContainer = context.appContainer()
    val widgetPinRequester = remember(context.applicationContext) {
        AppWidgetPinRequester(context.applicationContext)
    }
    val widgetUpdater = remember(context.applicationContext) {
        GlanceCoffeeWidgetUpdater(context.applicationContext)
    }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            versionName = BuildConfig.VERSION_NAME,
            coffeeRepository = appContainer.coffeeRepository,
            widgetPinRequester = widgetPinRequester,
            widgetUpdater = widgetUpdater,
        )
    )

    DisposableEffect(lifecycleOwner, viewModel) {
        viewModel.refreshAddWidgetVisibility()

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAddWidgetVisibility()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SettingsScreen(
        uiState = viewModel.uiState,
        onAddWidgetClick = viewModel::onAddWidgetClick,
        onDeleteHistoryClick = viewModel::onDeleteHistoryClick,
        onDismissDeleteHistoryConfirmation = viewModel::onDismissDeleteHistoryConfirmation,
        onConfirmDeleteHistory = viewModel::onConfirmDeleteHistory,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAddWidgetClick: () -> Unit,
    onDeleteHistoryClick: () -> Unit,
    onDismissDeleteHistoryConfirmation: () -> Unit,
    onConfirmDeleteHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (uiState.isAddWidgetVisible) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.add_widget_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.add_widget_supporting_text),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = onAddWidgetClick,
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .testTag(UiTestTags.SETTINGS_ADD_WIDGET_BUTTON),
                        ) {
                            Text(text = stringResource(R.string.add_widget_button_label))
                        }
                        if (uiState.isWidgetPinFallbackVisible) {
                            Text(
                                text = stringResource(R.string.add_widget_fallback_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_version_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = uiState.versionName,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.delete_all_history_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(
                        onClick = onDeleteHistoryClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .testTag(UiTestTags.SETTINGS_RESET_BUTTON),
                    ) {
                        Text(text = stringResource(R.string.delete_all_history_button_label))
                    }
                }
            }
        }
    }

    if (uiState.isDeleteHistoryConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onDismissDeleteHistoryConfirmation,
            title = { Text(text = stringResource(R.string.delete_all_history_confirmation_title)) },
            text = { Text(text = stringResource(R.string.delete_all_history_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteHistory) {
                    Text(text = stringResource(R.string.delete_all_history_confirm_label))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteHistoryConfirmation) {
                    Text(text = stringResource(R.string.delete_all_history_cancel_label))
                }
            },
        )
    }
}

private fun Context.appContainer() = (applicationContext as CoffeeCounterApplication).appContainer
