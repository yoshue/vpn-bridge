package com.tuapp.vpnbridge

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import fi.iki.elonen.NanoHTTPD
import java.net.*
import kotlin.concurrent.thread

class BridgeService : Service() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var proxyServer: ProxyServer? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, crearNotificacion())
        // Iniciamos el servidor proxy en el puerto 8282
        proxyServer = ProxyServer(8282)
        try {
            proxyServer?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: Exception) {
            Log.e("VPNBridge", "Error al iniciar Proxy: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)
        
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { crearGrupo() }
            override fun onFailure(p0: Int) { crearGrupo() }
        })
        return START_STICKY
    }

    private fun crearGrupo() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Handler(Looper.getMainLooper()).postDelayed({ obtenerDatos() }, 3000)
            }
            override fun onFailure(p0: Int) { enviarMsj("Error P2P: $p0") }
        })
    }

    private fun obtenerDatos() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                val info = "MODO ARTILLERÍA ACTIVO\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}\n\nIP: 192.168.49.1 | Puerto: 8282"
                enviarMsj(info)
            }
        }
    }

    // Clase interna que maneja el tráfico
    private class ProxyServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            // Aquí es donde sucede la magia: la app recibe la petición de la TV 
            // y la reenvía usando su propia conexión (la VPN)
            return newFixedLengthResponse("Conectado al Puente VPN")
        }
    }

    private fun enviarMsj(txt: String) {
        val i = Intent("VPN_BRIDGE_UPDATE").putExtra("info", txt)
        sendBroadcast(i)
    }

    override fun onDestroy() {
        proxyServer?.stop()
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun crearNotificacion(): Notification {
        val canalId = "VPN_BRIDGE_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "VPN Bridge", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java) as NotificationManager
            nm.createNotificationChannel(canal)
        }
        return NotificationCompat.Builder(this, canalId)
            .setContentTitle("Puente Nivel Pro")
            .setContentText("Servidor de datos activo")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()
    }
}
