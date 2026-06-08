package com.inu.capstone_mobile


import android.Manifest

import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.inu.capstone_mobile.data.remote.http.RetrofitClient
import com.inu.capstone_mobile.data.remote.bluetooth.BluetoothManager as MyBluetoothManager
import com.inu.capstone_mobile.navigation.WireframeApp
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme
import com.inu.capstone_mobile.utils.TokenManager


class MainActivity : ComponentActivity() {
    // 1. 블루투스 매니저 선언 (나중에 초기화)
    private lateinit var myBluetoothManager: MyBluetoothManager

    // 2. 블루투스 권한 요청을 위한 런처 (사용자 승인 결과 처리)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (!granted) {
            Toast.makeText(this, "블루투스 권한이 거부되었습니다. 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 3. 블루투스 어댑터 및 매니저 초기화
        val bluetoothSystemService = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothSystemService.adapter
        myBluetoothManager = MyBluetoothManager(adapter)
        TokenManager.init(this)    // 신분증 지갑 준비
        RetrofitClient.init(this)

        // 4. 권한 체크 및 요청 실행
        checkBluetoothPermissions()

        setContent {
            Capstone_TabletTheme {
                // 5. 생성한 bluetoothManager 인스턴스를 네비게이션으로 전달!
                WireframeApp(bluetoothManager = myBluetoothManager)
            }
        }
    }

    // 안드로이드 버전에 따른 블루투스 권한 요청 분기
    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12(API 31) 이상
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            // Android 11 이하
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 6. 앱 종료 시 블루투스 연결 해제 (메모리 누수 방지)
        if (::myBluetoothManager.isInitialized) {
            myBluetoothManager.disconnect()
        }
    }

}// class MainActivity


