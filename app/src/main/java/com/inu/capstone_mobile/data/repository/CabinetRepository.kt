package com.inu.capstone_mobile.data.repository

import android.util.Log
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.models.*
import com.inu.capstone_mobile.data.remote.bluetooth.BluetoothManager
import com.inu.capstone_mobile.data.remote.http.AdminCabinetActiveRequest
import com.inu.capstone_mobile.data.remote.http.CabinetUpdateRequest
import com.inu.capstone_mobile.data.remote.http.AdminCabinetSyncDto
import com.inu.capstone_mobile.data.remote.http.DeviceSyncUploadRequest
import com.inu.capstone_mobile.data.remote.http.UserCabinetSyncDto
import com.inu.capstone_mobile.data.remote.http.RetrofitClient
import com.inu.capstone_mobile.data.source.MockDataSource
import kotlinx.coroutines.flow.*

object CabinetRepository {
    private val api = RetrofitClient.apiService

    // 상태 관리 변수
    private val _userCabinets = MutableStateFlow<List<UserCabinet>>(emptyList())
    private val _adminCabinets = MutableStateFlow<List<AdminCabinet>>(emptyList())
    private val _isAdminDoorLocked = MutableStateFlow<Boolean?>(null)
    private val _deviceIndexByUserId = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _userNameByUserId = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _temp = MutableStateFlow<Float?>(null)
    private val _humid = MutableStateFlow<Float?>(null)
    private val _isFanOn = MutableStateFlow<Boolean?>(null)

    private const val MAXCABINET = 6

    // 외부 노출용
    val userCabinets: StateFlow<List<UserCabinet>> = _userCabinets.asStateFlow()
    val adminCabinets: StateFlow<List<AdminCabinet>> = _adminCabinets.asStateFlow()
    val isAdminDoorLocked: StateFlow<Boolean?> = _isAdminDoorLocked.asStateFlow()
    val temp: StateFlow<Float?> = _temp.asStateFlow()
    val humid: StateFlow<Float?> = _humid.asStateFlow()
    val isFanOn: StateFlow<Boolean?> = _isFanOn.asStateFlow()
    val userNameByUserId: StateFlow<Map<String, String>> = _userNameByUserId.asStateFlow()

    init { loadInitialData() }

    private fun loadInitialData() {
        if (DebugConfig.isTesting) {
            _userCabinets.value = MockDataSource.DEBUG_USER_CABINETS
            _adminCabinets.value = MockDataSource.DEBUG_ADMIN_CABINETS
            _isAdminDoorLocked.value = MockDataSource.DEBUG_ADMIN_LOCKED
            _temp.value = MockDataSource.DEBUG_TEMP
            _humid.value = MockDataSource.DEBUG_HUMID
            _isFanOn.value = MockDataSource.DEBUG_FAN_STATUS
        }
    }

    // 서버와 데이터 동기화 (0~5 인덱스 체계 통일)
    suspend fun syncAllUserCabinetsFromServer(): Boolean {
        return try {
            val response = api.getAllCabinets()
            if (response.isSuccessful && response.body()?.success == true) {
                val cabinets = response.body()?.cabinets.orEmpty()
                val existingCabinets = _userCabinets.value

                val mappedCabinets = (0 until MAXCABINET).map { slotIdx ->
                    val dto = cabinets.find { it.slot_index == slotIdx }
                    val fallbackId = "empty_slot_${slotIdx + 1}"
                    val finalUserId = dto?.user_id ?: fallbackId

                    val existing = existingCabinets.find { it.userId == finalUserId }

                    UserCabinet(
                        userId = finalUserId,
                        isActive = (dto?.is_active == 1),
                        slotBitmask = existing?.slotBitmask ?: 0,
                        isLocked = existing?.isLocked ?: true
                    )
                }.toMutableList()

                _userCabinets.value = mappedCabinets
                _deviceIndexByUserId.value = mappedCabinets
                    .mapIndexed { idx, cabinet -> cabinet.userId to (idx + 1) }
                    .toMap()

                val serverNameMap = cabinets.associate { dto ->
                    dto.user_id to (dto.user_name?.takeIf { it.isNotBlank() })
                }

                _userNameByUserId.value = mappedCabinets
                    .mapIndexed { idx, cabinet ->
                        cabinet.userId to (serverNameMap[cabinet.userId] ?: "사용자#${idx + 1}")
                    }
                    .toMap()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("CabinetRepository", "동기화 실패: ${e.message}")
            false
        }
    }

    // 관리자 보관함 서버 동기화 (slot_index, medicine_id, is_active)
    suspend fun syncAdminCabinetsFromServer(): Boolean {
        return try {
            val response = api.getAdminCabinets()
            if (response.isSuccessful && response.body()?.success == true) {
                val dtos = response.body()?.cabinets.orEmpty()
                val existing = _adminCabinets.value

                val updated = (0 until MAXCABINET).map { idx ->
                    val dto = dtos.find { it.slot_index == idx }
                    val prev = existing.find { it.slotIndex == idx }
                    AdminCabinet(
                        slotIndex = idx,
                        totalStatus = prev?.totalStatus ?: totalStatus.EMPTY, // BT 수신값 유지
                        isActive = dto?.is_active == 1
                    )
                }
                _adminCabinets.value = updated
                true
            } else {
                Log.e("CabinetRepository", "관리자 보관함 동기화 실패: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("CabinetRepository", "관리자 보관함 동기화 예외: ${e.message}")
            false
        }
    }

    // 관리자 보관함 활성화/비활성화 서버 전송 후 로컬 상태 갱신
    suspend fun updateAdminCabinetActive(slotIndex: Int, isActive: Boolean): Boolean {
        return try {
            val request = AdminCabinetActiveRequest(slot_index = slotIndex, is_active = isActive)
            val response = api.updateAdminCabinetActive(request)
            if (response.isSuccessful && response.body()?.success == true) {
                _adminCabinets.update { list ->
                    list.map {
                        if (it.slotIndex == slotIndex) it.copy(isActive = isActive) else it
                    }
                }
                true
            } else {
                Log.e("CabinetRepository", "관리자 보관함 active 업데이트 실패: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("CabinetRepository", "관리자 보관함 active 업데이트 예외: ${e.message}")
            false
        }
    }

    // 서버 업데이트 (0~5 인덱스 그대로 전송)
    suspend fun updateUserCabinetAssignment(slotIndex: Int, userId: String?, isActive: Boolean): Boolean {
        return try {
            val request = CabinetUpdateRequest(slot_index = slotIndex, user_id = userId, is_active = isActive)
            val response = api.updateUserCabinetStatus(request)
            if (response.isSuccessful) {
                syncAllUserCabinetsFromServer()
                true
            } else {
                Log.e("CabinetRepository", "업데이트 실패: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("CabinetRepository", "통신 예외: ${e.message}")
            false
        }
    }

    // 블루투스 데이터 처리
    fun processBluetoothData(data: String) {
        val parts = data.split(":")
        if (parts.size < 3) return
        val type = parts[0]
        val index = parts[1].toIntOrNull() ?: 0 // 0~5 인덱스로 들어옴
        val status = parts[2]

        when (type) {
            "T" -> _temp.value = status.toFloatOrNull()
            "H" -> _humid.value = status.toFloatOrNull()
            "F" -> _isFanOn.value = (status == "1")
            "A" -> {
                val stat = when (status) {
                    "0" -> totalStatus.EMPTY
                    "1" -> totalStatus.LOW
                    else -> totalStatus.ENOUGH
                }
                updateAdminCabinetStatus(index, stat)
            }
            "U" -> {
                val bitmask = status.toIntOrNull() ?: 0
                val targetUserId = _deviceIndexByUserId.value.entries.firstOrNull { it.value == index }?.key ?: return
                _userCabinets.update { list -> list.map { if (it.userId == targetUserId) it.copy(slotBitmask = bitmask) else it } }
            }
            "L" -> {
                val isLocked = (status == "1")
                when (index) {
                    0 -> _isAdminDoorLocked.value = isLocked
                    in 1..MAXCABINET -> {
                        val targetUserId = _deviceIndexByUserId.value.entries.firstOrNull { it.value == index }?.key ?: return
                        _userCabinets.update { list -> list.map { if (it.userId == targetUserId) it.copy(isLocked = isLocked) else it } }
                    }
                }
            }
        }
    }

    fun toggleUserCabinetLock(userId: String) {
        _userCabinets.update { list -> list.map { if (it.userId == userId) it.copy(isLocked = it.isLocked?.let { !it }) else it } }
    }

    fun toggleAdminDoor() {
        _isAdminDoorLocked.update { it?.let { !it } }
    }

    fun updateAdminCabinetStatus(slotIndex: Int, status: totalStatus) {
        _adminCabinets.update { list ->
            val existingCabinet = list.find { it.slotIndex == slotIndex }
            if (existingCabinet != null) {
                list.map { if (it.slotIndex == slotIndex) it.copy(totalStatus = status) else it }
            } else {
                list + AdminCabinet(
                    slotIndex = slotIndex,
                    totalStatus = status,
                    isActive = false
                )
            }
        }
    }

    fun reloadData() {
        loadInitialData()
    }

    suspend fun uploadBluetoothSyncSnapshot(): Boolean {
        val request = DeviceSyncUploadRequest(
            temp = _temp.value,
            humid = _humid.value,
            is_fan_on = _isFanOn.value,
            admin_cabinets = _adminCabinets.value.map { cabinet ->
                AdminCabinetSyncDto(
                    slot_index = cabinet.slotIndex,
                    cabinet_status = cabinet.totalStatus.name
                )
            },
            user_cabinets = _userCabinets.value.mapIndexed { index, cabinet ->
                UserCabinetSyncDto(
                    slot_index = index + 1,
                    slot_bitmask = cabinet.slotBitmask
                )
            }
        )

        return runCatching {
            val response = api.uploadDeviceSync(request)
            val success = response.isSuccessful && response.body()?.success == true
            success
        }.getOrElse {
            Log.e("CabinetRepository", "블루투스 동기화 업로드 실패: ${it.message}")
            false
        }
    }

    fun getDeviceIndexByUserId(userId: String): Int? {
        return _deviceIndexByUserId.value[userId]
         ?: _userCabinets.value.indexOfFirst { it.userId == userId }
        .takeIf { it >= 0 }
         ?.plus(1)
         }

    fun applyUserPermission(user: User) {
        if (user.userRoleId == "ADMIN") return

        _userCabinets.update { currentList ->
            currentList.map { cabinet ->
                if (cabinet.userId == user.userId) cabinet.copy(isLocked = false)
                else cabinet.copy(isLocked = true)
            }
        }
    }

    fun lockAllCabinets() {
        _userCabinets.update { currentList ->
            currentList.map { cabinet ->
                if (cabinet.isLocked != null) cabinet.copy(isLocked = true) else cabinet
            }
        }
    }

    fun sendLockAllCabinetsCommand(btManager: BluetoothManager) {
        btManager.sendData("L:0:1")

        val indexes = _deviceIndexByUserId.value.values
            .filter { it in 1..MAXCABINET }
            .distinct()
            .sorted()
            .ifEmpty { (1..MAXCABINET).toList() }

        indexes.forEach { idx ->
            btManager.sendData("L:$idx:1")
        }

        _isAdminDoorLocked.value = true
        lockAllCabinets()
    }

    fun sendUnlockCommandForUser(btManager: BluetoothManager, userId: String): Boolean {
        val deviceIndex = getDeviceIndexByUserId(userId) ?: return false
        btManager.sendData("L:$deviceIndex:0")
        _userCabinets.update { list ->
            list.map { cabinet ->
                if (cabinet.userId == userId) cabinet.copy(isLocked = false) else cabinet
            }
        }
        return true
    }
}