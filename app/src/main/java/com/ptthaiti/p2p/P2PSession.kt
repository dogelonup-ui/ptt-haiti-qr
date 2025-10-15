
package com.ptthaiti.p2p

import android.content.Context
import android.util.Base64
import com.ptthaiti.proto.*
import com.ptthaiti.store.LocalStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.min

class P2PSession(private val ctx: Context, private val p2p: QrP2P) {
    private val json = Json
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // When bytes arrive, route them
        p2p.incoming.collect { /* no-op placeholder for compiler */ }
    }

    fun bind() {
        // launch a collector
        scope.launch {
            p2p.incoming.collect { bytes ->
                try {
                    val txt = String(bytes)
                    when (json.decodeFromString<Map<String, @JvmSuppressWildcards Any>>(txt)["t"]) {
                        "manifest" -> onManifest(json.decodeFromString(WManifest.serializer(), txt))
                        "need" -> onNeed(json.decodeFromString(WNeed.serializer(), txt))
                        "blob" -> onBlob(json.decodeFromString(WBlob.serializer(), txt))
                        else -> {}
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Send our manifest
    fun sendManifest() {
        val msgs = LocalStore.list().map { ManItem(it.id, it.ts, it.dur, it.bytes, it.sha256) }
        val payload = json.encodeToString(WManifest.serializer(), WManifest(msgs = msgs))
        p2p.send(payload.toByteArray())
    }

    private fun onManifest(m: WManifest) {
        val missing = m.msgs.filter { !LocalStore.has(it.id) }.map { it.id }
        if (missing.isNotEmpty()) {
            val n = json.encodeToString(WNeed.serializer(), WNeed(ids = missing))
            p2p.send(n.toByteArray())
        }
    }

    private fun onNeed(n: WNeed) {
        scope.launch {
            for (id in n.ids) {
                val file = LocalStore.fileForId(ctx, id)
                if (!file.exists()) continue
                val bytes = file.readBytes()
                var off = 0
                var seq = 0
                val chunk = 24 * 1024
                while (off < bytes.size) {
                    val take = min(chunk, bytes.size - off)
                    val b64 = Base64.encodeToString(bytes, off, take, Base64.NO_WRAP)
                    val blob = WBlob(id = id, seq = seq, eof = (off + take) >= bytes.size, data = b64)
                    val txt = json.encodeToString(WBlob.serializer(), blob)
                    p2p.send(txt.toByteArray())
                    off += take
                    seq += 1
                }
            }
        }
    }

    private val assembling = HashMap<String, MutableList<Pair<Int, ByteArray>>>()

    private fun onBlob(b: WBlob) {
        val list = assembling.getOrPut(b.id) { mutableListOf() }
        val part = Base64.decode(b.data, Base64.NO_WRAP)
        list.add(b.seq to part)
        if (b.eof) {
            val sorted = list.sortedBy { it.first }.map { it.second }
            val out = LocalStore.fileForId(ctx, b.id)
            out.outputStream().use { o ->
                for (p in sorted) o.write(p)
            }
            // We don't know ts/dur/bytes/sha from the sender's WManifest here; real impl would cache last manifest
            // For demo, compute sha & bytes and fake dur=2.0
            val bytes = out.readBytes()
            val sha = LocalStore.sha256(bytes)
            LocalStore.add(com.ptthaiti.store.Clip(b.id, System.currentTimeMillis()/1000, 2.0, out, sha, bytes.size))
        }
    }
}
