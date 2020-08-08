package moe.misakachan.imhere

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import org.altbeacon.beacon.*
import org.altbeacon.beacon.startup.BootstrapNotifier
import org.altbeacon.beacon.startup.RegionBootstrap
import java.util.*


class ImhereApplication : Application(), BootstrapNotifier {

    var regionBootstrap : RegionBootstrap? = null

    override fun onCreate() {
        super.onCreate()
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val builder: NotificationCompat.Builder
        builder = if (Build.VERSION.SDK_INT >= 26) {
            val channelId = "Imhere_Service_channel"
            val channel = NotificationChannel(
                channelId,
                "I'm here Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            if (!getSharedPreferences(
                    "SETTING",
                    Context.MODE_PRIVATE
                ).getBoolean("isNotificationCreated", false)
            ) {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                    channel
                )
                getSharedPreferences("SETTING", Context.MODE_PRIVATE).edit()
                    .putBoolean("isNotificationCreated", true).apply()
            }
            NotificationCompat.Builder(this, channelId)
        } else {
            NotificationCompat.Builder(this)
        }
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("위치 사용 중")
            .setContentText("위치 및 차량 감지 중입니다.")
            .setContentIntent(pendingIntent)
        beaconManager.enableForegroundServiceScanning(builder.build(), 502)
        beaconManager.setEnableScheduledScanJobs(false)
        beaconManager.backgroundBetweenScanPeriod = 0
        beaconManager.backgroundScanPeriod = 1100

        beaconManager.beaconParsers.clear() // clearning all beacon parsers ensures nothing matches
        beaconManager.backgroundBetweenScanPeriod = Long.MAX_VALUE
        beaconManager.backgroundScanPeriod = 0
        beaconManager.foregroundBetweenScanPeriod = Long.MAX_VALUE
        beaconManager.foregroundScanPeriod = 0

        // The following code block activates the foreground service by starting background scanning

        // The following code block activates the foreground service by starting background scanning
        val region = Region(
            "dummy-region",
            Identifier.parse("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"), null, null
        )
        regionBootstrap = RegionBootstrap(this, region)
        val beacon = Beacon.Builder()
            .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
            .setId2("1")
            .setId3("2")
            .setManufacturer(0x0118) // Radius Networks.  Change this for other beacon layouts
            .setTxPower(-59)
            .build()

        // Change the layout below for other beacon types

        // Change the layout below for other beacon types
        val beaconParser = BeaconParser()
            .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24")
        val beaconTransmitter =
            BeaconTransmitter(applicationContext, beaconParser)
        beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                Log.e("MisakaMOE", "Advertisement start failed with code: $errorCode")
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.i("MisakaMOE", "Advertisement start succeeded.")
            }
        })
    }

    override fun didDetermineStateForRegion(p0: Int, p1: Region?) {
        //TODO("Not yet implemented")
        Log.d("MisakaMOE","determineStateForRegion")
    }

    override fun didEnterRegion(p0: Region?) {
        Log.d("MisakaMOe","beacon detected?")
    }

    override fun didExitRegion(p0: Region?) {
        //TODO("Not yet implemented")
        Log.d("MisakaMOE","didExitRegion")

    }
}