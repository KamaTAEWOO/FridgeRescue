package com.portfolio.fridgerescue.feature.notification

import com.portfolio.fridgerescue.core.model.FoodActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationFoodActionReceiverTest {
    @Test
    fun TC_NOTIFY_010_only_known_notification_actions_are_mapped() {
        with(NotificationFoodActionReceiver) {
            assertEquals(FoodActionType.CONSUME, ACTION_CONSUME.toFoodActionType())
            assertEquals(FoodActionType.STILL_HERE, ACTION_STILL_HERE.toFoodActionType())
            assertEquals(FoodActionType.DISCARD, ACTION_DISCARD.toFoodActionType())
            assertNull("unknown".toFoodActionType())
        }
    }
}
