package com.tuapp.vpnbridge

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.IBinder
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
            val canal = NotificationChannel(
                canalId, 
                "Servicio de Puente VPN", 
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(canal)
        }

        val notification = NotificationCompat.Builder(this, canalId)
            .setContentTitle("VPN Bridge Activo")
            .setContentText("Emitiendo señal WiFi Direct...")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        // Limpiamos cualquier grupo previo antes de iniciar para evitar conflictos
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { iniciarCreacionDeGrupo() }
            override fun onFailure(reason: Int) { iniciarCreacionDeGrupo() }
        })
        
        return START_STICKY
    }

    private fun iniciarCreacionDeGrupo() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("VPNBridge", "Grupo P2P Creado. Esperando datos...")
                
                // Damos 3 segundos al sistema para que genere el nombre y la clave
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    obtenerInformacionDeRed()
                }, 3000)
            }

            override fun onFailure(reason: Int) {
                enviarEstadoAMainActivity("Error al crear grupo: $reason\nVerifica WiFi y GPS.")
            }
        })
    }

    private fun obtenerInformacionDeRed() {
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                val datos = "CONECTA TU TV A:\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}"
                enviarEstadoAMainActivity(datos)
            } else {
                enviarEstadoAMainActivity("No se pudo obtener la clave.\nReintenta con el GPS activo.")
            }
        }
    }

    private fun enviarEstadoAMainActivity(mensaje: String) {
        val broadcast = Intent("VPN_BRIDGE_UPDATE")
        broadcast.putExtra("info", mensaje)
        sendBroadcast(broadcast)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Detenemos la red WiFi Direct al cerrar el servicio
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d("VPNBridge", "Red P2P cerrada.") }
            override fun onFailure(p0: Int) {}
        })
        super.onDestroy()
    }
}
