package com.gsusmonzon.coffeecounter.data.backup

import com.gsusmonzon.coffeecounter.data.model.CoffeeEvent
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CoffeeHistoryBackupJsonCodecTest {
    private val codec = CoffeeHistoryBackupJsonCodec()

    @Test
    fun encode_writesExpectedTopLevelFields() {
        val json = codec.encode(
            CoffeeHistoryBackup(
                schemaVersion = 1,
                appVersion = "1.1",
                exportedAt = "2026-03-21T10:00:00Z",
                events = listOf(
                    CoffeeEvent(LocalDateTime.of(2026, 3, 14, 8, 30, 0)),
                ),
            ),
        )

        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"appVersion\": \"1.1\""))
        assertTrue(json.contains("\"exportedAt\": \"2026-03-21T10:00:00Z\""))
        assertTrue(json.contains("\"reportedAtLocal\": \"2026-03-14T08:30:00\""))
    }

    @Test
    fun decode_rejectsNewerSchemaVersion() {
        val result = codec.decode(
            """
            {
              "schemaVersion": 2,
              "appVersion": "9.9",
              "exportedAt": "2026-03-21T10:00:00Z",
              "events": []
            }
            """.trimIndent(),
        )

        assertTrue(result.isFailure)
        assertEquals(
            CoffeeBackupFailureReason.UNSUPPORTED_NEWER_SCHEMA.name,
            result.exceptionOrNull()?.message,
        )
    }
}
