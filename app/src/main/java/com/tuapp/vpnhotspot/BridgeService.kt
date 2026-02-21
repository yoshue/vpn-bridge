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

    // --- ARTILLERÍA: Servidor Proxy para mover los datos ---
    private inner class ProxyServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            // Este servidor responde a la TV usando la conexión activa del móvil (VPN)
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Bridge Conectado")
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, crearNotificacion())
        
        // Iniciamos el motor de datos en el puerto 8282
        proxy = ProxyServer(8282)
        try {
            proxy?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: IOException) {
            Log.e("VPNBridge", "Error al iniciar motor de datos: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        // --- LIMPIEZA DE ANTENA (Para evitar Error P2P: 2) ---
        // Intentamos forzar el cierre de cualquier red previa que bloquee el hardware
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("VPNBridge", "Antena liberada, creando grupo...")
                iniciarCreacionDeRed()
            }
            override fun onFailure(reason: Int) {
                // Si falla (porque no hay grupo previo), intentamos crear la red igualmente
                iniciarCreacionDeRed()
            }
        })
        
        return START_STICKY
    }

    private fun iniciarCreacionDeRed() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Esperamos 3 segundos a que EMUI estabilice la red antes de pedir los datos
                Handler(Looper.getMainLooper()).postDelayed({
                    solicitarDatosDeRed()
                }, 3000)
            }
            override fun onFailure(reason: Int)
