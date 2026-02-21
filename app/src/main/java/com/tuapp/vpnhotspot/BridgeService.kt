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

class BridgeService : Service() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var proxy: ProxyServer? = null
    private val canalId = "VPN_BRIDGE_PRO"

    // Servidor Proxy Interno
    private inner class ProxyServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Bridge Conectado")
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 1. Iniciar Notificación inmediatamente para evitar cierres
        startForeground(1, obtenerNotificacion())
        
        // 2. Iniciar Servidor de datos
        proxy = ProxyServer(8282)
        try {
            proxy?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: IOException) {
            Log.e("VPNBridge", "Error Proxy: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        // Limpieza profunda para evitar el Error P2P: 2
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { iniciarGrupo() }
            override fun onFailure(reason: Int) { iniciarGrupo() }
        })
        
        return START_STICKY
    }

    private fun iniciarGrupo() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Esperamos a que el sistema asigne la clave
                Handler(Looper.getMainLooper()).postDelayed({
                    obtenerInformacion()
                }, 3000)
            }
            override fun onFailure(reason: Int) {
                enviarAMain("Error P2P: $reason\nRevisa: WiFi APAGADO y GPS activo.")
            }
        })
    }

    private fun obtenerInformacion() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                val datos = "PUENTE NIVEL PRO\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}\n\nPuerto: 8282 | IP: 192.168.49.1"
                enviarAMain(datos)
            } else {
                enviarAMain("Error al leer red.\nReintenta con GPS encendido.")
            }
        }
    }

    private fun enviarAMain(txt: String) {
        val intent = Intent("VPN_BRIDGE_UPDATE")
        intent.putExtra("info", txt)
        sendBroadcast(intent)
    }

    private fun obtenerNotificacion(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(canalId, "VPN Bridge", NotificationManager.IMPORTANCE_LOW))
        }
        return NotificationCompat.Builder(this, canalId)
            .setContentTitle("Puente VPN Activo")
            .setContentText("Enrutando datos para Smart TV...")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()
    }

    override fun onDestroy() {
        proxy?.stop()
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
