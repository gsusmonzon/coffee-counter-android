package com.gsusmonzon.coffeecounter.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import java.time.LocalDate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface LocalDateProvider {
    fun today(): LocalDate

    fun observeToday(): Flow<LocalDate>
}

class SystemLocalDateProvider(
    context: Context,
) : LocalDateProvider {
    private val applicationContext = context.applicationContext

    override fun today(): LocalDate = LocalDate.now()

    override fun observeToday(): Flow<LocalDate> = callbackFlow {
        var lastEmittedDate = today()
        trySend(lastEmittedDate)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                // Listen for date/time changes so app state rolls over at local midnight
                // without requiring the user to reopen the app.
                val currentDate = today()
                if (currentDate != lastEmittedDate) {
                    lastEmittedDate = currentDate
                    trySend(currentDate)
                }
            }
        }

        applicationContext.registerTimeChangeReceiver(receiver)

        awaitClose {
            applicationContext.unregisterReceiver(receiver)
        }
    }.distinctUntilChanged()
}

private fun Context.registerTimeChangeReceiver(receiver: BroadcastReceiver) {
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_DATE_CHANGED)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        @Suppress("DEPRECATION")
        registerReceiver(receiver, filter)
    }
}
