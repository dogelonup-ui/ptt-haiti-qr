
package com.ptthaiti

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.ptthaiti.qr.OfferActivity
import com.ptthaiti.qr.ScanActivity
import com.ptthaiti.audio.Recorder
import com.ptthaiti.audio.Player
import com.ptthaiti.store.LocalStore
import com.ptthaiti.store.Clip
import com.ptthaiti.p2p.QrP2P
import com.ptthaiti.p2p.P2PSession

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var ptt: Button
    private lateinit var inbox: LinearLayout
    private val reqPerms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    private val rec = Recorder()
    private lateinit var p2p: QrP2P
    private lateinit var sess: P2PSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        ptt = findViewById(R.id.pttBtn)
        inbox = findViewById(R.id.inbox)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            reqPerms.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
        }

        findViewById<Button>(R.id.btnOffer).setOnClickListener {
            startActivity(Intent(this, OfferActivity::class.java))
        }
        findViewById<Button>(R.id.btnScanOffer).setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java).putExtra("mode", "scanOffer"))
        }
        findViewById<Button>(R.id.btnScanAnswer).setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java).putExtra("mode", "scanAnswer"))
        }

        p2p = QrP2P(this)
        sess = P2PSession(this, p2p)
        sess.bind()

        ptt.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    status.text = "Recording…"
                    val id = java.lang.Long.toHexString(java.util.Random().nextLong()).take(8)
                    val f = LocalStore.fileForId(this, id)
                    rec.start(f)
                    ptt.tag = id
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dur = rec.stop()
                    val id = ptt.tag as? String ?: ""
                    if (id.isNotEmpty()) {
                        val f = LocalStore.fileForId(this, id)
                        if (f.exists()) {
                            val bytes = f.readBytes()
                            val sha = LocalStore.sha256(bytes)
                            val clip = Clip(id, System.currentTimeMillis()/1000, if (dur>0) dur else 2.0, f, sha, bytes.size)
                            LocalStore.add(clip)
                            status.text = "Saved. Sending manifest…"
                            sess.sendManifest()
                            renderInbox()
                        } else {
                            status.text = "No file."
                        }
                    }
                }
            }
            true
        }

        renderInbox()
    }

    private fun renderInbox() {
        inbox.removeAllViews()
        for (c in LocalStore.list().reversed()) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val tv = TextView(this).apply { text = "${c.id} • ${"%.1f".format(c.dur)}s" }
            val btn = Button(this).apply { text = "Play"; setOnClickListener { Player.play(c.file) } }
            row.addView(tv); row.addView(btn)
            inbox.addView(row)
        }
    }
}
