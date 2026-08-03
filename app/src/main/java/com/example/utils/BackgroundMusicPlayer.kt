package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

object BackgroundMusicPlayer {

    private const val TAG = "BackgroundMusicPlayer"
    private const val SAMPLE_RATE = 22050
    private var audioTrack: AudioTrack? = null
    private var musicJob: Job? = null
    private var isPlaying = false
    private var currentVolume = 0f
    private const val TARGET_VOLUME = 0.25f

    @Synchronized
    fun updateMusicState(context: Context) {
        val enabled = try {
            SettingsStore(context).getSettings().bgMusicEnabled
        } catch (e: Exception) {
            true
        }

        if (enabled) {
            start(context)
        } else {
            stop()
        }
    }

    @Synchronized
    fun start(context: Context? = null) {
        val ctx = context ?: try { BrainQuizApplication.instance } catch (e: Exception) { null }
        val enabled = try {
            if (ctx != null) SettingsStore(ctx).getSettings().bgMusicEnabled else true
        } catch (e: Exception) {
            true
        }

        if (!enabled) {
            stop()
            return
        }

        if (isPlaying) return
        isPlaying = true

        musicJob?.cancel()
        musicJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(SAMPLE_RATE / 2)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                // Soothing ambient pentatonic progression: C4, E4, G4, B4, D5 (261.6, 329.6, 392.0, 493.9, 587.3)
                val chordFreqs = doubleArrayOf(261.63, 329.63, 392.00, 493.88)
                val secondaryChord = doubleArrayOf(220.00, 261.63, 329.63, 392.00) // Am7

                var sampleIndex = 0L
                val pcmBuffer = ShortArray(1024)
                val durationSamples = SAMPLE_RATE * 6 // 6 seconds per chord phrase

                currentVolume = 0f

                while (isActive && isPlaying) {
                    // Smooth volume fade in
                    if (currentVolume < TARGET_VOLUME) {
                        currentVolume += 0.005f
                        if (currentVolume > TARGET_VOLUME) currentVolume = TARGET_VOLUME
                        audioTrack?.setVolume(currentVolume)
                    }

                    for (i in pcmBuffer.indices) {
                        val phaseInChord = (sampleIndex % (durationSamples * 2))
                        val currentChord = if (phaseInChord < durationSamples) chordFreqs else secondaryChord

                        var sampleVal = 0.0
                        for (freq in currentChord) {
                            val angle = 2.0 * Math.PI * freq * sampleIndex / SAMPLE_RATE
                            sampleVal += sin(angle) * 0.12
                        }

                        // Low-pass envelope for ambient warmth
                        val envelope = sin(Math.PI * (sampleIndex % durationSamples) / durationSamples)
                        sampleVal *= (0.6 + 0.4 * envelope)

                        pcmBuffer[i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        sampleIndex++
                    }

                    audioTrack?.write(pcmBuffer, 0, pcmBuffer.size)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error running background music track", e)
            } finally {
                releaseAudioTrack()
            }
        }
    }

    @Synchronized
    fun stop() {
        if (!isPlaying) return
        isPlaying = false
        musicJob?.cancel()
        musicJob = null
        releaseAudioTrack()
    }

    private fun releaseAudioTrack() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }
}
