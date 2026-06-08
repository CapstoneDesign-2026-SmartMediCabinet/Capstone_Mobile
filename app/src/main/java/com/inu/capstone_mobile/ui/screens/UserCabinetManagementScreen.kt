package com.inu.capstone_mobile.ui.screens


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.inu.capstone_mobile.data.models.MedicineSlot
import com.inu.capstone_mobile.data.models.User
import com.inu.capstone_mobile.data.repository.CabinetRepository
import com.inu.capstone_mobile.data.source.MockDataSource
import com.inu.capstone_mobile.ui.components.AdminNoticeBanner
import com.inu.capstone_mobile.ui.components.AppScaffold
import com.inu.capstone_mobile.ui.components.rememberAdminNoticeState
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme
import com.inu.capstone_mobile.viewmodel.UserCabinetManageViewModel

private const val SCAFFOLD_TITLE = "사용자 보관함 관리"

data class ManagedCabinetUi(
    val slotIndex: Int,
    val cabinetNo: Int,
    val assignedUserId: String? = null,
    val isActive: Boolean = true,
    val availableAction: String = ""
)


@Composable
fun UserCabinetManagementScreen(
    viewModel: UserCabinetManageViewModel = viewModel(),
    medicineSlots: List<MedicineSlot> = emptyList(),
    isBluetoothConnected: Boolean = false,
    onBack: () -> Unit = {}
) {
    // 뷰모델의 StateFlow 데이터를 UI 상태로 변환
    val patients by viewModel.patients.collectAsState()
    val cabinets by viewModel.managedCabinets.collectAsState()
    val selectedPatientId by viewModel.selectedPatientId.collectAsState()

    AppScaffold(title = SCAFFOLD_TITLE, onBackClick = onBack) { paddingValues ->
        UserCabinetManagementContent(
            patients = patients,
            cabinets = cabinets,
            selectedPatientId = selectedPatientId, // 현재 선택된 환자 ID 전달
            medicineSlots = medicineSlots,
            isBluetoothConnected = isBluetoothConnected,
            paddingValues = paddingValues,
            onPatientSelect = { userId -> viewModel.selectPatient(userId) }, // 클릭 시 뷰모델에 알림
            onConfirmAction = { cabinetNo ->
                viewModel.handleCabinetAction(cabinetNo) // 팝업 확인 시 뷰모델로 액션 전달
            }
        )
    }
}

@Composable
fun UserCabinetManagementContent(
    patients: List<User>,
    cabinets: List<ManagedCabinetUi>,
    selectedPatientId: String?,
    medicineSlots: List<MedicineSlot> = emptyList(),
    isBluetoothConnected: Boolean = false,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onPatientSelect: (String) -> Unit = {},
    onConfirmAction: (cabinetNo: Int,) -> Unit = { _ -> }
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedCabinetNo by remember { mutableStateOf<Int?>(null) }
    var selectedActionText by remember { mutableStateOf("") }

    val assignedCabinetNoByUserId = remember(cabinets) {
        cabinets.filter { it.isActive && !it.assignedUserId.isNullOrBlank() }
            .associate { it.assignedUserId!! to it.cabinetNo }
    }

    val isFanOn by CabinetRepository.isFanOn.collectAsState()
    val adminCabinets by CabinetRepository.adminCabinets.collectAsState()
    val noticeState = rememberAdminNoticeState(
        isBluetoothConnected = isBluetoothConnected,
        isFanOn = isFanOn,
        adminCabinets = adminCabinets,
        medicineSlots = medicineSlots
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFDDEAF8))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (noticeState.isVisible) {
            AdminNoticeBanner(notices = noticeState.notices)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .then(if (showConfirmDialog) Modifier.blur(10.dp) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(38.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("환자 목록", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(patients, key = { it.userId }) { patient ->
                            val assignedNo = assignedCabinetNoByUserId[patient.userId]?.toString().orEmpty()
                            // 현재 환자가 선택된 환자인지 확인
                            val isSelected = patient.userId == selectedPatientId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 선택 시 배경색 하이라이트 및 클릭 이벤트
                                    .background(
                                        color = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onPatientSelect(patient.userId) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = patient.userName,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                    fontSize = 28.sp,
                                    //  선택 시 글씨체를 더 굵고 파랗게 강조
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFF1976D2) else Color.Black
                                )
                                Text(text = assignedNo, color = Color.Gray, fontSize = 16.sp)
                            }
                            HorizontalDivider(color = Color(0xFFEAEAEA))
                        }
                    }
                }
            }
            //오른쪽 사물함
            Card(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = "보관함 배정 상태",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        items(cabinets, key = { it.cabinetNo }) { cabinet ->
                            val borderColor: Color
                            val bottomLabel: String
                            val action = cabinet.availableAction

                            when {
                                // [조건 1] 사물함이 비활성화된 상태
                                !cabinet.isActive -> {
                                    borderColor = Color.Gray
                                    bottomLabel = "비활성화됨"
                                }

                                // [조건 2] 사물함에 이미 환자가 배정된 상태
                                !cabinet.assignedUserId.isNullOrBlank() -> {
                                    borderColor = Color.Red
                                    bottomLabel = patients.find { it.userId == cabinet.assignedUserId }?.
                                    userName
                                        ?: cabinet.assignedUserId
                                }

                                // [조건 3] 사물함이 켜져 있고 '비어 있는' 상태
                                else -> {
                                    borderColor = Color(0xFF00C853)
                                    bottomLabel = "empty"
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .aspectRatio(1f / 0.68f)
                                    .border(width = 8.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
                                    .background(Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (action == "불가") return@clickable
                                        selectedCabinetNo = cabinet.cabinetNo
                                        selectedActionText = action
                                        showConfirmDialog = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cabinet.cabinetNo.toString(),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }

                                    // 상태 표시 박스 (배치용 외부 + 배경용 내부 레이어)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val statusText = when {
                                            !cabinet.isActive -> ""
                                            !cabinet.assignedUserId.isNullOrBlank() -> "사용중"
                                            else -> "사용 가능"
                                        }
                                        val statusColor = when {
                                            !cabinet.isActive -> Color.Gray
                                            !cabinet.assignedUserId.isNullOrBlank() -> Color.Red
                                            else -> Color(0xFF00C853)
                                        }

                                        // 배경용 내부 Box (텍스트 폭에 맞춤)
                                        if (statusText.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = when {
                                                            !cabinet.isActive -> Color.Transparent
                                                            !cabinet.assignedUserId.isNullOrBlank() -> Color(0xFFFFCDD2)
                                                            else -> Color(0xFFC8E6C9)
                                                        },
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = statusText,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(2f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val isDisabledLabel = bottomLabel == "비활성화됨"
                                        Text(
                                            text = bottomLabel,
                                            modifier = Modifier.padding(start = 6.dp),
                                            fontSize = if (isDisabledLabel) 26.sp else 51.sp,
                                            fontWeight = if (isDisabledLabel) FontWeight.Normal else FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showConfirmDialog && selectedCabinetNo != null) {
            Dialog(onDismissRequest = { showConfirmDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${selectedActionText} 하시겠습니까?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                            IconButton(
                                onClick = {
                                    Log.d("CabinetManage", "버튼 클릭! CabinetNo: $selectedCabinetNo, Action: $selectedActionText")
                                    selectedCabinetNo?.let { onConfirmAction(it) }
                                    showConfirmDialog = false
                                }
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "확인", tint = Color(0xFF00C853))
                            }
                            IconButton(onClick = { showConfirmDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "취소", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🌟 프리뷰를 위한 빈 ViewModel 모의 연결
@Preview(showBackground = true, name = "사용자 보관함 관리 화면", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun UserCabinetManagementScreenPreview() {
    val mockUserIds = MockDataSource.DEBUG_USER_CABINETS.map { it.userId }
    val previewCabinets = List(6) { index ->
        ManagedCabinetUi(
            slotIndex = index,
            cabinetNo = index + 1,
            assignedUserId = if (index < 2) mockUserIds.getOrNull(index) else null,
            isActive = index < 2 ,
            availableAction = if(index == 3) "사용자 등록" else "불가"
        )
    }

    Capstone_TabletTheme {
        UserCabinetManagementContent(
            patients = listOf(
                MockDataSource.DEBUG_USERINFO_PATIENT_1,
                MockDataSource.DEBUG_USERINFO_PATIENT_2
            ),
            cabinets = previewCabinets,
            selectedPatientId = MockDataSource.DEBUG_USERINFO_PATIENT_1.userId, // 프리뷰에서 선택된 상태 보여주기
            medicineSlots = MockDataSource.DEBUG_MEDICINE_SLOTS,
            isBluetoothConnected = false
        )
    }
}