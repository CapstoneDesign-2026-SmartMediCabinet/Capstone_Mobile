package com.inu.capstone_mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.inu.capstone_mobile.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.inu.capstone_mobile.viewmodel.AuthViewModel
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme
import com.inu.capstone_mobile.data.DebugConfig

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    LoginScreenContent(
        // 💡 1. ID, PW와 함께 '진동벨(onResult)'을 던져줍니다!
        onLoginClick = { id, pw, onResult ->
            // 뷰모델에게 비동기 로그인을 시키고, 뷰모델이 자신의 진동벨을 울리면({ isSuccess -> })
            viewModel.login(id, pw) { isSuccess ->
                if (isSuccess) {
                    onLoginSuccess() // 진짜 로그인 성공이면 화면을 넘기고
                }
                onResult(isSuccess) // UI 쪽 진동벨도 같이 울려줍니다!
            }
        }
    )
}

@Composable
fun LoginScreenContent(
    // 💡 2. 기존의 '-> Boolean' 대신, 결과(Boolean)를 나중에 받을 진동벨 함수를 파라미터로 받습니다.
    onLoginClick: (String, String, (Boolean) -> Unit) -> Unit
) {
    var inputId by remember { mutableStateOf("") }
    var inputPw by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        if (DebugConfig.isTesting) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                color = Color(0xFFFDEAEA),
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color(0xFFB00020).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, CircleShape)
                    )
                    Text("DEBUG MODE", color = Color(0xFFB00020), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Login Logo",
                modifier = Modifier.size(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("로그인", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = inputId,
                onValueChange = { inputId = it },
                label = { Text("ID") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inputPw,
                onValueChange = { inputPw = it },
                label = { Text("비밀번호") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    // 💡 3. 버튼을 누르면 대기하는 게 아니라, "끝나면 이 중괄호 { } 안의 코드를 실행해!" 하고 넘깁니다.
                    onLoginClick(inputId, inputPw) { isSuccess ->
                        if (!isSuccess) {
                            errorMessage = "아이디 또는 비밀번호가 틀립니다."
                        } else {
                            errorMessage = "" // 성공 시 에러메시지 초기화
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.3f).height(50.dp)
            ) {
                Text("로그인", fontSize = 18.sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "로그인 화면", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun LoginScreenPreview() {
    Capstone_TabletTheme {
        LoginScreenContent(
            // 프리뷰에서는 그냥 무조건 실패(false) 진동벨을 울리도록 가짜 껍데기 세팅
            onLoginClick = { _, _, onResult -> onResult(false) }
        )
    }
}