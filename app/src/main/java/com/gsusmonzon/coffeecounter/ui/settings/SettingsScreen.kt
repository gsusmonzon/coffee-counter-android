package com.gsusmonzon.coffeecounter.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gsusmonzon.coffeecounter.BuildConfig
import com.gsusmonzon.coffeecounter.CoffeeCounterApplication
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.reminder.ReminderNotificationStatusChecker
import com.gsusmonzon.coffeecounter.reminder.SystemReminderNotificationStatusChecker
import com.gsusmonzon.coffeecounter.ui.UiTestTags
import com.gsusmonzon.coffeecounter.widget.CoffeeWidgetUpdater
import com.gsusmonzon.coffeecounter.widget.GlanceCoffeeWidgetUpdater
import kotlinx.coroutines.launch

data class SettingsUiState(
    val versionName: String,
    val isAddWidgetVisible: Boolean,
    val isWidgetPinFallbackVisible: Boolean = false,
    val isReminderGuidanceVisible: Boolean = false,
    val isDeleteHistoryConfirmationVisible: Boolean = false,
)

class SettingsViewModel(
    versionName: String,
    private val coffeeRepository: CoffeeRepository,
    private val widgetPinRequester: WidgetPinRequester,
    private val reminderNotificationStatusChecker: ReminderNotificationStatusChecker,
    private val widgetUpdater: CoffeeWidgetUpdater,
) : ViewModel() {
    var uiState by mutableStateOf(
        SettingsUiState(
            versionName = versionName,
            isAddWidgetVisible = !widgetPinRequester.hasActiveWidget(),
            isReminderGuidanceVisible = !reminderNotificationStatusChecker.canPostReminderNotifications(),
        ),
    )
        private set

    fun refreshAddWidgetVisibility() {
        uiState = uiState.copy(isAddWidgetVisible = !widgetPinRequester.hasActiveWidget())
    }

    fun refreshReminderGuidanceVisibility() {
        uiState = uiState.copy(
            isReminderGuidanceVisible = !reminderNotificationStatusChecker.canPostReminderNotifications(),
        )
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
            reminderNotificationStatusChecker: ReminderNotificationStatusChecker,
            widgetUpdater: CoffeeWidgetUpdater,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    versionName = versionName,
                    coffeeRepository = coffeeRepository,
                    widgetPinRequester = widgetPinRequester,
                    reminderNotificationStatusChecker = reminderNotificationStatusChecker,
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
    val reminderNotificationStatusChecker = remember(context.applicationContext) {
        SystemReminderNotificationStatusChecker(context.applicationContext)
    }
    val widgetUpdater = remember(context.applicationContext) {
        GlanceCoffeeWidgetUpdater(context.applicationContext)
    }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            versionName = BuildConfig.VERSION_NAME,
            coffeeRepository = appContainer.coffeeRepository,
            widgetPinRequester = widgetPinRequester,
            reminderNotificationStatusChecker = reminderNotificationStatusChecker,
            widgetUpdater = widgetUpdater,
        ),
    )

    DisposableEffect(lifecycleOwner, viewModel) {
        viewModel.refreshAddWidgetVisibility()
        viewModel.refreshReminderGuidanceVisibility()

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAddWidgetVisibility()
                viewModel.refreshReminderGuidanceVisibility()
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
        onOpenNotificationSettingsClick = { context.openAppNotificationSettings() },
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
    onOpenNotificationSettingsClick: () -> Unit,
    onDeleteHistoryClick: () -> Unit,
    onDismissDeleteHistoryConfirmation: () -> Unit,
    onConfirmDeleteHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (uiState.isAddWidgetVisible) {
            item {
                SettingsCard(
                    title = stringResource(R.string.add_widget_label),
                    supporting = stringResource(R.string.add_widget_supporting_text),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            modifier = Modifier.testTag(UiTestTags.SETTINGS_ADD_WIDGET_BUTTON),
                            onClick = onAddWidgetClick,
                        ) {
                            Text(text = stringResource(R.string.add_widget_button_label))
                        }
                    }

                    if (uiState.isWidgetPinFallbackVisible) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.add_widget_fallback_message),
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isReminderGuidanceVisible) {
            item {
                SettingsCard(
                    title = stringResource(R.string.reminder_guidance_label),
                    supporting = stringResource(R.string.reminder_guidance_supporting_text),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(onClick = onOpenNotificationSettingsClick) {
                            Text(text = stringResource(R.string.reminder_guidance_button_label))
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = stringResource(R.string.app_version_label),
                supporting = stringResource(R.string.app_version_supporting_text),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.app_version_value_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = uiState.versionName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        item {
            SettingsCard(
                title = stringResource(R.string.delete_all_history_label),
                supporting = stringResource(R.string.delete_all_history_supporting_text),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                titleColor = MaterialTheme.colorScheme.onErrorContainer,
                supportingColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_RESET_BUTTON),
                        onClick = onDeleteHistoryClick,
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
                TextButton(
                    modifier = Modifier.testTag(UiTestTags.SETTINGS_RESET_CONFIRM_BUTTON),
                    onClick = onConfirmDeleteHistory,
                ) {
                    Text(text = stringResource(R.string.delete_all_history_confirm_label))
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag(UiTestTags.SETTINGS_RESET_CANCEL_BUTTON),
                    onClick = onDismissDeleteHistoryConfirmation,
                ) {
                    Text(text = stringResource(R.string.delete_all_history_cancel_label))
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    supporting: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor,
            )
            content()
        }
    }
}

private fun Context.appContainer() = (applicationContext as CoffeeCounterApplication).appContainer

private fun Context.openAppNotificationSettings() {
    val notificationSettingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    startActivity(
        if (notificationSettingsIntent.resolveActivity(packageManager) != null) {
            notificationSettingsIntent
        } else {
            appDetailsIntent
        },
    )
}
