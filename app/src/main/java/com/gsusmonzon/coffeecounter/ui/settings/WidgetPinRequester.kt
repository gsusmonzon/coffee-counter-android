package com.gsusmonzon.coffeecounter.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.gsusmonzon.coffeecounter.widget.CoffeeCounterWidgetReceiver

interface WidgetPinRequester {
    fun hasActiveWidget(): Boolean

    fun requestPin(): Boolean
}

class AppWidgetPinRequester(
    context: Context,
) : WidgetPinRequester {
    private val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
    private val provider = ComponentName(context, CoffeeCounterWidgetReceiver::class.java)

    override fun hasActiveWidget(): Boolean {
        return appWidgetManager.getAppWidgetIds(provider).isNotEmpty()
    }

    override fun requestPin(): Boolean {
        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            return false
        }

        return appWidgetManager.requestPinAppWidget(provider, null, null)
    }
}
