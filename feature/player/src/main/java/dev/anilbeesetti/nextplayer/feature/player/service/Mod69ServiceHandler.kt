package dev.anilbeesetti.nextplayer.feature.player.service

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SingleColorLut
import androidx.media3.exoplayer.ExoPlayer
import dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69AudioFx
import dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69AudioProcessor
import dev.anilbeesetti.nextplayer.feature.player.mod69.Mod69AudioStore
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 69 mod: player features that must live next to the real player (so they also work in
 * background play): sleep timer, A-B repeat and video picture adjustments.
 *
 * Everything goes through one custom command; [handle] reads "op" from the bundle.
 */
@OptIn(UnstableApi::class)
class Mod69ServiceHandler(
    private val context: Context,
    private val playerProvider: () -> Player?,
    private val audioProcessor: Mod69AudioProcessor,
) {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ---- audio effects on the player's audio session ----
    private var sessionId = C.AUDIO_SESSION_ID_UNSET
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = applyAudioSettings()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = applyAudioSettings()
    }

    init {
        Mod69AudioStore.init(context)
        runCatching { audioManager.registerAudioDeviceCallback(deviceCallback, handler) }
        scope.launch { Mod69AudioStore.state.collect { applyAudioSettings() } }
    }

    private fun isBluetoothOut(): Boolean = runCatching {
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= 31 && (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
        }
    }.getOrDefault(false)

    private fun ensureSessionEffects() {
        val exo = playerProvider() as? ExoPlayer ?: return
        val id = exo.audioSessionId
        if (id == C.AUDIO_SESSION_ID_UNSET || id == sessionId) return
        releaseEffects()
        sessionId = id
        equalizer = runCatching { Equalizer(0, id) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, id) }.getOrNull()
        virtualizer = runCatching { Virtualizer(0, id) }.getOrNull()
        reverb = runCatching { PresetReverb(0, id) }.getOrNull()
        equalizer?.let { eq ->
            runCatching {
                val range = eq.bandLevelRange
                Mod69AudioFx.levelMin = range[0].toInt()
                Mod69AudioFx.levelMax = range[1].toInt()
                Mod69AudioFx.bandFreqsHz = List(eq.numberOfBands.toInt()) { eq.getCenterFreq(it.toShort()) / 1000 }
                Mod69AudioFx.presetNames = List(eq.numberOfPresets.toInt()) { eq.getPresetName(it.toShort()) }
            }
        }
    }

    private fun releaseEffects() {
        listOf(equalizer, bassBoost, virtualizer, reverb).forEach { runCatching { it?.release() } }
        equalizer = null
        bassBoost = null
        virtualizer = null
        reverb = null
        sessionId = C.AUDIO_SESSION_ID_UNSET
    }

    fun applyAudioSettings() {
        val c = Mod69AudioStore.state.value
        val bt = isBluetoothOut()
        Mod69AudioFx.bluetoothActive = bt
        audioProcessor.targetDelayMs = c.delayMs + if (bt) c.btDelayMs else 0
        audioProcessor.channelMode = c.channelMode
        audioProcessor.fade = c.fade
        audioProcessor.leveling = c.leveling
        ensureSessionEffects()
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = c.eqOn
                if (c.eqOn) {
                    if (c.eqPreset >= 0 && c.eqPreset < eq.numberOfPresets) {
                        eq.usePreset(c.eqPreset.toShort())
                    } else {
                        c.bands.forEachIndexed { i, level ->
                            if (i < eq.numberOfBands) eq.setBandLevel(i.toShort(), level.toShort())
                        }
                    }
                }
            }
        }
        runCatching {
            bassBoost?.let {
                it.enabled = c.bass > 0
                if (c.bass > 0 && it.strengthSupported) it.setStrength(c.bass.toShort())
            }
        }
        runCatching {
            virtualizer?.let {
                it.enabled = c.virtualizer > 0
                if (c.virtualizer > 0 && it.strengthSupported) it.setStrength(c.virtualizer.toShort())
            }
        }
        runCatching {
            reverb?.let {
                it.enabled = c.reverb > 0
                if (c.reverb > 0) it.preset = c.reverb.toShort()
            }
        }
    }

    // ---- sleep timer ----
    private var sleepAtElapsed = 0L
    private var sleepEndOfFile = false
    private val sleepRunnable = Runnable {
        sleepAtElapsed = 0L
        fadeOutAndPause()
    }

    // ---- A-B repeat ----
    private var aMs = -1L
    private var bMs = -1L
    private val abRunnable = object : Runnable {
        override fun run() {
            val p = playerProvider() ?: return
            if (aMs >= 0 && bMs > aMs) {
                if (p.currentPosition >= bMs || p.currentPosition < aMs - 500) p.seekTo(aMs)
                handler.postDelayed(this, 40)
            }
        }
    }

    private var listenerAttachedTo: Player? = null
    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (sleepEndOfFile && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                sleepEndOfFile = false
                playerProvider()?.pause()
            }
            // A-B points belong to one video.
            clearAb()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            applyAudioSettings()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && sleepEndOfFile) sleepEndOfFile = false
        }
    }

    private fun ensureListener() {
        val p = playerProvider() ?: return
        if (listenerAttachedTo === p) return
        listenerAttachedTo?.removeListener(listener)
        p.addListener(listener)
        listenerAttachedTo = p
        applyAudioSettings()
    }

    /** Called by the service once the player exists. */
    fun attach() {
        ensureListener()
    }

    private fun fadeOutAndPause() {
        val p = playerProvider() ?: return
        val start = p.volume
        var step = 0
        val steps = 20
        val fade = object : Runnable {
            override fun run() {
                step++
                p.volume = start * (1f - step / steps.toFloat())
                if (step < steps) {
                    handler.postDelayed(this, 100)
                } else {
                    p.pause()
                    p.volume = start
                }
            }
        }
        handler.post(fade)
    }

    private fun clearAb() {
        aMs = -1L
        bMs = -1L
        handler.removeCallbacks(abRunnable)
    }

    fun handle(args: Bundle): Bundle {
        ensureListener()
        val out = Bundle()
        when (args.getString(OP)) {
            "sleep_set" -> {
                handler.removeCallbacks(sleepRunnable)
                sleepAtElapsed = 0L
                sleepEndOfFile = false
                val minutes = args.getInt("minutes")
                when {
                    minutes < 0 -> sleepEndOfFile = true
                    minutes > 0 -> {
                        sleepAtElapsed = SystemClock.elapsedRealtime() + minutes * 60_000L
                        handler.postDelayed(sleepRunnable, minutes * 60_000L)
                    }
                }
            }

            "sleep_get" -> Unit

            "ab_set" -> {
                handler.removeCallbacks(abRunnable)
                aMs = args.getLong("a", -1L)
                bMs = args.getLong("b", -1L)
                if (aMs >= 0 && bMs > aMs) {
                    playerProvider()?.seekTo(aMs)
                    handler.post(abRunnable)
                }
            }

            "effects" -> applyEffects(args)
        }
        out.putLong("sleep_left", if (sleepAtElapsed > 0) sleepAtElapsed - SystemClock.elapsedRealtime() else 0L)
        out.putBoolean("sleep_eof", sleepEndOfFile)
        out.putLong("a", aMs)
        out.putLong("b", bMs)
        return out
    }

    private fun applyEffects(args: Bundle) {
        val player = playerProvider() as? ExoPlayer ?: return
        val brightness = args.getFloat("brightness")
        val contrast = args.getFloat("contrast")
        val saturation = args.getFloat("saturation")
        val hue = args.getFloat("hue")
        val gamma = args.getFloat("gamma", 1f)
        val mirror = args.getBoolean("mirror")
        val flip = args.getBoolean("flip")
        val rotate = args.getInt("rotate")

        val effects = mutableListOf<Effect>()
        if (brightness != 0f) effects += Brightness(brightness.coerceIn(-1f, 1f))
        if (contrast != 0f) effects += Contrast(contrast.coerceIn(-1f, 1f))
        if (saturation != 0f || hue != 0f) {
            effects += HslAdjustment.Builder()
                .adjustSaturation(saturation.coerceIn(-100f, 100f))
                .adjustHue(hue)
                .build()
        }
        if (gamma != 1f && gamma > 0f) effects += gammaLut(gamma)
        if (mirror || flip || rotate != 0) {
            effects += ScaleAndRotateTransformation.Builder()
                .setScale(if (mirror) -1f else 1f, if (flip) -1f else 1f)
                .setRotationDegrees(rotate.toFloat())
                .build()
        }
        runCatching { player.setVideoEffects(effects) }
    }

    private fun gammaLut(gamma: Float): Effect {
        val n = 17
        val inv = 1f / gamma
        val level = IntArray(n) { i -> ((i / (n - 1f)).pow(inv) * 255f).roundToInt().coerceIn(0, 255) }
        val cube = Array(n) { r ->
            Array(n) { g ->
                IntArray(n) { b ->
                    (0xFF shl 24) or (level[r] shl 16) or (level[g] shl 8) or level[b]
                }
            }
        }
        return SingleColorLut.createFromCube(cube)
    }

    fun release() {
        scope.cancel()
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
        releaseEffects()
        handler.removeCallbacksAndMessages(null)
        listenerAttachedTo?.removeListener(listener)
        listenerAttachedTo = null
    }

    companion object {
        const val OP = "op"
    }
}
