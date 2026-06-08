package com.inu.capstone_mobile.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inu.capstone_mobile.data.models.UserCabinet

@Composable
fun StatusCard(modifier: Modifier = Modifier, temp: Float?, humid: Float?, isFanOn: Boolean?) {
    BaseCard(modifier = modifier, title = "온도 | 습도") {
        Column { // Row와 아래 Text를 세로로 배치하기 위해 Column 추가
            Row(verticalAlignment = Alignment.Bottom) {
                val tempColor = if ((temp ?: Float.MIN_VALUE) >= 30f) Color.Red else MaterialTheme.colorScheme.onSurface
                val humidColor = if ((humid ?: Float.MIN_VALUE) >= 60f) Color.Red else Color.Blue
                // null일 때 "--"라고 나오게 엘비스 연산자(?:) 사용
                Text("${temp ?: "--"}°", style = MaterialTheme.typography.titleLarge, color = tempColor)
                Text("/ ${humid ?: "--"}%", style = MaterialTheme.typography.titleLarge, color = humidColor)
            }
        }
    }
}

@Composable
fun AdminSlotCard(modifier: Modifier = Modifier, medList: List<Pair<String, Int>>) {
    BaseCard(modifier = modifier, title = "관리자함 재고") {
        medList.forEach { (name, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name)
                Text("${count}개", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PublicSlotCard(modifier: Modifier = Modifier, slots: List<Pair<Boolean, Boolean>>) {
    BaseCard(modifier = modifier, title = "공용 보관함 현황") {
        Column {
            slots.chunked(5).forEach { rowSlots ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    rowSlots.forEach { (isLocked, hasMed) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (hasMed) Color.Red else Color.Green
                            )
                            Text(if (hasMed) "약품있음" else "비었음", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserCabinetGridCard(modifier: Modifier, title: String, cabinet: UserCabinet) {
    BaseCard(modifier = modifier, title = "") {
        val mealLabels = listOf("아침", "점심", "저녁")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom =16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(3) { index ->
                    val isPresent = cabinet.isSlotOccupied(index)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = mealLabels[index],
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .aspectRatio(1.7f)
                                .background(
                                    color = if (isPresent) Color(0xFF00C853) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}