package com.tuapp.vpnbridge

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BridgeService : Service() {
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null

    override fun onCreate() {
        super.onCreate()
        val canalId = "VPN_BRIDGE_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Servicio VPN", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(canal)
        }
        val notification = NotificationCompat.Builder(this, canalId)
            .setContentTitle("VPN Bridge Corriendo")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                manager?.requestGroupInfo(channel) { group ->
                    if (group != null) {
                        val datos = "RED: ${group.networkName}\nCLAVE: ${group.passphrase}"
                        val broadcast = Intent("VPN_BRIDGE_UPDATE")
                        broadcast.putExtra("info", datos)
                        sendBroadcast(broadcast)
                    }
                }
            }
            override fun onFailure(p0: Int) {}
        })
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? = null
}
