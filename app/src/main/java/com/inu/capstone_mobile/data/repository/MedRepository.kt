package com.inu.capstone_mobile.data.repository

import android.util.Log
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.models.Medicine
import com.inu.capstone_mobile.data.models.MedicineSlot
import com.inu.capstone_mobile.data.remote.http.MedicineNameDto
import com.inu.capstone_mobile.data.remote.http.RetrofitClient
import com.inu.capstone_mobile.data.source.MockDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object MedRepository {
    private val api = RetrofitClient.apiService

    //  [데이터 보관] 초기값은 MockDataSource(창고)에서 가져옵니다.
    // 나중에 약품 DB나 처방전 서버를 연동할 때 이 부분을 수정하게 됩니다.
    private val _medicineSlots = MutableStateFlow<List<MedicineSlot>>(emptyList())

    // 외부 노출용
    val medicineSlots: StateFlow<List<MedicineSlot>> = _medicineSlots.asStateFlow()

    // 2. 클래스 생성 시 초기 데이터 로딩 실행
    init {
        loadInitialData()
    }

    // 💡 3. 핵심 분기 로직: DebugConfig.isTesting 값에 따라 약품 목록 로드
    private fun loadInitialData() {
        if (DebugConfig.isTesting) {
            // [테스트 모드] Mock 데이터로 약품 목록 세팅
            _medicineSlots.value = MockDataSource.DEBUG_MEDICINE_SLOTS
        } else {
            // [실전 모드] 로그인 전에는 사용자 정보가 없으므로 빈 상태로 시작합니다.
            _medicineSlots.value = emptyList()
        }
    }

    suspend fun loadInitialData(userId: String) {
        if (DebugConfig.isTesting) {
            loadInitialData()
            return
        }

        Log.d("MedRepository", "관리자 물리 슬롯 초기화: requestedBy=$userId")
        _medicineSlots.value = defaultAdminMedicineSlots()
        syncMedicineSlotsFromServer()
    }

    suspend fun refreshMedicineSlots() {
        if (DebugConfig.isTesting) {
            loadInitialData()
            return
        }

        syncMedicineSlotsFromServer()
    }

    private suspend fun syncMedicineSlotsFromServer() {
        try {
            val response = api.getMedicines()
            if (response.isSuccessful && response.body()?.success == true) {
                val medicinesById = response.body()?.medicines
                    .orEmpty()
                    .associateBy(MedicineNameDto::medicine_id)

                _medicineSlots.update { current ->
                    current.map { slot ->
                        val serverMedicine = medicinesById[slot.medicine.id]
                        if (serverMedicine != null && serverMedicine.item_name.isNotBlank()) {
                            slot.copy(medicine = slot.medicine.copy(name = serverMedicine.item_name.trim()))
                        } else {
                            slot
                        }
                    }
                }
            } else {
                Log.w(
                    "MedRepository",
                    "약품 목록 조회 실패: code=${response.code()}, message=${response.message()} - 기본값 유지"
                )
            }
        } catch (e: Exception) {
            Log.w("MedRepository", "약품 목록 조회 오류: ${e.message} - 기본값 유지")
        }
    }

    private fun defaultAdminMedicineSlots(): List<MedicineSlot> {
        // 처방전과 분리된 관리자 물리 슬롯(0~5) 기본값
        return (0 until 6).map { idx ->
            MedicineSlot(
                slotId = idx,
                medicine = Medicine(id = idx + 1, name = "약품 #${idx + 1}"),
                userId = "admin"
            )
        }
    }

    /**
     * [기능 1] 기존 약품의 이름을 변경(수정)합니다.
     * UI에서 '약품 수정' 요청이 들어왔을 때 뷰모델이 이 함수를 호출합니다.
     */
    fun registerMedicine(medicineId: Int, newName: String) {
        _medicineSlots.update { currentList ->
            currentList.map { slot ->
                if (slot.medicine.id == medicineId) {
                    slot.copy(medicine = slot.medicine.copy(name = newName))
                } else slot
            }
        }
    }

    /**
     * [기능 2] 새로운 약품을 특정 슬롯에 추가/배치합니다.
     * UI에서 '새 약품 등록' 요청이 들어왔을 때 호출됩니다.
     */
    fun addMedicineSlot(slotId: Int, medicine: Medicine, userId: String) {
        _medicineSlots.update { currentList ->
            currentList + MedicineSlot(slotId, medicine, userId) //TODO: 이 함수 활용되지 않은 이유 확인
        }
    }

    suspend fun removeMedicineFromSlot(medicineId: Int): Boolean {
        return try {
            val response = api.updateMedicine(
                com.inu.capstone_mobile.data.remote.http.MedicineUpdateRequest(
                    medicine_id = medicineId,
                    item_seq = null
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                refreshMedicineSlots()
                true
            } else {
                Log.e("MedRepository", "약품 해제 실패: code=${response.code()}, message=${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("MedRepository", "약품 해제 오류: ${e.message}", e)
            false
        }
    }

    /**
     * 테스트용 : Mock 데이터를 강제로 초기 상태로 되돌릴 때 사용합니다.
     */
    fun reloadData() {
        loadInitialData()
    }
}