package com.gsusmonzon.coffeecounter

import android.app.Application
import androidx.room.Room
import com.gsusmonzon.coffeecounter.data.local.CoffeeCounterDatabase
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.data.repository.LocalDateProvider
import com.gsusmonzon.coffeecounter.data.repository.RoomCoffeeRepository
import com.gsusmonzon.coffeecounter.data.repository.SystemLocalDateProvider

class CoffeeCounterApplication : Application() {
    val appContainer: AppContainer by lazy {
        DefaultAppContainer(this)
    }
}

interface AppContainer {
    val coffeeRepository: CoffeeRepository
    val localDateProvider: LocalDateProvider
}

private class DefaultAppContainer(
    application: Application,
) : AppContainer {
    private val database: CoffeeCounterDatabase by lazy {
        Room.databaseBuilder(
            application,
            CoffeeCounterDatabase::class.java,
            DATABASE_NAME,
        ).build()
    }

    override val localDateProvider: LocalDateProvider by lazy {
        SystemLocalDateProvider(application)
    }

    override val coffeeRepository: CoffeeRepository by lazy {
        RoomCoffeeRepository(
            database = database,
            localDateProvider = localDateProvider,
        )
    }

    private companion object {
        const val DATABASE_NAME = "coffee-counter.db"
    }
}
