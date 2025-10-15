
package com.ptthaiti.p2p
import kotlinx.serialization.Serializable
@Serializable data class QrIce(val sdpMid: String, val sdpMLineIndex: Int, val candidate: String)
@Serializable data class QrSdp(val type: String, val sdp: String, val ice: List<QrIce> = emptyList())
