package com.portfolio.fridgerescue.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.feature.notification.NotificationSettings

enum class AppSection { HOME, REPORT, SETTINGS }

@Composable
fun ReportContent(metrics: ReportMetrics, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
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
        SettingCard(
            title = stringResource(R.string.settings_data_title),
            description = stringResource(R.string.settings_data_description),
        )
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
