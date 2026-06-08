package com.inu.capstone_mobile.data.remote.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

// ================================ //
// 시연용 실제 단말 MAC : 98:DA:60:07:60:AD : MAIN_MAC
// bibagyte(주현) 보유중 임시 단말 MAC : : SPARE_MAC
//================================ //
@SuppressLint("MissingPermission")
class BluetoothManager(private val adapter: BluetoothAdapter?) {

    private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null

    // 💡 1. [여기에 아두이노의 실제 MAC 주소를 입력하세요!]
    private val MAIN_MAC_ADDRESS = "98:DA:60:07:60:AD"
    private val SPARE_MAC_ADDRESS = "00:00:00:00:00:00"
    private val TARGET_MAC_ADDRESS = MAIN_MAC_ADDRESS
    private var targetDevice: BluetoothDevice? = null

    private val _incomingData = MutableSharedFlow<String>()
    val incomingData = _incomingData.asSharedFlow()

    // 연결 상태를 UI나 ViewModel에서 알 수 있게 상태값 추가
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    // 2. MAC 주소로 강제 연결 시도
    suspend fun connectByMac(): Boolean = withContext(Dispatchers.IO) {
        targetDevice = adapter?.getRemoteDevice(TARGET_MAC_ADDRESS)
        return@withContext connect(targetDevice)
    }

    private suspend fun connect(device: BluetoothDevice?): Boolean = withContext(Dispatchers.IO) {
        if (device == null) return@withContext false
        return@withContext try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)
            adapter?.cancelDiscovery() // 검색 중단 (필수)
            bluetoothSocket?.connect()
            _isConnected.value = true
            Log.d("BT", "연결 성공!")
            true
        } catch (e: IOException) {
            Log.e("BT", "연결 실패: ${e.message}")
            closeSocket()
            false
        }
    }

    // 3. 수신 및 자동 재연결 무한 루프
    suspend fun startListeningWithReconnect() = withContext(Dispatchers.IO) {
        while (true) {
            if (_isConnected.value && bluetoothSocket != null) {
                try {
                    // 데이터 수신 대기
                    val reader = BufferedReader(InputStreamReader(bluetoothSocket?.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { _incomingData.emit(it) }
                    }
                } catch (e: IOException) {
                    // 수신 중 에러 발생 = 통신 끊김!
                    Log.e("BT", "연결 끊김 감지: ${e.message}")
                    closeSocket()
                }
            } else {
                // 끊겨있다면 3초 대기 후 재연결 시도
                Log.d("BT", "재연결 시도 중...")
                delay(3000)
                if (targetDevice != null) {
                    val reconnected = connect(targetDevice)
                    // 재연결 성공 시, 아두이노에 다시 한번 값 달라고 조르기(옵션)
                    if (reconnected) sendData("SYNC")
                }
            }
        }
    }

    fun sendData(message: String) {
        if (!_isConnected.value) return
        try {
            bluetoothSocket?.outputStream?.write((message + "\n").toByteArray())
        } catch (e: IOException) {
            Log.e("BT", "전송 실패 (연결 끊김): ${e.message}")
            closeSocket() // 전송 실패 시에도 끊김으로 간주하고 소켓 닫기
        }
    }

    fun disconnect() {
        closeSocket()
        targetDevice = null // 명시적 종료 시 재연결 루프 방지
    }

    private fun closeSocket() {
        try { bluetoothSocket?.close() } catch (e: IOException) {}
        bluetoothSocket = null
        _isConnected.value = false
    }
}