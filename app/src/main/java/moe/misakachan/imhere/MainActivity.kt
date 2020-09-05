package moe.misakachan.imhere

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    override fun onBackPressed() {
        val alBuilder = AlertDialog.Builder(this);
        alBuilder.setMessage("종료하시겠습니까?");
        // "예" 버튼을 누르면 실행되는 리스너
        alBuilder.setPositiveButton("예") { _, _ ->
            finish()
        }
        // "아니오" 버튼을 누르면 실행되는 리스너
        alBuilder.setNegativeButton("아니오") { _, _ -> null }
        alBuilder.setTitle("프로그램 종료")
        alBuilder.show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }


        if (Build.VERSION.SDK_INT >= 26) {
            val channelId = "Imhere_Service_channel"
            val channel = NotificationChannel(channelId, "I'm here Channel", NotificationManager.IMPORTANCE_DEFAULT)
            if (!getSharedPreferences("SETTING", Context.MODE_PRIVATE).getBoolean("isNotificationCreated", false)
            ) {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
                getSharedPreferences("SETTING", Context.MODE_PRIVATE).edit()
                    .putBoolean("isNotificationCreated", true).apply()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1){
            
        }
    }
}