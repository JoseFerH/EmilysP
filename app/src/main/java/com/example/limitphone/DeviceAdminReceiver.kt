package com.example.limitphone

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Administrador del dispositivo habilitado", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Administrador del dispositivo deshabilitado", Toast.LENGTH_SHORT).show()
    }
    
    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Toast.makeText(context, "Modo de bloqueo activado", Toast.LENGTH_SHORT).show()
    }
    
    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Toast.makeText(context, "Modo de bloqueo desactivado", Toast.LENGTH_SHORT).show()
    }
}


