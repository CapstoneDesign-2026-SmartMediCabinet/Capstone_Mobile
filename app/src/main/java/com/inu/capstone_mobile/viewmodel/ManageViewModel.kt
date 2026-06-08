package com.inu.capstone_mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.remote.bluetooth.BluetoothManager
import com.inu.capstone_mobile.data.repository.CabinetRepository
import com.inu.capstone_mobile.data.repository.MedRepository
import com.inu.capstone_mobile.data.repository.UserRepository
import kotlinx.coroutines.launch

class ManageViewModel : ViewModel() {

    init {
        viewModelScope.launch {
            if (!DebugConfig.isTesting) {
                CabinetRepository.syncAdminCabinetsFromServer()
            }
        }
    }

    val currentUser = UserRepository.currentUser
    val allUsers = UserRepository.allUsers

    val temp = CabinetRepository.temp
    val humid = CabinetRepository.humid
    val isFanOn = CabinetRepository.isFanOn
    val userCabinets = CabinetRepository.userCabinets
    val adminCabinets = CabinetRepository.adminCabinets
    val isAdminDoorLocked = CabinetRepository.isAdminDoorLocked

    val medicineSlots = MedRepository.medicineSlots

    fun toggleUserCabinetLockWithBT(userId: String, btManager: BluetoothManager) {
        val cabinet = userCabinets.value.find { it.userId == userId }
        val next = !(cabinet?.isLocked ?: true)
        val statusChar = if (next) "1" else "0"
        val deviceIndex = CabinetRepository.getDeviceIndexByUserId(userId) ?: return

        btManager.sendData("L:$deviceIndex:$statusChar")
        CabinetRepository.toggleUserCabinetLock(userId)
    }

    fun toggleAdminDoorWithBT(btManager: BluetoothManager) {
        val next = !(isAdminDoorLocked.value ?: true)
        val statusChar = if (next) "1" else "0"

        btManager.sendData("L:0:$statusChar")
        CabinetRepository.toggleAdminDoor()
    }
}// class ManageViewModel



