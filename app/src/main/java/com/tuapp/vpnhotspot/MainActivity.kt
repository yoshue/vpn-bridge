package com.tuapp.vpnbridge

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvDetails: TextView

    // Recibimos los datos de la red desde el servicio
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val info = intent?.getStringExtra("info")
            tvDetails.text = info
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDetails = findViewById(R.id.tvDetails)
        val btnConnect = findViewById<Button>(R.id.btnConnect)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        btnConnect.setOnClickListener {
            val intent = Intent(this, BridgeService::class.java)
            startForegroundService(intent)
            btnConnect.text = "BUSCANDO..."
            btnConnect.isEnabled = false
        }

        registerReceiver(receiver, IntentFilter("VPN_BRIDGE_UPDATE"), RECEIVER_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
