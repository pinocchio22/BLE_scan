package com.example.ble_scan

import android.bluetooth.BluetoothDevice

/**
 * @author CHOI
 * @email vviian.2@gmail.com
 * @created 2022-07-31
 * @desc
 */
data class BleData(var ble : BluetoothDevice, var rssi : Int)
