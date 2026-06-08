package com.inu.capstone_mobile.data.remote.http


import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ServerApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/users/{userId}/prescriptions")
    suspend fun getPrescriptions(@Path("userId") userId: String): Response<PrescriptionResponse>

    @GET("api/users/{userId}/cabinet")
    suspend fun getCabinetStatus(@Path("userId") userId: String): Response<CabinetResponse>

    @GET("api/users")
    suspend fun getUsers(@Query("role") role: String? = null): Response<UsersResponse>

    @GET("api/cabinets")
    suspend fun getAllCabinets(): Response<AllCabinetsResponse>

    @GET("api/device/env")
    suspend fun getDeviceEnv(): Response<EnvResponse>

    @POST("api/device/env")
    suspend fun uploadDeviceSync(@Body request: DeviceSyncUploadRequest): Response<DeviceStateResponse>

    @POST("api/admin/medicine/update")
    suspend fun updateMedicine(@Body request: MedicineUpdateRequest): Response<MedicineUpdateResponse>

    @GET("api/medicines")
    suspend fun getMedicines(): Response<MedicinesResponse>

    @GET("api/medicines/{medicineId}")
    suspend fun getMedicineInfo(@Path("medicineId") medicineId: Int): Response<MedicineInfoResponse>

    @GET("api/medicines/latest")
    suspend fun getLatestMedicine(): Response<MedicineUpdateResponse>

    @PUT("api/admin/medicine/rename")
    suspend fun renameMedicine(@Body request: MedicineRenameRequest): Response<MedicineRenameResponse>

    @POST("api/admin/cabinet/update")
    suspend fun updateUserCabinetStatus(@Body request: CabinetUpdateRequest): Response<CabinetUpdateResponse>

    @GET("api/admin/admincabinet")
    suspend fun getAdminCabinets(): Response<AdminCabinetsResponse>

    @PUT("api/admin/admincabinet/active")
    suspend fun updateAdminCabinetActive(@Body request: AdminCabinetActiveRequest): Response<AdminCabinetActiveResponse>


    // 얼굴 등록
    @Multipart
    @POST("register_face")
    suspend fun registerFace(
        @Part faceImage: MultipartBody.Part
    ): Response<FaceResponse>

    // 얼굴 식별 (원스톱 로그인)
    @Multipart
    @POST("verify_face")
    suspend fun verifyFace(
        @Part faceImage: MultipartBody.Part
    ): Response<LoginResponse>
}
