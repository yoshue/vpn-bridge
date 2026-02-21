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

    // El servidor que moverá los datos de la VPN a la TV
    private inner class ProxyServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            // Este es el túnel: recibe petición y responde usando la red del móvil (VPN)
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Bridge OK")
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, notification())
        
        // Iniciamos el motor en el puerto 8282
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
        
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { crearGrupo() }
            override fun onFailure(p0: Int) { crearGrupo() }
        })
        return START_STICKY
    }

    private fun crearGrupo() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Handler(Looper.getMainLooper()).postDelayed({ 
                    actualizarUI() 
                }, 3000)
            }
            override fun onFailure(p0: Int) { 
                enviarBroadcast("Error P2P: $p0") 
            }
        })
    }

    private fun actualizarUI() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                val info = "MODO PRO ACTIVO\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}\n\nPuerto: 8282"
                enviarBroadcast(info)
            }
        }
    }

    private fun enviarBroadcast(msg: String) {
        val i = Intent("VPN_BRIDGE_UPDATE").putExtra("info", msg)
        sendBroadcast(i)
    }

    override fun onDestroy() {
        proxy?.stop()
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun notification(): Notification {
        val canalId = "BRIDGE_CH"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(canalId, "VPN", NotificationManager.IMPORTANCE_LOW))
        }
        return NotificationCompat.Builder(this, canalId)
            .setContentTitle("Puente Pro")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()
    }
}
