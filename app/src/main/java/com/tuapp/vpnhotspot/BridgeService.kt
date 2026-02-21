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

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationCompat.Builder(this, canalId)
            .setContentTitle("Puente Activo (P40 Pro)")
            .setContentText("Forzando salida de datos...")
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
                // Iniciamos el "Eco de Red"
                thread { iniciarEcoRed() }
            }
            override fun onFailure(p0: Int) { enviarUpdate("Fallo: $p0") }
        })
        return START_STICKY
    }

    private fun iniciarEcoRed() {
        try {
            // Intentamos abrir un socket que "despierte" la ruta hacia la VPN
            // Esto le dice al kernel de Huawei: "hay tráfico saliendo de aquí"
            val selector = java.nio.channels.Selector.open()
            val socket = DatagramSocket(67) // Puerto DHCP común
            socket.broadcast = true
            Log.d("VPNBridge", "Eco de red iniciado")
        } catch (e: Exception) {
            Log.e("VPNBridge", "Error eco: ${e.message}")
        }
    }

    private fun obtenerDatos() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                // Forzamos la IP estándar de WiFi Direct en el mensaje
                val info = "TV CONECTADA A:\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}\n\nIP GATEWAY: 192.168.49.1"
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
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }
}
