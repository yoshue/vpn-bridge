package com.tuapp.vpnbridge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class BridgeService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VPNBridge", "Servicio de Puente Iniciado")
        // Aquí irá la lógica de redirección de tráfico (iptables o Proxy)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("VPNBridge", "Servicio de Puente Detenido")
    }
}
