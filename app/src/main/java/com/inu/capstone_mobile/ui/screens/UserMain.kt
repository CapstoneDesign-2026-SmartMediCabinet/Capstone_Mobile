package com.inu.capstone_mobile.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.atMostWrapContent
import com.inu.capstone_mobile.data.models.Prescription
import com.inu.capstone_mobile.data.models.User
import com.inu.capstone_mobile.data.models.UserCabinet
import com.inu.capstone_mobile.data.source.MockDataSource
import com.inu.capstone_mobile.data.remote.bluetooth.BluetoothManager
import com.inu.capstone_mobile.ui.components.AppScaffold
import com.inu.capstone_mobile.ui.components.BaseCard
import com.inu.capstone_mobile.ui.components.StatusDot
import com.inu.capstone_mobile.ui.components.UserCabinetGridCard
import com.inu.capstone_mobile.ui.components.UserCard
import com.inu.capstone_mobile.viewmodel.UserViewModel
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

private const val SCAFFOLD_TITLE = "사용자 메인 화면"
private val USER_MAIN_INACTIVE_CABINET_MAX_HEIGHT = 220.dp
private val USER_MAIN_ACTIVE_CABINET_MIN_HEIGHT = 180.dp

@Composable
fun UserMain(
    viewModel: UserViewModel,
    bluetoothManager: BluetoothManager,
    isBluetoothConnected: Boolean,
    onLogout: () -> Unit,
    onFaceRegister: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
) {
    val activity: Activity? = LocalActivity.current
    val userCabinets by viewModel.userCabinets.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentTimeSlot by viewModel.currentTimeSlot.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val medicineSlots by viewModel.medicineSlots.collectAsState()

    val viewConfiguration = LocalViewConfiguration.current
    val customViewConfiguration = object : ViewConfiguration by viewConfiguration {
        override val longPressTimeoutMillis: Long get() = 1500L
    }

    CompositionLocalProvider(LocalViewConfiguration provides customViewConfiguration) {
        AppScaffold(
            title = SCAFFOLD_TITLE,
            onFaceRegisterClick = onFaceRegister,
            onLogoutClick = onLogout,
            onExitClick = { activity?.finish() }
        ) { paddingValues ->
            UserMainContent(
                userName = currentUser?.userName ?: "사용자",
                userRole = currentUser?.userRole ?: "환자",
                currentUserId = currentUser?.userId ?: "",
                prescriptions = currentUser?.prescriptions ?: emptyList(),
                userCabinets = userCabinets,
                paddingValues = paddingValues,
                onToggleLock = { userId -> viewModel.toggleUserCabinetLockWithBT(userId, bluetoothManager) },
                onFaceRegister = onFaceRegister,
                allUsers = allUsers,
                isBluetoothConnected = isBluetoothConnected,
                medicineNameById = medicineSlots.associate { it.medicine.id to it.medicine.name }
            )
        }
    }
}

@Composable
fun UserMainContent(
    userName: String,
    userRole: String,
    currentUserId: String,
    //currentTimeSlot: String,
    prescriptions: List<Prescription> = emptyList(),
    userCabinets: List<UserCabinet> = emptyList(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onToggleLock: (String) -> Unit = {},
    onFaceRegister: () -> Unit = {},
    //onHistoryClick: () -> Unit = {},
    allUsers: List<User> = emptyList(),
    isBluetoothConnected: Boolean = false,
    medicineNameById: Map<Int, String> = emptyMap()
) {
    val todayDate = LocalDate.now()
    val yesterdayStr = todayDate.minusDays(1).toString()
    val todayStr = todayDate.toString()
    val tomorrowStr = todayDate.plusDays(1).toString()

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFDDEAF8))
            .padding(24.dp)
    ) {
        val (userCard, clockStatusCard, faceRegisterBtn, scheduleCard, bottomRow, middleText, minSpacer, leftSpacer) = createRefs()

        val vGuideline1 = createGuidelineFromStart(0.3f)
        Spacer(
            modifier = Modifier.constrainAs(minSpacer) {
                start.linkTo(parent.start)
                end.linkTo(vGuideline1)
                width = Dimension.fillToConstraints
            }
        )
        val leftSectionBarrier = createEndBarrier(userCard, clockStatusCard, minSpacer)
        val hGuideline = createGuidelineFromTop(0.45f)
        // val hGuideline2 = createGuidelineFromTop(0.55f)
        Spacer(
            modifier = Modifier.constrainAs(leftSpacer) {
                start.linkTo(vGuideline1)
                width = Dimension.value(2.dp)
            }
        )

        // [좌측 상단] 인사 카드
        UserCard(
            modifier = Modifier.constrainAs(userCard) {
                top.linkTo(parent.top, 10.dp)
                start.linkTo(parent.start, 20.dp)
                end.linkTo(vGuideline1, margin = 10.dp)
                width = Dimension.preferredWrapContent.atMostWrapContent
                horizontalBias = 0f
                height = Dimension.ratio("H, 1:0.6")
            },
            title = "안녕하세요,"
        ) {
            Column(
                modifier = Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(userRole, fontSize = 14.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${userName} 님", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // [좌측 하단] 시계 + 블루투스 상태 카드 (기존 영역의 약 70% 높이)
        Box(
            modifier = Modifier.constrainAs(clockStatusCard) {
                top.linkTo(userCard.bottom, 12.dp)
                start.linkTo(userCard.start)
                bottom.linkTo(hGuideline, 12.dp)
                width = Dimension.percent(0.15f)
                height = Dimension.fillToConstraints
            }
        ) {
            UserClockStatusCard(
                isConnected = isBluetoothConnected,
                onRefresh = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .align(Alignment.CenterStart)
            )
        }

        // [중앙 상단] 안면 인식 등록 버튼 (관리자 버튼 대비 절반 크기)
        Button(
            onClick = onFaceRegister,
            modifier = Modifier.constrainAs(faceRegisterBtn) {
                top.linkTo(parent.top, 8.dp)
                bottom.linkTo(hGuideline, 12.dp)
                start.linkTo(clockStatusCard.end, 8.dp)
                end.linkTo(scheduleCard.start, 10.dp)
                width = Dimension.value(140.dp)
                height = Dimension.ratio("1:1.4")
            },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 18.dp, horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF00897B))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("안면 정보", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text("등록", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // [중앙] 주간 복약 일정
        BaseCard(
            modifier = Modifier.constrainAs(scheduleCard) {
                top.linkTo(parent.top)
                start.linkTo(leftSectionBarrier, 40.dp)
                end.linkTo(parent.end, 20.dp)
                bottom.linkTo(hGuideline, 12.dp)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            },
            title = "주간 복약 안내"
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 2.dp,
                    color = Color.Black
                )

                if (prescriptions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("복약 일정이 없습니다.", color = Color.Gray)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                           .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DailyScheduleColumn("어제", yesterdayStr, prescriptions, medicineNameById, Modifier.weight(1f))
                        VerticalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )
                        DailyScheduleColumn("오늘", todayStr, prescriptions, medicineNameById, Modifier.weight(1f))
                        VerticalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )
                        DailyScheduleColumn("내일", tomorrowStr, prescriptions, medicineNameById, Modifier.weight(1f))
                    }
                }
            }
        }

        // [중앙 구분선]
        Column(
            modifier = Modifier
                .constrainAs(middleText) {
                    top.linkTo(scheduleCard.bottom, 8.dp)
                    start.linkTo(parent.start, 24.dp)
                    end.linkTo(parent.end, 24.dp)
                    width = Dimension.fillToConstraints
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(thickness = 5.dp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("환자 약품장", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }

        // [하단] 약품장 그리드
        Row(
            modifier = Modifier
                .constrainAs(bottomRow) {
                    top.linkTo(middleText.bottom, 16.dp)
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
                val displayUserName = allUsers.find { it.userId == cabinet.userId }?.userName ?: "사용자#${index + 1}"
                val isCurrentUserCabinet = cabinet.userId == currentUserId
                val nonCurrentUserBlur = if (isCurrentUserCabinet) Modifier else Modifier.blur(10.dp)

                Column(modifier = Modifier.width(340.dp)) {
                    if (!cabinet.isActive) {
                        BaseCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.85f)
                                .heightIn(max = USER_MAIN_INACTIVE_CABINET_MAX_HEIGHT)
                                .then(nonCurrentUserBlur),
                            title = displayUserName
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("비활성화됨", color = Color.Gray, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(nonCurrentUserBlur)
                        ) {
                            UserCabinetGridCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = USER_MAIN_ACTIVE_CABINET_MIN_HEIGHT),
                                title = displayUserName,
                                cabinet = cabinet
                            )
                            StatusDot(
                                isHealthy = isBluetoothConnected && cabinet.isLocked != null,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        LockStatusDp(
                            isLocked = if (isBluetoothConnected) cabinet.isLocked else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(nonCurrentUserBlur),
                            enabled = isCurrentUserCabinet && isBluetoothConnected,
                            onToggleLock = { onToggleLock(cabinet.userId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserClockStatusCard(
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
                UserBluetoothStatusContent(
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
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = currentTime.format(meridiemFormatter),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                    Text(
                        text = currentTime.format(timeFormatter),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun UserBluetoothStatusContent(
    isConnected: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = if (isConnected) Color(0xFF00C853) else Color.LightGray,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isConnected) "연결됨" else "오프라인",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isConnected) Color.Black else Color.Gray,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(4.dp))
        VerticalDivider(
            modifier = Modifier.height(14.dp),
            thickness = 1.dp,
            color = Color(0xFFE3E3E3)
        )
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(22.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LockStatusDp(
    isLocked: Boolean?,
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    onToggleLock: () -> Unit = {}
) {
    Card(
        modifier = modifier.combinedClickable(
            onClick = { if (enabled) onToggleLock() },
            onLongClick = { if (enabled) onToggleLock() }
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
                true -> Triple("이용 불가", Icons.Default.Lock, Color.Red)
                false -> Triple("이용 가능", Icons.Default.LockOpen, Color(0xFF00C853))
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
fun DailyScheduleColumn(
    title: String,
    targetDate: String,
    prescriptions: List<Prescription>,
    medicineNameById: Map<Int, String>,
    modifier: Modifier = Modifier
) {
    val target = parseIsoDateOrNull(targetDate)
    val filteredPrescriptions = prescriptions.filter { rx ->
        val start = parseIsoDateOrNull(rx.startDate)
        val end = parseIsoDateOrNull(rx.endDate)
        if (target != null && start != null && end != null) {
            !target.isBefore(start) && !target.isAfter(end)
        } else {
            // 날짜 포맷이 비표준이면 기존 문자열 비교로 폴백
            targetDate in rx.startDate..rx.endDate
        }
    }

    Column(
        modifier = modifier
            //.fillMaxHeight()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))

        if (filteredPrescriptions.isEmpty()) {
            Text("복약 일정 없음", color = Color.Gray, fontSize = 14.sp)
        } else {
            filteredPrescriptions.forEach { rx ->
                val dailyCount = rx.dailyTimings.size

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val medicineName = medicineNameById[rx.pillId]?.takeIf { it.isNotBlank() } ?: "약품 ${rx.pillId}"
                    val displayMedName = medicineName?.let { name ->
                        if (name.length > 6) {
                            name.take(6) + "···"
                        } else {
                            name
                        }
                    } ?: "약품 ${rx.pillId}"
                    Text(text = displayMedName, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(text = "1일 ${dailyCount}정", fontSize = 14.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)

                }
            }
        }
    }
}

private fun parseIsoDateOrNull(raw: String): LocalDate? {
    return try {
        LocalDate.parse(raw.trim())
    } catch (_: DateTimeParseException) {
        null
    }
}

@Preview(showBackground = true, name = "사용자 메인 페이지", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun UserMainPreview() {
    Capstone_TabletTheme {
        AppScaffold(title = "사용자 메인 화면") { paddingValues ->
            UserMainContent(
                userName = MockDataSource.DEBUG_USER_NAME,
                userRole = MockDataSource.DEBUG_USER_ROLE_NORMAL,
                currentUserId = MockDataSource.DEBUG_USERINFO_PATIENT_1.userId,
                prescriptions = MockDataSource.DEBUG_USERINFO_PATIENT_1.prescriptions,
               // currentTimeSlot = MockDataSource.DEBUG_TIME_LUNCH,
                userCabinets = MockDataSource.DEBUG_USER_CABINETS,
                paddingValues = paddingValues,
                allUsers = listOf(
                    MockDataSource.DEBUG_USERINFO_PATIENT_1,
                    MockDataSource.DEBUG_USERINFO_PATIENT_2
                ),
                isBluetoothConnected = true,
                medicineNameById = MockDataSource.DEBUG_MEDICINE_SLOTS.associate { it.medicine.id to it.medicine.name }
               // prescriptions = PillViewModel.DEBUG_USERINFO_PATIENT_1.prescriptions
            )
        }
    }
}