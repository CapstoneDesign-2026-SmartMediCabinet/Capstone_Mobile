package com.inu.capstone_mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inu.capstone_mobile.data.models.totalStatus
import com.inu.capstone_mobile.data.remote.bluetooth.BluetoothManager
import com.inu.capstone_mobile.data.repository.CabinetRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 앱 전역 1회 생성. 블루투스 연결 유지와 전역 경고 알림을 담당.
 * 화면 ViewModel들은 이 객체를 통해 BT 명령을 송신한다.
 */
class HardwareViewModel(
    val btManager: BluetoothManager
) : ViewModel() {

    val isBluetoothConnected = btManager.isConnected

    private val _globalAlerts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val globalAlerts = _globalAlerts.asSharedFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true

        viewModelScope.launch {
            btManager.incomingData.collect { raw ->
                CabinetRepository.processBluetoothData(raw)
                CabinetRepository.uploadBluetoothSyncSnapshot()
            }
        }

        viewModelScope.launch {
            if (btManager.connectByMac()) {
                delay(500)
                btManager.sendData("SYNC")
            }
            btManager.startListeningWithReconnect()
        }

        viewModelScope.launch {
            CabinetRepository.isFanOn.collect { on ->
                if (on == true) _globalAlerts.emit("⚠️ 환풍기가 가동 중입니다.")
            }
        }

        viewModelScope.launch {
            CabinetRepository.adminCabinets.collect { cabs ->
                val emptySlots = cabs.filter { it.totalStatus == totalStatus.EMPTY }
                if (emptySlots.isNotEmpty()) {
                    _globalAlerts.emit("⚠️ 관리자 보관함 슬롯이 비어있습니다.")
                }
            }
        }
        viewModelScope.launch {
            while (isActive) { // 뷰모델이 살아있는 동안 무한 반복
                delay(30000L) // 30초 대기
                if (btManager.isConnected.value) {
                    btManager.sendData("SYNC") // 앱이 살아있음을 알리고 센서값을 강제로 받아옴
                    CabinetRepository.uploadBluetoothSyncSnapshot()
                }
            }
        }
    }//fun start

    fun send(command: String) = btManager.sendData(command)

    class Factory(private val btManager: BluetoothManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HardwareViewModel(btManager) as T
        }
    }

    fun forceSyncOrReconnect() {
        viewModelScope.launch {
            if (btManager.isConnected.value) {
                // 이미 연결되어 있다면 상태 갱신만 요청
                btManager.sendData("SYNC")
                CabinetRepository.uploadBluetoothSyncSnapshot()
            } else {
                // 끊겨있다면 재연결 시도
                val connected = btManager.connectByMac()
                if (connected) {
                    delay(500)
                    btManager.sendData("SYNC")
                    btManager.startListeningWithReconnect()
                    CabinetRepository.uploadBluetoothSyncSnapshot()
                } else {
                    _globalAlerts.emit("⚠️ 블루투스 연결에 실패했습니다.")
                }
            }
        }
    }
}// class HardwareViewModel



