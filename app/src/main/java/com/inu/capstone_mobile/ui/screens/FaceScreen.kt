package com.inu.capstone_mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.source.MockDataSource
import com.inu.capstone_mobile.ui.components.AppScaffold
import com.inu.capstone_mobile.ui.components.BaseCard
import com.inu.capstone_mobile.ui.components.FaceCameraCapturePreview
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme
import com.inu.capstone_mobile.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import java.io.File

private const val FACE_LOGIN_TITLE = "안면 인증 로그인"
private const val FACE_LOGIN_DEFAULT_MESSAGE = "카메라를 정면으로 바라본 뒤 눈을 한 번 깜박여 주세요."

@Composable
fun FaceScreen(
	viewModel: AuthViewModel,
	onBack: () -> Unit,
	onLoginSuccess: () -> Unit,
) {
	val authMessage by viewModel.authMessage.collectAsState()
	var statusMessage by remember { mutableStateOf(FACE_LOGIN_DEFAULT_MESSAGE) }
	var isProcessing by remember { mutableStateOf(false) }
	var sessionKey by remember { mutableIntStateOf(0) }
	var loadingTitle by remember { mutableStateOf("로딩 중") }
	var loadingMessage by remember { mutableStateOf("얼굴을 확인하고 있습니다...") }
	var isSuccessPending by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		viewModel.clearAuthMessage()
		statusMessage = FACE_LOGIN_DEFAULT_MESSAGE
	}

	LaunchedEffect(authMessage) {
		authMessage?.let { statusMessage = it }
	}

	LaunchedEffect(isSuccessPending) {
		if (isSuccessPending) {
			delay(1000)
			onLoginSuccess()
		}
	}

	AppScaffold(
		title = FACE_LOGIN_TITLE,
		onBackClick = onBack
	) { paddingValues ->
		FaceLoginContent(
			paddingValues = paddingValues,
			statusMessage = statusMessage,
			isProcessing = isProcessing,
			loadingTitle = loadingTitle,
			loadingMessage = loadingMessage,
			sessionKey = sessionKey,
			onBlinkDetected = {
				viewModel.clearAuthMessage()
				isSuccessPending = false
				loadingTitle = "로딩 중"
				loadingMessage = "얼굴을 확인하고 있습니다..."
				statusMessage = "눈 깜박임이 감지되었습니다. 얼굴을 인증하고 있습니다..."
			},
			onCameraError = { message ->
				isProcessing = false
				statusMessage = message
			},
			onImageCaptured = { imageFile ->
				viewModel.clearAuthMessage()
				isSuccessPending = false
				isProcessing = true
				loadingTitle = "로딩 중"
				loadingMessage = "얼굴을 확인하고 있습니다..."
				statusMessage = "서버로 얼굴 정보를 전송하고 있습니다..."
				viewModel.VerifyFace(imageFile) { success ->
					imageFile.delete()
					if (success) {
						statusMessage = "로그인 성공!"
						loadingTitle = "인증 완료"
						loadingMessage = "로그인 성공!"
						isSuccessPending = true
					} else {
						isSuccessPending = false
						isProcessing = false
						loadingTitle = "로딩 중"
						loadingMessage = "얼굴을 확인하고 있습니다..."
						sessionKey += 1
						statusMessage = authMessage ?: "안면 인증에 실패했습니다. 다시 눈을 깜박여 주세요."
					}
				}
			}
		)
	}
}

@Composable
private fun FaceLoginContent(
	paddingValues: PaddingValues,
	statusMessage: String,
	isProcessing: Boolean,
	loadingTitle: String,
	loadingMessage: String,
	sessionKey: Int,
	onBlinkDetected: () -> Unit,
	onCameraError: (String) -> Unit,
	onImageCaptured: (File) -> Unit,
) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.padding(paddingValues)
			.background(Color(0xFFDDEAF8))
			.padding(28.dp)
	) {
		BaseCard(
			modifier = Modifier.fillMaxSize(),
			title = ""
		) {
			Row(
				modifier = Modifier
					.fillMaxSize()
					.padding(20.dp)
			) {
				Column(
					modifier = Modifier
						.weight(1.2f)
						.fillMaxHeight(),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(16.dp)
				) {
					Text(
						text = "카메라",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						textAlign = TextAlign.Center,
						modifier = Modifier.fillMaxWidth()
					)
					FaceCameraCapturePreview(
						modifier = Modifier
							.fillMaxWidth()
							.weight(1f),
						enabled = !isProcessing,
						sessionKey = sessionKey,
						onBlinkDetected = onBlinkDetected,
						onImageCaptured = onImageCaptured,
						onError = onCameraError
					)
				}

				VerticalDivider(
					modifier = Modifier
						.fillMaxHeight()
						.padding(horizontal = 18.dp, vertical = 8.dp),
					thickness = 1.dp,
					color = Color(0xFFE0E0E0)
				)

				Box(
					modifier = Modifier
						.weight(1f)
						.fillMaxHeight()
				) {
					Column(
						modifier = Modifier.fillMaxSize(),
						verticalArrangement = Arrangement.spacedBy(16.dp)
					) {
						FaceStatusCard(statusMessage = statusMessage, isProcessing = isProcessing)
						FaceGuideCard(
							guideLines = listOf(
								"정면을 바라본 상태에서 눈을 한 번 자연스럽게 깜박여 주세요.",
								"모자나 마스크, 짙은 그림자는 인식률을 낮출 수 있습니다.",
								"촬영 중에는 기기를 흔들지 말고 잠시만 고정해 주세요.",
								"인증이 실패하면 조명을 밝게 하고 다시 시도해 주세요."
							),
							modifier = Modifier.weight(1f)
						)
					}

					if (isProcessing) {
						FaceLoadingCard(
							modifier = Modifier
								.align(Alignment.BottomEnd)
								.padding(bottom = 4.dp),
							title = loadingTitle,
							message = loadingMessage
						)
					}
				}
			}
		}
	}
}

@Composable
private fun FaceStatusCard(
	statusMessage: String,
	isProcessing: Boolean,
) {
	val isError = !isProcessing && (
		statusMessage.contains("실패") ||
			statusMessage.contains("오류") ||
			statusMessage.contains("연결") ||
			statusMessage.contains("만료")
	)
	val title = when {
		isProcessing -> "인증 진행 중"
		isError -> "인증 오류"
		else -> "안면 인증 대기 중"
	}
	val icon = when {
		isProcessing -> Icons.Default.Visibility
		isError -> Icons.Default.ErrorOutline
		else -> Icons.Default.Face
	}
	val iconTint = if (isError) Color(0xFFD32F2F) else Color(0xFF1565C0)
	val messageColor = if (isError) Color(0xFFB71C1C) else Color.Unspecified

	Card(
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F9FF))
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(20.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = iconTint
				)
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center
				)
			}
			Text(
				text = statusMessage,
				style = MaterialTheme.typography.bodyLarge,
				lineHeight = 24.sp,
				color = messageColor,
				textAlign = TextAlign.Center,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}

@Composable
private fun FaceGuideCard(
	modifier: Modifier = Modifier,
	guideLines: List<String>,
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(containerColor = Color.White)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(20.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Text(
				text = "안내",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				textAlign = TextAlign.Center,
				modifier = Modifier.fillMaxWidth()
			)
			guideLines.forEach { text ->
				Row(
					verticalAlignment = Alignment.Top,
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Icon(
						imageVector = Icons.Default.CheckCircle,
						contentDescription = null,
						tint = Color(0xFF2E7D32),
						modifier = Modifier.padding(top = 2.dp)
					)
					Text(
						text = text,
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.weight(1f)
					)
				}
			}
		}
	}
}

@Composable
private fun FaceLoadingCard(
	modifier: Modifier = Modifier,
	title: String,
	message: String,
) {
	Card(
		modifier = modifier,
		shape = RoundedCornerShape(18.dp),
		colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F9FF)),
		elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
	) {
		Row(
			modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp)
		) {
			CircularProgressIndicator(
				modifier = Modifier.size(20.dp),
				strokeWidth = 2.5.dp,
				color = Color(0xFF1565C0)
			)
			Column(horizontalAlignment = Alignment.Start) {
				Text(
					text = title,
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.Bold
				)
				Text(
					text = message,
					style = MaterialTheme.typography.bodySmall,
					color = Color.Gray
				)
			}
		}
	}
}

@Preview(
	showBackground = true,
	name = "안면 인증 로그인",
	device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
fun FaceScreenPreview() {
	val previewStatus = if (DebugConfig.isTesting) {
		"DEBUG MODE · ${MockDataSource.DEBUG_USERINFO_PATIENT_1.userName} 님의 안면 인증을 대기 중입니다."
	} else {
		"카메라를 정면으로 바라본 뒤 눈을 한 번 깜박여 주세요."
	}

	Capstone_TabletTheme {
		AppScaffold(
			title = FACE_LOGIN_TITLE,
			onBackClick = {}
		) { paddingValues ->
			FaceLoginContent(
				paddingValues = paddingValues,
				statusMessage = previewStatus,
				isProcessing = false,
				loadingTitle = "로딩 중",
				loadingMessage = "얼굴을 확인하고 있습니다...",
				sessionKey = 0,
				onBlinkDetected = {},
				onCameraError = {},
				onImageCaptured = {}
			)
		}
	}
}

@Preview(
	showBackground = true,
	name = "안면 인증 로그인 - 진행 중",
	device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
fun FaceScreenLoadingPreview() {
	val previewStatus = if (DebugConfig.isTesting) {
		"DEBUG MODE · ${MockDataSource.DEBUG_USERINFO_PATIENT_1.userName} 님의 얼굴을 확인하고 있습니다."
	} else {
		"서버로 얼굴 정보를 전송하고 있습니다..."
	}

	Capstone_TabletTheme {
		AppScaffold(
			title = FACE_LOGIN_TITLE,
			onBackClick = {}
		) { paddingValues ->
			FaceLoginContent(
				paddingValues = paddingValues,
				statusMessage = previewStatus,
				isProcessing = true,
				loadingTitle = "로딩 중",
				loadingMessage = "얼굴을 확인하고 있습니다...",
				sessionKey = 0,
				onBlinkDetected = {},
				onCameraError = {},
				onImageCaptured = {}
			)
		}
	}
}

