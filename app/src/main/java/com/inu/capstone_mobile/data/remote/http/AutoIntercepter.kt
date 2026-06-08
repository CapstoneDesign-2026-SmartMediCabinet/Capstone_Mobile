package com.inu.capstone_mobile.data.remote.http

import com.inu.capstone_mobile.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 지갑에서 토큰을 꺼냅니다.
        val token = TokenManager.getToken()

        // 토큰이 없으면(로그인 전) 그냥 보냅니다.
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // 토큰이 있으면 헤더에 "Bearer [토큰]" 형태로 붙여서 보냅니다.
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(newRequest)
        if (response.code == 401) {
            TokenManager.notifySessionExpired()
            response.close()
            throw IOException("Unauthorized")
        }
        return response
    }
}