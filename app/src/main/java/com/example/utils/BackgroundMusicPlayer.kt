package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import com.example.BrainQuizApplication
import com.example.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin

object BackgroundMusicPlayer {

    private const val TAG = "BackgroundMusicPlayer"
    private const val SAMPLE_RATE = 44100
    private const val TARGET_VOLUME = 0.22f
    private const val FADE_DURATION_SAMPLES = 44100 // 1 second fade at 44.1kHz

    private var audioTrack: AudioTrack? = null
    private var musicJob: Job? = null
    @Volatile private var isPlaying = false
    @Volatile private var isPaused = false
    @Volatile private var targetVolume = TARGET_VOLUME
    @Volatile private var currentVolume = 0f

    private var currentSampleIndex = 0L
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                targetVolume = TARGET_VOLUME * 0.4f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                targetVolume = TARGET_VOLUME
                val ctx = try { BrainQuizApplication.instance } catch (e: Exception) { null }
                if (ctx != null) resume(ctx)
            }
        }
    }

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

        if (isPlaying && !isPaused) return

        if (isPaused) {
            resume(ctx)
            return
        }

        requestAudioFocus(ctx)

        isPlaying = true
        isPaused = false
        targetVolume = TARGET_VOLUME

        musicJob?.cancel()
        musicJob = CoroutineScope(Dispatchers.Default).launch {
            runMusicLoop()
        }
    }

    @Synchronized
    fun resume(context: Context? = null) {
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

        if (isPlaying && !isPaused) return

        requestAudioFocus(ctx)

        isPaused = false
        targetVolume = TARGET_VOLUME

        if (!isPlaying || musicJob == null || musicJob?.isActive != true) {
            isPlaying = true
            musicJob?.cancel()
            musicJob = CoroutineScope(Dispatchers.Default).launch {
                runMusicLoop()
            }
        }
    }

    @Synchronized
    fun pause() {
        if (!isPlaying || isPaused) return
        isPaused = true
        targetVolume = 0f
    }

    @Synchronized
    fun stop() {
        targetVolume = 0f
        isPlaying = false
        isPaused = false
        musicJob?.cancel()
        musicJob = null
        releaseAudioTrack()
        abandonAudioFocus()
    }

    @Synchronized
    fun release() {
        stop()
    }

    private fun requestAudioFocus(context: Context?) {
        try {
            val ctx = context ?: BrainQuizApplication.instance
            audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audioManager?.requestAudioFocus(focusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error requesting audio focus", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error abandoning audio focus", e)
        }
    }

    private suspend fun CoroutineScope.runMusicLoop() {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(SAMPLE_RATE)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            // 144-second luxury ambient brain-training chord progression in D Major / B Minor
            // 18 chord phrases x 8 seconds each = 144 seconds total
            val chordList = listOf(
                doubleArrayOf(146.83, 220.00, 277.18, 369.99, 587.33), // D maj9
                doubleArrayOf(123.47, 220.00, 293.66, 369.99, 739.99), // Bm9
                doubleArrayOf(98.00,  185.00, 246.94, 293.66, 587.33), // G maj7
                doubleArrayOf(110.00, 220.00, 277.18, 329.63, 659.25), // A add9
                doubleArrayOf(92.50,  220.00, 293.66, 369.99, 880.00), // D maj9/F#
                doubleArrayOf(123.47, 220.00, 293.66, 329.63, 659.25), // Bm11
                doubleArrayOf(82.41,  196.00, 293.66, 369.99, 739.99), // Em9
                doubleArrayOf(110.00, 196.00, 277.18, 329.63, 659.25), // A7sus4
                doubleArrayOf(146.83, 220.00, 277.18, 440.00, 587.33), // D maj7
                doubleArrayOf(123.47, 185.00, 246.94, 369.99, 739.99), // Bm7
                doubleArrayOf(98.00,  220.00, 293.66, 369.99, 587.33), // G add9
                doubleArrayOf(110.00, 220.00, 277.18, 440.00, 554.37), // A6
                doubleArrayOf(146.83, 220.00, 293.66, 369.99, 659.25), // D add9
                doubleArrayOf(123.47, 220.00, 277.18, 369.99, 739.99), // Bm9
                doubleArrayOf(98.00,  185.00, 293.66, 440.00, 587.33), // G maj7
                doubleArrayOf(110.00, 196.00, 277.18, 369.99, 659.25), // A9
                doubleArrayOf(146.83, 220.00, 329.63, 369.99, 587.33), // D add9
                doubleArrayOf(110.00, 220.00, 277.18, 329.63, 554.37)  // A add9
            )

            val totalTrackSamples = 144L * SAMPLE_RATE
            val chordDurationSamples = 8L * SAMPLE_RATE
            val pcmBuffer = ShortArray(2048) // 1024 stereo sample pairs

            while (isActive && isPlaying) {
                // Handle smooth 1s fade in / fade out
                if (currentVolume < targetVolume) {
                    currentVolume += (TARGET_VOLUME / FADE_DURATION_SAMPLES)
                    if (currentVolume > targetVolume) currentVolume = targetVolume
                } else if (currentVolume > targetVolume) {
                    currentVolume -= (TARGET_VOLUME / FADE_DURATION_SAMPLES)
                    if (currentVolume < targetVolume) currentVolume = targetVolume
                }

                if (isPaused && currentVolume <= 0.001f) {
                    // Smooth fade out finished, pause track playback
                    audioTrack?.pause()
                    while (isPaused && isActive && isPlaying) {
                        kotlinx.coroutines.delay(50)
                    }
                    if (isActive && isPlaying && !isPaused) {
                        audioTrack?.play()
                    }
                }

                var bufIdx = 0
                while (bufIdx < pcmBuffer.size) {
                    val loopSample = currentSampleIndex % totalTrackSamples
                    val chordIdx = ((loopSample / chordDurationSamples) % chordList.size).toInt()
                    val sampleInChord = loopSample % chordDurationSamples
                    val chordFreqs = chordList[chordIdx]

                    val t = sampleInChord.toDouble() / SAMPLE_RATE
                    val chordT = loopSample.toDouble() / SAMPLE_RATE

                    // Ambient Synth Pad (warm filtered chorus)
                    var padLeft = 0.0
                    var padRight = 0.0
                    val padEnvelope = sin(Math.PI * sampleInChord / chordDurationSamples)

                    for ((fIdx, freq) in chordFreqs.withIndex()) {
                        val angle = 2.0 * Math.PI * freq * t
                        val subHarmonic = sin(angle * 0.5) * 0.15
                        val mainWave = sin(angle) * 0.35
                        val shimmer = sin(angle * 2.0) * 0.10 * exp(-t / 4.0)
                        val noteSig = (subHarmonic + mainWave + shimmer) * padEnvelope

                        // Stereo spatial pan per harmonic note
                        val pan = sin(2.0 * Math.PI * 0.15 * chordT + fIdx) * 0.3
                        padLeft += noteSig * (0.5 - pan)
                        padRight += noteSig * (0.5 + pan)
                    }

                    // Soft Piano Note Arpeggios (beats 1, 3, 5, 7 within 8s measure)
                    var pianoLeft = 0.0
                    var pianoRight = 0.0
                    val beatIntervalSamples = 2L * SAMPLE_RATE // beat every 2 seconds
                    val currentBeat = sampleInChord / beatIntervalSamples
                    val sampleInBeat = sampleInChord % beatIntervalSamples

                    if (currentBeat in 0..3) {
                        val pianoFreq = chordFreqs[(currentBeat.toInt() + 2) % chordFreqs.size]
                        val pianoT = sampleInBeat.toDouble() / SAMPLE_RATE

                        if (pianoT >= 0) {
                            val attack = if (sampleInBeat < 220) sin(Math.PI * sampleInBeat / (2.0 * 220.0)) else 1.0
                            val decay = exp(-pianoT / 1.2)
                            val pianoTone = (sin(2.0 * Math.PI * pianoFreq * pianoT) * 0.5 +
                                            sin(4.0 * Math.PI * pianoFreq * pianoT) * 0.25 * exp(-pianoT / 0.5) +
                                            sin(6.0 * Math.PI * pianoFreq * pianoT) * 0.1 * exp(-pianoT / 0.2)) * attack * decay * 0.40

                            val pPan = (currentBeat - 1.5) * 0.2
                            pianoLeft += pianoTone * (0.5 - pPan)
                            pianoRight += pianoTone * (0.5 + pPan)
                        }
                    }

                    val masterValLeft = (padLeft + pianoLeft) * currentVolume
                    val masterValRight = (padRight + pianoRight) * currentVolume

                    pcmBuffer[bufIdx] = (masterValLeft * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    pcmBuffer[bufIdx + 1] = (masterValRight * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                    bufIdx += 2
                    currentSampleIndex++
                }

                audioTrack?.write(pcmBuffer, 0, pcmBuffer.size)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in background music loop", e)
        } finally {
            releaseAudioTrack()
        }
    }

    private fun releaseAudioTrack() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING || playState == AudioTrack.PLAYSTATE_PAUSED) {
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
