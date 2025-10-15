
package com.ptthaiti.qr

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.ptthaiti.R
import com.ptthaiti.p2p.QrP2P
import com.ptthaiti.p2p.QrSdp
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.serialization.json.Json
import android.util.Base64
import android.content.Intent

class ScanActivity : ComponentActivity() {
    private lateinit var barcode: DecoratedBarcodeView
    private lateinit var p2p: QrP2P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan)
        barcode = findViewById(R.id.barcode_scanner)
        p2p = QrP2P(this)
        val mode = intent.getStringExtra("mode") ?: "scanOffer"

        barcode.decodeContinuous(object: BarcodeCallback {
            override fun barcodeResult(result: com.journeyapps.barcodescanner.BarcodeResult?) {
                val txt = result?.text ?: return
                barcode.pause()
                val json = String(Base64.decode(txt, Base64.NO_WRAP))
                val obj = Json.decodeFromString<QrSdp>(json)

                if (mode == "scanOffer") {
                    p2p.acceptOffer(obj) { answer ->
                        val payload = Base64.encodeToString(Json.encodeToString(answer).toByteArray(), Base64.NO_WRAP)
                        startActivity(Intent(this@ScanActivity, OfferActivity::class.java).putExtra("answerPayload", payload))
                        finish()
                    }
                } else {
                    p2p.finalize(obj)
                    finish()
                }
            }
            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })
    }

    override fun onResume() { super.onResume(); barcode.resume() }
    override fun onPause() { super.onPause(); barcode.pause() }
}
