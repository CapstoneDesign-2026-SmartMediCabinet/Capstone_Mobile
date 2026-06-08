package com.inu.capstone_mobile.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.inu.capstone_mobile.data.models.MedicineSlot
import com.inu.capstone_mobile.data.source.MockDataSource
import com.inu.capstone_mobile.data.repository.CabinetRepository
import com.inu.capstone_mobile.ui.components.AdminNoticeBanner
import com.inu.capstone_mobile.ui.components.AppScaffold
import com.inu.capstone_mobile.ui.components.BaseCard
import com.inu.capstone_mobile.ui.components.rememberAdminNoticeState
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme

private const val SCAFFOLD_TITLE = "약품 등록"

@Composable
fun MedRegi(
    medicineInfo: MedicineInfoUi,
    cameraUrl: String = "192.168.137.133:5003",
    medicineSlots: List<MedicineSlot> = emptyList(),
    slotOccupiedStates: List<Boolean> = List(3) { false },
    isBluetoothConnected: Boolean = false,
    onRegisterAttempt: () -> Unit,
    onConfirm: (editedName: String, selectedSlot: Int) -> Unit,
    onCancel: () -> Unit
) {
    AppScaffold(
        title = SCAFFOLD_TITLE,
        onBackClick = onCancel
    ) { paddingValues ->
        MedRegiContent(
            medicineInfo = medicineInfo,
            cameraUrl = cameraUrl,
            medicineSlots = medicineSlots,
            slotOccupiedStates = slotOccupiedStates,
            isBluetoothConnected = isBluetoothConnected,
            paddingValues = paddingValues,
            onRegisterAttempt = onRegisterAttempt,
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
}

@Composable
fun MedRegiContent(
    medicineInfo: MedicineInfoUi,
    cameraUrl: String,
    medicineSlots: List<MedicineSlot>,
    slotOccupiedStates: List<Boolean>,
    isBluetoothConnected: Boolean,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onRegisterAttempt: () -> Unit,
    onConfirm: (editedName: String, selectedSlot: Int) -> Unit,
    onCancel: () -> Unit
) {
    // 약품명 편집 상태 - 새 약품이 인식될 때마다 초기화
    var editedName by remember { mutableStateOf(medicineInfo.name) }
    var showSlotDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(medicineInfo.name) { editedName = medicineInfo.name }
    val normalizedSlotStates = remember(slotOccupiedStates) {
        if (slotOccupiedStates.isEmpty()) List(3) { false } else slotOccupiedStates
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
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (noticeState.isVisible) {
            AdminNoticeBanner(notices = noticeState.notices)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BaseCard(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.95f)
                    .blur(if (showSlotDialog) 10.dp else 0.dp),
                title = ""
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                    Text("카메라", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        CameraWebView(url = cameraUrl)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onRegisterAttempt() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("등록 시도")
                    }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 우측: 약품 정보 (약품명만 편집 가능)
                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .padding(horizontal = 28.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            RegiInfoRow(label = "약품 고유번호", value = medicineInfo.medicine_Id)
                            // 약품명: 편집 가능
                            EditableRegiInfoRow(
                                label = "약품명",
                                value = editedName,
                                onValueChange = { editedName = it }
                            )
                            RegiInfoRow(label = "제조사", value = medicineInfo.manufacturer)
                            RegiInfoRow(label = "약품 종류", value = medicineInfo.type)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showSlotDialog = true },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(52.dp)
                            ) {
                                Text("예")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(52.dp)
                            ) {
                                Text("아니오")
                            }
                        }
                    }
                }
            }
        }

        if (showSlotDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
            )
        }

        if (showSlotDialog) {
            Dialog(onDismissRequest = { showSlotDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "보관하실 보관함을 선택하세요",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            normalizedSlotStates.forEachIndexed { index, isOccupied ->
                                val boxColor = if (isOccupied) Color(0xFFBDBDBD) else Color(0xFF4CAF50)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(boxColor, RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (isOccupied) {
                                                Toast.makeText(
                                                    context,
                                                    "이미 사용중인 보관함입니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                showSlotDialog = false
                                                onConfirm(editedName, index + 1)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = { showSlotDialog = false }) {
                                Text("취소")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun CameraWebView(url: String) {
    val isPreview = LocalInspectionMode.current
    val normalizedUrl = remember(url) { normalizeWebUrl(url) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    if (isPreview) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F3F3)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "카메라 WebView Preview", color = Color.Gray)
        }
        return
    }

    DisposableEffect(Unit) {
        onDispose {
            // MedRegi 화면을 벗어날 때 WebView 연결을 명시적으로 끊어
            // Flask의 generate_frames() finally 블록이 최대한 빨리 실행되도록 유도한다.
            webViewRef?.releaseCameraStream()
            webViewRef = null
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewRef = this
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                loadUrl(normalizedUrl)
            }
        },
        update = { webView ->
            webViewRef = webView
            if (webView.url != normalizedUrl) {
                webView.loadUrl(normalizedUrl)
            }
        }
    )
}

private fun normalizeWebUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return "about:blank"
    return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        trimmed
    } else {
        "http://$trimmed"
    }
}

private fun WebView.releaseCameraStream() {
    runCatching { stopLoading() }
    runCatching { loadUrl("about:blank") }
    runCatching { onPause() }
    runCatching { clearHistory() }
    runCatching { removeAllViews() }
    runCatching { destroy() }
}

@Composable
private fun RegiInfoRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(
            modifier = Modifier.padding(top = 10.dp),
            thickness = 0.5.dp,
            color = Color(0xFFE0E0E0)
        )
    }
}

@Composable
private fun EditableRegiInfoRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Preview(
    showBackground = true,
    name = " 약품 등록 페이지",
    device = "spec:width=1280dp,height=800dp,dpi=240"
 )
@Composable
fun MedRegiPreview(){
    Capstone_TabletTheme {
        MedRegi(
            medicineInfo = MedicineInfoUi(
                medicine_Id = MockDataSource.DEBUG_MED_INFO_ID,
                name = MockDataSource.DEBUG_MED_INFO_NAME,
                manufacturer = MockDataSource.DEBUG_MED_INFO_MANUFACTURER,
                type = MockDataSource.DEBUG_MED_INFO_TYPE,
                imageUrl = MockDataSource.DEBUG_MED_INFO_IMAGE_URL
            ),
            slotOccupiedStates = listOf(false, true, false),
            onRegisterAttempt = {},
            onConfirm = { _, _ -> },
            onCancel = {}
        )
    }
}