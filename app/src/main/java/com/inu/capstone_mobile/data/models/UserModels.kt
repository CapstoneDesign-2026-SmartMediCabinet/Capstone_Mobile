package com.inu.capstone_mobile.data.models


data class User(
    val userId: String,
    val userName: String,
    val userRole: String,
    val userRoleId: String,
    val prescriptions: List<Prescription>
)


data class Prescription(
    val pillId: Int,
    val dailyTimings: List<String>, // 예: ["아침", "점심", "저녁"]
    val startDate: String,          // 예: "2026-04-11"
    val endDate: String             // 예: "2026-04-13"
)