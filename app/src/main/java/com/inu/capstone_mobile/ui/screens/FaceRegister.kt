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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.inu.capstone_mobile.data.DebugConfig
import com.inu.capstone_mobile.data.source.MockDataSource
import com.inu.capstone_mobile.ui.components.AppScaffold
import com.inu.capstone_mobile.ui.components.BaseCard
import com.inu.capstone_mobile.ui.components.FaceCameraCapturePreview
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme
import com.inu.capstone_mobile.viewmodel.AuthViewModel
import java.io.File

private const val FACE_REGISTER_TITLE = "안면 정보 등록"
private const val FACE_REGISTER_DEFAULT_MESSAGE = "안면 인식을 위해 눈을 '깜박' 해 주세요."

@Composable
fun FaceRegister(
	viewModel: AuthViewModel,
	onBack: () -> Unit,
) {
	val currentUser by viewModel.currentUser.collectAsState()
	val authMessage by viewModel.authMessage.collectAsState()
	val displayUser = currentUser ?: if (DebugConfig.isTesting) MockDataSource.DEBUG_USERINFO_PATIENT_1 else null
	var statusMessage by remember { mutableStateOf(FACE_REGISTER_DEFAULT_MESSAGE) }
	var isProcessing by remember { mutableStateOf(false) }
	var sessionKey by remember { mutableIntStateOf(0) }

	LaunchedEffect(Unit) {
		viewModel.clearAuthMessage()
		statusMessage = FACE_REGISTER_DEFAULT_MESSAGE
	}

	LaunchedEffect(authMessage) {
		authMessage?.let { statusMessage = it }
	}

	AppScaffold(
		title = FACE_REGISTER_TITLE,
		onBackClick = onBack
	) { paddingValues ->
		FaceRegisterContent(
			paddingValues = paddingValues,
			userName = displayUser?.userName ?: "로그인 사용자",
			userId = displayUser?.userId ?: "-",
			statusMessage = statusMessage,
			isProcessing = isProcessing,
			sessionKey = sessionKey,
			onBlinkDetected = {
				viewModel.clearAuthMessage()
				statusMessage = "눈 깜박임이 감지되었습니다. 안면 정보를 등록하고 있습니다..."
			},
			onCameraError = { message ->
				isProcessing = false
				statusMessage = message
			},
			onImageCaptured = { imageFile ->
				viewModel.clearAuthMessage()
				isProcessing = true
				statusMessage = "Flask 서버로 안면 이미지를 전송하고 있습니다..."
				viewModel.registerFace(imageFile) { success ->
					imageFile.delete()
					isProcessing = false
					if (success) {
						statusMessage = authMessage ?: "안면 정보 등록이 완료되었습니다."
					} else {
						sessionKey += 1
						statusMessage = authMessage ?: "안면 정보 등록에 실패했습니다. 다시 시도해 주세요."
					}
				}
			}
		)
	}
}

@Composable
private fun FaceRegisterContent(
	paddingValues: PaddingValues,
	userName: String,
	userId: String,
	statusMessage: String,
	isProcessing: Boolean,
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
					.weight(1.1f)
					.fillMaxHeight(),
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
					.fillMaxHeight(0.85f)
					.padding(horizontal = 18.dp),
				thickness = 1.dp,
				color = Color(0xFFE0E0E0)
			)

			Column(
				modifier = Modifier
					.weight(0.9f)
					.fillMaxHeight(),
				verticalArrangement = Arrangement.spacedBy(16.dp)
			) {
				FaceRegisterUserCard(userName = userName, userId = userId)
				FaceRegisterGuideCard(statusMessage = statusMessage, isProcessing = isProcessing)
			}
		}
		}
	}
}

@Composable
private fun FaceRegisterUserCard(
	userName: String,
	userId: String,
) {
	Card(
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F9FF))
	) {
		Column(
			modifier = Modifier.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1565C0))
				Text("사용자 정보", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
			}
			FaceUserInfoRow(label = "성명", value = userName)
			FaceUserInfoRow(label = "User ID", value = userId)
		}
	}
}

@Composable
private fun FaceRegisterGuideCard(
	statusMessage: String,
	isProcessing: Boolean,
) {
	val isError = !isProcessing && (
		statusMessage.contains("실패") ||
			statusMessage.contains("오류") ||
			statusMessage.contains("연결") ||
			statusMessage.contains("만료")
	)
	val statusIcon = when {
		isProcessing -> Icons.Default.Badge
		isError -> Icons.Default.ErrorOutline
		else -> Icons.Default.CheckCircle
	}
	val statusTint = when {
		isProcessing -> Color(0xFFEF6C00)
		isError -> Color(0xFFD32F2F)
		else -> Color(0xFF1565C0)
	}
	val statusTextColor = if (isError) Color(0xFFB71C1C) else Color.Unspecified

	Card(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(containerColor = Color.White)
	) {
		Column(
			modifier = Modifier.padding(20.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Icon(Icons.Default.FaceRetouchingNatural, contentDescription = null, tint = Color(0xFF1565C0))
			Text("안내 문구", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
		}
			FaceGuideBullet("안면 인식을 위해 눈을 '깜박' 해 주세요.")
			FaceGuideBullet("너무 어두운 환경에서 시도할 시, 인식이 어려워질 수 있습니다.")
			FaceGuideBullet("정면을 유지하고 얼굴이 화면 중앙에 오도록 맞춰 주세요.")
			FaceGuideBullet("안경 반사나 역광이 심하면 잠시 위치를 조정해 주세요.")
			FaceGuideBullet("등록이 끝날 때까지 기기를 흔들지 말고 잠시만 기다려 주세요.")

		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 8.dp),
			colors = CardDefaults.cardColors(
				containerColor = if (isProcessing) Color(0xFFFFF3E0) else Color(0xFFF5F9FF)
			),
			shape = RoundedCornerShape(16.dp)
		) {
				Column(
					modifier = Modifier
						.padding(16.dp)
						.fillMaxWidth(),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Icon(
						imageVector = statusIcon,
						contentDescription = null,
						tint = statusTint,
						modifier = Modifier.padding(bottom = 8.dp)
					)
					Text(
						text = statusMessage,
						style = MaterialTheme.typography.bodyMedium,
						textAlign = TextAlign.Center,
						fontWeight = FontWeight.SemiBold,
						color = statusTextColor
					)
				}
			}
		}
	}
}

@Composable
private fun FaceGuideBullet(text: String) {
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
		Text(text = text, style = MaterialTheme.typography.bodyMedium)
	}
}

@Composable
private fun FaceUserInfoRow(label: String, value: String) {
	Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
		Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
		Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
	}
}

@Preview(
	showBackground = true,
	name = "안면 정보 등록",
	device = "spec:width=800dp,height=1280dp,dpi=180"
)
@Composable
fun FaceRegisterPreview() {
	val previewUser = if (DebugConfig.isTesting) {
		MockDataSource.DEBUG_USERINFO_PATIENT_1
	} else {
		MockDataSource.DEBUG_USERINFO_ADMIN
	}
	val previewStatus = if (DebugConfig.isTesting) {
		"DEBUG MODE · ${previewUser.userName}(${previewUser.userId}) 사용자의 안면 등록을 대기 중입니다."
	} else {
		"안면 인식을 위해 눈을 '깜박' 해 주세요."
	}

	Capstone_TabletTheme {
		AppScaffold(
			title = FACE_REGISTER_TITLE,
			onBackClick = {}
		) { paddingValues ->
			FaceRegisterContent(
				paddingValues = paddingValues,
				userName = previewUser.userName,
				userId = previewUser.userId,
				statusMessage = previewStatus,
				isProcessing = false,
				sessionKey = 0,
				onBlinkDetected = {},
				onCameraError = {},
				onImageCaptured = {}
			)
		}
	}
}
