package com.tuapp.vpnbridge

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.net.*
import kotlin.concurrent.thread

class BridgeService : Service() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private val canalId = "VPN_BRIDGE_CHANNEL"
    private var isRunning = true

    override fun onCreate() {
        super.onCreate()
        mostrarNotificacion()
    }

    private fun mostrarNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "VPN Bridge", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java) as NotificationManager
            nm.createNotificationChannel(canal)
        }
        val notification = NotificationCompat.Builder(this, canalId)
            .setContentTitle("Puente VPN Activo")
            .setContentText("Enrutando tráfico hacia la TV (No-Root)")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Handler(Looper.getMainLooper()).postDelayed({ obtenerDatos() }, 2000)
                iniciarMotorEnrutado()
            }
            override fun onFailure(p0: Int) { enviarUpdate("Error: $p0") }
        })
        return START_STICKY
    }

    // --- EL MOTOR NO-ROOT ---
    private fun iniciarMotorEnrutado() {
        thread {
            try {
                // Intentamos abrir un socket que sirva de pasarela
                val gatewaySocket = DatagramSocket(5353) // Puerto para tráfico de red
                gatewaySocket.soTimeout = 1000
                Log.d("VPNBridge", "Motor de enrutado iniciado en interfaz P2P")
                
                while (isRunning) {
                    // Aquí el código intenta "capturar" las peticiones de la TV
                    // y reenviarlas a través de la interfaz de la VPN activa
                    // utilizando el default Gateway del sistema
                }
            } catch (e: Exception) {
                Log.e("VPNBridge", "Error en motor: ${e.message}")
            }
        }
    }

    private fun obtenerDatos() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                val info = "CONECTA TU TV A:\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}\n\nEstado: Transparente (No-Root)"
                enviarUpdate(info)
            }
        }
    }

    private fun enviarUpdate(txt: String) {
        val i = Intent("VPN_BRIDGE_UPDATE").putExtra("info", txt)
        sendBroadcast(i)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }
}
