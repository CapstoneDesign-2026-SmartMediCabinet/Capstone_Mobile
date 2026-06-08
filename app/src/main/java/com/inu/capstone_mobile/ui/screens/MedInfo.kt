package com.inu.capstone_mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.inu.capstone_mobile.data.source.MockDataSource
import com.inu.capstone_mobile.ui.components.AppScaffold
import com.inu.capstone_mobile.ui.components.BaseCard
import com.inu.capstone_mobile.ui.theme.Capstone_TabletTheme

private const val SCAFFOLD_TITLE = "약품 상세 정보"

// ──────────────────────────────────────────────
// 데이터 홀더
// ──────────────────────────────────────────────
data class MedicineInfoUi(
    val medicine_Id: String,      // 약품 고유번호 (item_seq)
    val name: String,            // 약품명
    val manufacturer: String,    // 제조사
    val type: String,            // 약품 종류
    val imageUrl: String? = null // 이미지 URL (null이면 placeholder)
)


// ──────────────────────────────────────────────
// 전체 화면 진입점 (NavHost에서 호출)
// ──────────────────────────────────────────────
@Composable
fun MedInfo(
    medicineInfo: MedicineInfoUi,
    onBack: () -> Unit
) {
    AppScaffold(
        title = SCAFFOLD_TITLE,
        onBackClick = onBack
    ) { paddingValues ->
        MedInfoContent(info = medicineInfo, paddingValues = paddingValues)
    }
}

// ──────────────────────────────────────────────
// 콘텐츠 (Preview에서도 직접 사용)
// ──────────────────────────────────────────────
@Composable
fun MedInfoContent(
    info: MedicineInfoUi,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFDDEAF8))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        BaseCard(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.80f),
            title = ""
        ) {
            Row(modifier = Modifier.fillMaxSize()) {

                // ── 좌측 1/3 : 제목 + 이미지 ──────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "약품 정보",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3F3F3)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!info.imageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = info.imageUrl,
                                contentDescription = "${info.name} 이미지",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        } else {
                            Text("이미지 없음", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                // ── 세로 구분선 ────────────────────────────────────
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 20.dp),
                    thickness = 1.dp,
                    color = Color(0xFFE0E0E0)
                )

                // ── 우측 2/3 : 약품 상세 정보 ──────────────────────
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .padding(horizontal = 36.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically)
                ) {
                    MedInfoRow(label = "약품 고유번호", value = info.medicine_Id)
                    MedInfoRow(label = "약품명", value = info.name)
                    MedInfoRow(label = "제조사", value = info.manufacturer)
                    MedInfoRow(label = "약품 종류", value = info.type)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// 재사용 정보 행
// ──────────────────────────────────────────────
@Composable
private fun MedInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 10.dp),
            thickness = 0.5.dp,
            color = Color(0xFFE0E0E0)
        )
    }
}

// ──────────────────────────────────────────────
// Preview
// ──────────────────────────────────────────────
@Preview(
    showBackground = true,
    name = "약품 상세 정보",
    device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
fun MedInfoPreview() {
    Capstone_TabletTheme {
        AppScaffold(title = SCAFFOLD_TITLE) { paddingValues ->
            MedInfoContent(
                info = MedicineInfoUi(
                    medicine_Id = MockDataSource.DEBUG_MED_INFO_ID,
                    name = MockDataSource.DEBUG_MED_INFO_NAME,
                    manufacturer = MockDataSource.DEBUG_MED_INFO_MANUFACTURER,
                    type = MockDataSource.DEBUG_MED_INFO_TYPE,
                    imageUrl = MockDataSource.DEBUG_MED_INFO_IMAGE_URL
                ),
                paddingValues = paddingValues
            )
        }
    }
}
