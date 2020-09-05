package moe.misakachan.imhere

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class RebootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == Intent.ACTION_BOOT_COMPLETED) {
            //Check App Setting first
/*
            // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, WalkTrackerService::class.java))
            } else {
                context.startService(Intent(context, WalkTrackerService::class.java))
            }

 */
        }
    }
}