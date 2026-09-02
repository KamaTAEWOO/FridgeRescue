package com.portfolio.fridgerescue.debug

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.portfolio.fridgerescue.core.data.local.database.FridgeRescueDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class DemoDataSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SEED) return

        val database = FridgeRescueDatabase.create(context)
        runCatching {
            runBlocking(Dispatchers.IO) {
                DemoDataSeeder(database).replace()
            }
        }.onSuccess { result ->
            resultCode = Activity.RESULT_OK
            resultData = "foodItems=${result.foodItemCount},events=${result.eventCount}"
            Log.i(TAG, "Demo seed complete: $result")
        }.onFailure { error ->
            resultCode = Activity.RESULT_CANCELED
            resultData = error.message
            Log.e(TAG, "Demo seed failed", error)
        }
        database.close()
    }

    companion object {
        const val ACTION_SEED = "com.portfolio.fridgerescue.action.SEED_DEMO_DATA"
        private const val TAG = "FridgeRescueDemo"
    }
}
