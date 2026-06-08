package com.inu.capstone_mobile.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.inu.capstone_mobile.data.models.AdminCabinet
import com.inu.capstone_mobile.data.models.MedicineSlot
import com.inu.capstone_mobile.data.models.User
import com.inu.capstone_mobile.data.models.UserCabinet
import com.inu.capstone_mobile.data.models.totalStatus
import com.inu.capstone_mobile.data.remote.bluetooth.BluetoothManager
import com.inu.capstone_mobile.data.source.MockDataSource
import com.inu.capstone_mobile.ui.components.AdminNoticeBanner
import com.inu.capstone_mobile.ui.components.AppScaffold
import com.inu.capstone_mobile.ui.components.BaseCard
import com.inu.capstone_mobile.ui.components.StatusDot
import com.inu.capstone_mobile.ui.components.StatusCard
import com.inu.capstone_mobile.ui.components.UserCabinetGridCard
import com.inu.capstone_mobile.ui.components.UserCard
import com.inu.capstone_mobile.ui.components.rememberAdminNoticeState
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme
import com.inu.capstone_mobile.viewmodel.HardwareViewModel
import com.inu.capstone_mobile.viewmodel.ManageViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

private const val SCAFFOLD_TITLE = "관리자 메인 화면"
private const val MAXADMINSLOT = 6

@Composable
fun ManagerMain(
    viewModel: ManageViewModel,
    bluetoothManager: BluetoothManager,
    hardwareViewModel: HardwareViewModel,
    onMedRegi: () -> Unit = {},
    onPrescriptionManage: () -> Unit = {},
    onUserCabinetManage: () -> Unit = {},
    onFaceRegister: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val activity: Activity? = LocalActivity.current
    val user by viewModel.currentUser.collectAsState()
    val medicineSlots by viewModel.medicineSlots.collectAsState()
    val userCabinets by viewModel.userCabinets.collectAsState()
    val adminCabinets by viewModel.adminCabinets.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isAdminDoorLocked by viewModel.isAdminDoorLocked.collectAsState()
    val isBluetoothConnected by bluetoothManager.isConnected.collectAsState()

    val temp by viewModel.temp.collectAsState()
    val humid by viewModel.humid.collectAsState()
    val isFanOn by viewModel.isFanOn.collectAsState()

    val viewConfiguration = LocalViewConfiguration.current
    val customViewConfiguration = object : ViewConfiguration by viewConfiguration {
        override val longPressTimeoutMillis: Long
            get() = 1500L
    }

    CompositionLocalProvider(LocalViewConfiguration provides customViewConfiguration) {
        AppScaffold(
            title = SCAFFOLD_TITLE,
            onFaceRegisterClick = onFaceRegister,
            onMedRegiClick = onMedRegi,
            onPrescriptionManageClick = onPrescriptionManage,
            onUserCabinetManageClick = onUserCabinetManage,
            onLogoutClick = onLogout,
            onExitClick = { activity?.finish() }
        ) { paddingValues ->
            ManagerMainContent(
                userName = user?.userName ?: "알 수 없음",
                userRole = user?.userRole ?: "관리자",
                temp = temp,
                humid = humid,
                isFanOn = isFanOn,
                medicineSlots = medicineSlots,
                userCabinets = userCabinets,
                adminCabinets = adminCabinets,
                paddingValues = paddingValues,
                onToggleLock = { userId -> viewModel.toggleUserCabinetLockWithBT(userId,bluetoothManager) },
                isAdminDoorLocked = isAdminDoorLocked,
                isBluetoothConnected = isBluetoothConnected,
                onRefreshBluetooth = {
                    hardwareViewModel.forceSyncOrReconnect()
                },
                onAdminToggle = { viewModel.toggleAdminDoorWithBT(bluetoothManager) },
                onMedRegi = onMedRegi,
                onMedManage = onPrescriptionManage,
                onUserCabinetManage = onUserCabinetManage,
                onFaceRegister = onFaceRegister,
                allUsers = allUsers
            )
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ManagerMainContent(
    userName: String,
    userRole: String,
    temp: Float? = null,
    humid: Float? = null,
    isFanOn: Boolean? = null,
    medicineSlots: List<MedicineSlot> = emptyList(),
    userCabinets: List<UserCabinet> = emptyList(),
    adminCabinets: List<AdminCabinet> = emptyList(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    isAdminDoorLocked: Boolean?,
    isBluetoothConnected: Boolean = false,
    onRefreshBluetooth: () -> Unit = {},
    onToggleLock: (String) -> Unit = {},
    onMedRegi: () -> Unit = {},
    onMedManage: () -> Unit = {},
    onUserCabinetManage: () -> Unit = {},
    onFaceRegister: () -> Unit = {},
    onAdminToggle: () -> Unit,
    allUsers: List<User> = emptyList(),
) {
    val noticeState = rememberAdminNoticeState(
        isBluetoothConnected = isBluetoothConnected,
        isFanOn = isFanOn,
        adminCabinets = adminCabinets,
        medicineSlots = medicineSlots
    )

    // 🌟 2. paddingValues를 최상단 Box에 먹여서 Scaffold(상단 바) 아래로 강제 배치!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Scaffold 아래 영역으로 제한
            .background(Color(0xFFDDEAF8))
    ) {
        // 기존 메인 UI 내용 (배경색과 패딩 겹침 방지 위해 padding 분리)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                val (topSection, middleDivider, bottomSection) = createRefs()

                Row(
                    modifier = Modifier.constrainAs(topSection) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        height = Dimension.percent(0.45f)
                        width = Dimension.fillToConstraints
                    },
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // [1열: 사용자 정보]
                    Column(modifier = Modifier.weight(0.15f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        UserCard(modifier = Modifier.fillMaxWidth().height(120.dp), title = "환영합니다,") {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(userRole, fontSize = 18.sp, color = Color.DarkGray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${userName} 님", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        ManagerClockStatusCard(
                            isConnected = isBluetoothConnected,
                            onRefresh = onRefreshBluetooth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }

                    // [2열: 액션 버튼]
                    Column(modifier = Modifier.weight(0.15f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val shape = RoundedCornerShape(12.dp)
                        Button(onClick = onMedRegi, modifier = Modifier.fillMaxWidth().weight(1f), shape = shape, colors = ButtonDefaults.buttonColors(Color(0xFF6200EE))) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("새 약품 등록", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = onMedManage, modifier = Modifier.fillMaxWidth().weight(1f), shape = shape, colors = ButtonDefaults.buttonColors(Color(0xFF1565C0))) {
                            Icon(Icons.Default.Description, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("약품 관리", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = onFaceRegister, modifier = Modifier.fillMaxWidth().weight(1f), shape = shape, colors = ButtonDefaults.buttonColors(Color(0xFF00897B))) {
                            Icon(Icons.Default.Face, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("안면 정보 등록", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // [3열: 관리자 보관함] (🌟 독립 스크롤 적용 버전)
                    BaseCard(modifier = Modifier.weight(0.44f).fillMaxHeight(), title = "") {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 고정 헤더
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val adminDoorStateText = when {
                                    !isBluetoothConnected -> "오프라인"
                                    isAdminDoorLocked == null -> "연결 중..."
                                    isAdminDoorLocked -> "잠김"
                                    else -> "잠금 해제"
                                }
                                val adminDoorIcon = when {
                                    !isBluetoothConnected -> Icons.Default.AccessTimeFilled
                                    isAdminDoorLocked == null -> Icons.Default.Sync
                                    isAdminDoorLocked -> Icons.Default.Lock
                                    else -> Icons.Default.LockOpen
                                }
                                val adminDoorColor = when {
                                    !isBluetoothConnected -> Color.Gray
                                    isAdminDoorLocked == null -> Color(0xFFFFA000)
                                    isAdminDoorLocked -> Color.Red
                                    else -> Color(0xFF00C853)
                                }
                                Column {
                                    Text("관리자 보관함", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(text = adminDoorStateText, fontSize = 12.sp, color = Color.Gray)
                                }
                                Icon(imageVector = adminDoorIcon, contentDescription = null, tint = adminDoorColor,
                                    modifier = Modifier.size(32.dp).combinedClickable(onClick = { }, onLongClick = { if (isBluetoothConnected) onAdminToggle() })
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp)

                            // 약품 6개 스크롤 영역
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val slotCount = MAXADMINSLOT // 🌟 무조건 6칸 고정
                                (0 until slotCount).forEach { idx ->
                                    val slot = medicineSlots.find { it.slotId == idx }
                                    val cabinet = adminCabinets.find { it.slotIndex == idx }
                                    val medicineName = slot?.medicine?.name?.takeIf { it.isNotEmpty() } ?: "약품 #${idx + 1}"
                                    val isCabinetActive = cabinet?.isActive == true
                                    val hasRegisteredMedicine = !medicineName.startsWith("약품 #")
                                    val adminStatus = cabinet?.totalStatus ?: totalStatus.EMPTY

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = medicineName,
                                            modifier = Modifier.weight(1f),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isCabinetActive) Color.Black else Color.Gray
                                        )
                                        val (statusText, statusColor) = when {
                                            !isCabinetActive -> "비활성" to Color.Gray
                                            !hasRegisteredMedicine -> "없음" to Color.Red
                                            else -> when (adminStatus) {
                                                totalStatus.ENOUGH -> "충분" to Color(0xFF00C853)
                                                totalStatus.LOW -> "부족" to Color(0xFFFFA000)
                                                totalStatus.EMPTY -> "없음" to Color.Red
                                            }
                                        }
                                        Text(statusText, fontSize = 16.sp, color = statusColor)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // [4열: 우측 환경 상태]
                    Column(modifier = Modifier.weight(0.13f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            StatusCard(temp = temp, humid = humid, isFanOn = isFanOn, modifier = Modifier.fillMaxSize())
                            StatusDot(isHealthy = isBluetoothConnected && temp != null && humid != null, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp))
                        }
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            BaseCard(modifier = Modifier.fillMaxSize(), title = "환풍기 동작") {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    val fanText = when (isFanOn) { true -> "🌀 가동 중"; false -> "🛑 정지"; null -> "🔧 오프라인" }
                                    Text(fanText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            StatusDot(isHealthy = isBluetoothConnected && isFanOn != null, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp))
                        }
                        WarningTickerBar(
                            isBluetoothConnected = isBluetoothConnected,
                            isFanOn = isFanOn,
                            adminCabinets = adminCabinets,
                            medicineSlots = medicineSlots,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )
                    }
                }

                // [중앙 가로선]
                Column(
                    modifier = Modifier.constrainAs(middleDivider) {
                        top.linkTo(topSection.bottom, 24.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(thickness = 5.dp, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.weight(0.4f))
                        Text("사용자 약품장 현황", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.weight(0.3f))
                        TextButton(onClick = onUserCabinetManage) {
                            Text("관리 ➔", fontSize = 18.sp, color = Color.DarkGray, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // [하단 보관함]
                Row(
                    modifier = Modifier.constrainAs(bottomSection) {
                        top.linkTo(middleDivider.bottom, 16.dp)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(48.dp)
                ) {
                    userCabinets.forEachIndexed { index, cabinet ->
                        Column(modifier = Modifier.width(340.dp)) {
                            val displayUserName = allUsers.find { it.userId == cabinet.userId }?.userName ?: "사용자#${index + 1}"

                            if (!cabinet.isActive) {
                                //  weight(0.8f) 대신 fillMaxHeight()를 주어 다른 카드와 높이를 강제로 맞춤
                                BaseCard(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f), title = displayUserName) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("비활성화됨", color = Color.Gray, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    UserCabinetGridCard(modifier = Modifier.fillMaxWidth(), title = displayUserName, cabinet = cabinet)
                                    StatusDot(isHealthy = isBluetoothConnected && cabinet.isLocked != null, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LockStatusBox(isLocked = if(isBluetoothConnected) cabinet.isLocked else null, modifier = Modifier.fillMaxWidth(), onToggleLock = { onToggleLock(cabinet.userId) })
                            }
                        }
                    }
                }
            }
        }

        // 🌟 3. 부드러운 애니메이션 배너 (Scaffold 아래에 완벽하게 뜸!)
        AnimatedVisibility(
            visible = noticeState.isVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(), // 위에서 내려오기
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(), // 위로 올라가기
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp) // 상단 바 아래에서 살짝 여백
        ) {
            Box(modifier = Modifier.widthIn(max = 600.dp)) {
                AdminNoticeBanner(notices = noticeState.notices)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LockStatusBox(isLocked: Boolean?, modifier: Modifier = Modifier, onToggleLock: () -> Unit) {
    Card(
        modifier = modifier.combinedClickable(
            onClick = { },
            onLongClick = { if (isLocked != null) onToggleLock() }
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val (text, icon, color) = when (isLocked) {
                true -> Triple("잠김", Icons.Default.Lock, Color.Red)
                false -> Triple("잠금 해제", Icons.Default.LockOpen, Color(0xFF00C853))
                null -> Triple("오프라인", Icons.Default.AccessTimeFilled, Color.Gray)
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ManagerClockStatusCard(
    isConnected: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meridiemFormatter = remember { DateTimeFormatter.ofPattern("a", Locale.KOREAN) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm") }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1.seconds)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                BluetoothStatusContent(
                    isConnected = isConnected,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                thickness = 1.dp,
                color = Color(0xFFE3E3E3)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = currentTime.format(meridiemFormatter),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                    Text(
                        text = currentTime.format(timeFormatter),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun BluetoothStatusContent(
    isConnected: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = 12.dp
    val verticalPadding = 6.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // LED 표시등 (연결됨 = 초록색, 끊김 = 회색)
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(
                    color = if (isConnected) Color(0xFF00C853) else Color.LightGray,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isConnected) "연결됨" else "오프라인",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isConnected) Color.Black else Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        VerticalDivider(
            modifier = Modifier.height(18.dp),
            thickness = 1.dp,
            color = Color(0xFFE3E3E3)
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = Color.Gray)
        }
    }
}

@Composable
private fun WarningTickerBar(
    isBluetoothConnected: Boolean,
    isFanOn: Boolean?,
    adminCabinets: List<AdminCabinet>,
    medicineSlots: List<MedicineSlot>,
    modifier: Modifier = Modifier
) {
    val medicineBySlot = remember(medicineSlots) { medicineSlots.associateBy { it.slotId } }
    val hasDepletedStock = remember(adminCabinets, medicineBySlot) {
        adminCabinets.any { cabinet ->
            if (!cabinet.isActive || cabinet.totalStatus != totalStatus.EMPTY) {
                return@any false
            }
            val medicineName = medicineBySlot[cabinet.slotIndex]?.medicine?.name?.trim().orEmpty()
            medicineName.isNotEmpty() && !medicineName.startsWith("약품 #")
        }
    }

    val warnings = remember(isBluetoothConnected, isFanOn, hasDepletedStock) {
        buildList {
            if (!isBluetoothConnected) add("센서 확인 필요")
            if (isFanOn == true) add("고온 다습 경고")
            if (hasDepletedStock) add("약품 재고 소진")
        }
    }

    var warningIndex by remember { mutableStateOf(0) }
    LaunchedEffect(warnings) {
        warningIndex = 0
        if (warnings.size > 1) {
            while (true) {
                delay(2.seconds)
                warningIndex = (warningIndex + 1) % warnings.size
            }
        }
    }

    val hasWarning = warnings.isNotEmpty()
    val currentMessage = warnings.getOrNull(warningIndex) ?: "정상"

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = if (hasWarning) Color.Red else Color(0xFF00C853),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))

            AnimatedContent(
                targetState = currentMessage,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
                transitionSpec = {
                    (slideInVertically(initialOffsetY = { it }) + fadeIn()) togetherWith
                            (slideOutVertically(targetOffsetY = { -it }) + fadeOut())
                },
                label = "warningTicker"
            ) { message ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = message,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasWarning) Color(0xFFC62828) else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "관리자 메인 페이지",
    device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
fun ManagerMainPreview() {
    Capstone_TabletTheme {
        AppScaffold(title = SCAFFOLD_TITLE) { paddingValues ->
            ManagerMainContent(
                userName = MockDataSource.DEBUG_USER_NAME,
                userRole = MockDataSource.DEBUG_USER_ROLE_ADMIN,
                medicineSlots = MockDataSource.DEBUG_MEDICINE_SLOTS,
                userCabinets = MockDataSource.DEBUG_USER_CABINETS,
                adminCabinets = MockDataSource.DEBUG_ADMIN_CABINETS,
                temp = MockDataSource.DEBUG_TEMP,
                humid = MockDataSource.DEBUG_HUMID,
                isFanOn = MockDataSource.DEBUG_FAN_STATUS,
                isAdminDoorLocked = MockDataSource.DEBUG_ADMIN_LOCKED,
                isBluetoothConnected = true,
                paddingValues = paddingValues,
                // 💡 빈 함수(콜백)는 arrayOf()가 아니라 중괄호 {} 로 줍니다.
                onToggleLock = {},
                onAdminToggle = {},
                onMedRegi = {},
                allUsers = listOf(
                    MockDataSource.DEBUG_USERINFO_PATIENT_1,
                    MockDataSource.DEBUG_USERINFO_PATIENT_2
                )
            )
        }
    }
}