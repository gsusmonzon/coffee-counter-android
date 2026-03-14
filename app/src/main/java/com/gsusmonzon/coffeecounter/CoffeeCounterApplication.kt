package com.gsusmonzon.coffeecounter

import android.app.Application
import androidx.room.Room
import com.gsusmonzon.coffeecounter.data.local.CoffeeCounterDatabase
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.data.repository.RoomCoffeeRepository

class CoffeeCounterApplication : Application() {
    val appContainer: AppContainer by lazy {
        DefaultAppContainer(this)
    }
}

interface AppContainer {
    val coffeeRepository: CoffeeRepository
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

    override val coffeeRepository: CoffeeRepository by lazy {
        RoomCoffeeRepository(database)
    }

    private companion object {
        const val DATABASE_NAME = "coffee-counter.db"
    }
}
