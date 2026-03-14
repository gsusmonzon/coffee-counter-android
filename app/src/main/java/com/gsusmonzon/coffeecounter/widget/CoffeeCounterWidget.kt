package com.gsusmonzon.coffeecounter.widget

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gsusmonzon.coffeecounter.CoffeeCounterApplication

class CoffeeCounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CoffeeCounterWidget()
}

class CoffeeCounterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: androidx.glance.GlanceId,
    ) {
        val repository = (context.applicationContext as CoffeeCounterApplication)
            .appContainer
            .coffeeRepository

        provideContent {
            val todayCount by repository.observeTodayCount().collectAsState(initial = 0)
            CoffeeCounterWidgetContent(todayCount = todayCount)
        }
    }
}

@androidx.compose.runtime.Composable
private fun CoffeeCounterWidgetContent(todayCount: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .background(WidgetColors.surface)
                .clickable(onClick = actionRunCallback<AddCoffeeAction>())
                .padding(end = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "Today",
                    style = TextStyle(
                        color = WidgetColors.label,
                        fontSize = 12.sp,
                    ),
                )
                Text(
                    text = todayCount.toString(),
                    style = TextStyle(
                        color = WidgetColors.primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        Box(
            modifier = GlanceModifier
                .width(40.dp)
                .fillMaxHeight()
                .background(WidgetColors.primary)
                .clickable(onClick = actionRunCallback<UndoCoffeeAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "−",
                style = TextStyle(
                    color = WidgetColors.onPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

class AddCoffeeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters,
    ) {
        CoffeeWidgetActionHandler.from(context).addCoffee()
    }
}

class UndoCoffeeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters,
    ) {
        CoffeeWidgetActionHandler.from(context).undoCoffee()
    }
}

internal class CoffeeWidgetActionHandler(
    private val repositoryActions: CoffeeWidgetRepositoryActions,
    private val widgetUpdater: CoffeeWidgetUpdater,
    private val feedbackPerformer: CoffeeWidgetFeedbackPerformer,
) {
    suspend fun addCoffee() {
        repositoryActions.incrementToday()
        widgetUpdater.refresh()
        feedbackPerformer.performActionFeedback()
    }

    suspend fun undoCoffee() {
        repositoryActions.decrementToday()
        widgetUpdater.refresh()
        feedbackPerformer.performActionFeedback()
    }

    companion object {
        fun from(context: Context): CoffeeWidgetActionHandler {
            val application = context.applicationContext as CoffeeCounterApplication
            val repository = application.appContainer.coffeeRepository

            return CoffeeWidgetActionHandler(
                repositoryActions = CoffeeRepositoryWidgetActions(repository),
                widgetUpdater = GlanceCoffeeWidgetUpdater(context.applicationContext),
                feedbackPerformer = VibrationWidgetFeedbackPerformer(context.applicationContext),
            )
        }
    }
}

internal interface CoffeeWidgetRepositoryActions {
    suspend fun incrementToday()
    suspend fun decrementToday()
}

internal class CoffeeRepositoryWidgetActions(
    private val repository: com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository,
) : CoffeeWidgetRepositoryActions {
    override suspend fun incrementToday() {
        repository.incrementToday()
    }

    override suspend fun decrementToday() {
        repository.decrementToday()
    }
}

internal interface CoffeeWidgetUpdater {
    suspend fun refresh()
}

internal class GlanceCoffeeWidgetUpdater(
    private val context: Context,
) : CoffeeWidgetUpdater {
    override suspend fun refresh() {
        CoffeeCounterWidget().updateAll(context)
    }
}

internal interface CoffeeWidgetFeedbackPerformer {
    fun performActionFeedback()
}

internal class VibrationWidgetFeedbackPerformer(
    context: Context,
) : CoffeeWidgetFeedbackPerformer {
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

    override fun performActionFeedback() {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }
}

private object WidgetColors {
    val surface: ColorProvider = DayNightColorProvider(
        day = Color(0xFFF7F0E8),
        night = Color(0xFF2A211B),
    )
    val primary: ColorProvider = DayNightColorProvider(
        day = Color(0xFF5C3B22),
        night = Color(0xFFE6C7A8),
    )
    val onPrimary: ColorProvider = DayNightColorProvider(
        day = Color(0xFFFFF8F3),
        night = Color(0xFF2A211B),
    )
    val label: ColorProvider = DayNightColorProvider(
        day = Color(0xFF7A6553),
        night = Color(0xFFC9B3A0),
    )
}
