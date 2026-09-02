package com.portfolio.fridgerescue.core.di

import android.content.Context
import com.portfolio.fridgerescue.core.data.DataDeletionManager
import com.portfolio.fridgerescue.core.data.LocalDataDeletionManager
import com.portfolio.fridgerescue.core.data.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.data.repository.IntakeDraftRepository
import com.portfolio.fridgerescue.core.data.repository.RoomFoodRepository
import com.portfolio.fridgerescue.core.data.repository.RoomIntakeDraftRepository
import com.portfolio.fridgerescue.feature.family.DataStoreFamilySyncSettingsRepository
import com.portfolio.fridgerescue.feature.family.FamilySyncManager
import com.portfolio.fridgerescue.feature.family.FamilySyncSettingsRepository
import com.portfolio.fridgerescue.feature.family.HttpFamilySyncGateway
import com.portfolio.fridgerescue.feature.intake.ReceiveBarcodeUseCase
import com.portfolio.fridgerescue.feature.intake.FindDuplicateCandidatesUseCase
import com.portfolio.fridgerescue.feature.intake.SaveIntakeCandidatesUseCase
import com.portfolio.fridgerescue.feature.intake.SharedContentReceiver
import com.portfolio.fridgerescue.feature.intake.UpdateIntakeCandidateUseCase
import com.portfolio.fridgerescue.feature.notification.DataStoreNotificationSettingsRepository
import com.portfolio.fridgerescue.feature.notification.NotificationSettingsRepository
import com.portfolio.fridgerescue.feature.notification.GetNotificationCandidatesUseCase
import com.portfolio.fridgerescue.feature.notification.GetStaleFoodCandidatesUseCase
import com.portfolio.fridgerescue.feature.notification.QuietHoursPolicy
import com.portfolio.fridgerescue.feature.report.GetReportMetricsUseCase
import com.portfolio.fridgerescue.feature.rescue.domain.FilterRescueQueueUseCase
import com.portfolio.fridgerescue.feature.rescue.domain.GetRescueQueueUseCase
import com.portfolio.fridgerescue.feature.rescue.domain.SaveFoodItemUseCase
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueDependencies
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): FridgeRescueDatabase =
        FridgeRescueDatabase.create(context)

    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun foodRepository(database: FridgeRescueDatabase, clock: Clock): FoodRepository =
        RoomFoodRepository(database, database.foodItemDao(), database.foodEventDao(), clock)

    @Provides
    @Singleton
    fun intakeDraftRepository(database: FridgeRescueDatabase, clock: Clock): IntakeDraftRepository =
        RoomIntakeDraftRepository(database.intakeDraftDao(), database.intakeCandidateDao(), clock)

    @Provides
    @Singleton
    fun notificationSettings(@ApplicationContext context: Context): NotificationSettingsRepository =
        DataStoreNotificationSettingsRepository(context)

    @Provides
    @Singleton
    fun familySettings(@ApplicationContext context: Context): FamilySyncSettingsRepository =
        DataStoreFamilySyncSettingsRepository(context)

    @Provides
    @Singleton
    fun familySyncManager(
        database: FridgeRescueDatabase,
        settings: FamilySyncSettingsRepository,
        clock: Clock,
    ): FamilySyncManager = FamilySyncManager(
        database,
        database.foodItemDao(),
        settings,
        HttpFamilySyncGateway(),
        clock,
    )

    @Provides
    @Singleton
    fun dataDeletionManager(
        @ApplicationContext context: Context,
        database: FridgeRescueDatabase,
        notificationSettings: NotificationSettingsRepository,
        familySettings: FamilySyncSettingsRepository,
    ): DataDeletionManager = LocalDataDeletionManager(
        context,
        database,
        notificationSettings,
        familySettings,
    )

    @Provides
    @Singleton
    fun sharedContentReceiver(
        @ApplicationContext context: Context,
        repository: IntakeDraftRepository,
    ) = SharedContentReceiver(context, repository)

    @Provides
    fun receiveBarcode(repository: IntakeDraftRepository) = ReceiveBarcodeUseCase(repository)

    @Provides
    fun quietHoursPolicy() = QuietHoursPolicy()

    @Provides
    fun getStaleFoods() = GetStaleFoodCandidatesUseCase()

    @Provides
    fun getNotificationCandidates() = GetNotificationCandidatesUseCase()

    @Provides
    fun rescueDependencies(
        foodRepository: FoodRepository,
        intakeDraftRepository: IntakeDraftRepository,
        notificationSettings: NotificationSettingsRepository,
        dataDeletionManager: DataDeletionManager,
        familySyncManager: FamilySyncManager,
        clock: Clock,
    ) = RescueDependencies(
        foodRepository = foodRepository,
        intakeDraftRepository = intakeDraftRepository,
        notificationSettingsRepository = notificationSettings,
        dataDeletionManager = dataDeletionManager,
        familySyncManager = familySyncManager,
        clock = clock,
        getRescueQueue = GetRescueQueueUseCase(),
        filterRescueQueue = FilterRescueQueueUseCase(),
        saveFoodItem = SaveFoodItemUseCase(foodRepository),
        saveCandidateBatch = SaveIntakeCandidatesUseCase(foodRepository, clock),
        updateIntakeCandidate = UpdateIntakeCandidateUseCase(intakeDraftRepository),
        findDuplicateCandidates = FindDuplicateCandidatesUseCase(),
        getReportMetrics = GetReportMetricsUseCase(),
    )
}
