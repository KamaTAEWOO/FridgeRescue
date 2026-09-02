package com.portfolio.fridgerescue.feature.notification.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.portfolio.fridgerescue.core.domain.repository.FoodRepository
import com.portfolio.fridgerescue.core.domain.model.FoodActionRequest
import com.portfolio.fridgerescue.core.domain.model.FoodActionType
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** 알림 버튼 요청을 비동기로 완료하고, operation ID로 중복 전달을 방지한다. */
@AndroidEntryPoint
class NotificationFoodActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: FoodRepository

    override fun onReceive(context: Context, intent: Intent) {
        val foodId = intent.getStringExtra(EXTRA_FOOD_ID)?.takeIf(String::isNotBlank) ?: return
        val actionType = intent.action.toFoodActionType() ?: return
        val operationId = intent.getStringExtra(EXTRA_OPERATION_ID)
            ?.takeIf(String::isNotBlank)
            ?: UUID.randomUUID().toString()
        // BroadcastReceiver 수명 이후에도 Room 작업을 마칠 수 있도록 PendingResult를 유지한다.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repository.performAction(
                    FoodActionRequest(
                        foodItemId = FoodItemId(foodId),
                        type = actionType,
                        operationId = operationId,
                    ),
                )
                NotificationManagerCompat.from(context).cancel(ExpiryNotificationWorker.NOTIFICATION_ID)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CONSUME = "com.portfolio.fridgerescue.action.CONSUME"
        const val ACTION_STILL_HERE = "com.portfolio.fridgerescue.action.STILL_HERE"
        const val ACTION_DISCARD = "com.portfolio.fridgerescue.action.DISCARD"
        const val EXTRA_FOOD_ID = "food_id"
        const val EXTRA_OPERATION_ID = "operation_id"

        internal fun String?.toFoodActionType(): FoodActionType? = when (this) {
            ACTION_CONSUME -> FoodActionType.CONSUME
            ACTION_STILL_HERE -> FoodActionType.STILL_HERE
            ACTION_DISCARD -> FoodActionType.DISCARD
            else -> null
        }
    }
}
