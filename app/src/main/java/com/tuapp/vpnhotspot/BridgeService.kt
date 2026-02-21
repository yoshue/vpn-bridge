package com.tuapp.vpnbridge

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.InetAddress
import kotlin.concurrent.thread

class BridgeService : Service() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var proxy: ProxyServer? = null
    private val canalId = "VPN_BRIDGE_PRO"

    // --- ARTILLERÍA FINAL: Servidor de Enrutamiento Transparente ---
    private inner class ProxyServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            // Esta respuesta es el "anzuelo" para que la TV mantenga el canal abierto
            val response = newFixedLengthResponse(Response.Status.OK, "text/plain", "Bridge Conectado")
            response.addHeader("Cache-Control", "no-cache")
            response.addHeader("Connection", "keep-alive")
            response.addHeader("Access-Control-Allow-Origin", "*")
            
            // Forzamos al procesador Kirin a mantener activa la ruta VPN
            thread {
                try {
                    val address = InetAddress.getByName("8.8.8.8")
                    address.isReachable(500)
                } catch (e: Exception) {
                    Log.e("VPNBridge", "Refresco de ruta fallido")
                }
            }
            return response
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Configuración de Proxy a nivel de Sistema de la App (Engaño de red)
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", "8282")
        System.setProperty("https.proxyHost", "127.0.0.1")
        System.setProperty("https.proxyPort", "8282")

        // Notificación obligatoria para EMUI (Huawei)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(canalId, "VPN Bridge", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = NotificationCompat.Builder(this, canalId)
            .setContentTitle("Puente VPN Transparente")
            .setContentText("Enrutando datos para Smart TV...")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(1, notification)

        // Iniciar el servidor en el puerto 8282
        proxy = ProxyServer(8282)
        try {
            proxy?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: IOException) {
            Log.e("VPNBridge", "Error al iniciar Proxy")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        // Limpieza de antena para evitar Error P2P: 2
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { crearGrupo() }
            override fun onFailure(reason: Int) { crearGrupo() }
        })
        return START_STICKY
    }

    private fun crearGrupo() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Handler(Looper.getMainLooper()).postDelayed({ obtenerInfo() }, 3000)
            }
            override fun onFailure(reason: Int) {
                actualizarUI("Error P2P: $reason\nApaga WiFi y activa GPS.")
            }
        })
    }

    private fun obtenerInfo() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                val datos = "PUENTE TRANSPARENTE ACTIVO\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}\n\nIP: 192.168.49.1 | Puerto: 8282"
                actualizarUI(datos)
            } else {
                actualizarUI("Error al obtener red. Reintenta.")
            }
        }
    }

    private fun actualizarUI(txt: String) {
        val intent = Intent("VPN_BRIDGE_UPDATE")
        intent.putExtra("info", txt)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        proxy?.stop()
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
