package com.inu.capstone_mobile.data.models

// 1. 약품 정보
data class Medicine(
    val id: Int,
    var name: String
)

enum class totalStatus {
    EMPTY, // 0 - 비어있음
    LOW,    // 1 - 부족
    ENOUGH   // 2 - 충분
}

enum class MealTime {
    BREAKFAST,  // 아침
    LUNCH,      // 점심
    DINNER      // 저녁
}

// 2. 사용자용 캐비넷 (사용자 기준)
data class UserCabinet(
    val userId: String,           // 사용자 식별
    val slotBitmask: Int,         // 10비트: 센서 상태
    val isLocked: Boolean? = null,
    val isActive: Boolean = true
) {
    fun isSlotOccupied(index: Int): Boolean {
        val mask = 1 shl index
        return (slotBitmask and mask) != 0
    }

    fun getSlotIndex(dayOffset: Int, mealTime: MealTime): Int {
        return dayOffset * 3 + mealTime.ordinal
    }
}

// 3. 관리자용 캐비넷 (BT 기준: 물리 슬롯 상태만 보관)
// 약품명은 MedicineSlot(slotId)와 slotIndex로 연결하여 ManagerMain에서 표시
data class AdminCabinet(
    val slotIndex: Int,           // 물리적인 칸 번호 (0, 1, 2...) - BT A:슬롯:상태
    val totalStatus: totalStatus, // 잔량 상태 - BT에서 수신
    val isLocked: Boolean? = null, // 잠금 상태 - BT L:0:bool에서 수신
    val isActive: Boolean = true
)

// 4. 약품 배치 정보 (관리자가 설정)
data class MedicineSlot(
    val slotId: Int,        // 약품 위치 (0~9)
    val medicine: Medicine, // 해당 슬롯의 약품
    val userId: String      // 누가 받을 약인가
)