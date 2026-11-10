package com.example

import com.example.model.AudioMode
import com.example.model.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAudioModeNextCycle() {
        assertEquals(AudioMode.VIBRATE, AudioMode.NORMAL.next())
        assertEquals(AudioMode.SILENT, AudioMode.VIBRATE.next())
        assertEquals(AudioMode.DND, AudioMode.SILENT.next())
        assertEquals(AudioMode.NORMAL, AudioMode.DND.next())
    }

    @Test
    fun testScheduleRuleFormatting() {
        val rule = ScheduleRule(
            title = "Noche",
            daysOfWeek = "2,3,4,5,6",
            startHour = 23,
            startMinute = 5,
            endHour = 7,
            endMinute = 30,
            targetMode = AudioMode.SILENT
        )
        assertEquals("23:05 - 07:30", rule.getFormattedTime())
        assertEquals(listOf(2, 3, 4, 5, 6), rule.getDaysList())
    }

    @Test
    fun testDaysOfWeekParser() {
        val rule = ScheduleRule(
            title = "Fin de semana",
            daysOfWeek = "1,7",
            startHour = 0,
            startMinute = 0,
            endHour = 10,
            endMinute = 0,
            targetMode = AudioMode.NORMAL
        )
        val days = rule.getDaysList()
        assertEquals(2, days.size)
        assertTrue(days.contains(1))
        assertTrue(days.contains(7))
        assertFalse(days.contains(2))
    }
}
