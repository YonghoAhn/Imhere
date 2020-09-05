package moe.misakachan.imhere

import android.Manifest
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import moe.misakachan.imhere.Util.ACTION_DATA_AVAILABLE
import moe.misakachan.imhere.Util.ACTION_GATT_CONNECTED
import moe.misakachan.imhere.Util.ACTION_GATT_DISCONNECTED
import moe.misakachan.imhere.Util.ACTION_GATT_SERVICES_DISCOVERED
import moe.misakachan.imhere.Util.makeGattUpdateIntentFilter
import org.altbeacon.beacon.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class WalkTrackerService : Service(), BeaconConsumer {

    companion object {
        // SPP UUID service - this should work for most devices
        private val BTMODULEUUID: UUID = UUID.fromString("00000000-d161-11ea-87d0-0242ac130003")

        const val ALTBEACON = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"
        const val ALTBEACON2 = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"
    }

    private val mBinder: IBinder = WalkTrackerServiceBinder()

    private val beaconManager by lazy { BeaconManager.getInstanceForApplication(applicationContext) }
    private lateinit var beacon: Beacon

    private val mLatitude = MutableLiveData<Float>()
    private val mLongitude = MutableLiveData<Float>()

    private var major by Delegates.notNull<Long>()
    private var minor by Delegates.notNull<Long>()

    private val timer = Timer()
    private val timerTask = object : TimerTask() {
        override fun run() {
            if (mLatitude.value != null && mLongitude.value != null) {
                db.collection("ids").document("users").collection("user")
                    .whereEqualTo("major", major)
                    .whereEqualTo("minor", minor).get()
                    .addOnSuccessListener {
                        if (it != null) {
                            db.collection("ids").document("users").collection("user")
                                .document(it.documents[0].id).update(
                                    "beforePosition",
                                    it.documents[0].getGeoPoint("currentPosition"),
                                    "currentPosition",
                                    GeoPoint(
                                        mLatitude.value!!.toDouble(),
                                        mLongitude.value!!.toDouble()
                                    ), "connected", true
                                )
                        }
                    }
            }
        }

    }

    private val db = FirebaseFirestore.getInstance()

    private lateinit var client: FusedLocationProviderClient

    private val adapter by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val advertiser: BluetoothLeAdvertiser by lazy { BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser }

    private val highAccuracyLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation
            Log.d(
                "Location Service",
                "High-Accuracy location update lat: ${location.latitude}, lng: ${location.longitude}, alt: ${location.altitude}, provider:${location.provider}"
            )
            mLatitude.postValue(location.latitude.toFloat())
            mLongitude.postValue(location.longitude.toFloat())
        }
    }

    private lateinit var currentAdvertisingSet : AdvertisingSet
    private lateinit var soundPlayer: ImhereSoundPlayer

    @RequiresApi(Build.VERSION_CODES.O)
    fun example2() {
        // Check if all features are supported
        if (!adapter.isLe2MPhySupported) {
            Log.e("MisakaMOE", "2M PHY not supported!")
            return
        }
        if (!adapter.isLeExtendedAdvertisingSupported) {
            Log.e("MisakaMOE", "LE Extended Advertising not supported!")
            return
        }

        val maxDataLength = adapter.leMaximumAdvertisingDataLength

        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_2M)
            .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)

        val data = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(UUID.randomUUID()),
            "You should be able to fit large amounts of data up to maxDataLength. This goes up to 1650 bytes. For legacy advertising this would not work".toByteArray())
            .build()

        val callback = object : AdvertisingSetCallback() {
                override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                    Log.i("MisakaMOE", "onAdvertisingSetStarted(): txPower:$txPower , status: $status")
                    if (advertisingSet != null) {
                        currentAdvertisingSet = advertisingSet
                    }
                }

                override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                    Log.i("MisakaMOE", "onAdvertisingSetStopped():")
                }
            }

        advertiser.startAdvertisingSet(parameters.build(), data, null, null, null, callback)

        // After the set starts, you can modify the data and parameters of currentAdvertisingSet.
        currentAdvertisingSet.setAdvertisingData(AdvertiseData.Builder().addServiceData(ParcelUuid(UUID.randomUUID()),
            "Without disabling the advertiser first, you can set the data, if new data is less than 251 bytes long.".toByteArray()).build())

        // Wait for onAdvertisingDataSet callback...
        // Can also stop and restart the advertising
        currentAdvertisingSet.enableAdvertising(false, 0, 0)
        // Wait for onAdvertisingEnabled callback...
        currentAdvertisingSet.enableAdvertising(true, 0, 0)
        // Wait for onAdvertisingEnabled callback...

        // Or modify the parameters - i.e. lower the tx power
        currentAdvertisingSet.enableAdvertising(false, 0, 0)
        // Wait for onAdvertisingEnabled callback...
        currentAdvertisingSet.setAdvertisingParameters(parameters.setTxPowerLevel
            (AdvertisingSetParameters.TX_POWER_LOW).build())
        // Wait for onAdvertisingParametersUpdated callback...
        currentAdvertisingSet.enableAdvertising(true, 0, 0)
        // Wait for onAdvertisingEnabled callback...

        // When done with the advertising:
        advertiser.stopAdvertisingSet(callback)
    }

    override fun onBeaconServiceConnect() {
        beaconManager.addRangeNotifier { beacons, _ ->
            if(!beacons.isNullOrEmpty())
            {
                soundPlayer.play()
                for(beacon in beacons) {
                    if(beacon.distance <= 12.00f) {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createOneShot(300,100))
                        } else {
                            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(300)
                        }
                    }
                }
            }
        }
        beaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didDetermineStateForRegion(state: Int, p1: Region?) {
                Log.i("MisakaMOE", "I have just switched from seeing/not seeing beacons: $state")
            }

            override fun didEnterRegion(p0: Region?) {
                Log.d("MisakaMOE", "I just saw this beacon id2:${p0?.id2} id3:${p0?.id3} first time")
                //Alert Sound
                soundPlayer.play()
            }

            override fun didExitRegion(p0: Region?) {
                Log.i("MisakaMOE", "I no longer see an beacon id2:${p0?.id2} id3:${p0?.id3}")
            }
        })
        try {
            beaconManager.startMonitoringBeaconsInRegion(Region("beacon", Identifier.parse("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"), null, null))
        } catch (e: RemoteException) {
            Log.e("MisakaMOE", "Error while start monitor")
        }
        try {
            beaconManager.startRangingBeaconsInRegion(Region("beacon", Identifier.parse("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"), null, null))

        } catch (e: RemoteException) {
            Log.e("MisakaMOE", "Error while start ranging")
        }
    }

    var beaconTransmitter: BeaconTransmitter? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("BT SERVICE", "SERVICE CREATED")
        major = applicationContext.getSharedPreferences("imhere", Context.MODE_PRIVATE)
            .getLong("major", -1)
        minor = applicationContext.getSharedPreferences("imhere", Context.MODE_PRIVATE)
            .getLong("minor", -1)
        soundPlayer = ImhereSoundPlayer(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BT SERVICE", "SERVICE STARTED")
        //startBeacon()
        startForegroundService()
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(ALTBEACON))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(ALTBEACON2))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24"))
        beaconManager.bind(this)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SERVICE", "onDestroy")
        db.collection("ids").document("users").collection("user")
            .whereEqualTo("major", major)
            .whereEqualTo("minor", minor).get().addOnSuccessListener {
                db.collection("ids").document("users").collection("user")
                    .document(it.documents[0].id).update("connected",false)
            }
        //beaconTransmitter?.stopAdvertising()
        client.removeLocationUpdates(highAccuracyLocationCallback)
        beaconManager.unbind(this)
        timer.cancel()
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val builder: NotificationCompat.Builder
        builder =
            if (Build.VERSION.SDK_INT >= 26) {
                val channelId = "Imherechannel"
                val channel = NotificationChannel(
                    channelId,
                    "I'm here Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                if (!getSharedPreferences(
                        "Imhere",
                        Context.MODE_PRIVATE
                    ).getBoolean("isNotificationCreated", false)
                ) {
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
                    getSharedPreferences("Imhere", Context.MODE_PRIVATE).edit()
                        .putBoolean("isNotificationCreated", true).apply()
                }
                NotificationCompat.Builder(this, channelId)
            } else {
                NotificationCompat.Builder(this)
            }
        val noti = builder
            .setSmallIcon(R.drawable.baseline_gps_fixed_black_24)
            .setOngoing(true)
            .setContentTitle("위치 사용 중")
            .setContentText("위치 및 차량 감지 중입니다.")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, noti)

        requestLocationUpdates()
        timer.schedule(timerTask, 0, 2000)
    }

    private fun startBeacon() {
        beacon = Beacon.Builder()
            .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")  // uuid for beacon
            .setId2(major.toString())  // major
            .setId3(minor.toString())  // minor
            .setTxPower(-59)  // Power in dB
            .setDataFields(listOf(0L))  // Remove this for beacon layouts without d: fields
            .build()

        beaconManager.beaconParsers.clear()
        val beaconParser =
            BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")
        beaconTransmitter = BeaconTransmitter(applicationContext, beaconParser)
        beaconTransmitter!!.advertiseTxPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
        beaconTransmitter!!.advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
        beaconTransmitter!!.startAdvertising(beacon, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.d("MisakaMOE", "onStartSuccess: ")
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.d("MisakaMOE", "onStartFailure: $errorCode")
            }
        })

    }

    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.interval = 1000
        request.fastestInterval = 500
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        client = LocationServices.getFusedLocationProviderClient(this)

        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            client.requestLocationUpdates(request, highAccuracyLocationCallback, null)
        }
    }

    inner class WalkTrackerServiceBinder : Binder() {
        fun getService() = this@WalkTrackerService
    }
}