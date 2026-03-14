package com.gsusmonzon.coffeecounter.widget

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gsusmonzon.coffeecounter.MainActivity
import com.gsusmonzon.coffeecounter.CoffeeCounterApplication
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.feedback.ClackSoundPlayer
import com.gsusmonzon.coffeecounter.feedback.CoffeeDoseSoundPlayer
import kotlinx.coroutines.runBlocking

class CoffeeCounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CoffeeCounterWidget()

    internal var widgetUpdaterFactory: (Context) -> CoffeeWidgetUpdater = { context ->
        GlanceCoffeeWidgetUpdater(context.applicationContext)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)

        if (intent.action !in ROLLOVER_REFRESH_ACTIONS) {
            return
        }

        runBlocking {
            // Refresh the widget when Android reports a day/time boundary change so the
            // home screen count rolls over even if the user never opens the app.
            widgetUpdaterFactory(context).refresh()
        }
    }

    private companion object {
        val ROLLOVER_REFRESH_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

class CoffeeCounterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: androidx.glance.GlanceId,
    ) {
        val repository = (context.applicationContext as CoffeeCounterApplication)
            .appContainer
            .coffeeRepository
        val todayLabel = context.getString(R.string.today_title)

        provideContent {
            val todayCount by repository.observeTodayCount().collectAsState(initial = 0)
            CoffeeCounterWidgetContent(
                todayLabel = todayLabel,
                todayCount = todayCount,
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun CoffeeCounterWidgetContent(
    todayLabel: String,
    todayCount: Int,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.surface)
            .cornerRadius(28.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .background(WidgetColors.surface)
                .clickable(onClick = actionRunCallback<AddCoffeeAction>())
                .cornerRadius(22.dp)
                .padding(end = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = todayLabel,
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

        Column(
            modifier = GlanceModifier
                .width(32.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(WidgetColors.actionSurface)
                    .clickable(onClick = actionRunCallback<OpenAppAction>())
                    .cornerRadius(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_open_in_new),
                    contentDescription = null,
                    modifier = GlanceModifier.size(18.dp),
                    colorFilter = ColorFilter.tint(WidgetColors.actionContent),
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(WidgetColors.actionSurface)
                    .clickable(onClick = actionRunCallback<UndoCoffeeAction>())
                    .cornerRadius(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_undo),
                    contentDescription = null,
                    modifier = GlanceModifier.size(18.dp),
                    colorFilter = ColorFilter.tint(WidgetColors.actionContent),
                )
            }
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

class OpenAppAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters,
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launchIntent)
    }
}

internal class CoffeeWidgetActionHandler(
    private val repository: CoffeeRepository,
    private val widgetUpdater: CoffeeWidgetUpdater,
    private val feedbackPerformer: CoffeeWidgetFeedbackPerformer,
) {
    suspend fun addCoffee() {
        repository.incrementToday()
        widgetUpdater.refresh()
        feedbackPerformer.performAddFeedback()
    }

    suspend fun undoCoffee() {
        repository.decrementToday()
        widgetUpdater.refresh()
        feedbackPerformer.performUndoFeedback()
    }

    companion object {
        fun from(context: Context): CoffeeWidgetActionHandler {
            val application = context.applicationContext as CoffeeCounterApplication
            val repository = application.appContainer.coffeeRepository

            return CoffeeWidgetActionHandler(
                repository = repository,
                widgetUpdater = GlanceCoffeeWidgetUpdater(context.applicationContext),
                feedbackPerformer = CombinedCoffeeWidgetFeedbackPerformer(
                    hapticPerformer = VibrationWidgetFeedbackPerformer(context.applicationContext),
                    soundPerformer = ClackSoundWidgetFeedbackPerformer(context.applicationContext),
                ),
            )
        }
    }
}

interface CoffeeWidgetUpdater {
    suspend fun refresh()
}

class GlanceCoffeeWidgetUpdater(
    private val context: Context,
) : CoffeeWidgetUpdater {
    override suspend fun refresh() {
        CoffeeCounterWidget().updateAll(context)
    }
}

internal interface CoffeeWidgetFeedbackPerformer {
    fun performAddFeedback()

    fun performUndoFeedback()
}

internal class CombinedCoffeeWidgetFeedbackPerformer(
    private val hapticPerformer: CoffeeWidgetHapticPerformer,
    private val soundPerformer: CoffeeWidgetSoundPerformer,
) : CoffeeWidgetFeedbackPerformer {
    override fun performAddFeedback() {
        hapticPerformer.performHapticFeedback()
        soundPerformer.playDoseSound()
    }

    override fun performUndoFeedback() {
        hapticPerformer.performHapticFeedback()
        soundPerformer.playDoseSound()
    }
}

internal interface CoffeeWidgetHapticPerformer {
    fun performHapticFeedback()
}

internal interface CoffeeWidgetSoundPerformer {
    fun playDoseSound()
}

internal class VibrationWidgetFeedbackPerformer(
    context: Context,
) : CoffeeWidgetHapticPerformer {
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

    override fun performHapticFeedback() {
        if (!vibrator.hasVibrator()) {
            return
        }

        try {
            // Best effort only: widget actions run outside the app's foreground UI, so some
            // devices or launchers may suppress haptics even when vibration is available.
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } catch (exception: RuntimeException) {
            // Ignore runtime failures and keep widget actions functional.
        }
    }
}

internal class ClackSoundWidgetFeedbackPerformer(
    context: Context,
) : CoffeeWidgetSoundPerformer {
    private val soundPlayer: CoffeeDoseSoundPlayer = ClackSoundPlayer(context.applicationContext)

    override fun playDoseSound() {
        soundPlayer.playDoseSound()
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
    val actionSurface: ColorProvider = DayNightColorProvider(
        day = Color(0x1F5C3B22),
        night = Color(0x33E6C7A8),
    )
    val actionContent: ColorProvider = DayNightColorProvider(
        day = Color(0xFF5C3B22),
        night = Color(0xFFE6C7A8),
    )
    val label: ColorProvider = DayNightColorProvider(
        day = Color(0xFF7A6553),
        night = Color(0xFFC9B3A0),
    )
}
