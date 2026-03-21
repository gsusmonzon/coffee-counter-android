package com.gsusmonzon.coffeecounter.data.backup

import android.net.Uri
import com.gsusmonzon.coffeecounter.data.model.CoffeeEvent
import com.gsusmonzon.coffeecounter.data.model.toCoffeeEventLocalDateTime
import com.gsusmonzon.coffeecounter.data.model.toStorageTimestamp
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import java.io.IOException
import java.time.Clock
import java.time.Instant
import org.json.JSONException
import org.json.JSONObject

const val COFFEE_HISTORY_BACKUP_SCHEMA_VERSION = 1

enum class CoffeeHistoryImportMode {
    MERGE,
    REPLACE,
}

data class CoffeeHistoryImportSummary(
    val importedEvents: Int,
    val importedDays: Int,
    val skippedDays: Int,
)

data class CoffeeHistoryBackup(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: String,
    val events: List<CoffeeEvent>,
)

enum class CoffeeBackupFailureReason {
    FILE_READ_FAILED,
    FILE_WRITE_FAILED,
    INVALID_FORMAT,
    UNSUPPORTED_NEWER_SCHEMA,
}

sealed interface CoffeeBackupExportResult {
    data class Success(val exportedEvents: Int) : CoffeeBackupExportResult

    data class Failure(val reason: CoffeeBackupFailureReason) : CoffeeBackupExportResult
}

sealed interface CoffeeBackupImportResult {
    data class Success(val summary: CoffeeHistoryImportSummary) : CoffeeBackupImportResult

    data class Failure(val reason: CoffeeBackupFailureReason) : CoffeeBackupImportResult
}

interface CoffeeBackupManager {
    suspend fun exportTo(uri: Uri): CoffeeBackupExportResult

    suspend fun importFrom(
        uri: Uri,
        mode: CoffeeHistoryImportMode,
    ): CoffeeBackupImportResult
}

class CoffeeHistoryBackupJsonCodec {
    fun encode(backup: CoffeeHistoryBackup): String {
        val eventsJson = org.json.JSONArray().apply {
            backup.events.forEach { event ->
                put(
                    JSONObject().put(
                        "reportedAtLocal",
                        event.toStorageTimestamp(),
                    ),
                )
            }
        }

        return JSONObject()
            .put("schemaVersion", backup.schemaVersion)
            .put("appVersion", backup.appVersion)
            .put("exportedAt", backup.exportedAt)
            .put("events", eventsJson)
            .toString(2)
    }

    fun decode(json: String): Result<CoffeeHistoryBackup> {
        return runCatching {
            val root = JSONObject(json)
            val schemaVersion = root.getInt("schemaVersion")
            require(schemaVersion <= COFFEE_HISTORY_BACKUP_SCHEMA_VERSION) {
                CoffeeBackupFailureReason.UNSUPPORTED_NEWER_SCHEMA.name
            }
            require(schemaVersion > 0) {
                CoffeeBackupFailureReason.INVALID_FORMAT.name
            }

            val eventsJson = root.getJSONArray("events")
            CoffeeHistoryBackup(
                schemaVersion = schemaVersion,
                appVersion = root.getString("appVersion"),
                exportedAt = root.getString("exportedAt"),
                events = buildList {
                    for (index in 0 until eventsJson.length()) {
                        val eventJson = eventsJson.getJSONObject(index)
                        add(
                            CoffeeEvent(
                                reportedAtLocal = eventJson.getString("reportedAtLocal")
                                    .toCoffeeEventLocalDateTime(),
                            ),
                        )
                    }
                },
            )
        }
    }
}

class ContentResolverCoffeeBackupManager(
    private val io: CoffeeBackupIo,
    private val coffeeRepository: CoffeeRepository,
    private val codec: CoffeeHistoryBackupJsonCodec,
    private val appVersion: String,
    private val clock: Clock = Clock.systemUTC(),
) : CoffeeBackupManager {
    override suspend fun exportTo(uri: Uri): CoffeeBackupExportResult {
        val events = coffeeRepository.getAllCoffeeEvents()
        val backup = CoffeeHistoryBackup(
            schemaVersion = COFFEE_HISTORY_BACKUP_SCHEMA_VERSION,
            appVersion = appVersion,
            exportedAt = Instant.now(clock).toString(),
            events = events,
        )

        return try {
            io.writeText(uri, codec.encode(backup))
            CoffeeBackupExportResult.Success(exportedEvents = events.size)
        } catch (_: IOException) {
            CoffeeBackupExportResult.Failure(CoffeeBackupFailureReason.FILE_WRITE_FAILED)
        }
    }

    override suspend fun importFrom(
        uri: Uri,
        mode: CoffeeHistoryImportMode,
    ): CoffeeBackupImportResult {
        val payload = try {
            io.readText(uri)
        } catch (_: IOException) {
            return CoffeeBackupImportResult.Failure(CoffeeBackupFailureReason.FILE_READ_FAILED)
        }

        val backup = try {
            codec.decode(payload).getOrElse { throwable ->
                throw throwable
            }
        } catch (exception: IllegalArgumentException) {
            val reason = exception.message
                ?.let { runCatching { CoffeeBackupFailureReason.valueOf(it) }.getOrNull() }
                ?: CoffeeBackupFailureReason.INVALID_FORMAT
            return CoffeeBackupImportResult.Failure(reason)
        } catch (_: JSONException) {
            return CoffeeBackupImportResult.Failure(CoffeeBackupFailureReason.INVALID_FORMAT)
        } catch (_: Exception) {
            return CoffeeBackupImportResult.Failure(CoffeeBackupFailureReason.INVALID_FORMAT)
        }

        return CoffeeBackupImportResult.Success(
            summary = coffeeRepository.importCoffeeEvents(
                events = backup.events,
                mode = mode,
            ),
        )
    }
}
