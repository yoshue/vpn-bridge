package com.tuapp.vpnbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tuapp.vpnbridge.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnConnect = findViewById<Button>(R.id.btnConnect)
        
        btnConnect.setOnClickListener {
            val intent = Intent(this, BridgeService::class.java)
            startService(intent) // Inicia el motor en segundo plano
            Toast.makeText(this, "Puente VPN iniciado en segundo plano", Toast.LENGTH_SHORT).show()
            btnConnect.text = "PUENTE ACTIVO"
            btnConnect.isEnabled = false
        }
    }
}
