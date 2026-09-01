package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.core.model.FoodDate
import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.core.model.StorageLocation
import com.portfolio.fridgerescue.feature.rescue.components.FoodRescueCard
import com.portfolio.fridgerescue.feature.intake.IntakeDraftSheet
import com.portfolio.fridgerescue.feature.intake.IntakeOptionsSheet
import com.portfolio.fridgerescue.feature.rescue.domain.GetRescueQueueUseCase
import com.portfolio.fridgerescue.feature.report.AppSection
import com.portfolio.fridgerescue.feature.report.ReportContent
import com.portfolio.fridgerescue.feature.report.ReportMetrics
import com.portfolio.fridgerescue.feature.report.SettingsContent
import com.portfolio.fridgerescue.feature.notification.NotificationSettings
import com.portfolio.fridgerescue.ui.theme.FridgeRescueTheme
import java.time.LocalDate

@Composable
fun RescueScreen(
    uiState: RescueUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (RescueAction) -> Unit,
    onPickReceipt: () -> Unit = {},
    onCaptureReceipt: () -> Unit = {},
    notificationsEnabled: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    selectedSection: AppSection = AppSection.HOME,
    reportMetrics: ReportMetrics = ReportMetrics(),
    notificationSettings: NotificationSettings = NotificationSettings(),
    onSectionSelected: (AppSection) -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onQuietHoursEnabledChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState is RescueUiState.Content) {
                NavigationBar {
                    AppSection.entries.forEach { section ->
                        NavigationBarItem(
                            selected = selectedSection == section,
                            onClick = { onSectionSelected(section) },
                            icon = {
                                Icon(
                                    imageVector = section.icon(),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(section.label()) },
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (uiState) {
                RescueUiState.Loading -> LoadingContent()
                is RescueUiState.Content -> when (selectedSection) {
                    AppSection.HOME -> RescueContent(
                        uiState = uiState,
                        onAction = onAction,
                        notificationsEnabled = notificationsEnabled,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                    )
                    AppSection.REPORT -> ReportContent(reportMetrics)
                    AppSection.SETTINGS -> SettingsContent(
                        notificationsEnabled = notificationsEnabled,
                        notificationSettings = notificationSettings,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        onQuietHoursEnabledChange = onQuietHoursEnabledChange,
                    )
                }
            }
        }
    }

    val editor = (uiState as? RescueUiState.Content)?.editor?.takeIf {
        selectedSection == AppSection.HOME
    }
    if (editor != null) {
        FoodEditorSheet(
            state = editor,
            onAction = onAction,
        )
    }
    val detail = (uiState as? RescueUiState.Content)?.detail?.takeIf {
        selectedSection == AppSection.HOME
    }
    if (detail != null) {
        FoodDetailSheet(state = detail, onAction = onAction)
    }
    val intakeReview = (uiState as? RescueUiState.Content)?.intakeReview?.takeIf {
        selectedSection == AppSection.HOME
    }
    if (intakeReview != null) {
        IntakeDraftSheet(
            state = intakeReview,
            onDismiss = {
                onAction(RescueAction.DismissIntakeDraft(intakeReview.draft.id))
            },
            onCandidateSelected = { id, selected ->
                onAction(RescueAction.ToggleIntakeCandidate(id, selected))
            },
            onSave = { onAction(RescueAction.SaveIntakeCandidates(intakeReview.draft.id)) },
            onManualEntry = {
                onAction(RescueAction.StartManualFromIntake(intakeReview.draft.id))
            },
        )
    }
    val showImportOptions = selectedSection == AppSection.HOME &&
        (uiState as? RescueUiState.Content)?.showImportOptions == true
    if (showImportOptions) {
        IntakeOptionsSheet(
            onDismiss = { onAction(RescueAction.DismissImportOptions) },
            onCaptureReceipt = {
                onAction(RescueAction.DismissImportOptions)
                onCaptureReceipt()
            },
            onPickReceipt = {
                onAction(RescueAction.DismissImportOptions)
                onPickReceipt()
            },
            onManualEntry = { onAction(RescueAction.StartAddFood) },
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.rescue_loading),
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RescueContent(
    uiState: RescueUiState.Content,
    onAction: (RescueAction) -> Unit,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxSize()
                    .widthIn(max = 1120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(0.42f),
                    contentPadding = PaddingValues(start = 24.dp, top = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    overviewItems(
                        uiState = uiState,
                        onAction = onAction,
                        notificationsEnabled = notificationsEnabled,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(0.58f),
                    contentPadding = PaddingValues(start = 12.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    queueItems(uiState, onAction)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                overviewItems(
                    uiState = uiState,
                    onAction = onAction,
                    notificationsEnabled = notificationsEnabled,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                )
                queueItems(uiState, onAction)
            }
        }
    }
}

private fun LazyListScope.overviewItems(
    uiState: RescueUiState.Content,
    onAction: (RescueAction) -> Unit,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
) {
    item {
        RescueHeader(
            onImport = { onAction(RescueAction.OpenImportOptions) },
            onAddFood = { onAction(RescueAction.StartAddFood) },
        )
    }
    item {
        SummaryRow(
            urgentCount = uiState.urgentCount,
            needsReviewCount = uiState.needsReviewCount,
        )
    }
    if (uiState.urgentCount > 0 && !notificationsEnabled) {
        item { NotificationPermissionCard(onRequestNotificationPermission) }
    }
}

private fun LazyListScope.queueItems(
    uiState: RescueUiState.Content,
    onAction: (RescueAction) -> Unit,
) {
    item {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.rescue_queue_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.rescue_queue_description),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (uiState.items.isEmpty()) {
        item { EmptyContent() }
    } else {
        items(items = uiState.items, key = { it.foodItem.id.value }) { queueItem ->
            FoodRescueCard(
                queueItem = queueItem,
                onEdit = { onAction(RescueAction.StartEditFood(queueItem.foodItem.id)) },
                onOpenActions = { onAction(RescueAction.OpenFoodActions(queueItem.foodItem.id)) },
                onMarkConsumed = { onAction(RescueAction.MarkConsumed(queueItem.foodItem.id)) },
            )
        }
    }
    item { Spacer(modifier = Modifier.height(16.dp)) }
}

@Composable
private fun NotificationPermissionCard(onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.notification_permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.notification_permission_description),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            androidx.compose.material3.Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(top = 10.dp),
            ) { Text(stringResource(R.string.notification_permission_action)) }
        }
    }
}

@Composable
private fun RescueHeader(onImport: () -> Unit, onAddFood: () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            )
            Text(
                text = stringResource(R.string.rescue_eyebrow),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = stringResource(R.string.rescue_title),
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(R.string.rescue_subtitle),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.padding(top = 14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = stringResource(R.string.rescue_storage_badge),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.Button(
            onClick = onImport,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.rescue_import),
                modifier = Modifier.padding(vertical = 3.dp),
                fontWeight = FontWeight.Bold,
            )
        }
        androidx.compose.material3.OutlinedButton(
            onClick = onAddFood,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.rescue_add_food),
                modifier = Modifier.padding(vertical = 3.dp),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    urgentCount: Int,
    needsReviewCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SummaryTile(
            label = stringResource(R.string.rescue_urgent_label),
            count = urgentCount,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f),
        )
        SummaryTile(
            label = stringResource(R.string.rescue_needs_review_label),
            count = needsReviewCount,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryTile(
    label: String,
    count: Int,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.rescue_count_format, count),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = label,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.rescue_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.rescue_empty_description),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppSection.label(): String = when (this) {
    AppSection.HOME -> stringResource(R.string.nav_home)
    AppSection.REPORT -> stringResource(R.string.nav_report)
    AppSection.SETTINGS -> stringResource(R.string.nav_settings)
}

private fun AppSection.icon() = when (this) {
    AppSection.HOME -> Icons.Filled.Home
    AppSection.REPORT -> Icons.AutoMirrored.Filled.List
    AppSection.SETTINGS -> Icons.Filled.Settings
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun RescueScreenPreview() {
    val today = LocalDate.of(2026, 9, 1)
    val items = listOf(
        FoodItem(
            id = FoodItemId("preview-spinach"),
            name = "시금치",
            quantity = 1,
            storageLocation = StorageLocation.REFRIGERATED,
            dates = listOf(FoodDate(today, FoodDateSource.APP_ESTIMATED)),
            isOpened = true,
        ),
        FoodItem(
            id = FoodItemId("preview-tofu"),
            name = "찌개용 두부",
            quantity = 2,
            storageLocation = StorageLocation.REFRIGERATED,
            dates = listOf(FoodDate(today.plusDays(1), FoodDateSource.MANUFACTURER_DISPLAYED)),
        ),
        FoodItem(
            id = FoodItemId("preview-dumplings"),
            name = "냉동 만두",
            storageLocation = StorageLocation.FROZEN,
            status = FoodStatus.NEEDS_REVIEW,
        ),
    )
    val queue = GetRescueQueueUseCase()(items, today)

    FridgeRescueTheme {
        RescueScreen(
            uiState = RescueUiState.Content(
                items = queue,
                urgentCount = 2,
                needsReviewCount = 1,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}
