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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gsusmonzon.coffeecounter.CoffeeCounterApplication
import com.gsusmonzon.coffeecounter.BuildConfig
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.widget.CoffeeWidgetUpdater
import com.gsusmonzon.coffeecounter.widget.GlanceCoffeeWidgetUpdater
import kotlinx.coroutines.launch

data class SettingsUiState(
    val versionName: String,
    val isResetConfirmationVisible: Boolean = false,
)

class SettingsViewModel(
    versionName: String,
    private val coffeeRepository: CoffeeRepository,
    private val widgetUpdater: CoffeeWidgetUpdater,
) : ViewModel() {
    var uiState by mutableStateOf(
        SettingsUiState(versionName = versionName)
    )
        private set

    fun onResetAllClick() {
        uiState = uiState.copy(isResetConfirmationVisible = true)
    }

    fun onDismissResetConfirmation() {
        uiState = uiState.copy(isResetConfirmationVisible = false)
    }

    fun onConfirmResetAll() {
        uiState = uiState.copy(isResetConfirmationVisible = false)

        viewModelScope.launch {
            coffeeRepository.resetAll()
            widgetUpdater.refresh()
        }
    }

    companion object {
        fun factory(
            versionName: String,
            coffeeRepository: CoffeeRepository,
            widgetUpdater: CoffeeWidgetUpdater,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    versionName = versionName,
                    coffeeRepository = coffeeRepository,
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
    val appContainer = context.appContainer()
    val widgetUpdater = remember(context.applicationContext) {
        GlanceCoffeeWidgetUpdater(context.applicationContext)
    }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            versionName = BuildConfig.VERSION_NAME,
            coffeeRepository = appContainer.coffeeRepository,
            widgetUpdater = widgetUpdater,
        )
    )

    SettingsScreen(
        uiState = viewModel.uiState,
        onResetAllClick = viewModel::onResetAllClick,
        onDismissResetConfirmation = viewModel::onDismissResetConfirmation,
        onConfirmResetAll = viewModel::onConfirmResetAll,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onResetAllClick: () -> Unit,
    onDismissResetConfirmation: () -> Unit,
    onConfirmResetAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
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
                        text = stringResource(R.string.reset_all_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.reset_all_supporting_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onResetAllClick,
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text(text = stringResource(R.string.reset_all_button_label))
                    }
                }
            }
        }
    }

    if (uiState.isResetConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onDismissResetConfirmation,
            title = { Text(text = stringResource(R.string.reset_all_confirmation_title)) },
            text = { Text(text = stringResource(R.string.reset_all_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmResetAll) {
                    Text(text = stringResource(R.string.reset_all_confirm_label))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissResetConfirmation) {
                    Text(text = stringResource(R.string.reset_all_cancel_label))
                }
            },
        )
    }
}

private fun Context.appContainer() = (applicationContext as CoffeeCounterApplication).appContainer
