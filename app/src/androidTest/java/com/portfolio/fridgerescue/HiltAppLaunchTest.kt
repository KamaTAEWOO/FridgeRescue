package com.portfolio.fridgerescue

import com.portfolio.fridgerescue.app.MainActivity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiltAppLaunchTest {
    @Test
    fun TC_ARCH_001_hilt_graph_starts_main_activity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use {
            it.moveToState(Lifecycle.State.RESUMED)
            it.onActivity { activity -> assertFalse(activity.isFinishing) }
        }
    }
}
