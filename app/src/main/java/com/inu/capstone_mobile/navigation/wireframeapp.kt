package com.inu.capstone_mobile.navigation

import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.remote.bluetooth.BluetoothManager
import com.inu.capstone_mobile.data.repository.CabinetRepository
import com.inu.capstone_mobile.data.source.MockDataSource
import com.inu.capstone_mobile.ui.screens.FaceRegister
import com.inu.capstone_mobile.ui.screens.FaceScreen
import com.inu.capstone_mobile.ui.screens.LoginScreen
import com.inu.capstone_mobile.ui.screens.ManagerMain
import com.inu.capstone_mobile.ui.screens.MedInfo
import com.inu.capstone_mobile.ui.screens.MedicineInfoUi
import com.inu.capstone_mobile.ui.screens.MedRegi
import com.inu.capstone_mobile.ui.screens.MedManageScreen
import com.inu.capstone_mobile.ui.screens.Startscreen
import com.inu.capstone_mobile.ui.screens.UserCabinetManagementScreen
import com.inu.capstone_mobile.ui.screens.UserMain
import com.inu.capstone_mobile.utils.TokenManager
import com.inu.capstone_mobile.viewmodel.AuthViewModel
import com.inu.capstone_mobile.viewmodel.HardwareViewModel
import com.inu.capstone_mobile.viewmodel.ManageViewModel
import com.inu.capstone_mobile.viewmodel.MediRegiViewModel
import com.inu.capstone_mobile.viewmodel.UserViewModel

@Composable
fun WireframeApp(bluetoothManager: BluetoothManager) {
    val navController = rememberNavController()
    val appContext = LocalContext.current

    // 전역 ViewModel — NavHost 바깥에서 1회 생성, 앱 생존 동안 유지
    val hardwareVm: HardwareViewModel = viewModel(factory = HardwareViewModel.Factory(bluetoothManager))

    // Auth/MediRegi는 화면 간 데이터 공유가 필요하므로 NavHost 바깥에 hoist
    val authVm: AuthViewModel = viewModel()
    val mediRegiVm: MediRegiViewModel = viewModel()

    fun navigateToHome() {
        hardwareVm.start()
        val loggedInUser = authVm.currentUser.value

        if (bluetoothManager.isConnected.value) {
            when (loggedInUser?.userRoleId) {
                "ADMIN" -> CabinetRepository.sendLockAllCabinetsCommand(bluetoothManager)
                "USER" -> loggedInUser.userId.takeIf { it.isNotBlank() }?.let { userId ->
                    CabinetRepository.sendUnlockCommandForUser(bluetoothManager, userId)
                }
            }
        }

        when (authVm.currentUser.value?.userRoleId) {
            "ADMIN" -> navController.navigate("Manager") {
                popUpTo("start") { inclusive = true }
            }
            "USER" -> navController.navigate("UserMain") {
                popUpTo("start") { inclusive = true }
            }
            else -> authVm.logout()
        }
    }

    LaunchedEffect(Unit) {
        TokenManager.sessionEvents.collect { event ->
            when (event) {
                is TokenManager.SessionEvent.Expired -> {
                    if (bluetoothManager.isConnected.value) {
                        CabinetRepository.sendLockAllCabinetsCommand(bluetoothManager)
                    }
                    authVm.logout()
                    navController.navigate("start") {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "start",
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(500)) },
        popExitTransition = { fadeOut(animationSpec = tween(500)) }
    ) {
        composable("start") {
            Startscreen(
                onNavigateToFaceLogin = {
                    navController.navigate("Face_Login")
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }

        composable("login") {
            LoginScreen(
                viewModel = authVm,
                onLoginSuccess = {
                    navigateToHome()
                }
            )
        }

        composable("Face_Login") {
            FaceScreen(
                viewModel = authVm,
                onBack = { navController.popBackStack() },
                onLoginSuccess = { navigateToHome() }
            )
        }

        composable("Manager") {
            val manageVm: ManageViewModel = viewModel()
            ManagerMain(
                viewModel = manageVm,
                bluetoothManager = bluetoothManager,
                hardwareViewModel = hardwareVm,
                onFaceRegister = { navController.navigate("Face_Register") },
                onMedRegi = { navController.navigate("Med_Register") },
                onPrescriptionManage = { navController.navigate("Med_Manage") },
                onUserCabinetManage = { navController.navigate("UserCabinet_Manage") },
                onLogout = {
                    if (bluetoothManager.isConnected.value) {
                        CabinetRepository.sendLockAllCabinetsCommand(bluetoothManager)
                    }
                    authVm.logout()
                    navController.navigate("start") {
                        popUpTo("Manager") { inclusive = true }
                    }
                },
            )
        }

        composable("UserMain") {
            val userVm: UserViewModel = viewModel()
            LaunchedEffect(Unit) { userVm.autoUnlockMyCabinet(bluetoothManager) }
            val isBluetoothConnected by bluetoothManager.isConnected.collectAsState()
            UserMain(
                viewModel = userVm,
                bluetoothManager = bluetoothManager,
                isBluetoothConnected = isBluetoothConnected,
                onFaceRegister = { navController.navigate("Face_Register") },
                onLogout = {
                    if (bluetoothManager.isConnected.value) {
                        CabinetRepository.sendLockAllCabinetsCommand(bluetoothManager)
                    }
                    authVm.logout()
                    navController.navigate("start") {
                        popUpTo("UserMain") { inclusive = true }
                    }
                },
                onHistoryClick = { navController.navigate("start") }
            )
        }

        composable("Face_Register") {
            FaceRegister(
                viewModel = authVm,
                onBack = { navController.popBackStack() }
            )
        }

        composable("Med_Register") {
            LaunchedEffect(Unit) { mediRegiVm.clearLastRegisteredMedicine() }
            val dto by mediRegiVm.lastRegisteredMedicine.collectAsState()
            val medicineSlots by mediRegiVm.medicineSlots.collectAsState()
            val isBluetoothConnected by bluetoothManager.isConnected.collectAsState()
            val context = LocalContext.current
            val slotCount = maxOf(3, (medicineSlots.maxOfOrNull { it.slotId } ?: -1) + 1)
            fun isRealMedicineName(name: String): Boolean {
                val trimmed = name.trim()
                return trimmed.isNotEmpty() && !trimmed.startsWith("약품 #")
            }
            val slotOccupiedStates = List(slotCount) { index ->
                val slot = medicineSlots.find { it.slotId == index }
                isRealMedicineName(slot?.medicine?.name.orEmpty())
            }

            val initialMedicineInfo = if (DebugConfig.isTesting && dto == null) {
                MedicineInfoUi(
                    medicine_Id = MockDataSource.DEBUG_MED_INFO_ID,
                    name = MockDataSource.DEBUG_MED_INFO_NAME,
                    manufacturer = MockDataSource.DEBUG_MED_INFO_MANUFACTURER,
                    type = MockDataSource.DEBUG_MED_INFO_TYPE,
                    imageUrl = MockDataSource.DEBUG_MED_INFO_IMAGE_URL
                )
            } else {
                MedicineInfoUi(
                    medicine_Id = dto?.item_seq ?: "",
                    name = dto?.item_name ?: "약품을 인식시켜주세요",
                    manufacturer = dto?.entp_name ?: "",
                    type = dto?.class_name ?: "",
                    imageUrl = dto?.image_url
                )
            }

            MedRegi(
                medicineInfo = initialMedicineInfo,
                medicineSlots = medicineSlots,
                slotOccupiedStates = slotOccupiedStates,
                isBluetoothConnected = isBluetoothConnected,
                onRegisterAttempt = {
                    mediRegiVm.checkLatestMedicine { _, msg ->
                        Toast.makeText(context, msg ?: "인식 대기 중...", Toast.LENGTH_SHORT).show()
                    }
                },
                onConfirm = { editedName, selectedSlot ->
                    val currentDto = mediRegiVm.lastRegisteredMedicine.value
                    if (currentDto != null) {
                        mediRegiVm.submitMedicineRegistration(
                            medicineId = selectedSlot,
                            itemSeq = currentDto.item_seq,
                            onSuccess = {
                                if (editedName.trim() != currentDto.item_name.trim()) {
                                    mediRegiVm.updateMedicineName(
                                        medicineId = selectedSlot,
                                        newName = editedName.trim(),
                                        onSuccess = {
                                            Toast.makeText(context, "성공적으로 등록되었습니다.", Toast.LENGTH_SHORT).show()
                                            navController.navigate("Med_Info")
                                        },
                                        onFailure = { _ ->
                                            Toast.makeText(context, "등록 완료 (이름 변경 실패)", Toast.LENGTH_SHORT).show()
                                            navController.navigate("Med_Info")
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "성공적으로 등록되었습니다.", Toast.LENGTH_SHORT).show()
                                    navController.navigate("Med_Info")
                                }
                            },
                            onFailure = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "먼저 약품을 인식시켜주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable("Med_Manage") {
            val isBluetoothConnected by bluetoothManager.isConnected.collectAsState()
            MedManageScreen(
                isBluetoothConnected = isBluetoothConnected,
                onMedicineClick = { medicineId ->
                    medicineId.toIntOrNull()?.let { medicineIdInt ->
                        mediRegiVm.loadMedicineInfoById(medicineIdInt)
                        navController.navigate("Med_Info")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("UserCabinet_Manage") {
            val medicineSlots by mediRegiVm.medicineSlots.collectAsState()
            val isBluetoothConnected by bluetoothManager.isConnected.collectAsState()
            val userCabinetVm: com.inu.capstone_mobile.viewmodel.UserCabinetManageViewModel = viewModel()

            UserCabinetManagementScreen(
                viewModel = userCabinetVm, // 전용 뷰모델을 넘겨줌
                medicineSlots = medicineSlots,
                isBluetoothConnected = isBluetoothConnected,
                onBack = { navController.popBackStack() }
            )
        }

        composable("Med_Info") {
            val dto by mediRegiVm.lastRegisteredMedicine.collectAsState()
            val medicineInfo = if (dto != null) {
                MedicineInfoUi(
                    medicine_Id = dto!!.item_seq,
                    name = dto!!.item_name,
                    manufacturer = dto!!.entp_name,
                    type = dto!!.class_name,
                    imageUrl = dto!!.image_url
                )
            } else {
                MedicineInfoUi(
                    medicine_Id = "",
                    name = "불러오는 중...",
                    manufacturer = "",
                    type = "",
                    imageUrl = null
                )
            }
            MedInfo(
                medicineInfo = medicineInfo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
