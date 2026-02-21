package com.tuapp.vpnbridge.final

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Esta línea conecta con el archivo activity_main.xml que creamos
        setContentView(R.layout.activity_main)

        // Buscamos el botón por su ID para darle una función básica
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        
        btnConnect.setOnClickListener {
            // Esto mostrará un mensaje rápido al tocar el botón
            Toast.makeText(this, "Iniciando servicio de puente...", Toast.LENGTH_SHORT).show()
        }
    }
}
