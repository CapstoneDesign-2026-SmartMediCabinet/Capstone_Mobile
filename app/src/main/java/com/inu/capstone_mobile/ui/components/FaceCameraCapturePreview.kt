package com.inu.capstone_mobile.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.inu.capstone_mobile.utils.FaceAnalyzer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun FaceCameraCapturePreview(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sessionKey: Int = 0,
    onBlinkDetected: () -> Unit = {},
    onImageCaptured: (File) -> Unit,
    onError: (String) -> Unit,
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var captureErrorMessage by remember { mutableStateOf<String?>(null) }
    var rebindKey by remember { mutableIntStateOf(0) }
    val hasCapturedInSession = remember(sessionKey) { AtomicBoolean(false) }
    var faceBounds by remember { mutableStateOf<RectF?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            val message = "카메라 권한이 필요합니다. 설정에서 권한을 허용해 주세요."
            captureErrorMessage = message
            onError(message)
        }
    }

    DisposableEffect(cameraExecutor) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    if (isPreview) {
        Box(
            modifier = modifier
                .background(Color(0xFF101418), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Face Camera Preview", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (enabled) "프리뷰에서는 카메라 대신 플레이스홀더가 표시됩니다."
                    else "촬영/업로드 중 상태 미리보기",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(previewView, hasCameraPermission, enabled, sessionKey, rebindKey) {
        val currentPreviewView = previewView
        if (currentPreviewView == null || !hasCameraPermission) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val listener = Runnable {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(currentPreviewView.getSurfaceProvider())
                    }
                    val captureUseCase = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    val analysisUseCase = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                cameraExecutor,
                                FaceAnalyzer(
                                    onBlinkDetected = {
                                        if (!enabled || hasCapturedInSession.getAndSet(true)) return@FaceAnalyzer
                                        val activeImageCapture = imageCapture ?: captureUseCase
                                        mainExecutor.execute {
                                            onBlinkDetected()
                                            val photoFile = createTempFaceImageFile(context.cacheDir)
                                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                            activeImageCapture.takePicture(
                                                outputOptions,
                                                mainExecutor,
                                                object : ImageCapture.OnImageSavedCallback {
                                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                        captureErrorMessage = null
                                                        onImageCaptured(photoFile)
                                                    }

                                                    override fun onError(exception: ImageCaptureException) {
                                                        Log.e("FaceCamera", "얼굴 사진 저장 실패", exception)
                                                        photoFile.delete()
                                                        hasCapturedInSession.set(false)
                                                        val message = "사진 저장에 실패했습니다. 다시 눈을 깜박여 주세요."
                                                        captureErrorMessage = message
                                                        onError(message)
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    onFaceBoundsChanged = { bounds ->
                                        mainExecutor.execute {
                                            faceBounds = bounds
                                        }
                                    }
                                )
                            )
                        }

                    cameraProvider.unbindAll()
                    imageCapture = captureUseCase
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        captureUseCase,
                        analysisUseCase
                    )
                } catch (e: Exception) {
                    Log.e("FaceCamera", "카메라 초기화 실패", e)
                    imageCapture = null
                    faceBounds = null
                    hasCapturedInSession.set(false)
                    val message = "카메라를 시작할 수 없습니다. 잠시 후 다시 시도해 주세요."
                    captureErrorMessage = message
                    onError(message)
                }
            }
            cameraProviderFuture.addListener(listener, mainExecutor)

            onDispose {
                runCatching {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF101418), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            !hasCameraPermission -> {
                PermissionRequestCard(
                    message = captureErrorMessage ?: "카메라 권한을 허용하면 안면 인증을 시작할 수 있습니다.",
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }

            else -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { previewContext ->
                        PreviewView(previewContext).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    },
                    update = { updatedView ->
                        previewView = updatedView
                    }
                )

                val detectedBounds = faceBounds
                if (detectedBounds != null && enabled) {
                    // Front camera preview is mirrored, so overlay X coordinates must be mirrored too.
                    val mirroredBounds = RectF(
                        1f - detectedBounds.right,
                        detectedBounds.top,
                        1f - detectedBounds.left,
                        detectedBounds.bottom
                    )
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val boxLeft = mirroredBounds.left * size.width
                        val boxTop = mirroredBounds.top * size.height
                        val boxRight = mirroredBounds.right * size.width
                        val boxBottom = mirroredBounds.bottom * size.height
                        drawRect(
                            color = Color(0xFF36C36A),
                            topLeft = androidx.compose.ui.geometry.Offset(boxLeft, boxTop),
                            size = androidx.compose.ui.geometry.Size(boxRight - boxLeft, boxBottom - boxTop),
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xAA0D1F2D))
                            .border(1.dp, Color(0x6636C36A), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = Color(0xFF6EE7A1)
                            )
                            Text(
                                text = "얼굴 감지됨",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (!enabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(
    message: String,
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onRequestPermission) {
            Text("권한 허용")
        }
    }
}

private fun createTempFaceImageFile(cacheDir: File): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.KOREA).format(Date())
    return File(cacheDir, "face_capture_$timestamp.jpg")
}





