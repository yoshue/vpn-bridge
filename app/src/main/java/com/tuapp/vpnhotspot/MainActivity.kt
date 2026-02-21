package com.tuapp.vpnbridge

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvDetails: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnStop: Button

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val info = intent?.getStringExtra("info")
            tvDetails.text = info
            if (info?.contains("RED:") == true) {
                btnConnect.text = "PUENTE ACTIVO"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDetails = findViewById(R.id.tvDetails)
        btnConnect = findViewById(R.id.btnConnect)
        btnStop = findViewById(R.id.btnStop)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        btnConnect.setOnClickListener {
            val intent = Intent(this, BridgeService::class.java)
            startForegroundService(intent)
            btnConnect.isEnabled = false
            btnConnect.text = "BUSCANDO..."
            btnStop.visibility = View.VISIBLE
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, BridgeService::class.java))
            btnConnect.isEnabled = true
            btnConnect.text = "INICIAR PUENTE"
            btnStop.visibility = View.GONE
            tvDetails.text = "Puente detenido."
        }

        registerReceiver(receiver, IntentFilter("VPN_BRIDGE_UPDATE"), RECEIVER_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
