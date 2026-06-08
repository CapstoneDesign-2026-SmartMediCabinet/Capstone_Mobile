package com.inu.capstone_mobile.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.core.content.edit

object TokenManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private lateinit var prefs: SharedPreferences
    private val _sessionEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val sessionEvents = _sessionEvents.asSharedFlow()

    sealed class SessionEvent {
        data class Expired(val message: String) : SessionEvent()
    }

    // 앱 시작 시(MyApplication 등) 한 번 호출해주면 좋습니다.
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 로그인 성공 시 토큰 저장
    fun saveToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    // API 요청 시 토큰 꺼내기
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // 로그아웃 시 토큰 삭제
    fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    fun notifySessionExpired(message: String = "세션이 만료되어 자동 로그아웃됩니다.") {
        clearToken()
        _sessionEvents.tryEmit(SessionEvent.Expired(message))
    }
}