package com.inu.capstone_mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inu.capstone_mobile.data.models.AdminCabinet
import com.inu.capstone_mobile.data.models.Medicine
import com.inu.capstone_mobile.data.models.MedicineSlot
import com.inu.capstone_mobile.data.models.Prescription
import com.inu.capstone_mobile.data.models.User
import com.inu.capstone_mobile.data.models.totalStatus
import com.inu.capstone_mobile.data.repository.CabinetRepository
import com.inu.capstone_mobile.ui.components.AdminNoticeBanner
import com.inu.capstone_mobile.ui.components.AppScaffold
import com.inu.capstone_mobile.ui.components.rememberAdminNoticeState
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme
import com.inu.capstone_mobile.viewmodel.MedManageViewModel
import kotlin.collections.find
import kotlin.collections.isNotEmpty



// ---------------------------------------------------------------------------
// 🌟 메인 스크린 래퍼
// ---------------------------------------------------------------------------
@Composable
fun MedManageScreen(
    viewModel: MedManageViewModel = viewModel(), // 뷰모델 주입
    isBluetoothConnected: Boolean = false,
    onMedicineClick: (String) -> Unit,           // MedInfo 화면 이동용 (NavHost에서 처리)
    onBack: () -> Unit = {}
) {
    val patients by viewModel.patients.collectAsState()
    val cabinets by viewModel.cabinets.collectAsState()
    val prescriptions by viewModel.selectedPatientPrescriptions.collectAsState()
    val selectedPatientId by viewModel.selectedPatientId.collectAsState()
    AppScaffold(title = "약품 관리", onBackClick = onBack) { paddingValues ->
        MedManageContent(
            patients = patients,
            cabinets = cabinets,
            selectedPatientId = selectedPatientId,
            prescriptions = prescriptions,
            onPatientSelect = { userId -> viewModel.selectPatient(userId) },
            onConfirmAction = { slotIndex -> viewModel.handleCabinetActiveAction(slotIndex) },
            onConfirmRemoveAction = { slotIndex -> viewModel.handleCabinetMedicineRemoveAction(slotIndex) },
            isBluetoothConnected = isBluetoothConnected,
            onMedicineClick = onMedicineClick,
            paddingValues = paddingValues
        )
    }
}

// ---------------------------------------------------------------------------
// 🌟 실제 콘텐츠 화면
// ---------------------------------------------------------------------------
@Composable
fun MedManageContent(
    patients: List<User>,
    cabinets: List<Pair<AdminCabinet?, MedicineSlot?>>,
    selectedPatientId: String?,
    prescriptions: List<Prescription>,
    onPatientSelect: (String) -> Unit,
    onMedicineClick: (String) -> Unit,
    onConfirmAction: (Int) -> Unit,
    onConfirmRemoveAction: (Int) -> Unit,
    isBluetoothConnected: Boolean = false,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedSlotIndex by remember { mutableStateOf<Int?>(null) }
    var selectedActionText by remember { mutableStateOf("") }
    var selectedDialogMode by remember { mutableStateOf("toggle") }

    val isFanOn by CabinetRepository.isFanOn.collectAsState()
    val noticeState = rememberAdminNoticeState(
        isBluetoothConnected = isBluetoothConnected,
        isFanOn = isFanOn,
        cabinets = cabinets
    )

    if (showConfirmDialog && selectedSlotIndex != null) {
        Dialog(onDismissRequest = {
            showConfirmDialog = false
            selectedSlotIndex = null
            selectedActionText = ""
        }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "${selectedSlotIndex!! + 1}번 보관함${selectedActionText}하시겠습니까?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                        IconButton(onClick = {
                            selectedSlotIndex?.let { slotIndex ->
                                if (selectedDialogMode == "remove") {
                                    onConfirmRemoveAction(slotIndex)
                                } else {
                                    onConfirmAction(slotIndex)
                                }
                            }
                            showConfirmDialog = false
                            selectedSlotIndex = null
                            selectedActionText = ""
                            selectedDialogMode = "toggle"
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "확인", tint = Color(0xFF00C853))
                        }
                        IconButton(onClick = {
                            showConfirmDialog = false
                            selectedSlotIndex = null
                            selectedActionText = ""
                            selectedDialogMode = "toggle"
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "취소", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFDDEAF8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .then(if (showConfirmDialog) Modifier.blur(10.dp) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                    Text("환자 목록",
                        modifier = Modifier.padding(start = 25.dp),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(patients, key = { it.userId }) { patient ->
                            val isSelected = patient.userId == selectedPatientId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onPatientSelect(patient.userId) }
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = patient.userName,
                                    modifier = Modifier.padding(start = 15.dp),
                                    fontSize = 24.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFF1976D2) else Color.Black
                                )
                            }
                            HorizontalDivider(color = Color(0xFFEAEAEA))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(2.5f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Text(
                            text = "처방전 및 일일 약품 소요량",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedPatientId == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("환자를 선택해주세요", color = Color.Gray.copy(alpha = 0.7f), fontSize = 20.sp)
                            }
                        } else if (prescriptions.isNotEmpty()) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(prescriptions) { rx ->
                                    val relatedCabinet = cabinets.find { it.second?.medicine?.id == rx.pillId }
                                    val medicineName = relatedCabinet?.second?.medicine?.name ?: "약품 #${rx.pillId}"
                                    val statusText = when (relatedCabinet?.first?.totalStatus) {
                                        totalStatus.ENOUGH -> "충분"
                                        totalStatus.LOW -> "부족"
                                        totalStatus.EMPTY -> "없음"
                                        null -> "미등록"
                                    }
                                    val statusColor = when (relatedCabinet?.first?.totalStatus) {
                                        totalStatus.ENOUGH -> Color(0xFF00C853)
                                        totalStatus.LOW -> Color(0xFFFF9800)
                                        totalStatus.EMPTY, null -> Color.Red
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = medicineName,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(2f).padding(start = 6.dp)
                                        )
                                        Text(
                                            text = "하루 ${rx.dailyTimings.size}회",
                                            fontSize = 20.sp,
                                            color = Color.DarkGray,
                                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "보관함: $statusText", color = statusColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("등록된 처방전이 없습니다.", color = Color.Gray, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Text("관리자 약품 보관함 상태", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(cabinets, key = { index, _ -> index }) { index, cabinetPair ->
                                val adminCabinet = cabinetPair.first
                                val medicineSlot = cabinetPair.second
                                val slotIndex = adminCabinet?.slotIndex ?: medicineSlot?.slotId ?: index
                                val isActive = adminCabinet?.isActive ?: false
                                val status = adminCabinet?.totalStatus ?: totalStatus.EMPTY
                                val medicineName = medicineSlot?.medicine?.name

                                val borderColor = if (!isActive) Color.LightGray else when (status) {
                                    totalStatus.ENOUGH -> Color(0xFF00C853)
                                    totalStatus.LOW -> Color(0xFFFF9800)
                                    totalStatus.EMPTY -> Color.Red
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.5f)
                                        .border(4.dp, borderColor, RoundedCornerShape(12.dp))
                                        .background(if (isActive) Color.Transparent else Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (!isActive) return@clickable
                                            medicineSlot?.medicine?.id?.let { onMedicineClick(it.toString()) }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "보관함 ${slotIndex + 1}번",
                                                modifier = Modifier.padding(start = 12.dp),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )
                                            if (adminCabinet != null) {
                                                Switch(
                                                    checked = isActive,
                                                    onCheckedChange = { _ ->
                                                        selectedSlotIndex = slotIndex
                                                        selectedActionText = if (isActive) "을 비활성화 " else "을 활성화 "
                                                        selectedDialogMode = "toggle"
                                                        showConfirmDialog = true
                                                    },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1976D2))
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        val displayName = medicineName?.let { name ->
                                            if (name.length > 6) name.take(6) + "···" else name
                                        } ?: "비어 있음"

                                        Text(
                                            text = displayName,
                                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isActive) Color.Gray else Color.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (isActive && medicineName != null) {
                                            val badgeText = when (status) {
                                                totalStatus.ENOUGH -> "충분"
                                                totalStatus.LOW -> "부족"
                                                totalStatus.EMPTY -> "없음"
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(start = 15.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(borderColor, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(text = badgeText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                IconButton(onClick = {
                                                    selectedSlotIndex = slotIndex
                                                    selectedActionText = "의 약품을 제거"
                                                    selectedDialogMode = "remove"
                                                    showConfirmDialog = true
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Default.RemoveCircleOutline,
                                                        contentDescription = "약품 해제",
                                                        tint = Color.Red
                                                    )
                                                }
                                            }
                                        } else if (!isActive) {
                                            Text("비활성화됨", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = noticeState.isVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Box(modifier = Modifier.widthIn(max = 600.dp)) {
                AdminNoticeBanner(notices = noticeState.notices)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 🌟 프리뷰 (테스트용 데이터)
// ---------------------------------------------------------------------------
@Preview(showBackground = true, name = "약품 관리 화면 (태블릿)", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun MedManageScreenPreview() {
    // Mock 데이터 정적 구성
    val mockPatients = listOf(
        User(
            userId = "user01",
            userName = "김가람",
            userRole = "환자",
            userRoleId = "USER",
            prescriptions = listOf(
                Prescription(pillId = 0, dailyTimings = listOf("아침", "점심", "저녁"), startDate = "2026-04-11", endDate = "2026-04-13"),
                Prescription(pillId = 1, dailyTimings = listOf("저녁"), startDate = "2026-04-11", endDate = "2026-04-20")
            )
        ),
        User(
            userId = "user02",
            userName = "이아름",
            userRole = "환자",
            userRoleId = "USER",
            prescriptions = emptyList()
        )
    )

    val mockCabinets: List<Pair<AdminCabinet?, MedicineSlot?>> = listOf(
        Pair(
            AdminCabinet(slotIndex = 0, totalStatus = totalStatus.ENOUGH, isActive = true),
            MedicineSlot(slotId = 0, medicine = Medicine(0, "타이레놀"), userId = "user01")
        ),
        Pair(
            AdminCabinet(slotIndex = 1, totalStatus = totalStatus.LOW, isActive = true),
            MedicineSlot(slotId = 1, medicine = Medicine(1, "자큐보정"), userId = "user02")
        ),
        Pair(
            AdminCabinet(slotIndex = 2, totalStatus = totalStatus.EMPTY, isActive = true),
            MedicineSlot(slotId = 2, medicine = Medicine(2, "비타민C"), userId = "user01")
        ),
        Pair(null, null),
        Pair(null, null),
        Pair(null, null)
    )

    Capstone_TabletTheme {
        AppScaffold(title = "약품 관리") { paddingValues ->
            MedManageContent(
                patients = mockPatients,
                cabinets = mockCabinets,
                selectedPatientId = "user01",
                prescriptions = mockPatients[0].prescriptions,
                onPatientSelect = {},
                onMedicineClick = {},
                onConfirmAction = { },
                onConfirmRemoveAction = { },
                isBluetoothConnected = true,
                paddingValues = paddingValues
            )
        }
    }
}