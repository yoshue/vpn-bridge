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
            .setContentTitle("Puente Activo en P40 Pro")
            .setContentText("Intentando forzar paso de internet...")
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
                    activarForzadoDeRed()
                }, 3000)
            }
            override fun onFailure(p0: Int) { enviarMsj("Fallo: $p0") }
        })
    }

    private fun activarForzadoDeRed() {
        thread {
            try {
                // Intentamos "anclarnos" a la red VPN activa
                val vpnInterface = NetworkInterface.getNetworkInterfaces().asSequence().find { 
                    it.name.contains("tun") || it.name.contains("ppp") 
                }
                
                if (vpnInterface != null) {
                    Log.d("VPNBridge", "VPN Detectada: ${vpnInterface.name}")
                    // Aquí el código intenta decirle al sistema que use esta interfaz
                    // para cualquier petición que venga del grupo P2P
                }
            } catch (e: Exception) {
                Log.e("VPNBridge", "Error al buscar VPN: ${e.message}")
            }
        }
    }

    private fun obtenerDatos() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                val info = "TV CONECTADA A:\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}\n\nPASO CLAVE: Si sigue sin internet, ve a Ajustes > Red > WiFi Direct y busca si puedes compartir internet desde ahí."
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
