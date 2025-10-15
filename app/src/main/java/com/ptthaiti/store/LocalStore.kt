
package com.ptthaiti.store

import android.content.Context
import java.io.File
import java.security.MessageDigest

data class Clip(val id: String, val ts: Long, val dur: Double, val file: File, val sha256: String, val bytes: Int)

object LocalStore {
    private val clips = LinkedHashMap<String, Clip>()

    fun list(): List<Clip> = clips.values.toList()

    fun has(id: String): Boolean = clips.containsKey(id)

    fun add(c: Clip) { clips[c.id] = c }

    fun fileForId(ctx: Context, id: String): File = File(ctx.cacheDir, "$id.webm")

    fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val dig = md.digest(bytes)
        return dig.joinToString("") { "%02x".format(it) }
    }
}
