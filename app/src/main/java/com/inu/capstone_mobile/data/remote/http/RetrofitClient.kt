package com.inu.capstone_mobile.data.remote.http

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet4Address

object RetrofitClient {
    // 기본값 설정 (만약을 대비한 노트북 핫스팟 IP)
    private var BASE_URL: String = "http://192.168.137.1:3000/" //

    // 에뮬레이터 확인 로직
    private val isEmulator: Boolean
        get() = Build.FINGERPRINT.contains("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MODEL.contains("Android SDK built for x86_64")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("vbox86p")
                || Build.MANUFACTURER.contains("Genymotion")

    // 1. 앱 시작 시 한 번 호출되어 자동으로 서버 IP를 추적하는 함수
    fun init(context: Context) {
        if (isEmulator) {
            BASE_URL = "http://10.0.2.2:3000/" //
            Log.d("RetrofitClient", "에뮬레이터 모드 IP 세팅: $BASE_URL")
        } else {
            // LinkProperties의 route gateway를 읽어 서버(핫스팟 호스트) IP를 추정합니다.
            val connectivityManager =
                context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val gatewayIp = connectivityManager.activeNetwork
                ?.let { network -> connectivityManager.getLinkProperties(network) }
                ?.routes
                ?.asSequence()
                ?.mapNotNull { route -> route.gateway as? Inet4Address }
                ?.firstOrNull { ip -> !ip.isLoopbackAddress && !ip.isAnyLocalAddress }
                ?.hostAddress

            // 게이트웨이를 찾지 못하면 기본값(192.168.137.1) 유지
            if (!gatewayIp.isNullOrBlank()) {
                BASE_URL = "http://$gatewayIp:3000/"
            }
            Log.d("RetrofitClient", "실제 기기(핫스팟) IP 세팅: $BASE_URL")
        }
    }

    // 2. JWT 토큰을 자동으로 붙여주는 인터셉터 장착
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor()) // 직전 대화에서 만든 인터셉터
            .build()
    }

    // 3. 실제 통신 객체 (앱 전체에서 사용)
    val apiService: ServerApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 인터셉터가 달린 클라이언트 연결
            .addConverterFactory(GsonConverterFactory.create()) //
            .build()
            .create(ServerApi::class.java) //
    }

    val flaskApiService: ServerApi by lazy {
        val flaskUrl = BASE_URL.replace(":3000", "33:5001")
        Retrofit.Builder()
            .baseUrl(flaskUrl)
            .client(okHttpClient) // 인터셉터 그대로 공유
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServerApi::class.java)
    }
}