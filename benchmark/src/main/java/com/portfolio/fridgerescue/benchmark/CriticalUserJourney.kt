package com.portfolio.fridgerescue.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By

internal const val TARGET_PACKAGE = "com.portfolio.fridgerescue"

internal fun MacrobenchmarkScope.openPantryAndSettings() {
    device.findObject(By.text("재료"))?.click()
    device.waitForIdle()
    device.findObject(By.text("설정"))?.click()
    device.waitForIdle()
}
