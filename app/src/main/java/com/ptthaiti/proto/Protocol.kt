
package com.ptthaiti.proto

import kotlinx.serialization.Serializable

@Serializable data class ManItem(val id: String, val ts: Long, val dur: Double, val bytes: Int, val sha256: String)
@Serializable data class WManifest(val t: String = "manifest", val msgs: List<ManItem>)
@Serializable data class WNeed(val t: String = "need", val ids: List<String>)
@Serializable data class WBlob(val t: String = "blob", val id: String, val seq: Int, val eof: Boolean, val data: String)
