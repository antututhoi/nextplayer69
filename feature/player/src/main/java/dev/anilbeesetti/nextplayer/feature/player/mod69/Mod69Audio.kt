package dev.anilbeesetti.nextplayer.feature.player.mod69

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** 69 mod audio settings (kept between videos and app restarts). */
data class Mod69AudioConfig(
    val eqOn: Boolean = false,
    /** Device preset index, or -1 for custom bands. */
    val eqPreset: Int = -1,
    /** Custom band levels in millibels. */
    val bands: List<Int> = emptyList(),
    val bass: Int = 0,
    val virtualizer: Int = 0,
    /** android.media.audiofx.PresetReverb preset, 0 = none. */
    val reverb: Int = 0,
    val delayMs: Int = 0,
    val btDelayMs: Int = 0,
    val fade: Boolean = false,
    /** 0 = normal, 1 = mono, 2 = left/right swapped. */
    val channelMode: Int = 0,
    val leveling: Boolean = false,
)

object Mod69AudioStore {
    private val stateInternal = MutableStateFlow(Mod69AudioConfig())
    val state: StateFlow<Mod69AudioConfig> = stateInternal.asStateFlow()
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("mod69_audio", Context.MODE_PRIVATE)
        prefs = p
        stateInternal.value = runCatching {
            val o = JSONObject(p.getString("cfg", null) ?: return)
            val arr = o.optJSONArray("bands") ?: JSONArray()
            Mod69AudioConfig(
                eqOn = o.optBoolean("eqOn"),
                eqPreset = o.optInt("eqPreset", -1),
                bands = List(arr.length()) { arr.optInt(it) },
                bass = o.optInt("bass"),
                virtualizer = o.optInt("virtualizer"),
                reverb = o.optInt("reverb"),
                delayMs = o.optInt("delayMs"),
                btDelayMs = o.optInt("btDelayMs"),
                fade = o.optBoolean("fade"),
                channelMode = o.optInt("channelMode"),
                leveling = o.optBoolean("leveling"),
            )
        }.getOrDefault(Mod69AudioConfig())
    }

    fun update(transform: (Mod69AudioConfig) -> Mod69AudioConfig) {
        val c = transform(stateInternal.value)
        stateInternal.value = c
        val o = JSONObject()
            .put("eqOn", c.eqOn).put("eqPreset", c.eqPreset).put("bands", JSONArray(c.bands))
            .put("bass", c.bass).put("virtualizer", c.virtualizer).put("reverb", c.reverb)
            .put("delayMs", c.delayMs).put("btDelayMs", c.btDelayMs).put("fade", c.fade)
            .put("channelMode", c.channelMode).put("leveling", c.leveling)
        prefs?.edit()?.putString("cfg", o.toString())?.apply()
    }
}

/** What the phone's equalizer offers; filled in by the player service. */
object Mod69AudioFx {
    var bandFreqsHz by mutableStateOf<List<Int>>(emptyList())
    var levelMin by mutableStateOf(-1500)
    var levelMax by mutableStateOf(1500)
    var presetNames by mutableStateOf<List<String>>(emptyList())
    var bluetoothActive by mutableStateOf(false)
}

/**
 * Sound processing done on the decoded audio: audio delay (positive = sound later),
 * mono / swapped channels, fade-in after start and seek, and simple volume levelling.
 */
@OptIn(UnstableApi::class)
class Mod69AudioProcessor : BaseAudioProcessor() {

    @Volatile var targetDelayMs: Int = 0
    @Volatile var channelMode: Int = 0
    @Volatile var fade: Boolean = false
    @Volatile var leveling: Boolean = false

    private var appliedDelayMs = 0
    private var silenceBytes = 0L
    private var dropBytes = 0L
    private var fadeTotal = 0
    private var fadeLeft = 0
    private var levelGain = 1f

    private val bytesPerFrame get() = inputAudioFormat.bytesPerFrame
    private fun msToBytes(ms: Int): Long =
        ms.toLong() * inputAudioFormat.sampleRate / 1000L * bytesPerFrame

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        startOver()
    }

    private fun startOver() {
        appliedDelayMs = targetDelayMs
        silenceBytes = if (appliedDelayMs > 0) msToBytes(appliedDelayMs) else 0L
        dropBytes = if (appliedDelayMs < 0) msToBytes(-appliedDelayMs) else 0L
        fadeTotal = if (fade) inputAudioFormat.sampleRate * 400 / 1000 else 0
        fadeLeft = fadeTotal
    }

    override fun onReset() {
        appliedDelayMs = 0
        silenceBytes = 0
        dropBytes = 0
        levelGain = 1f
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bpf = bytesPerFrame
        if (bpf <= 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }
        val target = targetDelayMs
        if (target != appliedDelayMs) {
            val diff = target - appliedDelayMs
            if (diff > 0) silenceBytes += msToBytes(diff) else dropBytes += msToBytes(-diff)
            // An earlier "drop" and a new "silence" cancel each other out.
            val both = minOf(silenceBytes, dropBytes)
            silenceBytes -= both
            dropBytes -= both
            appliedDelayMs = target
        }
        if (dropBytes > 0) {
            val n = (minOf(dropBytes, inputBuffer.remaining().toLong()) / bpf * bpf).toInt()
            inputBuffer.position(inputBuffer.position() + n)
            dropBytes -= n
            if (dropBytes < bpf) dropBytes = 0
            if (!inputBuffer.hasRemaining()) return
        }
        // Insert at most one second of silence per call so buffers stay small.
        val maxSilence = (inputAudioFormat.sampleRate.toLong() * bpf)
        val silenceNow = (minOf(silenceBytes, maxSilence) / bpf * bpf).toInt()
        silenceBytes -= silenceNow
        val remaining = inputBuffer.remaining()
        val out = replaceOutputBuffer(silenceNow + remaining)
        repeat(silenceNow) { out.put(0) }

        val channels = inputAudioFormat.channelCount
        val isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val frames = remaining / bpf
        val start = inputBuffer.position()
        val sample = FloatArray(channels)

        // Volume levelling: measure this block, then move the gain slowly toward the target.
        if (leveling && frames > 0) {
            var sum = 0.0
            for (f in 0 until frames) {
                for (c in 0 until channels) {
                    val v = read(inputBuffer, start + (f * channels + c) * (bpf / channels), isFloat)
                    sum += v * v
                }
            }
            val rms = sqrt(sum / (frames * channels)).toFloat()
            if (rms > 0.0005f) {
                val wanted = (0.12f / rms).coerceIn(0.5f, 4f)
                val speed = if (wanted < levelGain) 0.3f else 0.03f
                levelGain += (wanted - levelGain) * speed
            }
        } else {
            levelGain = 1f
        }

        val bytesPerSample = bpf / channels
        for (f in 0 until frames) {
            val base = start + f * bpf
            for (c in 0 until channels) sample[c] = read(inputBuffer, base + c * bytesPerSample, isFloat)
            when (channelMode) {
                1 -> {
                    var m = 0f
                    for (c in 0 until channels) m += sample[c]
                    m /= channels
                    for (c in 0 until channels) sample[c] = m
                }
                2 -> if (channels >= 2) {
                    val l = sample[0]
                    sample[0] = sample[1]
                    sample[1] = l
                }
            }
            var g = levelGain
            if (fadeLeft > 0) {
                g *= 1f - fadeLeft.toFloat() / fadeTotal
                fadeLeft--
            }
            for (c in 0 until channels) {
                var v = sample[c] * g
                if (abs(v) > 1f) v = if (v > 0) 1f else -1f
                if (isFloat) out.putFloat(v) else out.putShort((v * 32767f).toInt().toShort())
            }
        }
        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }

    private fun read(buf: ByteBuffer, index: Int, isFloat: Boolean): Float =
        if (isFloat) buf.getFloat(index) else buf.getShort(index) / 32768f
}
