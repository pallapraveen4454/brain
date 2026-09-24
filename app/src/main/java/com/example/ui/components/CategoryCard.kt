package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun CategoryCard(
    title: String,
    questionsCount: String? = null,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false,
    nextAvailableDate: String? = null,
    testTag: String = "category_card"
) {
    GlassCard(
        modifier = modifier
            .height(132.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        borderColor = if (isCompleted) Color(0xFF2ECC71).copy(alpha = 0.6f) else accentColor.copy(alpha = 0.35f),
        elevation = 6.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isCompleted) Color(0xFF2ECC71).copy(alpha = 0.15f) else accentColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        radius = 200f
                    )
                )
                .padding(12.dp)
        ) {
            // Top Completed Badge if completed
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2ECC71).copy(alpha = 0.25f))
                        .border(0.8.dp, Color(0xFF2ECC71).copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color(0xFF2ECC71),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "DONE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF2ECC71)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) Color(0xFF2ECC71).copy(alpha = 0.2f) else accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else icon,
                        contentDescription = title,
                        tint = if (isCompleted) Color(0xFF2ECC71) else accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isCompleted) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!nextAvailableDate.isNullOrBlank()) "Next: $nextAvailableDate" else "Completed Today",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFF2ECC71),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!questionsCount.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = questionsCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
