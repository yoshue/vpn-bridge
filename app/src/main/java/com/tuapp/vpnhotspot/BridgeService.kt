package com.tuapp.vpnbridge

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class BridgeService : Service() {

    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setupWifiP2P()
        return START_STICKY
    }

    private fun setupWifiP2P() {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        // Intentamos crear el grupo (Hotspot P2P)
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("VPNBridge", "Grupo P2P Creado Exitosamente")
                // Aquí el teléfono ya está emitiendo señal
            }

            override fun onFailure(reason: Int) {
                Log.e("VPNBridge", "Error al crear grupo: $reason")
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Al detener el servicio, cerramos el grupo para ahorrar batería
        manager?.removeGroup(channel, null)
        super.onDestroy()
    }
}
