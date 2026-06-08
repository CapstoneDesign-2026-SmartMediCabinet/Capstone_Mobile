package com.inu.capstone_mobile.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBackClick: (() -> Unit)? = null,
    onFaceRegisterClick: (() -> Unit)? = null,
    onMedRegiClick: (() -> Unit)? = null,
    onPrescriptionManageClick: (() -> Unit)? = null,
    onUserCabinetManageClick: (() -> Unit)? = null,
    onLogoutClick: () -> Unit = {}, // 💡 로그아웃 눌렀을 때 실행할 함수
    onExitClick: () -> Unit = {},   // 💡 종료 눌렀을 때 실행할 함수
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    // 서랍(Drawer) 상태 관리 (열림/닫힘)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // 서랍을 열고 닫는 애니메이션을 실행할 코루틴 스코프
    val scope = rememberCoroutineScope()

    // 💡 화면 전체를 서랍장(ModalNavigationDrawer)으로 감쌉니다.
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "스마트약품장",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()

                Spacer(Modifier.height(8.dp))

                if (onFaceRegisterClick != null) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Face, contentDescription = "안면 정보 등록") },
                        label = { Text("안면 정보 등록") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onFaceRegisterClick()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                if (onMedRegiClick != null) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = "새 약품 등록") },
                        label = { Text("새 약품 등록") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onMedRegiClick()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                if (onPrescriptionManageClick != null) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Description, contentDescription = "약품 관리") },
                        label = { Text("약품 관리") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onPrescriptionManageClick()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                if (onUserCabinetManageClick != null) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = "사용자 약품장 관리") },
                        label = { Text("사용자 약품장 관리") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onUserCabinetManageClick()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // 1. 로그아웃 메뉴
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "로그아웃") },
                    label = { Text("로그아웃") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() } // 서랍 닫고
                        onLogoutClick()                      // 로그아웃 실행!
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // 2. 앱 종료 메뉴
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Close, contentDescription = "종료") },
                    label = { Text("종료") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() } // 서랍 닫고
                        onExitClick()                        // 종료 실행!
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        // 기존의 Scaffold는 서랍 안에 쏙 들어갑니다.
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    // 💡 핵심 로직: 뒤로가기 버튼이 있으면 뒤로가기를 띄우고, 없으면 햄버거 메뉴 띄움!
                    navigationIcon = {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "메뉴 열기")
                            }
                        }
                    },
                    actions = { actions() }
                )
            },
            floatingActionButton = floatingActionButton,
            content = { paddingValues ->
                content(paddingValues)
            }
        )
    }
}