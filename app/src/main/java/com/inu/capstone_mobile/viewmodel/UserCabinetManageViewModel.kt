package com.inu.capstone_mobile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.models.User
import com.inu.capstone_mobile.data.repository.CabinetRepository
import com.inu.capstone_mobile.data.repository.UserRepository
import com.inu.capstone_mobile.ui.screens.ManagedCabinetUi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserCabinetManageViewModel : ViewModel() {

    init {
        viewModelScope.launch {
            if (!DebugConfig.isTesting) {
                UserRepository.syncAllUsersFromServer("USER")
                CabinetRepository.syncAllUserCabinetsFromServer()
            }
        }
    }

    val patients: StateFlow<List<User>> = UserRepository.allUsers
        .map { users -> users.filter { it.userRoleId == "USER" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedPatientId = MutableStateFlow<String?>(null)
    val selectedPatientId: StateFlow<String?> = _selectedPatientId.asStateFlow()

    fun selectPatient(userId: String) {
        Log.d("CabinetManage", "환자 클릭됨: $userId")
        _selectedPatientId.value = userId
    }


    val managedCabinets: StateFlow<List<ManagedCabinetUi>> = combine(
        CabinetRepository.userCabinets,
        _selectedPatientId
    ) { cabinets, selectedId ->

        // 현재 배정된 환자 목록 확인 (중복 배정 방지용)
        val assignedUserIds = cabinets
            .filter { it.isActive && !it.userId.startsWith("empty_slot") }
            .map { it.userId }

        cabinets.mapIndexed { index, cabinet ->
            val assignedUserId = if (cabinet.userId.startsWith("empty_slot")) null else cabinet.userId
            val isSelectedAlreadyAssigned = selectedId != null && assignedUserIds.contains(selectedId)

            // 우측 보관함 선택할 때의 행동 분기
            val action = when {
                !cabinet.isActive -> "활성화"
                assignedUserId != null -> {
                    if (selectedId != null && selectedId != assignedUserId) {
                        "불가"
                    } else {
                        "사용자 해제"
                    }
                }
                else -> { // 비어 있는 상태
                    when {
                        selectedId == null -> "비활성화" // 환자 미선택 시 끄기 가능
                        isSelectedAlreadyAssigned -> "불가" // 이미 배정된 환자를 선택한 상태면 중복 등록 방어
                        else -> "사용자 등록"
                    }
                }
            }

            ManagedCabinetUi(
                slotIndex = index,
                cabinetNo = index + 1,
                assignedUserId = assignedUserId,
                isActive = cabinet.isActive,
                availableAction = action // UI로 판단 결과(Action)를 같이 넘겨줌
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun handleCabinetAction(cabinetNo: Int) {
        val cabinet = managedCabinets.value.find { it.cabinetNo == cabinetNo } ?: return
        val action = cabinet.availableAction

        if (action == "불가") return // 비정상적인 접근 원천 차단

        Log.d("CabinetManage", "뷰모델 자체 판단 - 캐비넷 번호: $cabinetNo, 수행할 액션: $action")
        val slotIndex = cabinetNo - 1

        viewModelScope.launch {
            val success = when (action) {
                "활성화" -> CabinetRepository.updateUserCabinetAssignment(slotIndex, null, true)
                "비활성화" -> CabinetRepository.updateUserCabinetAssignment(slotIndex, null, false)
                "사용자 해제" -> CabinetRepository.updateUserCabinetAssignment(slotIndex, null, true)
                "사용자 등록" -> {
                    val targetUserId = _selectedPatientId.value
                    if (targetUserId != null) {
                        val result = CabinetRepository.updateUserCabinetAssignment(slotIndex, targetUserId, true)
                        if (result) _selectedPatientId.value = null // 성공 시 환자 선택 하이라이트 해제
                        result
                    } else false
                }
                else -> false
            }

            if (success) {
                Log.d("CabinetManage", "✅ 서버 업데이트 성공 및 UI 갱신 완료")
            } else {
                Log.e("CabinetManage", "❌ 서버 업데이트 실패")
            }
        }
    }
}