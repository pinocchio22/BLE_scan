package com.example.ble_scan

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.os.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.pow

/**
 * @author CHOI
 * @email vviian.2@gmail.com
 * @created 2022-08-01
 * @desc
 */

private val TAG = "gattClienCallback"

class BleService : Service() {
    private var devicesArr = ArrayList<BluetoothDevice>()
    var distance = 0.0
    var BleData = ArrayList<BleData>()

    private val SCAN_PERIOD = 1000
    private val handler = Handler()
    private var scanning : Boolean = false

    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var recyclerViewAdapter : MainActivity.RecyclerViewAdapter

    private var bluetoothAdapter: BluetoothAdapter? = null

    var mBinder : BLEBinder = BLEBinder()

    inner class BLEBinder : Binder() {
        fun getService() : BleService {
            // 바인더 반환
            return this@BleService
        }
    }

    override fun onCreate() {
        super.onCreate()

        viewManager = LinearLayoutManager(this)
        recyclerViewAdapter = MainActivity.RecyclerViewAdapter(BleData)

        startForegroundService()
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundService() {
        // 블루투스 스캔
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            // 알림채널 설정
            val mChannel = NotificationChannel("CHANNEL_ID", "CHANNEL_NAME", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(mChannel)
            // 알림 생성
            val notification : Notification = Notification.Builder(this, "CHANNEL_ID")
                .setSmallIcon(R.drawable.bluetooth)
                .setContentTitle("블루투스 스캔")
                .setContentText("스캔중입니다.")
                .build()

            startForeground(1, notification)
            scanDevice(true)
        }
    }

    @SuppressLint("MissingPermission")
    fun scanDevice(state:Boolean) = if(state){
        handler.postDelayed({
            scanning = false
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
        }, SCAN_PERIOD)
        scanning = true
        devicesArr.clear()
        BleData.clear()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(mLeScanCallback)
    }else{
        scanning = false
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        }
    }

    private val mLeScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("scanCallback", "BLE Scan Failed : $errorCode")
        }
        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.let{
                for (result in it){
                    // 거리를 계산하는 올바른 알고리즘 필요
                    distance = 10.0.pow((result.rssi - result.txPower)/20.0)
                    // distance < 1km
                    if (distance < 1000) {
                        if (!devicesArr.contains(result.device) && result.device.name!=null) {
                            devicesArr.add(result.device)
                            BleData.add(BleData(result.device, result.rssi))
                        }
                    }
                }
            }
        }

        @SuppressLint("MissingPermission", "NotifyDataSetChanged")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                // 거리를 계산하는 올바른 알고리즘 필요
                distance = 10.0.pow((result.rssi - result.txPower)/20.0)
                // distance < 1km
                if (distance > 0) {
                    if (!devicesArr.contains(it.device) && it.device.name!=null) {
                        devicesArr.add(it.device)
                        BleData.add(BleData(result.device, result.rssi))
                    }
                    recyclerViewAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    // 스캔중인지 확인
    @SuppressLint("MissingPermission")
    fun isScanning() : Boolean {
        return scanning
    }

    override fun onBind(intent: Intent?): IBinder? {
        // 바인드
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 시작상태, 백그라운드
        return START_STICKY
    }
}

private fun Handler.postDelayed(function: () -> Unit?, scanPeriod: Int) {

}