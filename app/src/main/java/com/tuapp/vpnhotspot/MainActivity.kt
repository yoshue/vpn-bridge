package com.tuapp.vpnhotspot
import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            manager.createGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    manager.requestGroupInfo(channel) { group ->
                        group?.let {
                            findViewById<TextView>(R.id.tvSSID).text = "Red: ${it.networkName}"
                            findViewById<TextView>(R.id.tvPass).text = "Clave: ${it.passphrase}"
                        }
                    }
                }
                override fun onFailure(p0: Int) {}
            })
        }
    }
}
