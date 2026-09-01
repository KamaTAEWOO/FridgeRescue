package com.portfolio.fridgerescue.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.feature.notification.NotificationSettings
import com.portfolio.fridgerescue.feature.rescue.presentation.FamilySyncFeedback
import com.portfolio.fridgerescue.feature.rescue.presentation.FamilySyncUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class AppSection { HOME, REPORT, SETTINGS }

@Composable
fun ReportContent(metrics: ReportMetrics, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.report_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(R.string.report_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                count = metrics.rescuedCount,
                label = stringResource(R.string.report_rescued),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                count = metrics.discardedCount,
                label = stringResource(R.string.report_discarded),
                modifier = Modifier.weight(1f),
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.report_savings_title),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.report_savings_unknown),
                    modifier = Modifier.padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        metrics.shoppingHint?.let { hint ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(stringResource(R.string.report_hint_title), fontWeight = FontWeight.Bold)
                    Text(hint, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(count: Int, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("${count}개", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsContent(
    notificationsEnabled: Boolean,
    notificationSettings: NotificationSettings,
    onOpenNotificationSettings: () -> Unit,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    isDeletingData: Boolean = false,
    familySyncState: FamilySyncUiState = FamilySyncUiState(),
    onDeleteAllData: () -> Unit = {},
    onCreateFamilyAccount: (String, String) -> Unit = { _, _ -> },
    onJoinFamily: (String) -> Unit = {},
    onSyncFamily: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        SettingCard(
            title = stringResource(R.string.settings_notification_title),
            description = if (notificationsEnabled) {
                stringResource(R.string.settings_notification_on)
            } else {
                stringResource(R.string.settings_notification_off)
            },
        ) {
            Button(onClick = onOpenNotificationSettings) {
                Text(stringResource(R.string.settings_notification_action))
            }
        }
        SettingCard(
            title = stringResource(R.string.settings_quiet_hours_title),
            description = stringResource(
                R.string.settings_quiet_hours_description,
                notificationSettings.quietStartHour,
                notificationSettings.quietEndHour,
            ),
        ) {
            Switch(
                checked = notificationSettings.quietHoursEnabled,
                onCheckedChange = onQuietHoursEnabledChange,
            )
        }
        SettingCard(
            title = stringResource(R.string.settings_privacy_title),
            description = stringResource(R.string.settings_privacy_description),
        )
        FamilySyncCard(
            state = familySyncState,
            onCreateAccount = onCreateFamilyAccount,
            onJoinFamily = onJoinFamily,
            onSync = onSyncFamily,
        )
        SettingCard(
            title = stringResource(R.string.settings_data_title),
            description = stringResource(R.string.settings_data_description),
        ) {
            Button(
                onClick = { showDeleteConfirmation = true },
                enabled = !isDeletingData,
            ) {
                Text(
                    if (isDeletingData) {
                        stringResource(R.string.settings_delete_in_progress)
                    } else {
                        stringResource(R.string.settings_delete_action)
                    },
                )
            }
        }
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteAllData()
                    },
                ) { Text(stringResource(R.string.settings_delete_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.editor_cancel))
                }
            },
        )
    }
}

@Composable
private fun FamilySyncCard(
    state: FamilySyncUiState,
    onCreateAccount: (String, String) -> Unit,
    onJoinFamily: (String) -> Unit,
    onSync: () -> Unit,
) {
    val settings = state.settings
    var serverUrl by rememberSaveable(settings.serverBaseUrl) {
        mutableStateOf(settings.serverBaseUrl)
    }
    var displayName by rememberSaveable { mutableStateOf("") }
    var inviteCode by rememberSaveable { mutableStateOf("") }
    SettingCard(
        title = stringResource(R.string.family_title),
        description = if (settings.isConnected) {
            stringResource(
                R.string.family_connected_description,
                settings.displayName.orEmpty(),
                settings.familyName.orEmpty(),
            )
        } else stringResource(R.string.family_disconnected_description),
    ) {
        if (!settings.isConnected) {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.family_server_url)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text(stringResource(R.string.family_display_name)) },
                singleLine = true,
            )
            Button(
                onClick = { onCreateAccount(serverUrl, displayName) },
                modifier = Modifier.padding(top = 10.dp),
                enabled = !state.isWorking && serverUrl.isNotBlank() && displayName.isNotBlank(),
            ) { Text(stringResource(R.string.family_create)) }
        } else {
            Text(
                text = stringResource(R.string.family_invite_code, settings.inviteCode.orEmpty()),
                modifier = Modifier.padding(top = 4.dp),
                fontWeight = FontWeight.Bold,
            )
            settings.lastSyncedAtEpochMillis?.let { syncedAt ->
                val formatted = remember(syncedAt) {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .format(Instant.ofEpochMilli(syncedAt).atZone(ZoneId.systemDefault()))
                }
                Text(
                    stringResource(R.string.family_last_sync, formatted),
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase() },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                label = { Text(stringResource(R.string.family_join_code)) },
                singleLine = true,
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onJoinFamily(inviteCode) },
                    enabled = !state.isWorking && inviteCode.isNotBlank(),
                ) { Text(stringResource(R.string.family_join)) }
                Button(onClick = onSync, enabled = !state.isWorking) {
                    Text(stringResource(R.string.family_sync_now))
                }
            }
        }
        val feedback = when (state.feedback) {
            FamilySyncFeedback.ACCOUNT_CREATED -> stringResource(R.string.family_created_done)
            FamilySyncFeedback.FAMILY_JOINED -> stringResource(R.string.family_joined_done)
            FamilySyncFeedback.SYNCED -> stringResource(R.string.family_sync_done)
            FamilySyncFeedback.FAILED -> stringResource(R.string.family_sync_failed)
            null -> if (state.isWorking) stringResource(R.string.family_working) else null
        }
        feedback?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 10.dp),
                color = if (state.feedback == FamilySyncFeedback.FAILED) {
                    MaterialTheme.colorScheme.error
                } else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    description: String,
    content: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                description,
                modifier = Modifier.padding(top = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (content != null) {
                Column(modifier = Modifier.padding(top = 12.dp)) { content() }
            }
        }
    }
}
