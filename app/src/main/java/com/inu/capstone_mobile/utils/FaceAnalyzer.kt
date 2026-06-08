package com.inu.capstone_mobile.utils

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    private val onBlinkDetected: () -> Unit,
    private val onFaceBoundsChanged: (RectF?) -> Unit = {}
) : ImageAnalysis.Analyzer {

    // 얼굴 윤곽 대신 눈/코/입 특징점과 눈 깜빡임 확률을 얻기 위한 옵션
    private val options = FaceDetectorOptions.Builder()
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()

    private val detector = FaceDetection.getClient(options)

    // 상태 관리: 눈이 켜져 있다가(false) 감기면(true) 다시 떠질 때 감지!
    private var isEyesClosed = false
    private var isCooldown = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (isCooldown) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val detectedFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    if (detectedFace != null) {
                        onFaceBoundsChanged(
                            toNormalizedFaceRect(
                                rect = detectedFace.boundingBox,
                                imageWidth = imageProxy.width,
                                imageHeight = imageProxy.height,
                                rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            )
                        )
                    } else {
                        onFaceBoundsChanged(null)
                    }

                    for (face in faces) {
                        val leftEyeOpen = face.leftEyeOpenProbability ?: continue
                        val rightEyeOpen = face.rightEyeOpenProbability ?: continue

                        // 두 눈이 모두 20% 이하로 떠지면(감기면) 상태 변경
                        if (leftEyeOpen < 0.2f && rightEyeOpen < 0.2f) {
                            isEyesClosed = true
                        }

                        // 감겼던 눈이 다시 80% 이상 떠지면 깜빡임 성공!
                        if (isEyesClosed && leftEyeOpen > 0.8f && rightEyeOpen > 0.8f) {
                            isCooldown = true // 중복 감지 방지
                            onBlinkDetected()
                        }
                    }
                }
                .addOnFailureListener {
                    onFaceBoundsChanged(null)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}

private fun toNormalizedFaceRect(
    rect: Rect,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
): RectF {
    val l = rect.left.toFloat()
    val t = rect.top.toFloat()
    val r = rect.right.toFloat()
    val b = rect.bottom.toFloat()

    return when (rotationDegrees) {
        90 -> RectF(
            (imageHeight - b) / imageHeight,
            l / imageWidth,
            (imageHeight - t) / imageHeight,
            r / imageWidth
        )
        180 -> RectF(
            (imageWidth - r) / imageWidth,
            (imageHeight - b) / imageHeight,
            (imageWidth - l) / imageWidth,
            (imageHeight - t) / imageHeight
        )
        270 -> RectF(
            t / imageHeight,
            (imageWidth - r) / imageWidth,
            b / imageHeight,
            (imageWidth - l) / imageWidth
        )
        else -> RectF(
            l / imageWidth,
            t / imageHeight,
            r / imageWidth,
            b / imageHeight
        )
    }.let { normalized ->
        RectF(
            normalized.left.coerceIn(0f, 1f),
            normalized.top.coerceIn(0f, 1f),
            normalized.right.coerceIn(0f, 1f),
            normalized.bottom.coerceIn(0f, 1f)
        )
    }
}
