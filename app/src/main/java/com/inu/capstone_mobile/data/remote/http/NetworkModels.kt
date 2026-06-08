package com.inu.capstone_mobile.data.remote.http

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// [로그인]
data class LoginRequest(val user_id: String, val password: String)
data class LoginResponse(val success: Boolean, val message: String?, val token: String?, val user: UserDto?)
data class UserDto(val user_pk: Int, val user_id: String, val name: String, val role: String)
data class UsersResponse(val success: Boolean, val message: String?, val users: List<UserDto>?)

// [처방전]
data class PrescriptionResponse(val success: Boolean, val message: String?, val prescriptions: List<PrescriptionDto>?)
data class PrescriptionDto(
    @SerializedName("prescription_id") val prescription_id: Int,
    @SerializedName(value = "pillName", alternate = ["pill_name"]) val pillName: String? = null,
    @SerializedName(value = "pillId", alternate = ["pill_id"]) val pillId: Int,
    @SerializedName(value = "startDate", alternate = ["start_date"]) val startDate: String,
    @SerializedName(value = "endDate", alternate = ["end_date"]) val endDate: String,
    @SerializedName(value = "dailyTimings", alternate = ["daily_timings"]) val dailyTimings: JsonElement? = null
)

// [사물함]
data class CabinetResponse(val success: Boolean, val message: String?, val cabinet: CabinetDto?)
data class CabinetDto(val slot_index: Int, val slot_bitmask: Int, val is_locked: Boolean?)
data class AllCabinetsResponse(val success: Boolean, val message: String?, val cabinets: List<UserCabinetDto>?)
data class UserCabinetDto(
    val slot_index: Int?,
    val user_id: String?,
    val user_name: String? = null,
    val is_active: Int? = null
)

// [환경 온습도]
data class EnvResponse(val success: Boolean, val message: String?, val env: EnvDto?)
data class EnvDto(val temp: Float?, val humid: Float?, val is_fan_on: Boolean?)

// [디바이스 상태 적재]
data class DeviceStateResponse(
    val success: Boolean,
    val message: String?,
    val saved_count: Int? = null
)

data class DeviceSyncUploadRequest(
    val temp: Float? = null,
    val humid: Float? = null,
    val is_fan_on: Boolean? = null,
    val admin_cabinets: List<AdminCabinetSyncDto>? = null,
    val user_cabinets: List<UserCabinetSyncDto>? = null
)

data class AdminCabinetSyncDto(
    val slot_index: Int,
    val cabinet_status: String? = null,
)

data class UserCabinetSyncDto(
    val slot_index: Int,
    val slot_bitmask: Int? = null,
)

// [약품 등록 / 조회]
data class MedicineUpdateRequest(val medicine_id: Int, val item_seq: String?)
data class MedicineUpdateResponse(val success: Boolean, val message: String?, val medicine: MedicineInfoDto?)
data class MedicinesResponse(val success: Boolean, val message: String?, val medicines: List<MedicineNameDto>?)
data class MedicineInfoResponse(val success: Boolean, val message: String?, val medicine: MedicineInfoDto?)
data class MedicineNameDto(
    val medicine_id: Int,
    val item_name: String
)
data class MedicineInfoDto(
    val medicine_id: Int,       // 약품 고유번호 (DB PK)
    val item_seq: String,       // 식약처 품목일련번호
    val item_name: String,      // 약품명
    val entp_name: String,      // 제조사/업체명
    val class_name: String,     // 약품 종류(분류)
    @SerializedName(value = "image_url", alternate = ["item_image"])
    val image_url: String?      // 약품 이미지 URL (null 가능)
)

// [약품명 변경]
data class MedicineRenameRequest(val medicine_id: Int, val name: String)
data class MedicineRenameResponse(val success: Boolean, val message: String?)

// [보관함 배정 관리]
data class CabinetUpdateRequest(val slot_index: Int, val user_id: String?, val is_active: Boolean)
data class CabinetUpdateResponse(val success: Boolean, val message: String?)

// [관리자 보관함 조회]
data class AdminCabinetsResponse(val success: Boolean, val message: String?, val cabinets: List<AdminCabinetDto>?)
data class AdminCabinetDto(
    val slot_index: Int,
    val medicine_id: Int?,      // 1번 슬롯이면 1, null이면 약품 미등록
    val is_active: Int?         // 0/1, null 가능
)

// [관리자 보관함 활성화 토글]
data class AdminCabinetActiveRequest(val slot_index: Int, val is_active: Boolean)
data class AdminCabinetActiveResponse(val success: Boolean, val message: String?)

