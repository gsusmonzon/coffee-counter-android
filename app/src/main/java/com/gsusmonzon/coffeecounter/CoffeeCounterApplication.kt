package com.gsusmonzon.coffeecounter

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.gsusmonzon.coffeecounter.data.local.CoffeeCounterDatabase
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.data.repository.LocalDateProvider
import com.gsusmonzon.coffeecounter.data.repository.LocalDateTimeProvider
import com.gsusmonzon.coffeecounter.data.repository.RoomCoffeeRepository
import com.gsusmonzon.coffeecounter.data.repository.SystemLocalDateProvider
import com.gsusmonzon.coffeecounter.data.repository.SystemLocalDateTimeProvider
import com.gsusmonzon.coffeecounter.feedback.ClackSoundPlayer
import com.gsusmonzon.coffeecounter.reminder.AlarmManagerLateLogReminderScheduler
import com.gsusmonzon.coffeecounter.widget.AlarmManagerMidnightWidgetRefreshScheduler

class CoffeeCounterApplication : Application() {
    val appContainer: AppContainer by lazy {
        DefaultAppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        ClackSoundPlayer(this).preload()
        AlarmManagerLateLogReminderScheduler(this).scheduleNextReminder()
        // Intentional: users typically have a widget, so we always keep the daily rollover alarm scheduled.
        AlarmManagerMidnightWidgetRefreshScheduler(this).schedule()
    }
}

interface AppContainer {
    val coffeeRepository: CoffeeRepository
    val localDateProvider: LocalDateProvider
}

internal fun Context.appContainer(): AppContainer {
    return (applicationContext as CoffeeCounterApplication).appContainer
}

private class DefaultAppContainer(
    application: Application,
) : AppContainer {
    private val database: CoffeeCounterDatabase by lazy {
        Room.databaseBuilder(
            application,
            CoffeeCounterDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(
            CoffeeCounterDatabase.MIGRATION_1_2,
        ).build()
    }

    override val localDateProvider: LocalDateProvider by lazy {
        SystemLocalDateProvider(application)
    }

    private val localDateTimeProvider: LocalDateTimeProvider by lazy {
        SystemLocalDateTimeProvider()
    }

    override val coffeeRepository: CoffeeRepository by lazy {
        RoomCoffeeRepository(
            database = database,
            localDateProvider = localDateProvider,
            localDateTimeProvider = localDateTimeProvider,
        )
    }

    private companion object {
        const val DATABASE_NAME = "coffee-counter.db"
    }
}
