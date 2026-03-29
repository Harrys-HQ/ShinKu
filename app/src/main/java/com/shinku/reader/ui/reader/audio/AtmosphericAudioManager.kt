package com.shinku.reader.ui.reader.audio

import android.content.Context
import android.media.MediaPlayer
import com.shinku.reader.exh.source.ShinKuPreferences
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat

class AtmosphericAudioManager(
    private val context: Context,
    private val shinkuPreferences: ShinKuPreferences,
) {
    private var mediaPlayer: MediaPlayer? = null

    fun start(genres: List<String>) {
        if (!shinkuPreferences.atmosphericAudio().get()) return
        
        val audioRes = getAudioResForGenres(genres) ?: return
        
        try {
            stop()
            mediaPlayer = MediaPlayer.create(context, audioRes).apply {
                isLooping = true
                setVolume(0.3f, 0.3f)
                start()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to start atmospheric audio" }
        }
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun getAudioResForGenres(genres: List<String>): Int? {
        // Placeholder for mapping genres to R.raw resources
        // In a real app, these would be valid raw resource IDs
        return when {
            genres.any { it.contains("Horror", true) || it.contains("Thriller", true) } -> {
                context.resources.getIdentifier("atmosphere_horror", "raw", context.packageName).takeIf { it != 0 }
            }
            genres.any { it.contains("Slice of Life", true) || it.contains("Romance", true) } -> {
                context.resources.getIdentifier("atmosphere_rain", "raw", context.packageName).takeIf { it != 0 }
            }
            genres.any { it.contains("Adventure", true) || it.contains("Fantasy", true) } -> {
                context.resources.getIdentifier("atmosphere_forest", "raw", context.packageName).takeIf { it != 0 }
            }
            else -> null
        }
    }
}
