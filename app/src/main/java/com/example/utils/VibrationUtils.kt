package com.example.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.SettingsStore

object VibrationUtils {

    private fun isVibrationEnabled(context: Context): Boolean {
        return try {
            SettingsStore(context).getSettings().vibrationEnabled
        } catch (e: Exception) {
            true
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w("VibrationUtils", "Could not get vibrator service", e)
            null
        }
    }

    fun vibrateClick(context: Context) {
        if (!isVibrationEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (e: Exception) {
            Log.w("VibrationUtils", "Error playing click vibration", e)
        }
    }

    fun vibrateCorrect(context: Context) {
        if (!isVibrationEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (e: Exception) {
            Log.w("VibrationUtils", "Error playing correct vibration", e)
        }
    }

    fun vibrateWrong(context: Context) {
        if (!isVibrationEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 50, 50, 50)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
            }
        } catch (e: Exception) {
            Log.w("VibrationUtils", "Error playing wrong vibration", e)
        }
    }

    fun vibrateAchievement(context: Context) {
        if (!isVibrationEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 30, 40, 50, 40, 80)
                val amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 40, 50, 40, 80), -1)
            }
        } catch (e: Exception) {
            Log.w("VibrationUtils", "Error playing achievement vibration", e)
        }
    }

    fun vibrateComplete(context: Context) {
        if (!isVibrationEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 40, 60, 60, 60, 100)
                val amplitudes = intArrayOf(0, 120, 0, 180, 0, 250)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 40, 60, 60, 60, 100), -1)
            }
        } catch (e: Exception) {
            Log.w("VibrationUtils", "Error playing complete vibration", e)
        }
    }
}
