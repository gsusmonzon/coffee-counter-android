package com.gsusmonzon.coffeecounter.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.ui.UiTestTags
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun HistoryEditDialog(
    uiState: HistoryEditDialogUiState,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onSaveClick: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(UiTestTags.HOME_HISTORY_EDIT_DIALOG),
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = {
            Text(text = stringResource(R.string.history_edit_title, uiState.title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.history_edit_supporting_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = uiState.input,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.history_edit_count_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (!uiState.isSaveEnabled) {
                    Text(
                        text = stringResource(R.string.history_edit_invalid_message),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSaveClick,
                enabled = uiState.isSaveEnabled,
            ) {
                Text(text = stringResource(R.string.history_edit_save_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.history_edit_cancel_label))
            }
        },
    )
}
