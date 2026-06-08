package com.inu.capstone_mobile.data.source

import com.inu.capstone_mobile.data.models.AdminCabinet
import com.inu.capstone_mobile.data.models.Medicine
import com.inu.capstone_mobile.data.models.MedicineSlot
import com.inu.capstone_mobile.data.models.Prescription
import com.inu.capstone_mobile.data.models.User
import com.inu.capstone_mobile.data.models.UserCabinet
import com.inu.capstone_mobile.data.models.totalStatus


object MockDataSource {
    const val DEBUG_USER_NAME = "김명오"
    const val DEBUG_USER_ROLE_ADMIN = "전문의"
    const val DEBUG_USER_ROLE_NORMAL = "환자"
    const val DEBUG_TEMP = 23.0f
    const val DEBUG_HUMID = 45.0f
    const val DEBUG_TIME_BREAKFAST = "아침"
    const val DEBUG_TIME_LUNCH = "점심"
    const val DEBUG_TIME_DINNER = "저녁"
    val DEBUG_FAN_STATUS: Boolean? = null
    val DEBUG_ADMIN_LOCKED: Boolean = true

    // 👇 변경: 테스트 데이터
    val DEBUG_MEDICINE_SLOTS = listOf(
        MedicineSlot(0, Medicine(1, "타이레놀"), "user01"),
        MedicineSlot(1, Medicine(2, "자큐보정"), "user01"),
        MedicineSlot(2, Medicine(3, "약품 #3"), "admin"),
        MedicineSlot(3, Medicine(4, "약품 #4"), "admin"),
        MedicineSlot(4, Medicine(5, "약품 #5"), "admin"),
        MedicineSlot(5, Medicine(6, "약품 #6"), "admin")
    )

    val DEBUG_USER_CABINETS = listOf(
        UserCabinet("user01", 0b101000, false, isActive = true),
        UserCabinet("user02", 0b101000, true, isActive = true),
        UserCabinet("user03", 0b000000, null, isActive = true)
    )

    val DEBUG_ADMIN_CABINETS = listOf(
        AdminCabinet(slotIndex = 0, totalStatus = totalStatus.ENOUGH, isActive = true),
        AdminCabinet(slotIndex = 1, totalStatus = totalStatus.LOW, isActive = true),
        AdminCabinet(slotIndex = 2, totalStatus = totalStatus.EMPTY, isActive = true)
    )

    val DEBUG_USERINFO_ADMIN = User(
        userId = "admin01",
        userName = "김명오",
        userRole = "전문의",
        userRoleId = "ADMIN",
        prescriptions = emptyList()
    )

    val DEBUG_USERINFO_PATIENT_1 = User(
        userId = "user01",
        userName = "김가람",
        userRole = "환자",
        userRoleId = "USER",
        prescriptions = listOf(
            Prescription(
                pillId = 1,
                dailyTimings = listOf("아침", "점심", "저녁"),
                startDate = "2026-04-11",
                endDate = "2026-06-13"
            ),
            Prescription(
                pillId = 2,
                dailyTimings = listOf("아침, 저녁"),
                startDate = "2026-04-12",
                endDate = "2026-06-14"
            )
        )
    )

    /** isTesting=true 일 때 allUsers로 쓰는 환자 목록 */
    val ALL_PATIENTS: List<User> by lazy { listOf(DEBUG_USERINFO_PATIENT_1, DEBUG_USERINFO_PATIENT_2) }

    // 약품 상세 정보 (타이레놀 기준 mock)
    const val DEBUG_MED_INFO_ID = "200200053"
    const val DEBUG_MED_INFO_NAME = "타이레놀"
    const val DEBUG_MED_INFO_MANUFACTURER = "회사명"
    const val DEBUG_MED_INFO_TYPE = "해열·진통·소염제"
    const val DEBUG_MED_INFO_IMAGE_URL =
        "https://nedrug.mfds.go.kr/pbp/cmn/itemImageDownload/148604585418800149"

    val DEBUG_USERINFO_PATIENT_2 = User(
        userId = "user02",
        userName = "이아름",
        userRole = "환자",
        userRoleId = "USER",
        prescriptions = listOf(
            Prescription(
                pillId = 1,
                dailyTimings = listOf("아침", "점심", "저녁"),
                startDate = "2026-05-11",
                endDate = "2026-06-13"
            ),
            Prescription(
                pillId = 2,
                dailyTimings = listOf("아침", "점심", "저녁"),
                startDate = "2026-05-12",
                endDate = "2026-06-14"
            )
        )
    )
}