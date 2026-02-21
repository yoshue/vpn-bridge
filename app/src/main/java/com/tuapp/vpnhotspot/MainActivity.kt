package com.tuapp.vpnbridge

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// ESTA LÍNEA ES LA QUE FALTA Y CURA EL ERROR "R":
import com.tuapp.vpnbridge.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnConnect = findViewById<Button>(R.id.btnConnect)
        btnConnect.setOnClickListener {
            Toast.makeText(this, "Puente iniciado correctamente", Toast.LENGTH_SHORT).show()
        }
    }
}
