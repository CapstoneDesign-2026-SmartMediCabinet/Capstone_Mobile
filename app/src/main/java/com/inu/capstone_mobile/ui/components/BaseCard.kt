package com.inu.capstone_mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun BaseCard(modifier: Modifier = Modifier, title: String, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if(title.isNotBlank()){
                Text(text = title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,      // 글자 가운데 정렬
                    style = MaterialTheme.typography.titleMedium, // 크기 살짝 키움
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF212121)
                )
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

@Composable
fun UserCard(modifier: Modifier = Modifier, title: String, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if(title.isNotBlank()){
                Text(text = title,
                    modifier = Modifier.align(Alignment.End),
                    textAlign = TextAlign.End,      // 글자 정렬
                    style = MaterialTheme.typography.bodySmall, // 크기
                    fontWeight = FontWeight.ExtraLight,
                    color = Color.Gray,
                    fontSize = 24.sp,
                    )
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}