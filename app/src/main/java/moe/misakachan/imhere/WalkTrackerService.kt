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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import moe.misakachan.imhere.Util.ACTION_DATA_AVAILABLE
import moe.misakachan.imhere.Util.ACTION_GATT_CONNECTED
import moe.misakachan.imhere.Util.ACTION_GATT_DISCONNECTED
import moe.misakachan.imhere.Util.ACTION_GATT_SERVICES_DISCOVERED
import moe.misakachan.imhere.Util.makeGattUpdateIntentFilter
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter
import java.util.*
import kotlin.collections.ArrayList

class WalkTrackerService : Service(), LocationListener {

    companion object {
        // SPP UUID service - this should work for most devices
        private val BTMODULEUUID: UUID = UUID.fromString("00000000-d161-11ea-87d0-0242ac130003")
        private val CHARUUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
        private val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    }

    private val mBinder: IBinder = WalkTrackerServiceBinder()
    private var connected: Boolean = false
    private var isScanning = false
    private val btAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val btManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val highAccuracyLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation
            Log.d(
                "Location Service",
                "High-Accuracy location update lat: ${location.latitude}, lng: ${location.longitude}, alt: ${location.altitude}, provider:${location.provider}"
            )
        }
    }

    private val lowAccuracyLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation
            Log.d("Location Service", "Low-Accuracy location update $location")
        }
    }

    private val leScanCallback = BLEScanCallback()
    private val gattCallback: BluetoothGattCallback = BLEGattCallback()

    private val connectedDeviceList = ArrayList<BluetoothGatt>()

    private var bluetoothGatt: BluetoothGatt? = null

    private val bluetoothScannerTimer = Timer()
    private val scanTimerTask = object : TimerTask() {
        override fun run() {
            if (isScanning) {
                bluetoothLeScanner.stopScan(leScanCallback)
                isScanning = false
            }
            startScan()
        }
    }

    private val readCharacteristicTimer = Timer()
    private val readCharacteristicTimerTask = object : TimerTask() {
        override fun run() {
            for (gatt in connectedDeviceList) {
                gatt.readCharacteristic(gatt.services[2].getCharacteristic(CHARUUID))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BT SERVICE", "SERVICE CREATED")
        startForegroundService()
        //registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BT SERVICE", "SERVICE STARTED")
        //bluetoothScannerTimer.schedule(scanTimerTask, 0, 30000)
        //readCharacteristicTimer.schedule(readCharacteristicTimerTask, 0, 1000)
        //startBeacon()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SERVICE", "onDestroy")
        //bluetoothScannerTimer.cancel()
        //unregisterReceiver(gattUpdateReceiver)
    }

    private fun startForegroundService() {
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
        builder.setSmallIcon(R.drawable.baseline_gps_fixed_black_36)
            .setContentTitle("위치 사용 중")
            .setContentText("위치 및 차량 감지 중입니다.")
            .setContentIntent(pendingIntent)
        startForeground(1, builder.build())
        requestLocationUpdates()
    }

    private fun startBeacon() {
        val beacon = Beacon.Builder()
            .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")  // uuid for beacon
            .setId2("1")  // major
            .setId3("1")  // minor
             // Radius Networks. 0x0118 : Change this for other beacon layouts // 0x004C : for iPhone
            .setTxPower(-100)  // Power in dB
            .setDataFields(listOf(0L))  // Remove this for beacon layouts without d: fields
            .build()

        val beaconParser = BeaconParser()
            .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")
        val beaconTransmitter = BeaconTransmitter(applicationContext, beaconParser)

        beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
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
        request.interval = 3000
        request.fastestInterval = 1000
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, highAccuracyLocationCallback, null)
        }
    }

    private fun startScan() {
        val filter = arrayListOf<ScanFilter>(
            ScanFilter.Builder().setServiceUuid(
                ParcelUuid(
                    BTMODULEUUID
                ), ParcelUuid.fromString("00000000-1111-1111-1111-111111111111")
            ).build()
        )
        val setting = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()
        bluetoothLeScanner.startScan(filter, setting, leScanCallback)
        isScanning = true
    }

    inner class BLEScanCallback : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("MisakaMOE", "ERROR: $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                Log.d(
                    "MisakaMOE",
                    "device : ${result.device.name}, bondstate : ${result.device.bondState}"
                )
                Log.d("MisakaMOE", "RSSI : ${result.rssi}")
                Log.d("MisakaMOE", "Scan data size: ${result.scanRecord?.bytes?.size}")
                Log.d(
                    "MisakaMOE",
                    "Manufacturer Data : ${result.scanRecord?.manufacturerSpecificData to Int.toString()}"
                )
                when (btManager.getConnectionState(result.device, BluetoothProfile.GATT)) {
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        bluetoothGatt =
                            result.device.connectGatt(applicationContext, false, gattCallback)
                    }
                }
            } else {
                Log.d("MisakaMOE", "Result was null")
            }

        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

    }

    inner class BLEGattCallback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status == BluetoothGatt.GATT_FAILURE) {
                //disconnect it
                Log.d("MisakaMOE", "Failed to connect")
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    //Connected to car
                    Log.d("MisakaMOE", "Connected to device")
                    val intentAction = ACTION_GATT_CONNECTED
                    broadcastUpdate(intentAction)
                    gatt?.discoverServices()
                    //Log.d("MisakaMOE", "Characteristics value by int: ${gatt!!.services[0].characteristics[0].getIntValue(0,0)}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    //Handle disconnect
                    connectedDeviceList.remove(gatt!!)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.d("MisakaMOE", "OnServiceReceived")
                    connectedDeviceList.add(gatt!!)
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                }
                else -> Log.w("MisakaMOE", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            when (status) {
                0 -> {
                    val data = characteristic!!.getStringValue(0)
                    Log.d("MisakaMOE", "OnCharacteristicRead: ${data}")
                    //split it by comma
                    val parseData = data.split(',')
                    characteristic.setValue("test2")
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    gatt!!.writeCharacteristic(characteristic)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d("MisakaMOE", "onCharacteristicChanged: ${characteristic?.value}")
            if (characteristic != null) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        private fun broadcastUpdate(action: String) {
            val intent = Intent(action)
            sendBroadcast(intent)
        }

        private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
            val intent = Intent(action)
            //Data
            // parsing is carried out as per profile specifications.
            when (characteristic.uuid) {
                BTMODULEUUID -> {
                    Log.d("MisakaMOE", "Characteristic format FLOAT.")
                    val speed =
                        characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0)
                    Log.d("MisakaMOE", "Received speed: $speed")
                    intent.putExtra("extra", (speed).toString())
                }
            }
            sendBroadcast(intent)
        }
    }

    private val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_GATT_CONNECTED -> {
                    connected = true
                    Log.d("MisakaMOE", "GATT Connected@Receiver")
                    //(context as? Activity)?.invalidateOptionsMenu()
                }
                ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    Log.d("MisakaMOE", "GATT Disconnected@Receiver")
                    //(context as? Activity)?.invalidateOptionsMenu()
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the
                    // user interface.
                    //We should find 3 characteristics for receive GPS lat/lng/speed
                    /*
                    Log.d("MisakaMOE", "list of services : ${getSupportedGattServices()}")
                    if(bluetoothGatt!!.getService(BTMODULEUUID)!=null)
                    {
                        val speedCharacteristic = bluetoothGatt!!.getService(BTMODULEUUID).getCharacteristic(BTMODULEUUID)
                        bluetoothGatt!!.setCharacteristicNotification(speedCharacteristic,true)
                        val gattDescriptor = speedCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        gattDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        bluetoothGatt!!.writeDescriptor(gattDescriptor)
                    }
                    */
                }
                ACTION_DATA_AVAILABLE -> {
                    Log.d("MisakaMOE", "Get DATA: ${intent.getStringExtra("extra")}")
                }
            }
        }
    }

    private fun getSupportedGattServices(): MutableList<BluetoothGattService>? {
        return if (bluetoothGatt != null)
            bluetoothGatt!!.services
        else null
    }

    override fun onLocationChanged(location: Location?) {

    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }

    inner class WalkTrackerServiceBinder : Binder() {
        fun getService() = this@WalkTrackerService
    }
}