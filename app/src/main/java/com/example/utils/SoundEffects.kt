package com.example.utils

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

object SoundEffects {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
        } catch (e: Exception) {
            Log.w("SoundEffects", "ToneGenerator could not be initialized", e)
        }
    }

    fun playCorrectSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing sound", e)
        }
    }

    fun playWrongSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing sound", e)
        }
    }

    fun playCoinSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_3, 100)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing sound", e)
        }
    }

    fun playCompleteSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_8, 250)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error playing sound", e)
        }
    }
}
