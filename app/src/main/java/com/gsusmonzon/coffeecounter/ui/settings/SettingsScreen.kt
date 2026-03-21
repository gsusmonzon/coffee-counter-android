package com.gsusmonzon.coffeecounter.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gsusmonzon.coffeecounter.BuildConfig
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.appContainer
import com.gsusmonzon.coffeecounter.data.backup.CoffeeBackupExportResult
import com.gsusmonzon.coffeecounter.data.backup.CoffeeBackupFailureReason
import com.gsusmonzon.coffeecounter.data.backup.CoffeeBackupImportResult
import com.gsusmonzon.coffeecounter.data.backup.CoffeeBackupManager
import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryBackupJsonCodec
import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportMode
import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportSummary
import com.gsusmonzon.coffeecounter.data.backup.ContentResolverCoffeeBackupIo
import com.gsusmonzon.coffeecounter.data.backup.ContentResolverCoffeeBackupManager
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.reminder.ReminderNotificationStatusChecker
import com.gsusmonzon.coffeecounter.reminder.SystemReminderNotificationStatusChecker
import com.gsusmonzon.coffeecounter.ui.UiTestTags
import com.gsusmonzon.coffeecounter.widget.CoffeeWidgetUpdater
import com.gsusmonzon.coffeecounter.widget.GlanceCoffeeWidgetUpdater
import java.time.LocalDate
import kotlinx.coroutines.launch

sealed interface SettingsNotice {
    data class ExportSuccess(val exportedEvents: Int) : SettingsNotice

    data class ImportSuccess(
        val mode: CoffeeHistoryImportMode,
        val summary: CoffeeHistoryImportSummary,
    ) : SettingsNotice

    data class BackupFailure(val reason: CoffeeBackupFailureReason) : SettingsNotice
}

data class ImportConfirmationUiState(
    val mode: CoffeeHistoryImportMode,
)

data class SettingsUiState(
    val versionName: String,
    val isAddWidgetVisible: Boolean,
    val isWidgetPinFallbackVisible: Boolean = false,
    val isReminderGuidanceVisible: Boolean = false,
    val isDeleteHistoryConfirmationVisible: Boolean = false,
    val isImportModeDialogVisible: Boolean = false,
    val importConfirmation: ImportConfirmationUiState? = null,
    val isWorking: Boolean = false,
    val notice: SettingsNotice? = null,
)

class SettingsViewModel(
    versionName: String,
    private val coffeeRepository: CoffeeRepository,
    private val coffeeBackupManager: CoffeeBackupManager,
    private val widgetPinRequester: WidgetPinRequester,
    private val reminderNotificationStatusChecker: ReminderNotificationStatusChecker,
    private val widgetUpdater: CoffeeWidgetUpdater,
) : ViewModel() {
    private var pendingImportUri: Uri? = null
    private var pendingImportMode: CoffeeHistoryImportMode? = null

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

    fun onDismissNotice() {
        uiState = uiState.copy(notice = null)
    }

    fun onImportClick() {
        uiState = uiState.copy(
            isImportModeDialogVisible = true,
            importConfirmation = null,
            notice = null,
        )
    }

    fun onDismissImportModeDialog() {
        uiState = uiState.copy(isImportModeDialogVisible = false)
    }

    fun onImportModeSelected(mode: CoffeeHistoryImportMode) {
        pendingImportMode = mode
        uiState = uiState.copy(isImportModeDialogVisible = false)
    }

    fun onImportFilePicked(uri: Uri?) {
        val mode = pendingImportMode
        pendingImportMode = null

        if (uri == null || mode == null) {
            return
        }

        pendingImportUri = uri
        uiState = uiState.copy(
            importConfirmation = ImportConfirmationUiState(mode = mode),
            notice = null,
        )
    }

    fun onDismissImportConfirmation() {
        pendingImportUri = null
        uiState = uiState.copy(importConfirmation = null)
    }

    fun onConfirmImport() {
        val importUri = pendingImportUri ?: return
        val importMode = uiState.importConfirmation?.mode ?: return
        pendingImportUri = null
        uiState = uiState.copy(
            importConfirmation = null,
            isWorking = true,
            notice = null,
        )

        viewModelScope.launch {
            when (val result = coffeeBackupManager.importFrom(importUri, importMode)) {
                is CoffeeBackupImportResult.Success -> {
                    widgetUpdater.refresh()
                    uiState = uiState.copy(
                        isWorking = false,
                        notice = SettingsNotice.ImportSuccess(
                            mode = importMode,
                            summary = result.summary,
                        ),
                    )
                }

                is CoffeeBackupImportResult.Failure -> {
                    uiState = uiState.copy(
                        isWorking = false,
                        notice = SettingsNotice.BackupFailure(result.reason),
                    )
                }
            }
        }
    }

    fun onExportFilePicked(uri: Uri?) {
        if (uri == null) {
            return
        }

        uiState = uiState.copy(isWorking = true, notice = null)

        viewModelScope.launch {
            when (val result = coffeeBackupManager.exportTo(uri)) {
                is CoffeeBackupExportResult.Success -> {
                    uiState = uiState.copy(
                        isWorking = false,
                        notice = SettingsNotice.ExportSuccess(result.exportedEvents),
                    )
                }

                is CoffeeBackupExportResult.Failure -> {
                    uiState = uiState.copy(
                        isWorking = false,
                        notice = SettingsNotice.BackupFailure(result.reason),
                    )
                }
            }
        }
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
            coffeeBackupManager: CoffeeBackupManager,
            widgetPinRequester: WidgetPinRequester,
            reminderNotificationStatusChecker: ReminderNotificationStatusChecker,
            widgetUpdater: CoffeeWidgetUpdater,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    versionName = versionName,
                    coffeeRepository = coffeeRepository,
                    coffeeBackupManager = coffeeBackupManager,
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
    val coffeeBackupManager = remember(context.applicationContext, appContainer.coffeeRepository) {
        ContentResolverCoffeeBackupManager(
            io = ContentResolverCoffeeBackupIo(context.applicationContext),
            coffeeRepository = appContainer.coffeeRepository,
            codec = CoffeeHistoryBackupJsonCodec(),
            appVersion = BuildConfig.VERSION_NAME,
        )
    }
    val widgetUpdater = remember(context.applicationContext) {
        GlanceCoffeeWidgetUpdater(context.applicationContext)
    }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            versionName = BuildConfig.VERSION_NAME,
            coffeeRepository = appContainer.coffeeRepository,
            coffeeBackupManager = coffeeBackupManager,
            widgetPinRequester = widgetPinRequester,
            reminderNotificationStatusChecker = reminderNotificationStatusChecker,
            widgetUpdater = widgetUpdater,
        ),
    )
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("application/json")) { uri ->
        viewModel.onExportFilePicked(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        viewModel.onImportFilePicked(uri)
    }

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
        onExportClick = {
            exportLauncher.launch(
                context.getString(
                    R.string.backup_export_default_filename,
                    LocalDate.now().toString(),
                ),
            )
        },
        onImportClick = viewModel::onImportClick,
        onDismissImportModeDialog = viewModel::onDismissImportModeDialog,
        onImportModeSelected = { mode ->
            viewModel.onImportModeSelected(mode)
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        },
        onDismissImportConfirmation = viewModel::onDismissImportConfirmation,
        onConfirmImport = viewModel::onConfirmImport,
        onDismissNotice = viewModel::onDismissNotice,
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
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismissImportModeDialog: () -> Unit,
    onImportModeSelected: (CoffeeHistoryImportMode) -> Unit,
    onDismissImportConfirmation: () -> Unit,
    onConfirmImport: () -> Unit,
    onDismissNotice: () -> Unit,
    onAddWidgetClick: () -> Unit,
    onOpenNotificationSettingsClick: () -> Unit,
    onDeleteHistoryClick: () -> Unit,
    onDismissDeleteHistoryConfirmation: () -> Unit,
    onConfirmDeleteHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(UiTestTags.SETTINGS_LIST),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        uiState.notice?.let { notice ->
            item {
                NoticeCard(
                    notice = notice,
                    onDismiss = onDismissNotice,
                )
            }
        }

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
                title = stringResource(R.string.backup_history_label),
                supporting = stringResource(R.string.backup_history_supporting_text),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    Button(
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_EXPORT_BUTTON),
                        enabled = !uiState.isWorking,
                        onClick = onExportClick,
                    ) {
                        Text(text = stringResource(R.string.backup_export_button_label))
                    }
                    Button(
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_IMPORT_BUTTON),
                        enabled = !uiState.isWorking,
                        onClick = onImportClick,
                    ) {
                        Text(text = stringResource(R.string.backup_import_button_label))
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = stringResource(R.string.app_version_label),
                supporting = stringResource(R.string.app_version_supporting_text),
                headerTrailingContent = {
                    Text(
                        text = uiState.versionName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            ) {
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

    if (uiState.isImportModeDialogVisible) {
        AlertDialog(
            onDismissRequest = onDismissImportModeDialog,
            title = { Text(text = stringResource(R.string.backup_import_mode_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.backup_import_mode_message))
                    ImportModeActionButton(
                        title = stringResource(R.string.backup_import_mode_merge_label),
                        supporting = stringResource(R.string.backup_import_mode_merge_supporting),
                        onClick = { onImportModeSelected(CoffeeHistoryImportMode.MERGE) },
                    )
                    ImportModeActionButton(
                        title = stringResource(R.string.backup_import_mode_replace_label),
                        supporting = stringResource(R.string.backup_import_mode_replace_supporting),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { onImportModeSelected(CoffeeHistoryImportMode.REPLACE) },
                    )
                }
            },
            confirmButton = { Spacer(modifier = Modifier) },
            dismissButton = {
                TextButton(onClick = onDismissImportModeDialog) {
                    Text(text = stringResource(R.string.backup_dialog_cancel_label))
                }
            },
        )
    }

    uiState.importConfirmation?.let { confirmation ->
        val titleRes = when (confirmation.mode) {
            CoffeeHistoryImportMode.MERGE -> R.string.backup_import_merge_confirmation_title
            CoffeeHistoryImportMode.REPLACE -> R.string.backup_import_replace_confirmation_title
        }
        val messageRes = when (confirmation.mode) {
            CoffeeHistoryImportMode.MERGE -> R.string.backup_import_merge_confirmation_message
            CoffeeHistoryImportMode.REPLACE -> R.string.backup_import_replace_confirmation_message
        }
        val confirmLabelRes = when (confirmation.mode) {
            CoffeeHistoryImportMode.MERGE -> R.string.backup_import_confirm_label
            CoffeeHistoryImportMode.REPLACE -> R.string.backup_import_replace_confirm_label
        }

        AlertDialog(
            onDismissRequest = onDismissImportConfirmation,
            title = { Text(text = stringResource(titleRes)) },
            text = { Text(text = stringResource(messageRes)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmImport,
                    enabled = !uiState.isWorking,
                ) {
                    Text(text = stringResource(confirmLabelRes))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissImportConfirmation) {
                    Text(text = stringResource(R.string.backup_dialog_cancel_label))
                }
            },
        )
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
private fun NoticeCard(
    notice: SettingsNotice,
    onDismiss: () -> Unit,
) {
    val containerColor = when (notice) {
        is SettingsNotice.BackupFailure -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val titleColor = when (notice) {
        is SettingsNotice.BackupFailure -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = when (notice) {
        is SettingsNotice.BackupFailure -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    SettingsCard(
        title = stringResource(R.string.backup_history_label),
        supporting = notice.toMessage(),
        containerColor = containerColor,
        titleColor = titleColor,
        supportingColor = supportingColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.backup_status_dismiss_label))
            }
        }
    }
}

@Composable
private fun ImportModeActionButton(
    title: String,
    supporting: String,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.86f),
            )
        }
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
    headerTrailingContent: @Composable (() -> Unit)? = null,
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
            if (headerTrailingContent == null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor,
                    )
                    Row(modifier = Modifier.padding(start = 16.dp)) {
                        headerTrailingContent()
                    }
                }
            }
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor,
            )
            content()
        }
    }
}

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

@Composable
private fun SettingsNotice.toMessage(): String {
    return when (this) {
        is SettingsNotice.ExportSuccess -> stringResource(
            R.string.backup_status_export_success,
            exportedEvents,
        )

        is SettingsNotice.ImportSuccess -> when (mode) {
            CoffeeHistoryImportMode.MERGE -> stringResource(
                R.string.backup_status_import_merge_success,
                summary.importedEvents,
                summary.importedDays,
                summary.skippedDays,
            )

            CoffeeHistoryImportMode.REPLACE -> stringResource(
                R.string.backup_status_import_replace_success,
                summary.importedEvents,
                summary.importedDays,
            )
        }

        is SettingsNotice.BackupFailure -> when (reason) {
            CoffeeBackupFailureReason.FILE_READ_FAILED -> stringResource(R.string.backup_status_file_read_failed)
            CoffeeBackupFailureReason.FILE_WRITE_FAILED -> stringResource(R.string.backup_status_file_write_failed)
            CoffeeBackupFailureReason.INVALID_FORMAT -> stringResource(R.string.backup_status_invalid_format)
            CoffeeBackupFailureReason.UNSUPPORTED_NEWER_SCHEMA -> stringResource(
                R.string.backup_status_unsupported_newer_schema,
            )
        }
    }
}
