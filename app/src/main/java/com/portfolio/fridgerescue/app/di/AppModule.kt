package com.portfolio.fridgerescue.app.di

import android.content.Context
import com.portfolio.fridgerescue.core.data.repository.LocalDataDeletionManager
import com.portfolio.fridgerescue.core.data.local.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.core.domain.repository.DataDeletionManager
import com.portfolio.fridgerescue.core.domain.repository.FoodRepository
import com.portfolio.fridgerescue.core.data.repository.RoomFoodRepository
import com.portfolio.fridgerescue.core.data.repository.RoomIntakeDraftRepository
import com.portfolio.fridgerescue.core.domain.repository.IntakeDraftRepository
import com.portfolio.fridgerescue.feature.family.data.FamilySyncManager
import com.portfolio.fridgerescue.feature.family.data.DataStoreFamilySyncSettingsRepository
import com.portfolio.fridgerescue.feature.family.data.HttpFamilySyncGateway
import com.portfolio.fridgerescue.feature.family.domain.FamilySyncService
import com.portfolio.fridgerescue.feature.family.domain.FamilySyncSettingsRepository
import com.portfolio.fridgerescue.feature.intake.domain.ReceiveBarcodeUseCase
import com.portfolio.fridgerescue.feature.intake.domain.FindDuplicateCandidatesUseCase
import com.portfolio.fridgerescue.feature.intake.domain.SaveIntakeCandidatesUseCase
import com.portfolio.fridgerescue.feature.intake.data.SharedContentReceiver
import com.portfolio.fridgerescue.feature.intake.domain.UpdateIntakeCandidateUseCase
import com.portfolio.fridgerescue.feature.notification.data.DataStoreNotificationSettingsRepository
import com.portfolio.fridgerescue.feature.notification.domain.GetNotificationCandidatesUseCase
import com.portfolio.fridgerescue.feature.notification.domain.GetStaleFoodCandidatesUseCase
import com.portfolio.fridgerescue.feature.notification.domain.QuietHoursPolicy
import com.portfolio.fridgerescue.feature.notification.domain.NotificationSettingsRepository
import com.portfolio.fridgerescue.feature.report.domain.GetReportMetricsUseCase
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

/**
 * 앱 수명과 함께 공유해야 하는 데이터 계층과 진입점 의존성을 구성한다.
 * UI·Worker·Receiver가 구현 클래스를 직접 만들지 않도록 생성 책임을 이 모듈에 모은다.
 */
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

    // DB와 Repository는 하나의 인스턴스를 공유해 모든 Flow가 같은 데이터 변경을 관찰한다.
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
    ): FamilySyncService = FamilySyncManager(
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
        familySyncManager: FamilySyncService,
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
