package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.abs

private val MonogramPalettes = listOf(
    Color(0xFF0E7490) to Color(0xFF164E63),
    Color(0xFFBE185D) to Color(0xFF831843),
    Color(0xFF4338CA) to Color(0xFF312E81),
    Color(0xFF0F766E) to Color(0xFF134E4A),
    Color(0xFFB45309) to Color(0xFF78350F),
    Color(0xFF6D28D9) to Color(0xFF4C1D95)
)

@Composable
fun ChannelLogo(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val shape = RoundedCornerShape(14.dp)
    val palette = remember(name) {
        MonogramPalettes[abs(name.hashCode()) % MonogramPalettes.size]
    }
    val initials = remember(name) {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> "TV"
        }
    }

    var loadFailed by remember(logoUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(palette.first, palette.second)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank() && !loadFailed) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1526).copy(alpha = 0.72f))
                    .padding(6.dp),
                onError = { loadFailed = true }
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.34f).sp
                ),
                color = Color.White
            )
        }
    }
}
