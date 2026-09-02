package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.testTag
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
import com.portfolio.fridgerescue.feature.rescue.domain.PantryStatusFilter
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
    onScanBarcode: () -> Unit = {},
    notificationsEnabled: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    selectedSection: AppSection = AppSection.HOME,
    reportMetrics: ReportMetrics = ReportMetrics(),
    notificationSettings: NotificationSettings = NotificationSettings(),
    isDeletingData: Boolean = false,
    familySyncState: FamilySyncUiState = FamilySyncUiState(),
    onDeleteAllData: () -> Unit = {},
    onCreateFamilyAccount: (String, String) -> Unit = { _, _ -> },
    onJoinFamily: (String) -> Unit = {},
    onSyncFamily: () -> Unit = {},
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
                RescueNavigationBar(
                    selectedSection = selectedSection,
                    onSectionSelected = onSectionSelected,
                )
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
                        isDeletingData = isDeletingData,
                        familySyncState = familySyncState,
                        onDeleteAllData = onDeleteAllData,
                        onCreateFamilyAccount = onCreateFamilyAccount,
                        onJoinFamily = onJoinFamily,
                        onSyncFamily = onSyncFamily,
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
            onCandidateUpdated = { id, name, quantity ->
                onAction(RescueAction.UpdateIntakeCandidate(id, name, quantity))
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
            onScanBarcode = {
                onAction(RescueAction.DismissImportOptions)
                onScanBarcode()
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
                    contentPadding = PaddingValues(start = 24.dp, top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    overviewItems(
                        uiState = uiState,
                        onAction = onAction,
                        notificationsEnabled = notificationsEnabled,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(0.58f)
                        .testTag(PantryFilterTestTags.LIST),
                    contentPadding = PaddingValues(start = 10.dp, end = 24.dp, top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    queueItems(uiState, onAction)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag(PantryFilterTestTags.LIST),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
            urgentCount = uiState.urgentCount,
            needsReviewCount = uiState.needsReviewCount,
            onImport = { onAction(RescueAction.OpenImportOptions) },
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
        PantryFilterPanel(
            totalItemCount = uiState.totalItemCount,
            filter = uiState.pantryFilter,
            onAction = onAction,
        )
    }
    if (uiState.items.isEmpty()) {
        item {
            if (uiState.totalItemCount > 0 && uiState.pantryFilter.isActive) {
                FilterEmptyContent { onAction(RescueAction.ClearPantryFilters) }
            } else {
                EmptyContent()
            }
        }
    } else {
        items(items = uiState.items, key = { it.foodItem.id.value }) { queueItem ->
            FoodRescueCard(
                queueItem = queueItem,
                onOpenActions = { onAction(RescueAction.OpenFoodActions(queueItem.foodItem.id)) },
                onMarkConsumed = { onAction(RescueAction.MarkConsumed(queueItem.foodItem.id)) },
            )
        }
    }
    item { Spacer(modifier = Modifier.height(16.dp)) }
}

object PantryFilterTestTags {
    const val LIST = "pantry-list"
    const val SEARCH = "pantry-filter-search"
    const val TOGGLE = "pantry-filter-toggle"
    const val CLEAR = "pantry-filter-clear"
}

@Composable
private fun PantryFilterPanel(
    totalItemCount: Int,
    filter: com.portfolio.fridgerescue.feature.rescue.domain.PantryFilter,
    onAction: (RescueAction) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(filter.isActive) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.pantry_title_count, totalItemCount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (filter.isActive) {
                    TextButton(onClick = { onAction(RescueAction.ClearPantryFilters) }) {
                        Text(stringResource(R.string.pantry_filter_clear))
                    }
                }
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag(PantryFilterTestTags.TOGGLE),
                ) {
                    Text(
                        stringResource(
                            if (expanded) R.string.pantry_filter_close else R.string.pantry_filter_open,
                        ),
                    )
                }
            }
        }
        if (!expanded) return@Column
        OutlinedTextField(
            value = filter.query,
            onValueChange = { onAction(RescueAction.ChangePantrySearch(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PantryFilterTestTags.SEARCH),
            shape = RoundedCornerShape(16.dp),
            label = { Text(stringResource(R.string.pantry_search_label)) },
            placeholder = { Text(stringResource(R.string.pantry_search_placeholder)) },
            singleLine = true,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StorageLocationFilterChip(
                label = stringResource(R.string.filter_all),
                selected = filter.storageLocation == null,
                onClick = { onAction(RescueAction.FilterPantryStorage(null)) },
            )
            StorageLocation.entries.forEach { location ->
                StorageLocationFilterChip(
                    label = location.filterLabel(),
                    selected = filter.storageLocation == location,
                    onClick = { onAction(RescueAction.FilterPantryStorage(location)) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PantryStatusFilter.entries.forEach { status ->
                FilterChip(
                    selected = filter.status == status,
                    onClick = { onAction(RescueAction.FilterPantryStatus(status)) },
                    label = { Text(status.label()) },
                )
            }
        }
    }
}

@Composable
private fun StorageLocationFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun PantryStatusFilter.label(): String = when (this) {
    PantryStatusFilter.ALL -> stringResource(R.string.filter_status_all)
    PantryStatusFilter.ACTIVE -> stringResource(R.string.filter_status_active)
    PantryStatusFilter.NEEDS_REVIEW -> stringResource(R.string.filter_status_review)
}

@Composable
private fun StorageLocation.filterLabel(): String = when (this) {
    StorageLocation.REFRIGERATED -> stringResource(R.string.storage_refrigerated)
    StorageLocation.FROZEN -> stringResource(R.string.storage_frozen)
    StorageLocation.ROOM_TEMPERATURE -> stringResource(R.string.storage_room_temperature)
}

@Composable
private fun FilterEmptyContent(onClear: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.pantry_filter_empty), fontWeight = FontWeight.Bold)
            androidx.compose.material3.TextButton(
                onClick = onClear,
                modifier = Modifier.testTag(PantryFilterTestTags.CLEAR),
            ) { Text(stringResource(R.string.pantry_filter_clear)) }
        }
    }
}

@Composable
private fun NotificationPermissionCard(onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notification_permission_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.notification_permission_description),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.TextButton(
                onClick = onRequestPermission,
                modifier = Modifier.padding(start = 4.dp),
            ) { Text(stringResource(R.string.notification_permission_action)) }
        }
    }
}

@Composable
private fun RescueHeader(
    urgentCount: Int,
    needsReviewCount: Int,
    onImport: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.rescue_eyebrow),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.rescue_title),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(
                R.string.rescue_summary_sentence,
                urgentCount,
                needsReviewCount,
            ),
            modifier = Modifier.padding(top = 3.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.material3.Button(
            onClick = onImport,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.rescue_add_food),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RescueNavigationBar(
    selectedSection: AppSection,
    onSectionSelected: (AppSection) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppSection.entries.forEach { section ->
                    val selected = selectedSection == section
                    val foreground = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            )
                            .selectable(
                                selected = selected,
                                onClick = { onSectionSelected(section) },
                                role = Role.Tab,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = section.icon(),
                            contentDescription = null,
                            tint = foreground,
                            modifier = Modifier.size(21.dp),
                        )
                        Text(
                            text = section.label(),
                            modifier = Modifier.padding(top = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = foreground,
                        )
                    }
                }
            }
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
