package moe.misakachan.imhere

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import java.util.*

object Util {
    fun isMyServiceRunning(serviceClass: Class<*>, mActivity: Activity): Boolean {
        val manager: ActivityManager =
            mActivity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.i("Service status", "Running")
                return true
            }
        }
        Log.i("Service status", "Not running")
        return false
    }

    const val ACTION_GATT_SERVICES_DISCOVERED = "moe.misakachan.imhere.ACTION_GATT_SERVICES_DISCOVERED"
    const val ACTION_DATA_AVAILABLE = "moe.misakachan.imhere.ACTION_DATA_AVAILABLE"
    const val ACTION_GATT_CONNECTED = "moe.misakachan.imhere.ACTION_GATT_CONNECTED"
    const val ACTION_GATT_DISCONNECTED = "moe.misakachan.imhere.ACTION_GATT_DISCONNECTED"

    val LATITUDE_GATT_UUID = UUID.fromString("fc7781ea-d3a0-11ea-87d0-0242ac130003")
    val LONGITUDE_GATT_UUID = UUID.fromString("03799ba4-d3a1-11ea-87d0-0242ac130003")
    fun makeGattUpdateIntentFilter(): IntentFilter? {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_GATT_CONNECTED)
        intentFilter.addAction(ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(ACTION_DATA_AVAILABLE)
        return intentFilter
    }
}