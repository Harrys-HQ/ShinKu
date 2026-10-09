package com.shinku.reader.ui.reader.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.shinku.reader.R
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.OkHttpClient
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileOutputStream

enum class AmbientTrack(
    val id: String,
    val title: String,
    val fileName: String,
    val bundledFallbackRes: Int,
    val basePriority: Int,
    val highPriorityTags: List<String>,
    val secondaryTags: List<String>,
) {
    ACTION(
        id = "action",
        title = "Battle Campfire & Winds",
        fileName = "atmosphere_action.mp3",
        bundledFallbackRes = R.raw.atmosphere_forest,
        basePriority = 100,
        highPriorityTags = listOf("action", "battle", "combat", "fight", "war", "military", "super power", "murim", "martial arts"),
        secondaryTags = listOf("shounen", "seinen", "sports", "might", "mayhem"),
    ),
    HORROR(
        id = "horror",
        title = "Dark & Creepy Drone",
        fileName = "atmosphere_horror.mp3",
        bundledFallbackRes = R.raw.atmosphere_horror,
        basePriority = 95,
        highPriorityTags = listOf("horror", "thriller", "gore", "terror", "creepy", "dark", "survival", "monster"),
        secondaryTags = listOf("psychological"),
    ),
    CYBERPUNK(
        id = "cyberpunk",
        title = "Cyberpunk Synth & Ambience",
        fileName = "atmosphere_cyberpunk.mp3",
        bundledFallbackRes = R.raw.atmosphere_forest,
        basePriority = 90,
        highPriorityTags = listOf("cyberpunk", "sci-fi", "scifi", "mecha", "futuristic", "space"),
        secondaryTags = listOf("steampunk", "dystopian", "virtual reality", "alien"),
    ),
    MYSTERY(
        id = "mystery",
        title = "Noir & Rainy Window",
        fileName = "atmosphere_mystery.mp3",
        bundledFallbackRes = R.raw.atmosphere_horror,
        basePriority = 85,
        highPriorityTags = listOf("mystery", "detective", "noir", "crime", "investigation"),
        secondaryTags = listOf("suspense", "conspiracy"),
    ),
    HISTORICAL(
        id = "historical",
        title = "Bamboo Temple & Wind Chimes",
        fileName = "atmosphere_historical.mp3",
        bundledFallbackRes = R.raw.atmosphere_forest,
        basePriority = 80,
        highPriorityTags = listOf("historical", "wuxia", "xianxia", "samurai", "feudal", "cultivation"),
        secondaryTags = listOf("period", "traditional"),
    ),
    CAFE(
        id = "cafe",
        title = "Cozy Cafe & Lofi Hum",
        fileName = "atmosphere_cafe.mp3",
        bundledFallbackRes = R.raw.atmosphere_rain,
        basePriority = 60,
        highPriorityTags = listOf("slice of life", "comedy", "school", "school life", "iyashikei", "cooking", "gourmet"),
        secondaryTags = listOf("daily life", "relaxing", "music"),
    ),
    RAIN(
        id = "rain",
        title = "Gentle Rainfall",
        fileName = "atmosphere_rain.mp3",
        bundledFallbackRes = R.raw.atmosphere_rain,
        basePriority = 50,
        highPriorityTags = listOf("romance", "drama", "shoujo", "josei", "melodrama"),
        secondaryTags = listOf("heartwarming", "emotional", "tragedy"),
    ),
    FOREST(
        id = "forest",
        title = "Ancient Forest Wilderness",
        fileName = "atmosphere_forest.mp3",
        bundledFallbackRes = R.raw.atmosphere_forest,
        basePriority = 40,
        highPriorityTags = listOf("wilderness", "dungeon", "quest", "exploration", "nature"),
        secondaryTags = listOf("fantasy", "adventure", "isekai", "magic"),
    );

    companion object {
        fun fromId(id: String): AmbientTrack? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }

        fun forGenres(genres: List<String>): AmbientTrack {
            val allTags = genres.flatMap { it.split(",") }
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }

            if (allTags.isEmpty()) return RAIN

            var bestTrack = RAIN
            var maxScore = -1

            for (track in entries) {
                var score = 0
                for (tag in allTags) {
                    if (track.highPriorityTags.any { tag.contains(it) }) {
                        score += 30
                    } else if (track.secondaryTags.any { tag.contains(it) }) {
                        score += 10
                    }
                }
                if (score > 0) {
                    val total = score + track.basePriority
                    if (total > maxScore) {
                        maxScore = total
                        bestTrack = track
                    }
                }
            }

            return if (maxScore > 0) bestTrack else RAIN
        }
    }
}

class AtmosphericAudioManager(
    private val context: Context,
    private val shinkuPreferences: ShinKuPreferences,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val okHttpClient: OkHttpClient by lazy { Injekt.get<NetworkHelper>().client }

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: AmbientTrack? = null
    private var isPaused: Boolean = false
    private var currentVolume: Float = 0.3f

    fun getCurrentPlayingTrack(): AmbientTrack? = currentTrack

    fun play(genres: List<String>, volumePercent: Int = shinkuPreferences.atmosphericAudioVolume().get()) {
        if (!shinkuPreferences.atmosphericAudio().get()) {
            stop()
            return
        }

        val override = shinkuPreferences.atmosphericAudioOverride().get()
        val track = if (override == "auto" || override.isEmpty()) {
            AmbientTrack.forGenres(genres)
        } else {
            AmbientTrack.fromId(override) ?: AmbientTrack.forGenres(genres)
        }
        val targetVolume = (volumePercent.coerceIn(0, 100) / 100f)
        currentVolume = targetVolume

        // If the same track is already loaded, update volume and ensure playing if not paused
        if (mediaPlayer != null && currentTrack == track) {
            try {
                mediaPlayer?.setVolume(targetVolume, targetVolume)
                if (!isPaused && mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to update atmospheric audio volume" }
            }
            return
        }

        stop()

        val cacheFile = getCacheFile(track)
        var player: MediaPlayer? = null
        var isCached = false

        if (cacheFile.exists() && cacheFile.length() > 1024) {
            player = createPlayerForFile(cacheFile)
            if (player != null) {
                isCached = true
            }
        }

        // Fallback to bundled soundscape if remote file is not yet cached or failed
        if (player == null) {
            player = createPlayerForRes(track.bundledFallbackRes)
        }

        if (player == null) {
            logcat(LogPriority.WARN) { "Failed to initialize player for track: ${track.id}" }
            return
        }

        try {
            player.isLooping = true
            player.setVolume(targetVolume, targetVolume)
            player.setOnErrorListener { _, what, extra ->
                logcat(LogPriority.ERROR) { "MediaPlayer error: what=$what, extra=$extra" }
                stop()
                true
            }

            mediaPlayer = player
            currentTrack = track
            if (!isPaused) {
                player.start()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to start atmospheric audio" }
            stop()
        }

        // Pre-cache remote track in background if not yet cached
        if (!isCached) {
            scope.launch {
                downloadTrack(track)
            }
        }
    }

    fun start(genres: List<String>) {
        play(genres, shinkuPreferences.atmosphericAudioVolume().get())
    }

    fun setVolume(volumePercent: Int) {
        val targetVolume = (volumePercent.coerceIn(0, 100) / 100f)
        currentVolume = targetVolume
        try {
            mediaPlayer?.setVolume(targetVolume, targetVolume)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to set atmospheric audio volume" }
        }
    }

    fun pause() {
        isPaused = true
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to pause atmospheric audio" }
        }
    }

    fun resume() {
        isPaused = false
        if (!shinkuPreferences.atmosphericAudio().get()) return
        try {
            if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.setVolume(currentVolume, currentVolume)
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to resume atmospheric audio" }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to stop atmospheric audio" }
        } finally {
            mediaPlayer = null
            currentTrack = null
            isPaused = false
        }
    }

    private fun getCacheFile(track: AmbientTrack): File {
        val dir = File(context.filesDir, "atmosphere_audio")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, track.fileName)
    }

    private fun createPlayerForFile(file: File): MediaPlayer? {
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build(),
                )
                setDataSource(file.absolutePath)
                prepare()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to load MediaPlayer from file: ${file.name}" }
            file.delete()
            null
        }
    }

    private fun createPlayerForRes(resId: Int): MediaPlayer? {
        return try {
            MediaPlayer.create(context, resId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to load MediaPlayer from res: $resId" }
            null
        }
    }

    suspend fun downloadTrack(track: AmbientTrack): Boolean = withContext(Dispatchers.IO) {
        val dest = getCacheFile(track)
        if (dest.exists() && dest.length() > 1024) return@withContext true

        val dir = dest.parentFile ?: return@withContext false
        val temp = File(dir, "${track.fileName}.tmp")
        val baseUrl = shinkuPreferences.atmosphericAudioHostUrl().get().trimEnd('/')
        val url = "$baseUrl/${track.fileName}"

        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body.byteStream().use { input ->
                    FileOutputStream(temp).use { output ->
                        input.copyTo(output)
                    }
                }
                if (temp.length() > 1024) {
                    if (temp.renameTo(dest)) {
                        logcat(LogPriority.INFO) { "Downloaded atmospheric track: ${track.fileName}" }
                        return@withContext true
                    }
                }
            }
            temp.delete()
            false
        } catch (e: Exception) {
            temp.delete()
            logcat(LogPriority.WARN, e) { "Could not download atmospheric track from $url" }
            false
        }
    }

    suspend fun downloadAllSoundscapes(onProgress: (Int, Int) -> Unit = { _, _ -> }): Boolean = withContext(Dispatchers.IO) {
        val tracks = AmbientTrack.entries
        var successCount = 0
        tracks.forEachIndexed { index, track ->
            val success = downloadTrack(track)
            if (success) successCount++
            onProgress(index + 1, tracks.size)
        }
        successCount > 0
    }

    fun getCacheSizeBytes(): Long {
        val dir = File(context.filesDir, "atmosphere_audio")
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile && !it.name.endsWith(".tmp") }.map { it.length() }.sum()
    }

    fun getFormattedCacheSize(): String {
        val bytes = getCacheSizeBytes()
        return if (bytes < 1024 * 1024) {
            "%.1f KB".format(bytes / 1024f)
        } else {
            "%.1f MB".format(bytes / (1024f * 1024f))
        }
    }

    fun clearCache() {
        val dir = File(context.filesDir, "atmosphere_audio")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }
}
