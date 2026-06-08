package com.inu.capstone_mobile.data.repository

import android.util.Log
import com.google.gson.JsonElement
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.models.Prescription
import com.inu.capstone_mobile.data.models.User
import com.inu.capstone_mobile.data.remote.http.LoginRequest
import com.inu.capstone_mobile.data.remote.http.PrescriptionResponse
import com.inu.capstone_mobile.data.remote.http.RetrofitClient
import com.inu.capstone_mobile.data.remote.http.UserDto
import com.inu.capstone_mobile.data.source.MockDataSource
import com.inu.capstone_mobile.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response

object UserRepository {
    private val api = RetrofitClient.apiService

    private val mockUserDatabase = mapOf(
        "admin01" to MockDataSource.DEBUG_USERINFO_ADMIN,
        "garam01" to MockDataSource.DEBUG_USERINFO_PATIENT_1,
        "alum01" to MockDataSource.DEBUG_USERINFO_PATIENT_2
    )

    // 전역 공유 세션 상태
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(
        if (DebugConfig.isTesting) MockDataSource.ALL_PATIENTS else emptyList()
    )
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    fun setCurrentUser(user: User) { _currentUser.value = user }
    fun clearCurrentUser() { _currentUser.value = null }

    suspend fun syncAllUsersFromServer(role: String = "USER"): Boolean {
        if (DebugConfig.isTesting) {
            _allUsers.value = MockDataSource.ALL_PATIENTS.filter { it.userRoleId == role }
            return true
        }

        return try {
            val response = api.getUsers(role)
            if (response.isSuccessful && response.body()?.success == true) {
                val users = response.body()?.users.orEmpty().map { dto ->
                    val roleFromDb = dto.role.uppercase().trim()
                    User(
                        userId = dto.user_id,
                        userName = dto.name,
                        userRole = if (roleFromDb == "ADMIN") "전문의" else "환자",
                        userRoleId = roleFromDb,
                        prescriptions = emptyList()
                    )
                }
                _allUsers.value = users
                true
            } else {
                Log.e("UserRepository", "사용자 목록 조회 실패: code=${response.code()}, message=${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "사용자 목록 조회 오류: ${e.message}", e)
            false
        }
    }

    suspend fun buildUserFromDto(userDto: UserDto): User {
        Log.d("UserRepository", "Server Role Received: '${userDto.role}'")
        val roleFromDb = userDto.role.uppercase().trim()

        val prescriptions: List<Prescription> = try {
            val resp = api.getPrescriptions(userDto.user_id)
            val mapped = mapPrescriptions(resp)
            Log.d("UserRepository", "처방전 조회 결과(user_id 기준): user=${userDto.user_id}, count=${mapped.size}")
            mapped
        } catch (e: Exception) {
            Log.e("UserRepository", "처방전 조회 오류: ${e.message}", e)
            emptyList()
        }

        return User(
            userId = userDto.user_id,
            userName = userDto.name,
            userRole = if (roleFromDb == "ADMIN") "전문의" else "환자",
            userRoleId = roleFromDb,
            prescriptions = prescriptions
        )
    }

    suspend fun fetchPrescriptionsForUser(userId: String): List<Prescription> {
        return try {
            val resp = api.getPrescriptions(userId)
            mapPrescriptions(resp)
        } catch (e: Exception) {
            Log.e("UserRepository", "처방전 조회 오류: userId=$userId, ${e.message}")
            emptyList()
        }
    }

    fun refreshAllUsersFromCabinets() {
        // user list source is users table via syncAllUsersFromServer(); keep this no-op for compatibility.
    }

    suspend fun login(id: String, pw: String): User? {
        return if (DebugConfig.isTesting) {
            if (pw == "1234") mockUserDatabase[id] else null
        } else {
            try {
                val response = api.login(LoginRequest(id, pw))

                if (response.isSuccessful && response.body()?.success == true) {
                    val loginResponse = response.body()!!
                    val token = loginResponse.token?.takeIf { it.isNotBlank() }
                    if (token == null) {
                        Log.e("UserRepository", "로그인 실패: 토큰 누락")
                        return null
                    }
                    TokenManager.saveToken(token)

                    val userDto = loginResponse.user ?: run {
                        Log.e("UserRepository", "로그인 실패: 사용자 정보 누락")
                        return null
                    }
                    buildUserFromDto(userDto)
                } else {
                    Log.e("UserRepository", "로그인 실패: ${response.body()?.message}")
                    null
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "네트워크 오류: ${e.message}")
                null
            }
        }
    }

    private fun parseDailyTimings(raw: JsonElement?): List<String> {
        if (raw == null || raw.isJsonNull) return emptyList()

        return when {
            raw.isJsonArray -> raw.asJsonArray.mapNotNull { it.asString?.trim() }.filter { it.isNotEmpty() }
            raw.isJsonPrimitive -> raw.asString
                .removePrefix("[")
                .removeSuffix("]")
                .replace("\"", "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    private fun mapPrescriptions(response: Response<PrescriptionResponse>): List<Prescription> {
        if (!response.isSuccessful || response.body()?.success != true) {
            Log.w(
                "UserRepository",
                "처방전 조회 실패: code=${response.code()}, message=${response.message()}, bodyMessage=${response.body()?.message}"
            )
            return emptyList()
        }

        return response.body()?.prescriptions.orEmpty().mapNotNull { dto ->
            val dailyTimings = parseDailyTimings(dto.dailyTimings)
            if (dto.startDate.isBlank() || dto.endDate.isBlank()) {
                Log.w("UserRepository", "처방전 날짜 누락으로 건너뜀: prescription_id=${dto.prescription_id}")
                null
            } else {
                Prescription(
                    pillId = dto.pillId,
                    dailyTimings = dailyTimings,
                    startDate = dto.startDate,
                    endDate = dto.endDate
                )
            }
        }
    }
}
