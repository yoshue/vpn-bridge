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
        try {
            mostrarNotificacion()
        } catch (e: Exception) {
            Log.e("VPNBridge", "Error en onCreate: ${e.message}")
        }
    }

    private fun mostrarNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Servicio VPN", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java) as NotificationManager
            nm.createNotificationChannel(canal)
        }

        val notification = NotificationCompat.Builder(this, canalId)
            .setContentTitle("Puente Activo")
            .setContentText("Enrutando tráfico...")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        // Reiniciamos el grupo de forma segura
        try {
            manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { crearNuevoGrupo() }
                override fun onFailure(p0: Int) { crearNuevoGrupo() }
            })
        } catch (e: Exception) {
            crearNuevoGrupo()
        }
        
        return START_STICKY
    }

    private fun crearNuevoGrupo() {
        try {
            manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Handler(Looper.getMainLooper()).postDelayed({ obtenerInfo() }, 2000)
                    // Iniciamos el enrutado en un hilo separado para no bloquear la app
                    thread { motorDeRed() }
                }
                override fun onFailure(p0: Int) { enviarMsj("Error P2P: $p0") }
            })
        } catch (e: Exception) {
            enviarMsj("Crash prevent: ${e.message}")
        }
    }

    private fun motorDeRed() {
        // Operación mínima para no causar crash en el P40
        try {
            val dummy = DatagramSocket()
            dummy.connect(InetAddress.getByName("8.8.8.8"), 53)
            dummy.close()
        } catch (e: Exception) {
            Log.e("VPNBridge", "Motor de red en espera")
        }
    }

    private fun obtenerInfo() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                enviarMsj("TV CONECTADA A:\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}")
            }
        }
    }

    private fun enviarMsj(txt: String) {
        val i = Intent("VPN_BRIDGE_UPDATE").putExtra("info", txt)
        sendBroadcast(i)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            manager?.removeGroup(channel, null)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
