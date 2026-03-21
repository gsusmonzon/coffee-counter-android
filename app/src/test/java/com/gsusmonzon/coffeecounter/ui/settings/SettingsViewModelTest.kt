package com.gsusmonzon.coffeecounter.ui.settings

import android.net.Uri
import com.gsusmonzon.coffeecounter.data.backup.CoffeeBackupExportResult
import com.gsusmonzon.coffeecounter.data.backup.CoffeeBackupFailureReason
import com.gsusmonzon.coffeecounter.data.backup.CoffeeBackupImportResult
import com.gsusmonzon.coffeecounter.data.backup.CoffeeBackupManager
import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportMode
import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportSummary
import com.gsusmonzon.coffeecounter.data.model.CoffeeEvent
import com.gsusmonzon.coffeecounter.data.model.DailyCount
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.reminder.ReminderNotificationStatusChecker
import com.gsusmonzon.coffeecounter.widget.CoffeeWidgetUpdater
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCoffeeRepository
    private lateinit var backupManager: FakeCoffeeBackupManager
    private lateinit var widgetPinRequester: FakeWidgetPinRequester
    private lateinit var reminderNotificationStatusChecker: FakeReminderNotificationStatusChecker
    private lateinit var widgetUpdater: FakeCoffeeWidgetUpdater

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCoffeeRepository()
        backupManager = FakeCoffeeBackupManager()
        widgetPinRequester = FakeWidgetPinRequester()
        reminderNotificationStatusChecker = FakeReminderNotificationStatusChecker()
        widgetUpdater = FakeCoffeeWidgetUpdater()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onResetAllClick_showsConfirmationDialog() {
        val viewModel = newViewModel()

        viewModel.onDeleteHistoryClick()

        assertEquals(true, viewModel.uiState.isDeleteHistoryConfirmationVisible)
    }

    @Test
    fun onDismissResetConfirmation_hidesConfirmationDialog() {
        val viewModel = newViewModel()

        viewModel.onDeleteHistoryClick()
        viewModel.onDismissDeleteHistoryConfirmation()

        assertEquals(false, viewModel.uiState.isDeleteHistoryConfirmationVisible)
    }

    @Test
    fun onConfirmResetAll_clearsHistoryResetsTodayAndRefreshesWidget() = runTest(dispatcher) {
        repository.seedTodayCount(3)
        repository.seedHistory(
            listOf(
                DailyCount(LocalDate.of(2026, 3, 13), 4),
                DailyCount(LocalDate.of(2026, 3, 14), 3),
            ),
        )
        val viewModel = newViewModel()
        viewModel.onDeleteHistoryClick()

        viewModel.onConfirmDeleteHistory()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.isDeleteHistoryConfirmationVisible)
        assertEquals(1, repository.resetAllCalls)
        assertEquals(0, repository.todayCount.value)
        assertEquals(emptyList<DailyCount>(), repository.dailyCounts.value)
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    @Test
    fun init_hidesAddWidgetWhenWidgetAlreadyExists() {
        widgetPinRequester.hasActiveWidget = true

        val viewModel = newViewModel()

        assertEquals(false, viewModel.uiState.isAddWidgetVisible)
    }

    @Test
    fun refreshAddWidgetVisibility_showsCardAgainAfterWidgetRemoval() {
        widgetPinRequester.hasActiveWidget = true
        val viewModel = newViewModel()

        widgetPinRequester.hasActiveWidget = false
        viewModel.refreshAddWidgetVisibility()

        assertEquals(true, viewModel.uiState.isAddWidgetVisible)
    }

    @Test
    fun onAddWidgetClick_hidesFallbackWhenPinRequestStarts() {
        widgetPinRequester.requestPinResult = true
        val viewModel = newViewModel()

        viewModel.onAddWidgetClick()

        assertEquals(1, widgetPinRequester.requestPinCalls)
        assertEquals(false, viewModel.uiState.isWidgetPinFallbackVisible)
    }

    @Test
    fun onAddWidgetClick_showsFallbackWhenPinRequestIsUnavailable() {
        widgetPinRequester.requestPinResult = false
        val viewModel = newViewModel()

        viewModel.onAddWidgetClick()

        assertEquals(1, widgetPinRequester.requestPinCalls)
        assertEquals(true, viewModel.uiState.isWidgetPinFallbackVisible)
    }

    @Test
    fun init_showsReminderGuidanceWhenNotificationsAreUnavailable() {
        reminderNotificationStatusChecker.canPostNotifications = false

        val viewModel = newViewModel()

        assertEquals(true, viewModel.uiState.isReminderGuidanceVisible)
    }

    @Test
    fun onImportClick_showsImportModeDialog() {
        val viewModel = newViewModel()

        viewModel.onImportClick()

        assertEquals(true, viewModel.uiState.isImportModeDialogVisible)
    }

    @Test
    fun onImportFilePicked_setsConfirmationStateForSelectedMode() {
        val viewModel = newViewModel()

        viewModel.onImportModeSelected(CoffeeHistoryImportMode.MERGE)
        viewModel.onImportFilePicked(Uri.parse("content://coffee/import.json"))

        assertEquals(CoffeeHistoryImportMode.MERGE, viewModel.uiState.importConfirmation?.mode)
    }

    @Test
    fun onConfirmImport_runsImportRefreshesWidgetAndShowsNotice() = runTest(dispatcher) {
        val summary = CoffeeHistoryImportSummary(importedEvents = 4, importedDays = 2, skippedDays = 1)
        backupManager.importResult = CoffeeBackupImportResult.Success(summary)
        val viewModel = newViewModel()
        viewModel.onImportModeSelected(CoffeeHistoryImportMode.MERGE)
        viewModel.onImportFilePicked(Uri.parse("content://coffee/import.json"))

        viewModel.onConfirmImport()
        advanceUntilIdle()

        assertEquals(1, backupManager.importCalls)
        assertEquals(1, widgetUpdater.refreshCalls)
        assertEquals(
            SettingsNotice.ImportSuccess(
                mode = CoffeeHistoryImportMode.MERGE,
                summary = summary,
            ),
            viewModel.uiState.notice,
        )
    }

    @Test
    fun onExportFilePicked_showsFailureNoticeWhenWriteFails() = runTest(dispatcher) {
        backupManager.exportResult = CoffeeBackupExportResult.Failure(CoffeeBackupFailureReason.FILE_WRITE_FAILED)
        val viewModel = newViewModel()

        viewModel.onExportFilePicked(Uri.parse("content://coffee/export.json"))
        advanceUntilIdle()

        assertEquals(1, backupManager.exportCalls)
        assertEquals(
            SettingsNotice.BackupFailure(CoffeeBackupFailureReason.FILE_WRITE_FAILED),
            viewModel.uiState.notice,
        )
    }

    @Test
    fun refreshReminderGuidanceVisibility_hidesGuidanceAfterNotificationsReturn() {
        reminderNotificationStatusChecker.canPostNotifications = false
        val viewModel = newViewModel()

        reminderNotificationStatusChecker.canPostNotifications = true
        viewModel.refreshReminderGuidanceVisibility()

        assertEquals(false, viewModel.uiState.isReminderGuidanceVisible)
    }

    private fun newViewModel(): SettingsViewModel {
        return SettingsViewModel(
            versionName = "1.0",
            coffeeRepository = repository,
            coffeeBackupManager = backupManager,
            widgetPinRequester = widgetPinRequester,
            reminderNotificationStatusChecker = reminderNotificationStatusChecker,
            widgetUpdater = widgetUpdater,
        )
    }
}

private class FakeCoffeeRepository : CoffeeRepository {
    val todayCount = MutableStateFlow(0)
    val dailyCounts = MutableStateFlow<List<DailyCount>>(emptyList())
    var resetAllCalls: Int = 0

    override fun observeTodayCount(): Flow<Int> = todayCount

    override suspend fun getTodayCount(): Int = todayCount.value

    override fun observeOldestLoggedDate(): Flow<LocalDate?> = dailyCounts.map { counts ->
        counts.minOfOrNull(DailyCount::date)
    }

    override fun observeDailyCounts(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyCount>> = dailyCounts.map { counts ->
        counts.filter { dailyCount ->
            dailyCount.date >= startDate && dailyCount.date <= endDate
        }
    }

    override suspend fun setDailyCount(
        date: LocalDate,
        count: Int,
    ) {
        dailyCounts.value = dailyCounts.value
            .filterNot { dailyCount -> dailyCount.date == date }
            .let { counts ->
                if (count == 0) {
                    counts
                } else {
                    counts + DailyCount(date = date, count = count)
                }
            }
        if (date == LocalDate.of(2026, 3, 14)) {
            todayCount.value = count
        }
    }

    override suspend fun incrementToday() {
        todayCount.value += 1
    }

    override suspend fun decrementToday() {
        todayCount.value = (todayCount.value - 1).coerceAtLeast(0)
    }

    override suspend fun getAllCoffeeEvents(): List<CoffeeEvent> {
        return dailyCounts.value.flatMap { dailyCount ->
            List(dailyCount.count) { index ->
                CoffeeEvent(
                    reportedAtLocal = dailyCount.date.atStartOfDay().plusSeconds(index.toLong()),
                )
            }
        }
    }

    override suspend fun importCoffeeEvents(
        events: List<CoffeeEvent>,
        mode: CoffeeHistoryImportMode,
    ): CoffeeHistoryImportSummary {
        return CoffeeHistoryImportSummary(
            importedEvents = events.size,
            importedDays = events.map(CoffeeEvent::localDate).distinct().size,
            skippedDays = 0,
        )
    }

    override suspend fun resetAll() {
        resetAllCalls += 1
        todayCount.value = 0
        dailyCounts.value = emptyList()
    }

    fun seedTodayCount(count: Int) {
        todayCount.value = count
    }

    fun seedHistory(counts: List<DailyCount>) {
        dailyCounts.value = counts
    }
}

private class FakeCoffeeBackupManager : CoffeeBackupManager {
    var exportCalls: Int = 0
    var importCalls: Int = 0
    var exportResult: CoffeeBackupExportResult = CoffeeBackupExportResult.Success(0)
    var importResult: CoffeeBackupImportResult =
        CoffeeBackupImportResult.Success(CoffeeHistoryImportSummary(0, 0, 0))

    override suspend fun exportTo(uri: Uri): CoffeeBackupExportResult {
        exportCalls += 1
        return exportResult
    }

    override suspend fun importFrom(
        uri: Uri,
        mode: CoffeeHistoryImportMode,
    ): CoffeeBackupImportResult {
        importCalls += 1
        return importResult
    }
}

private class FakeCoffeeWidgetUpdater : CoffeeWidgetUpdater {
    var refreshCalls: Int = 0

    override suspend fun refresh() {
        refreshCalls += 1
    }
}

private class FakeWidgetPinRequester : WidgetPinRequester {
    var requestPinCalls: Int = 0
    var requestPinResult: Boolean = true
    var hasActiveWidget: Boolean = false

    override fun hasActiveWidget(): Boolean = hasActiveWidget

    override fun requestPin(): Boolean {
        requestPinCalls += 1
        return requestPinResult
    }
}

private class FakeReminderNotificationStatusChecker : ReminderNotificationStatusChecker {
    var canPostNotifications: Boolean = true

    override fun canPostReminderNotifications(): Boolean = canPostNotifications
}
