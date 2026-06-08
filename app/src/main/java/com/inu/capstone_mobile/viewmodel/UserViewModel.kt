package com.inu.capstone_mobile.viewmodel

import androidx.lifecycle.ViewModel
import com.inu.capstone_mobile.data.remote.bluetooth.BluetoothManager
import com.inu.capstone_mobile.data.repository.CabinetRepository
import com.inu.capstone_mobile.data.repository.MedRepository
import com.inu.capstone_mobile.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserViewModel : ViewModel() {

    val currentUser = UserRepository.currentUser
    val allUsers = UserRepository.allUsers
    val userCabinets = CabinetRepository.userCabinets
    val medicineSlots = MedRepository.medicineSlots

    private val _currentTimeSlot = MutableStateFlow("점심")
    val currentTimeSlot = _currentTimeSlot.asStateFlow()

    private var autoUnlocked = false

    fun autoUnlockMyCabinet(btManager: BluetoothManager) {
        if (autoUnlocked) return
        val user = currentUser.value ?: return
        if (user.userRoleId != "USER") return

        autoUnlocked = CabinetRepository.sendUnlockCommandForUser(btManager, user.userId)
    }

    fun toggleUserCabinetLockWithBT(userId: String, btManager: BluetoothManager) {
        val user = currentUser.value ?: return
        if (user.userRoleId != "USER") return
        if (user.userId != userId) return

        val cabinet = userCabinets.value.find { it.userId == userId } ?: return
        val next = !(cabinet.isLocked ?: true)
        val statusChar = if (next) "1" else "0"
        val deviceIndex = CabinetRepository.getDeviceIndexByUserId(userId) ?: return

        btManager.sendData("L:$deviceIndex:$statusChar")
        CabinetRepository.toggleUserCabinetLock(userId)
    }
}
