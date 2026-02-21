override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager?.initialize(this, mainLooper, null)

        // Eliminamos grupos viejos antes de crear uno nuevo para evitar errores
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { crearNuevoGrupo() }
            override fun onFailure(p0: Int) { crearNuevoGrupo() }
        })
        
        return START_STICKY
    }

    private fun crearNuevoGrupo() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Esperamos 2 segundos a que la red se estabilice para pedir la clave
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    manager?.requestGroupInfo(channel) { group ->
                        val datos = if (group != null) {
                            "CONECTA TU TV A:\n\nRED: ${group.networkName}\nCLAVE: ${group.passphrase}"
                        } else {
                            "Error: No se pudo obtener la clave.\nVerifica que el GPS esté ON."
                        }
                        val broadcast = Intent("VPN_BRIDGE_UPDATE")
                        broadcast.putExtra("info", datos)
                        sendBroadcast(broadcast)
                    }
                }, 2000)
            }
            override fun onFailure(p0: Int) {
                val broadcast = Intent("VPN_BRIDGE_UPDATE")
                broadcast.putExtra("info", "Fallo al crear grupo. Error: $p0")
                sendBroadcast(broadcast)
            }
        })
    }
