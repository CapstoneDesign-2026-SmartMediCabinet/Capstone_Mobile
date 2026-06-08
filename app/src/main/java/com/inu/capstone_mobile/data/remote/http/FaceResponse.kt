package com.inu.capstone_mobile.data.remote.http

data class FaceResponse(
    val success: Boolean,
    val message: String? = null,
    val user_id: String? = null,
    val face_vector: String? = null
)