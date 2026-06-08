package com.inu.capstone_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inu.capstone_mobile.data.models.AdminCabinet
import com.inu.capstone_mobile.data.models.MedicineSlot
import com.inu.capstone_mobile.data.models.totalStatus
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

enum class AdminNoticeLevel {
    WARNING,
    CAUTION,
    NORMALIZED
}

data class AdminNotice(
    val message: String,
    val level: AdminNoticeLevel
)

@Stable
data class AdminNoticeState(
    val notices: List<AdminNotice> = emptyList(),
    val isVisible: Boolean = false
)

private object AdminNoticeMemory {
    private var announcedLowMedicineNames: Set<String> = emptySet()
    private var announcedMedicineWarnings: Set<String> = emptySet()
    private var lastFanOn: Boolean? = null

    @Synchronized
    fun syncResolvedLowMedicines(currentLowMedicineNames: List<String>) {
        announcedLowMedicineNames = announcedLowMedicineNames.intersect(currentLowMedicineNames.toSet())
    }

    @Synchronized
    fun consumeNewLowMedicines(currentLowMedicineNames: List<String>): List<String> {
        val currentLowSet = currentLowMedicineNames.toSet()
        val newLowSet = currentLowSet - announcedLowMedicineNames
        announcedLowMedicineNames = currentLowSet
        return currentLowMedicineNames.filter { it in newLowSet }
    }

    @Synchronized
    fun syncResolvedMedicineWarnings(currentMedicineWarnings: List<String>) {
        announcedMedicineWarnings = announcedMedicineWarnings.intersect(currentMedicineWarnings.toSet())
    }

    @Synchronized
    fun consumeNewMedicineWarnings(currentMedicineWarnings: List<String>): List<String> {
        val currentWarningSet = currentMedicineWarnings.toSet()
        val newWarningSet = currentWarningSet - announcedMedicineWarnings
        announcedMedicineWarnings = currentWarningSet
        return currentMedicineWarnings.filter { it in newWarningSet }
    }

    @Synchronized
    fun consumeFanNormalizedNotice(isFanOn: Boolean?): String? {
        val notice = if (lastFanOn == true && isFanOn == false) {
            "환풍기 가동을 중단합니다."
        } else {
            null
        }
        lastFanOn = isFanOn
        return notice
    }
}

@Composable
fun StatusDot(isHealthy: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = if (isHealthy) Color(0xFF00C853) else Color.Red,
            shape = CircleShape
        )
    )
}

@Composable
fun AdminNoticeBanner(
    notices: List<AdminNotice>,
    modifier: Modifier = Modifier
) {
    if (notices.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notices.forEach { notice ->
                val (accentColor, textColor, categoryText) = when (notice.level) {
                    AdminNoticeLevel.WARNING -> Triple(Color(0xFFD32F2F), Color(0xFFB71C1C), "경고")
                    AdminNoticeLevel.CAUTION -> Triple(Color(0xFFF9A825), Color(0xFF8A6D1F), "주의")
                    AdminNoticeLevel.NORMALIZED -> Triple(Color(0xFF2E7D32), Color(0xFF1B5E20), "")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(10.dp)
                            .background(color = accentColor, shape = CircleShape)
                    )

                    if (categoryText.isNotEmpty()) {
                        Text(
                            text = categoryText,
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "  |  ",
                            color = accentColor.copy(alpha = 0.45f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Text(
                        text = notice.message,
                        fontSize = 15.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AdminWarningBanner(
    warnings: List<String>,
    modifier: Modifier = Modifier
) {
    AdminNoticeBanner(
        notices = warnings.map { AdminNotice(message = it, level = AdminNoticeLevel.WARNING) },
        modifier = modifier
    )
}

@Composable
fun rememberAdminNoticeState(
    isBluetoothConnected: Boolean,
    isFanOn: Boolean?,
    cabinets: List<Pair<AdminCabinet?, MedicineSlot?>>, 
    popupDurationMillis: Long = 3500L,
    cautionDelayMillis: Long = 1000L
): AdminNoticeState {
    val medicineSlots = remember(cabinets) { cabinets.mapNotNull { it.second } }
    val emptyMedicineNames = remember(cabinets) {
        cabinets.mapNotNull { (adminCabinet, medicineSlot) ->
            if (adminCabinet?.totalStatus == totalStatus.EMPTY) {
                normalizeAdminNoticeMedicineName(medicineSlot?.medicine?.name)
            } else null
        }.distinct()
    }
    val lowMedicineNames = remember(cabinets) {
        cabinets.mapNotNull { (adminCabinet, medicineSlot) ->
            if (adminCabinet?.totalStatus == totalStatus.LOW) {
                normalizeAdminNoticeMedicineName(medicineSlot?.medicine?.name)
            } else null
        }.distinct()
    }

    val persistentWarnings = remember(isBluetoothConnected, isFanOn) {
        buildPersistentAdminWarnings(
            isBluetoothConnected = isBluetoothConnected,
            isFanOn = isFanOn
        )
    }

    val medicineWarnings = remember(medicineSlots, emptyMedicineNames) {
        buildOneTimeMedicineWarnings(
            medicineSlots = medicineSlots,
            emptyMedicineNames = emptyMedicineNames
        )
    }

    var oneTimeMedicineWarnings by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(medicineWarnings) {
        AdminNoticeMemory.syncResolvedMedicineWarnings(medicineWarnings)
        oneTimeMedicineWarnings = if (medicineWarnings.isEmpty()) {
            emptyList()
        } else {
            AdminNoticeMemory.consumeNewMedicineWarnings(medicineWarnings)
        }
    }

    var delayedLowMedicineNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(lowMedicineNames) {
        AdminNoticeMemory.syncResolvedLowMedicines(lowMedicineNames)
        if (lowMedicineNames.isEmpty()) {
            delayedLowMedicineNames = emptyList()
        } else {
            delay(cautionDelayMillis.milliseconds)
            delayedLowMedicineNames = AdminNoticeMemory.consumeNewLowMedicines(lowMedicineNames)
        }
    }

    val concerns = remember(delayedLowMedicineNames) {
        delayedLowMedicineNames.map { "${it}이 부족합니다." }
    }

    var normalizedNotices by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(isFanOn) {
        normalizedNotices = AdminNoticeMemory.consumeFanNormalizedNotice(isFanOn)
            ?.let(::listOf)
            .orEmpty()
    }

    val notices = remember(persistentWarnings, oneTimeMedicineWarnings, concerns, normalizedNotices) {
        buildList {
            addAll(persistentWarnings.map { AdminNotice(message = it, level = AdminNoticeLevel.WARNING) })
            addAll(oneTimeMedicineWarnings.map { AdminNotice(message = it, level = AdminNoticeLevel.WARNING) })
            addAll(concerns.map { AdminNotice(message = it, level = AdminNoticeLevel.CAUTION) })
            addAll(normalizedNotices.map { AdminNotice(message = it, level = AdminNoticeLevel.NORMALIZED) })
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(notices) {
        if (notices.isNotEmpty()) {
            isVisible = true
            delay(popupDurationMillis.milliseconds)
            isVisible = false
            if (normalizedNotices.isNotEmpty()) {
                normalizedNotices = emptyList()
            }
        } else {
            isVisible = false
        }
    }

    return AdminNoticeState(notices = notices, isVisible = isVisible)
}

@Composable
fun rememberAdminNoticeState(
    isBluetoothConnected: Boolean,
    isFanOn: Boolean?,
    adminCabinets: List<AdminCabinet>,
    medicineSlots: List<MedicineSlot>,
    popupDurationMillis: Long = 3500L,
    cautionDelayMillis: Long = 1000L
): AdminNoticeState {
    val cabinetPairs = remember(adminCabinets, medicineSlots) {
        val maxSlotCount = maxOf(
            adminCabinets.maxOfOrNull { it.slotIndex + 1 } ?: 0,
            medicineSlots.maxOfOrNull { it.slotId + 1 } ?: 0
        )

        (0 until maxSlotCount).map { index ->
            adminCabinets.find { it.slotIndex == index } to medicineSlots.find { it.slotId == index }
        }
    }

    return rememberAdminNoticeState(
        isBluetoothConnected = isBluetoothConnected,
        isFanOn = isFanOn,
        cabinets = cabinetPairs,
        popupDurationMillis = popupDurationMillis,
        cautionDelayMillis = cautionDelayMillis
    )
}

fun buildPersistentAdminWarnings(
    isBluetoothConnected: Boolean,
    isFanOn: Boolean?
): List<String> {
    val warnings = mutableListOf<String>()

    if (!isBluetoothConnected) {
        warnings += "블루투스 연결이 끊겼습니다."
    }
    if (isFanOn == true) {
        warnings += "약품장 온도, 혹은 습도가 너무 높습니다."
    }

    return warnings
}

fun buildOneTimeMedicineWarnings(
    medicineSlots: List<MedicineSlot>,
    emptyMedicineNames: List<String> = emptyList()
): List<String> {
    val warnings = mutableListOf<String>()

    // 잔여량이 '없음'인 약품이 있으면 약품명 기준 경고를 추가합니다.
    val normalizedEmptyNames = emptyMedicineNames
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    if (normalizedEmptyNames.isNotEmpty()) {
        warnings += normalizedEmptyNames.map { "${it}이 없습니다!" }
    } else if (!hasRegisteredMedicine(medicineSlots)) {
        // 약품이 전혀 없을 때도 약품 관련 경고로 취급하여 일회성으로 노출합니다.
        warnings += "관리자 보관함에 등록된 약품이 없습니다."
    }

    return warnings
}

fun buildAdminWarnings(
    isBluetoothConnected: Boolean,
    isFanOn: Boolean?,
    medicineSlots: List<MedicineSlot>,
    emptyMedicineNames: List<String> = emptyList()
): List<String> {
    return buildPersistentAdminWarnings(
        isBluetoothConnected = isBluetoothConnected,
        isFanOn = isFanOn
    ) + buildOneTimeMedicineWarnings(
        medicineSlots = medicineSlots,
        emptyMedicineNames = emptyMedicineNames
    )
}

fun hasRegisteredMedicine(medicineSlots: List<MedicineSlot>): Boolean {
    return medicineSlots.any { slot ->
        val trimmed = slot.medicine.name.trim()
        trimmed.isNotEmpty() && !trimmed.startsWith("약품 #")
    }
}

private fun normalizeAdminNoticeMedicineName(rawName: String?): String? {
    return rawName
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.startsWith("약품 #") }
        ?.take(4)
}

