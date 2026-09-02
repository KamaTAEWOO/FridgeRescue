package com.portfolio.fridgerescue.feature.notification.domain

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuietHoursPolicyTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val policy = QuietHoursPolicy()

    @Test
    fun TC_NOTIFY_006_late_notification_is_deferred_until_morning() {
        val now = ZonedDateTime.of(2026, 9, 2, 23, 30, 0, 0, zone)

        assertEquals(
            Duration.ofHours(8).plusMinutes(30),
            policy.delayUntilAllowed(now, NotificationSettings()),
        )
    }

    @Test
    fun TC_NOTIFY_006_daytime_or_disabled_quiet_hours_do_not_defer() {
        val daytime = ZonedDateTime.of(2026, 9, 2, 12, 0, 0, 0, zone)
        val late = ZonedDateTime.of(2026, 9, 2, 23, 0, 0, 0, zone)

        assertNull(policy.delayUntilAllowed(daytime, NotificationSettings()))
        assertNull(
            policy.delayUntilAllowed(late, NotificationSettings(quietHoursEnabled = false)),
        )
    }
}
