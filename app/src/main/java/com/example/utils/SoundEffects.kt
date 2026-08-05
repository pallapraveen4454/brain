package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.SettingsStore
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.exp
import kotlin.math.sin

object SoundEffects {

    private const val TAG = "SoundEffects"
    private const val SAMPLE_RATE = 44100
    private const val CLICK_POOL_SIZE = 4

    // Pool of static AudioTracks for instant 0ms latency click playback at 60 FPS
    private val clickTracks = arrayOfNulls<AudioTrack>(CLICK_POOL_SIZE)
    private val clickPoolIndex = AtomicInteger(0)

    // Single static AudioTracks for other sound effects
    private var correctTrack: AudioTrack? = null
    private var wrongTrack: AudioTrack? = null
    private var coinTrack: AudioTrack? = null
    private var achievementTrack: AudioTrack? = null
    private var completeTrack: AudioTrack? = null
    private var levelUpTrack: AudioTrack? = null

    init {
        try {
            initClickPool()
            initEffectTracks()
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing SoundEffects audio tracks", e)
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

    private fun buildAudioTrack(pcmShorts: ShortArray, usage: Int = AudioAttributes.USAGE_ASSISTANCE_SONIFICATION): AudioTrack? {
        return try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmShorts.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcmShorts, 0, pcmShorts.size)
            track
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build static AudioTrack", e)
            null
        }
    }

    private fun initClickPool() {
        // Soft glass-like tap sound (50ms duration = 2205 samples)
        val durationMs = 50
        val sampleCount = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ShortArray(sampleCount)

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE

            // 3ms attack ramp (132 samples) to avoid transient click/DC pop
            val attack = if (i < 132) sin(Math.PI * i / (2.0 * 132.0)) else 1.0

            // Exponential decay envelope (tau = 12ms)
            val decay = exp(-t / 0.012)

            // Frequencies: Primary glass sine 1320Hz, overtone 2640Hz, warm body 480Hz
            val f1 = sin(2.0 * Math.PI * 1320.0 * t) * 0.55
            val f2 = sin(2.0 * Math.PI * 2640.0 * t) * 0.18 * exp(-t / 0.006)
            val f3 = sin(2.0 * Math.PI * 480.0 * t) * 0.27

            val sampleVal = attack * decay * (f1 + f2 + f3) * 0.22
            pcm[i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        for (k in 0 until CLICK_POOL_SIZE) {
            clickTracks[k] = buildAudioTrack(pcm)
        }
    }

    private fun initEffectTracks() {
        // Correct Sound: Glass chime triad (E5 659Hz, G#5 830Hz, B5 987Hz, E6 1318Hz), 240ms
        correctTrack = buildAudioTrack(generateChime(doubleArrayOf(659.25, 830.61, 987.77, 1318.51), 240, 0.28))

        // Wrong Sound: Soft damped dual-thud (D3 146Hz, G#3 207Hz), 200ms
        wrongTrack = buildAudioTrack(generateSoftThud(200, 0.26))

        // Coin Sound: Sparkly metallic glass ring (B5 987Hz -> F#6 1480Hz), 140ms
        coinTrack = buildAudioTrack(generateCoinChime(140, 0.30))

        // Achievement Sound: Ascending 4-note chord cascade, 340ms
        achievementTrack = buildAudioTrack(generateChime(doubleArrayOf(523.25, 659.25, 783.99, 1046.50), 340, 0.32))

        // Complete Sound: Victory swell chord, 420ms
        completeTrack = buildAudioTrack(generateChime(doubleArrayOf(392.00, 523.25, 659.25, 783.99, 1046.50), 420, 0.32))

        // Level Up Sound: Ascending major 7th chime, 380ms
        levelUpTrack = buildAudioTrack(generateChime(doubleArrayOf(587.33, 739.99, 880.00, 1108.73, 1318.51), 380, 0.32))
    }

    private fun generateChime(freqs: DoubleArray, durationMs: Int, volume: Double): ShortArray {
        val sampleCount = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ShortArray(sampleCount)
        val noteCount = freqs.size

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = if (i < 220) sin(Math.PI * i / (2.0 * 220.0)) else 1.0
            val decay = exp(-t / (durationMs * 0.00065))

            var sampleVal = 0.0
            for ((index, freq) in freqs.withIndex()) {
                val noteOffsetMs = index * (durationMs.toDouble() / (noteCount * 1.5))
                val noteT = t - (noteOffsetMs / 1000.0)
                if (noteT >= 0) {
                    val noteAttack = if (noteT < 0.005) sin(Math.PI * noteT / 0.01) else 1.0
                    val noteDecay = exp(-noteT / 0.08)
                    sampleVal += noteAttack * noteDecay * sin(2.0 * Math.PI * freq * noteT)
                }
            }
            sampleVal = attack * decay * (sampleVal / noteCount) * volume
            pcm[i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    private fun generateSoftThud(durationMs: Int, volume: Double): ShortArray {
        val sampleCount = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = if (i < 150) sin(Math.PI * i / (2.0 * 150.0)) else 1.0
            val decay = exp(-t / 0.035)
            val wave = sin(2.0 * Math.PI * 146.83 * t) * 0.6 + sin(2.0 * Math.PI * 207.65 * t) * 0.4
            val sampleVal = attack * decay * wave * volume
            pcm[i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    private fun generateCoinChime(durationMs: Int, volume: Double): ShortArray {
        val sampleCount = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = if (i < 100) sin(Math.PI * i / (2.0 * 100.0)) else 1.0
            val decay = exp(-t / 0.04)
            val freq1 = 987.77
            val freq2 = 1479.98
            val wave = if (t < 0.04) sin(2.0 * Math.PI * freq1 * t) else sin(2.0 * Math.PI * freq2 * (t - 0.04))
            val sampleVal = attack * decay * wave * volume
            pcm[i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    private fun playTrack(track: AudioTrack?, context: Context? = null) {
        if (!isSoundEnabled(context) || track == null) return
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.setPlaybackHeadPosition(0)
            track.play()
        } catch (e: Exception) {
            Log.w(TAG, "Error playing audio track", e)
        }
    }

    fun playClickSound(context: Context? = null) {
        if (!isSoundEnabled(context)) return
        try {
            val idx = (clickPoolIndex.getAndIncrement() and Int.MAX_VALUE) % CLICK_POOL_SIZE
            val track = clickTracks[idx]
            playTrack(track, context)
        } catch (e: Exception) {
            Log.w(TAG, "Error playing click sound", e)
        }
    }

    fun playCorrectSound(context: Context? = null) = playTrack(correctTrack, context)
    fun playWrongSound(context: Context? = null) = playTrack(wrongTrack, context)
    fun playCoinSound(context: Context? = null) = playTrack(coinTrack, context)
    fun playAchievementSound(context: Context? = null) = playTrack(achievementTrack, context)
    fun playCompleteSound(context: Context? = null) = playTrack(completeTrack, context)
    fun playLevelUpSound(context: Context? = null) = playTrack(levelUpTrack, context)
}
