package com.portfolio.fridgerescue.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.model.FoodActionRequest
import com.portfolio.fridgerescue.core.model.FoodActionType
import com.portfolio.fridgerescue.core.model.FoodItemId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationFoodActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: FoodRepository

    override fun onReceive(context: Context, intent: Intent) {
        val foodId = intent.getStringExtra(EXTRA_FOOD_ID)?.takeIf(String::isNotBlank) ?: return
        val actionType = intent.action.toFoodActionType() ?: return
        val operationId = intent.getStringExtra(EXTRA_OPERATION_ID)
            ?.takeIf(String::isNotBlank)
            ?: UUID.randomUUID().toString()
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
