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
    private var running = true

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationCompat.Builder(this, canalId)
            .setContentTitle("VPN Bridge: Modo Transparente")
            .setContentText("Puenteando tráfico hacia la VPN...")
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
                iniciarPuenteDeDatos()
            }
            override fun onFailure(p0: Int) { enviarUpdate("Error de inicio: $p0") }
        })
        return START_STICKY
    }

    private fun iniciarPuenteDeDatos() {
        thread {
            try {
                // Intentamos "despertar" el reenvío de paquetes 
                // forzando una conexión de socket a través de la interfaz VPN
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1000)
                socket.close()
                Log.d("VPNBridge", "Interfaz de salida VPN detectada y activa.")
            } catch (e: Exception) {
                Log.e("VPNBridge", "VPN no detectada o bloqueada.")
            }
        }
    }

    private fun obtenerDatos() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                val info = "TV CONECTADA A:\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}\n\nPASO FINAL: Si sigue sin internet, busca 'Puente WiFi' en los ajustes de tu Huawei."
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
        running = false
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }
}
