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
        startForeground(1, crearNotificacion())
    }

    private fun crearNotificacion(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "VPN Bridge", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java) as NotificationManager
            nm.createNotificationChannel(canal)
        }
        return NotificationCompat.Builder(this, canalId)
            .setContentTitle("Puente P40 Pro Activo")
            .setContentText("Forzando enlace de datos...")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()
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
                    obtenerDatos() 
                    forzarRutaVPN()
                }, 3000)
            }
            override fun onFailure(p0: Int) { enviarMsj("Fallo P2P: $p0") }
        })
    }

    private fun forzarRutaVPN() {
        thread {
            try {
                // Forzamos al sistema a crear un socket vinculado a la VPN
                // Esto a veces "abre" el túnel para otras interfaces
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 2000)
                Log.d("VPNBridge", "Enlace VPN forzado con éxito")
                socket.close()
            } catch (e: Exception) {
                Log.e("VPNBridge", "No se pudo forzar el enlace: ${e.message}")
            }
        }
    }

    private fun obtenerDatos() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                val info = "TV CONECTADA A:\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}\n\nSi no hay internet, pasaremos a la fase de Proxy Nativo."
                enviarMsj(info)
            }
        }
    }

    private fun enviarMsj(txt: String) {
        val i = Intent("VPN_BRIDGE_UPDATE").putExtra("info", txt)
        sendBroadcast(i)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }
}
