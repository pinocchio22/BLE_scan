package com.example.ble_scan

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ble_scan.databinding.ActivityMainBinding
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding
    private val REQUEST_ALL_PERMISSION = 1
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanning : Boolean = false
    private var devicesArr = ArrayList<BluetoothDevice>()
    private val SCAN_PERIOD = 1000
    private val handler = Handler()
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var recyclerViewAdapter : RecyclerViewAdapter
    private var bleGatt : BluetoothGatt? = null
    private var mContext :Context? = null
    var BleData = ArrayList<BleData>()
    var distance = 0.0
    var power = 0
    var mBleService : BleService ?= null
    val mBleServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mBleService = (service as BleService.BLEBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBleService = null
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
                // results is not null
                for (result in it){
                    // 거리를 계산하는 올바른 알고리즘 필요
                    power = result.txPower
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
                power = result.txPower
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
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun scanDevice(state:Boolean) = if(state){
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

    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }
    // 퍼미션 체크
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ALL_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
                } else {
                    requestPermissions(permissions, REQUEST_ALL_PERMISSION)
                    Toast.makeText(this, "Permissions must be granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        viewManager = LinearLayoutManager(this)
        recyclerViewAdapter =  RecyclerViewAdapter(BleData)
        // 기기연결
        mContext = this
        recyclerViewAdapter.conListener = object : RecyclerViewAdapter.OnItemClickListener {
            override fun onClick(view: View, position: Int) {
                scanDevice(false) // scan 중지
                val device = devicesArr[position]
                bleGatt = ConnectActivity(mContext, bleGatt).connectGatt(device)
            }
        }
        recyclerViewAdapter.disconListener = object  : RecyclerViewAdapter.OnItemClickListener {
            override fun onClick(view: View, position: Int) {
                ConnectActivity(mContext, bleGatt).disconnectGatt()
            }
        }

        binding.recyclerview.apply {
            layoutManager = viewManager
            adapter = recyclerViewAdapter
        }
        binding.search.setOnClickListener { v: View? ->
            if(bluetoothAdapter?.isEnabled==false){
                Toast.makeText(this, "블루투스를 확인하세요!", Toast.LENGTH_SHORT).show()
            } else{
                if (!hasPermissions(this, PERMISSIONS)) {
                    requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
                }
                scanDevice(true)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        // 서비스 실행
        if (mBleService == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, BleService::class.java))
            } else {
                startService(Intent(applicationContext, BleService::class.java))
            }

            // 액티비티와 서비스 바인드
            val intent = Intent(this, BleService::class.java)
            bindService(intent, mBleServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onPause() {
        super.onPause()
        // 액티비티를 떠나면
        if (mBleService != null) {
            // 스캔중이지 않으면 서비스 중단
            if (!mBleService!!.isScanning()) {
                mBleService!!.stopSelf()
            }
            unbindService(mBleServiceConnection)
            mBleService = null
        }
    }

    class RecyclerViewAdapter(private val myDataset: ArrayList<BleData>) :
        RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>() {

        var conListener : OnItemClickListener? = null
        var disconListener : OnItemClickListener? = null

        interface OnItemClickListener{
            fun onClick(view: View, position: Int)
        }

        class MyViewHolder(val constView: ConstraintLayout) : RecyclerView.ViewHolder(constView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val linearView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false) as ConstraintLayout
            return MyViewHolder(linearView)
        }

        @SuppressLint("MissingPermission")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val DeviceName : TextView = holder.constView.findViewById(R.id.device_name)
            val DeviceRssi : TextView = holder.constView.findViewById(R.id.device_rssi)
            val connect_btn : Button = holder.constView.findViewById(R.id.connect)
            val disconnect_btn : Button = holder.constView.findViewById(R.id.disconnect)
            DeviceName.text = myDataset[position].ble.name
            DeviceRssi.text = myDataset[position].rssi.toString()
            if(conListener!=null) {
                connect_btn.setOnClickListener{ v ->
                    conListener?.onClick(v, position)
                }
            }
            if (disconListener!=null) {
                disconnect_btn.setOnClickListener { v ->
                    disconListener?.onClick(v, position)
                }
            }
        }

        override fun getItemCount() = myDataset.size
    }
}

private fun Handler.postDelayed(function: () -> Unit?, scanPeriod: Int) {

}