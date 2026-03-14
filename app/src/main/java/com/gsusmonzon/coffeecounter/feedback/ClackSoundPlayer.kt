package com.gsusmonzon.coffeecounter.feedback

import android.media.AudioAttributes
import android.content.Context
import android.media.SoundPool
import com.gsusmonzon.coffeecounter.R

interface CoffeeDoseSoundPlayer {
    fun playDoseSound()

    fun preload()
}

class ClackSoundPlayer(
    context: Context,
) : CoffeeDoseSoundPlayer {
    private val applicationContext = context.applicationContext

    override fun playDoseSound() {
        SharedClackSoundPool.play(applicationContext)
    }

    override fun preload() {
        SharedClackSoundPool.preload(applicationContext)
    }
}

private object SharedClackSoundPool {
    @Volatile
    private var soundPool: SoundPool? = null

    @Volatile
    private var clackSoundId: Int = 0

    @Volatile
    private var isLoaded: Boolean = false

    fun preload(context: Context) {
        ensureInitialized(context.applicationContext)
    }

    fun play(context: Context) {
        ensureInitialized(context.applicationContext)
        if (!isLoaded) {
            return
        }

        soundPool?.play(
            clackSoundId,
            1f,
            1f,
            1,
            0,
            1f,
        )
    }

    @Synchronized
    private fun ensureInitialized(context: Context) {
        if (soundPool != null) {
            return
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val createdSoundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        createdSoundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == clackSoundId) {
                isLoaded = true
            }
        }

        clackSoundId = createdSoundPool.load(context, R.raw.clack_sound, 1)
        soundPool = createdSoundPool
    }
}
