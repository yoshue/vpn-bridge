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
import java.net.InetAddress
import kotlin.concurrent.thread

class BridgeService : Service() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var proxy: ProxyServer? = null
    private val canalId = "VPN_BRIDGE_PRO"

    // --- ARTILLERÍA: Servidor Proxy con Refuerzo de DNS ---
    private inner class ProxyServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            // Cabeceras para mantener el túnel abierto con la Smart TV
            val response = newFixedLengthResponse(Response.Status.OK, "text/plain", "Bridge Conectado")
            response.addHeader("Cache-Control", "no-cache")
            response.addHeader("Connection", "keep-alive")
            response.addHeader("Access-Control-Allow-Origin", "*")
            
            // REFUERZO DE RED: Obligamos al P40 Pro a resolver tráfico externo
            // Esto ayuda a que las apps de la TV no se queden "colgadas"
            thread {
                try {
                    val address = InetAddress.getByName("8.8.8.8")
                    address.isReachable(500) 
                    Log.d("VPNBridge", "DNS Refresh exitoso")
                } catch (e: Exception) {
                    Log.e("VPNBridge", "Refresco fallido: ${e.message}")
                }
            }
            return response
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Iniciar notificación para que el P40 Pro no cierre la app
        startForeground(1, obtenerNotificacion())
        
        // Iniciar el motor de datos en el puerto 8282
        proxy = ProxyServer(8282)
        try {
            proxy?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: IOException) {
            Log.e("VPNBridge", "Error al iniciar Proxy: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        // LIMPIEZA DE ANTENA: Evita el "Error P2P: 2"
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { iniciarGrupo() }
            override fun onFailure(reason: Int) { iniciarGrupo() }
        })
        
        return START_STICKY
    }

    private fun iniciarGrupo() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Espera de 3 segundos para estabilidad en EMUI
                Handler(Looper.getMainLooper()).postDelayed({
                    obtenerInformacion()
                }, 3000)
            }
            override fun onFailure(reason: Int) {
                enviarAMain("Error P2P: $reason\nIMPORTANTE: WiFi APAGADO y GPS ON.")
            }
        })
