package com.tuapp.vpnbridge

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class BridgeService : Service() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private val canalId = "VPN_BRIDGE_CHANNEL"

    override fun onCreate() {
        super.onCreate()
        mostrarNotificacion()
    }

    private fun mostrarNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Servicio VPN", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java) as NotificationManager
            nm.createNotificationChannel(canal)
        }

        val notification = NotificationCompat.Builder(this, canalId)
            .setContentTitle("VPN Bridge Activo")
            .setContentText("Emitiendo señal WiFi Direct...")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()

        startForeground(1, notification)
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
                }, 3000)
            }
            override fun onFailure(p0: Int) {
                enviarUpdate("Error al crear grupo: $p0")
            }
        })
    }

    private fun obtenerDatos() {
        manager?.requestGroupInfo(channel) { group ->
            val msg = if (group != null) {
                "CONECTA TU TV A:\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}"
            } else {
                "Error: No se obtuvo clave. ¿GPS activo?"
            }
            enviarUpdate(msg)
        }
    }

    private fun enviarUpdate(txt: String) {
        val i = Intent("VPN_BRIDGE_UPDATE")
        i.putExtra("info", txt)
        sendBroadcast(i)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }
}
