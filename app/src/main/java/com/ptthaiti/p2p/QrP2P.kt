
package com.ptthaiti.p2p

import android.content.Context
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.webrtc.*
import java.nio.ByteBuffer

class QrP2P(ctx: Context) {
    init { PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(ctx).createInitializationOptions()) }
    private val pcFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    private val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
    private var pc: PeerConnection? = null
    private var dc: DataChannel? = null
    private val iceList = mutableListOf<IceCandidate>()
    val incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun createOffer(onReady: (QrSdp) -> Unit) {
        pc = pcFactory.createPeerConnection(PeerConnection.RTCConfiguration(iceServers), object: PeerConnection.Observer {
            override fun onDataChannel(channel: DataChannel) { dc = channel; hookDc() }
            override fun onIceCandidate(c: IceCandidate) { iceList.add(c) }
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState) { if (s == PeerConnection.IceGatheringState.COMPLETE) emitSdp(onReady) }
            override fun onConnectionChange(p0: PeerConnection.PeerConnectionState) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState) {}
        })
        dc = pc!!.createDataChannel("ptt", DataChannel.Init())
        hookDc()
        pc!!.createOffer(object: SdpObserver {
            override fun onCreateSuccess(s: SessionDescription) { pc!!.setLocalDescription(this, s) }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }

    fun acceptOffer(offer: QrSdp, onReady: (QrSdp) -> Unit) {
        pc = pcFactory.createPeerConnection(PeerConnection.RTCConfiguration(iceServers), object: PeerConnection.Observer {
            override fun onDataChannel(channel: DataChannel) { dc = channel; hookDc() }
            override fun onIceCandidate(c: IceCandidate) { iceList.add(c) }
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState) { if (s == PeerConnection.IceGatheringState.COMPLETE) emitSdp(onReady) }
            override fun onConnectionChange(p0: PeerConnection.PeerConnectionState) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState) {}
        })
        pc!!.setRemoteDescription(object: SdpObserver {
            override fun onSetSuccess() {
                pc!!.createAnswer(object: SdpObserver {
                    override fun onCreateSuccess(ans: SessionDescription) { pc!!.setLocalDescription(this, ans) }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, MediaConstraints())
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, SessionDescription(SessionDescription.Type.OFFER, offer.sdp))
    }

    fun finalize(answer: QrSdp) {
        pc?.setRemoteDescription(object: SdpObserver {
            override fun onSetSuccess() {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, SessionDescription(SessionDescription.Type.ANSWER, answer.sdp))
        answer.ice.forEach { pc?.addIceCandidate(IceCandidate(it.sdpMid, it.sdpMLineIndex, it.candidate)) }
    }

    private fun emitSdp(cb: (QrSdp) -> Unit) {
        val sdp = pc!!.localDescription
        cb(QrSdp(sdp.type.canonicalForm(), sdp.description, iceList.map { QrIce(it.sdpMid, it.sdpMLineIndex, it.sdp) }))
    }

    private fun hookDc() {
        dc?.registerObserver(object: DataChannel.Observer {
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                incoming.tryEmit(bytes)
            }
            override fun onStateChange() {}
            override fun onBufferedAmountChange(prev: Long) {}
        })
    }

    fun send(bytes: ByteArray) { dc?.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false)) }
}
