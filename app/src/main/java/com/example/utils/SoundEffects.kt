package com.example.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.SettingsStore

object SoundEffects {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            Log.w("SoundEffects", "ToneGenerator could not be initialized", e)
        }
    }

    private fun isSoundEnabled(context: Context? = null): Boolean {
        return try {
            val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
            if (ctx == null) return true
            SettingsStore(ctx).getSettings().soundEffectsEnabled
        } catch (e: Exception) {
            true
        }
    }

    fun playClickSound(context: Context? = null) {
        if (!isSoundEnabled(context)) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing click sound", e)
        }
    }

    fun playCorrectSound(context: Context? = null) {
        if (!isSoundEnabled(context)) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing correct sound", e)
        }
    }

    fun playWrongSound(context: Context? = null) {
        if (!isSoundEnabled(context)) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 220)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing wrong sound", e)
        }
    }

    fun playCoinSound(context: Context? = null) {
        if (!isSoundEnabled(context)) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_3, 110)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing coin sound", e)
        }
    }

    fun playAchievementSound(context: Context? = null) {
        if (!isSoundEnabled(context)) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 250)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing achievement sound", e)
        }
    }

    fun playCompleteSound(context: Context? = null) {
        if (!isSoundEnabled(context)) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_8, 300)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing complete sound", e)
        }
    }

    fun playLevelUpSound(context: Context? = null) {
        if (!isSoundEnabled(context)) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 350)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing level up sound", e)
        }
    }
}
