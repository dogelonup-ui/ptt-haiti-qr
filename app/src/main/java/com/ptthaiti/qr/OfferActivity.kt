
package com.ptthaiti.qr

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.ptthaiti.R
import com.ptthaiti.p2p.QrP2P
import com.ptthaiti.p2p.QrSdp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import android.util.Base64

class OfferActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.offer)
        val img = findViewById<ImageView>(R.id.qrImage)
        val answerPayload = intent.getStringExtra("answerPayload")
        if (answerPayload != null) {
            img.setImageBitmap(qr(answerPayload))
            return
        }
        val p2p = QrP2P(this)
        p2p.createOffer { offer ->
            img.setImageBitmap(qr(makePayload(offer)))
        }
    }

    private fun makePayload(offer: QrSdp): String {
        val json = Json.encodeToString(offer)
        return Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
    }

    private fun qr(data: String, size: Int = 800): Bitmap {
        val bits = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size) {
            bmp.setPixel(x, y, if (bits.get(x,y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
        return bmp
    }
}
