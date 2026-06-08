package com.inu.capstone_mobile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.models.Prescription
import com.inu.capstone_mobile.data.models.User
import com.inu.capstone_mobile.data.repository.CabinetRepository
import com.inu.capstone_mobile.data.repository.MedRepository
import com.inu.capstone_mobile.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


private const val MAX_ADMIN_CAB_SLOT = 6
class MedManageViewModel : ViewModel() {

    init {
        viewModelScope.launch {
            if (!DebugConfig.isTesting) {
                UserRepository.syncAllUsersFromServer("USER")
                CabinetRepository.syncAdminCabinetsFromServer()
            }
        }
    }

    // 1. 환자 목록 (USER 역할만)
    val patients: StateFlow<List<User>> = UserRepository.allUsers
        .map { users -> users.filter { it.userRoleId == "USER" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedPatientId = MutableStateFlow<String?>(null)
    val selectedPatientId: StateFlow<String?> = _selectedPatientId.asStateFlow()

    // 2. 선택 환자의 처방전 - 서버에서 직접 조회
    private val _selectedPatientPrescriptions = MutableStateFlow<List<Prescription>>(emptyList())
    val selectedPatientPrescriptions: StateFlow<List<Prescription>> = _selectedPatientPrescriptions.asStateFlow()

    fun selectPatient(userId: String) {
        _selectedPatientId.value = userId

        viewModelScope.launch {
            if (DebugConfig.isTesting) {
                // 테스트 모드: allUsers 에서 읽음 (mock 데이터)
                val patient = UserRepository.allUsers.value.find { it.userId == userId }
                _selectedPatientPrescriptions.value = patient?.prescriptions ?: emptyList()
            } else {
                // 실전 모드: API 직접 호출
                try {
                    val prescriptions = UserRepository.fetchPrescriptionsForUser(userId)
                    _selectedPatientPrescriptions.value = prescriptions
                } catch (e: Exception) {
                    Log.e("MedManageViewModel", "처방전 조회 오류: ${e.message}")
                    _selectedPatientPrescriptions.value = emptyList()
                }
            }
        }
    }

    // 3. 관리자 보관함 상태 (기존 AdminCabinet + MedicineSlot 모델 활용)
    val cabinets = combine(
        CabinetRepository.adminCabinets,
        MedRepository.medicineSlots
    ) { adminList, medSlots ->
        (0 until MAX_ADMIN_CAB_SLOT).map { index ->
            // 여기에 정의된 모델들이 기존 모델들입니다.
            val medSlot = medSlots.find { it.slotId == index }
            val adminCabinet = adminList.find { it.slotIndex == index }

            // 기존 모델들을 조합한 결과만 UI로 전달
            Pair(adminCabinet, medSlot)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 5. 관리자 보관함 활성/비활성 제어 - 현재 상태 판단은 ViewModel이 담당
    fun handleCabinetActiveAction(slotIndex: Int) {
        val target = cabinets.value.getOrNull(slotIndex)?.first ?: return
        val nextActive = !target.isActive

        viewModelScope.launch {
            val success = CabinetRepository.updateAdminCabinetActive(slotIndex, nextActive)
            if (success) {
                Log.d("MedManageViewModel", "관리자 보관함 $slotIndex active=$nextActive 전송 성공")
            } else {
                Log.e("MedManageViewModel", "관리자 보관함 $slotIndex active=$nextActive 전송 실패")
            }
        }
    }

    fun handleCabinetMedicineRemoveAction(slotIndex: Int) {
        val medicineId = cabinets.value.getOrNull(slotIndex)?.second?.medicine?.id ?: return

        viewModelScope.launch {
            val success = MedRepository.removeMedicineFromSlot(medicineId)
            if (success) {
                CabinetRepository.syncAdminCabinetsFromServer()
                Log.d("MedManageViewModel", "관리자 보관함 $slotIndex 약품 해제 성공")
            } else {
                Log.e("MedManageViewModel", "관리자 보관함 $slotIndex 약품 해제 실패")
            }
        }
    }
}

