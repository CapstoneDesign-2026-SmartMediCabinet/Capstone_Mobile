package com.inu.capstone_mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.models.User
import com.inu.capstone_mobile.data.remote.http.RetrofitClient
import com.inu.capstone_mobile.data.repository.CabinetRepository
import com.inu.capstone_mobile.data.repository.MedRepository
import com.inu.capstone_mobile.data.repository.UserRepository
import com.inu.capstone_mobile.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.File
import java.io.IOException

class AuthViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Error(val message: String) : LoginState()
        object Success : LoginState()
    }

    val currentUser = UserRepository.currentUser
    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage = _authMessage.asStateFlow()



    fun login(inputId: String, inputPw: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = UserRepository.login(inputId, inputPw)
            completeLogin(user, onResult)
        }
    }



    private suspend fun completeLogin(user: User?, onResult: (Boolean) -> Unit) {
        if (user == null) { onResult(false); return }
        UserRepository.setCurrentUser(user)
        MedRepository.loadInitialData(user.userId)
        if (!DebugConfig.isTesting) {
            CabinetRepository.syncAllUserCabinetsFromServer()
            UserRepository.syncAllUsersFromServer("USER")
            CabinetRepository.uploadBluetoothSyncSnapshot()
        }
        CabinetRepository.applyUserPermission(user)
        onResult(true)
    }

    fun registerFace(imageFile: File, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // MultiPart 요청 준비
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("face_image", imageFile.name, requestFile)

                // 🌟 Node.js가 아닌 Flask 전용 API 호출! (userId 파라미터 삭제)
                val response = RetrofitClient.flaskApiService.registerFace(imagePart)

                if (response.isSuccessful && response.body()?.success == true) {
                    _authMessage.value = "얼굴 등록 성공!"
                    onResult(true)
                } else {
                    val serverMessage = response.body()?.message
                        ?: parseServerMessage(response.errorBody()?.string())
                    _authMessage.value = mapFaceRegisterError(
                        statusCode = response.code(),
                        serverMessage = serverMessage
                    )
                    onResult(false)
                }
            } catch (e: Exception) {
                _authMessage.value = mapNetworkExceptionMessage(e, isRegister = true)
                onResult(false)
            }
        }
    }

    fun VerifyFace(imageFile: File, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("face_image", imageFile.name, requestFile)

                // 🌟 Flask 전용 API 호출
                val response = RetrofitClient.flaskApiService.verifyFace(imagePart)

                // 🌟 원스톱 로그인 처리: 성공 시 토큰과 유저 정보를 한 방에 받음
                val body = response.body()
                if (response.isSuccessful && body?.success == true && body.token != null && body.user != null) {
                    _authMessage.value = "얼굴 인증 성공!"

                    // 1. 발급받은 진짜 JWT 토큰을 지갑에 저장
                    TokenManager.saveToken(body.token)

                    // 2. UserDto를 프로젝트 내 User 모델로 변환하여 완전한 로그인 처리
                    val user = UserRepository.buildUserFromDto(body.user)
                    completeLogin(user, onResult)

                } else {
                    val serverMessage = body?.message
                        ?: parseServerMessage(response.errorBody()?.string())
                    _authMessage.value = mapFaceVerifyError(
                        statusCode = response.code(),
                        serverMessage = serverMessage
                    )
                    onResult(false)
                }
            } catch (e: Exception) {
                _authMessage.value = mapNetworkExceptionMessage(e, isRegister = false)
                onResult(false)
            }
        }
    }

    private fun mapFaceVerifyError(statusCode: Int, serverMessage: String?): String {
        val msg = serverMessage.orEmpty()
        if (msg.contains("등록되지 않은 얼굴")) return "등록되지 않은 얼굴입니다. 먼저 안면 정보를 등록해 주세요."
        if (msg.contains("얼굴을 인식하지 못했습니다")) return "얼굴을 찾지 못했습니다. 조명을 밝게 하고 다시 시도해 주세요."
        if (msg.contains("사진 파일이 누락")) return "사진 전송이 올바르지 않습니다. 다시 시도해 주세요."
        if (msg.contains("명단 조회 실패") || msg.contains("Node.js") || msg.contains("토큰 발급")) {
            return "인증 서버 연결에 실패했습니다. 서버 주소/포트를 확인해 주세요."
        }

        return when (statusCode) {
            400 -> "요청 형식이 올바르지 않습니다. 카메라 촬영 후 다시 시도해 주세요."
            401, 403 -> "서버 인증에 실패했습니다. 내부 API 키 설정을 확인해 주세요."
            404 -> "등록되지 않은 얼굴입니다."
            422 -> "얼굴을 찾지 못했습니다. 얼굴을 화면 중앙에 맞춰 주세요."
            in 500..599 -> "인증 서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
            else -> if (msg.isNotBlank()) "안면 인증 실패: $msg" else "안면 인증에 실패했습니다. 다시 시도해 주세요."
        }
    }

    private fun mapFaceRegisterError(statusCode: Int, serverMessage: String?): String {
        val msg = serverMessage.orEmpty()
        if (msg.contains("인증 토큰이 없습니다")) return "로그인 토큰이 없습니다. 다시 로그인해 주세요."
        if (msg.contains("만료된 토큰")) return "로그인 세션이 만료되었습니다. 다시 로그인해 주세요."
        if (msg.contains("위조된 토큰")) return "토큰 검증에 실패했습니다. 다시 로그인해 주세요."
        if (msg.contains("사진 파일이 누락")) return "사진 전송이 올바르지 않습니다. 다시 촬영해 주세요."
        if (msg.contains("특징점 추출 실패") || msg.contains("얼굴")) return "얼굴 특징을 추출하지 못했습니다. 정면을 보고 다시 시도해 주세요."
        if (msg.contains("DB 통신 실패") || msg.contains("DB 저장") || msg.contains("Node.js")) {
            return "등록 서버 연결에 실패했습니다. 서버 주소/포트를 확인해 주세요."
        }

        return when (statusCode) {
            400 -> "요청 데이터가 올바르지 않습니다. 다시 촬영해 주세요."
            401 -> "로그인 정보가 만료되었습니다. 다시 로그인해 주세요."
            422 -> "얼굴을 인식하지 못했습니다. 조명을 밝게 하고 다시 시도해 주세요."
            in 500..599 -> "등록 서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
            else -> if (msg.isNotBlank()) "안면 등록 실패: $msg" else "안면 등록에 실패했습니다. 다시 시도해 주세요."
        }
    }

    private fun mapNetworkExceptionMessage(e: Exception, isRegister: Boolean): String {
        val prefix = if (isRegister) "안면 등록" else "안면 인증"
        return when (e) {
            is UnknownHostException,
            is ConnectException -> "$prefix 서버에 연결할 수 없습니다. 서버 주소와 포트를 확인해 주세요."
            is SocketTimeoutException -> "$prefix 서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."
            is IOException -> "네트워크 연결이 불안정합니다. Wi-Fi 상태를 확인해 주세요."
            else -> "$prefix 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
        }
    }

    private fun parseServerMessage(rawErrorBody: String?): String? {
        if (rawErrorBody.isNullOrBlank()) return null
        return runCatching {
            JSONObject(rawErrorBody).optString("message").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun clearAuthMessage() {
        _authMessage.value = null
    }

    fun logout(clearToken: Boolean = true) {
        viewModelScope.launch {
            if (!DebugConfig.isTesting) {
                CabinetRepository.uploadBluetoothSyncSnapshot()
            }
        }
        UserRepository.clearCurrentUser()
        MedRepository.reloadData()
        CabinetRepository.lockAllCabinets()
        if (clearToken) {
            TokenManager.clearToken()
        }
    }
}
