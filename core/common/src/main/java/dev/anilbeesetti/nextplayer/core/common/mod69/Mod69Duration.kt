package dev.anilbeesetti.nextplayer.core.common.mod69

import android.content.Context
import android.net.Uri
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * 69 mod: video length without decoding: MP4/MOV (mvhd in moov) and MKV/WebM (Segment > Info > Duration).
 * Uses positioned reads, so only a few KB are read even for huge files on cloud folders.
 */
object Mod69Duration {

    fun read(context: Context, uri: Uri): Long? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            FileInputStream(pfd.fileDescriptor).channel.use { ch ->
                val head = bytes(ch, 0, 12) ?: return@use null
                when {
                    head[0] == 0x1A.toByte() && head[1] == 0x45.toByte() && head[2] == 0xDF.toByte() && head[3] == 0xA3.toByte() -> mkv(ch)
                    String(head, 4, 4, Charsets.US_ASCII) in setOf("ftyp", "moov", "mdat", "free", "wide", "skip") -> mp4(ch)
                    else -> null
                }
            }
        }
    }.getOrNull()

    private fun bytes(ch: FileChannel, pos: Long, n: Int): ByteArray? {
        val b = ByteBuffer.allocate(n)
        var p = pos
        while (b.hasRemaining()) {
            val r = ch.read(b, p)
            if (r <= 0) break
            p += r
        }
        return if (b.position() == n) b.array() else null
    }

    // ---------- MP4 / MOV ----------

    private fun mp4(ch: FileChannel): Long? {
        val size = ch.size()
        var pos = 0L
        var guard = 0
        while (pos + 8 <= size && guard++ < 512) {
            val h = bytes(ch, pos, 16) ?: bytes(ch, pos, 8) ?: return null
            val bb = ByteBuffer.wrap(h).order(ByteOrder.BIG_ENDIAN)
            var len = bb.getInt(0).toLong() and 0xFFFFFFFFL
            val type = String(h, 4, 4, Charsets.US_ASCII)
            var header = 8
            if (len == 1L && h.size >= 16) {
                len = bb.getLong(8)
                header = 16
            } else if (len == 0L) {
                len = size - pos
            }
            if (len < header) return null
            if (type == "moov") return mvhd(ch, pos + header, pos + len)
            pos += len
        }
        return null
    }

    private fun mvhd(ch: FileChannel, start: Long, end: Long): Long? {
        var pos = start
        var guard = 0
        while (pos + 8 <= end && guard++ < 256) {
            val h = bytes(ch, pos, 8) ?: return null
            val len = ByteBuffer.wrap(h).order(ByteOrder.BIG_ENDIAN).getInt(0).toLong() and 0xFFFFFFFFL
            val type = String(h, 4, 4, Charsets.US_ASCII)
            if (type == "mvhd") {
                val box = bytes(ch, pos, 40) ?: return null
                val b = ByteBuffer.wrap(box).order(ByteOrder.BIG_ENDIAN)
                val version = box[8].toInt()
                val (scale, dur) = if (version == 1) {
                    (b.getInt(28).toLong() and 0xFFFFFFFFL) to b.getLong(32)
                } else {
                    (b.getInt(20).toLong() and 0xFFFFFFFFL) to (b.getInt(24).toLong() and 0xFFFFFFFFL)
                }
                return if (scale > 0 && dur > 0) dur * 1000 / scale else null
            }
            if (len < 8) return null
            pos += len
        }
        return null
    }

    // ---------- Matroska / WebM ----------

    private class Reader(val ch: FileChannel, var pos: Long) {
        private fun byte(): Int {
            val b = bytes(ch, pos, 1) ?: throw IllegalStateException("eof")
            pos++
            return b[0].toInt() and 0xFF
        }

        /** Element id, with its length marker kept (as in the Matroska spec). */
        fun id(): Long {
            val first = byte()
            var mask = 0x80
            var len = 1
            while (len <= 4 && first and mask == 0) {
                mask = mask shr 1
                len++
            }
            if (len > 4) throw IllegalStateException("bad id")
            var v = first.toLong()
            repeat(len - 1) { v = (v shl 8) or byte().toLong() }
            return v
        }

        /** Element size; -1 = unknown. */
        fun size(): Long {
            val first = byte()
            var mask = 0x80
            var len = 1
            while (len <= 8 && first and mask == 0) {
                mask = mask shr 1
                len++
            }
            if (len > 8) throw IllegalStateException("bad size")
            var v = (first and (mask - 1)).toLong()
            var allOnes = v == (mask - 1).toLong()
            repeat(len - 1) {
                val b = byte()
                if (b != 0xFF) allOnes = false
                v = (v shl 8) or b.toLong()
            }
            return if (allOnes) -1 else v
        }
    }

    private fun mkv(ch: FileChannel): Long? {
        val r = Reader(ch, 0)
        // EBML header
        if (r.id() != 0x1A45DFA3L) return null
        val hs = r.size()
        if (hs < 0) return null
        r.pos += hs
        if (r.id() != 0x18538067L) return null // Segment
        val segSize = r.size()
        val segEnd = if (segSize < 0) ch.size() else minOf(ch.size(), r.pos + segSize)
        var guard = 0
        while (r.pos < segEnd && guard++ < 64) {
            val id = r.id()
            val sz = r.size()
            if (id == 0x1549A966L && sz > 0) return info(ch, r.pos, r.pos + sz)
            if (id == 0x1F43B675L || sz < 0) return null // a Cluster before Info: give up
            r.pos += sz
        }
        return null
    }

    private fun info(ch: FileChannel, start: Long, end: Long): Long? {
        val r = Reader(ch, start)
        var scale = 1_000_000L
        var duration = -1.0
        var guard = 0
        while (r.pos < end && guard++ < 64) {
            val id = r.id()
            val sz = r.size()
            if (sz < 0) return null
            when (id) {
                0x2AD7B1L -> {
                    val b = bytes(ch, r.pos, sz.toInt()) ?: return null
                    var v = 0L
                    b.forEach { v = (v shl 8) or (it.toLong() and 0xFF) }
                    if (v > 0) scale = v
                }
                0x4489L -> {
                    val b = bytes(ch, r.pos, sz.toInt()) ?: return null
                    val bb = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN)
                    duration = when (sz.toInt()) {
                        4 -> bb.getFloat(0).toDouble()
                        8 -> bb.getDouble(0)
                        else -> -1.0
                    }
                }
            }
            r.pos += sz
        }
        return if (duration > 0) (duration * scale / 1_000_000.0).toLong() else null
    }
}
