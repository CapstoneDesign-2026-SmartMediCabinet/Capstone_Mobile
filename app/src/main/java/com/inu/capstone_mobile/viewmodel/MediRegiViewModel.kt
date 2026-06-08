package com.inu.capstone_mobile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inu.capstone_mobile.data.remote.http.MedicineInfoDto
import com.inu.capstone_mobile.data.remote.http.MedicineRenameRequest
import com.inu.capstone_mobile.data.remote.http.MedicineUpdateRequest
import com.inu.capstone_mobile.data.remote.http.RetrofitClient
import com.inu.capstone_mobile.data.repository.MedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class MediRegiViewModel : ViewModel() {

    val medicineSlots = MedRepository.medicineSlots

    private val _lastRegisteredMedicine = MutableStateFlow<MedicineInfoDto?>(null)
    val lastRegisteredMedicine = _lastRegisteredMedicine.asStateFlow()

    fun clearLastRegisteredMedicine() {
        _lastRegisteredMedicine.value = null
    }

    fun loadMedicineInfoById(medicineId: Int) {
        _lastRegisteredMedicine.value = null
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getMedicineInfo(medicineId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _lastRegisteredMedicine.value = response.body()?.medicine
                } else {
                    Log.w(
                        "MediRegiViewModel",
                        "약품 상세 조회 실패: code=${response.code()}, message=${response.message()}"
                    )
                    _lastRegisteredMedicine.value = null
                }
            } catch (e: Exception) {
                Log.e("MediRegiViewModel", "약품 상세 조회 오류: ${e.message}", e)
                _lastRegisteredMedicine.value = null
            }
        }
    }

    fun checkLatestMedicine(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getLatestMedicine()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.medicine != null) {
                        _lastRegisteredMedicine.value = body.medicine
                        onResult(true, "약품 인식 성공!")
                    } else {
                        onResult(false, body?.message ?: "약품을 찾을 수 없습니다.")
                    }
                } else onResult(false, "서버 응답 오류")
            } catch (e: Exception) {
                Log.e("MediRegiViewModel", "최신 약품 조회 오류: ${e.message}")
                onResult(false, "통신 오류: ${e.message}")
            }
        }
    }

    fun submitMedicineRegistration(
        medicineId: Int,
        itemSeq: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService
                    .updateMedicine(MedicineUpdateRequest(medicineId, itemSeq))
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.medicine?.let { _lastRegisteredMedicine.value = it }
                    MedRepository.refreshMedicineSlots()
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "MediRegiViewModel",
                        "약품 등록 실패: code=${response.code()}, message=${response.message()}, body=${response.body()?.message}, errorBody=$errorBody"
                    )
                    val readable = extractReadableErrorMessage(
                        response.body()?.message,
                        errorBody,
                        "등록 실패 (code=${response.code()})"
                    )
                    onFailure(readable)
                }
            } catch (e: Exception) {
                Log.e("MediRegiViewModel", "약품 등록 오류: ${e.message}", e)
                onFailure("네트워크 오류: ${e.message}")
            }
        }
    }

    fun updateMedicineName(
        medicineId: Int,
        newName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService
                    .renameMedicine(MedicineRenameRequest(medicineId, newName))
                if (response.isSuccessful && response.body()?.success == true) {
                    MedRepository.registerMedicine(medicineId, newName)
                    _lastRegisteredMedicine.value =
                        _lastRegisteredMedicine.value?.copy(item_name = newName)
                    onSuccess()
                } else onFailure(response.body()?.message ?: "이름 변경 실패")
            } catch (e: Exception) {
                Log.e("MediRegiViewModel", "약품명 변경 오류: ${e.message}", e)
                onFailure("네트워크 오류: ${e.message}")
            }
        }
    }

    fun refreshMedicineSlots() {
        viewModelScope.launch { MedRepository.refreshMedicineSlots() }
    }

    private fun extractReadableErrorMessage(
        bodyMessage: String?,
        errorBody: String?,
        fallback: String
    ): String {
        if (!bodyMessage.isNullOrBlank()) return bodyMessage
        if (errorBody.isNullOrBlank()) return fallback
        return try {
            JSONObject(errorBody).optString("message").ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }
}
