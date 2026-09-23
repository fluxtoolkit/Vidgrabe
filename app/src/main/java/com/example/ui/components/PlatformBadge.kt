package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlatformType
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.InstagramPurple
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.VidGrabIndigo
import com.example.ui.theme.YouTubeRed

@Composable
fun PlatformBadge(
    platform: PlatformType,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    val (bgColor, textColor, icon) = when (platform) {
        PlatformType.YOUTUBE -> Triple(YouTubeRed, Color.White, Icons.Default.PlayCircle)
        PlatformType.TIKTOK -> Triple(Color(0xFF1E1E2E), TikTokCyan, Icons.Default.MusicNote)
        PlatformType.INSTAGRAM -> Triple(InstagramPurple, Color.White, Icons.Default.CameraAlt)
        PlatformType.FACEBOOK -> Triple(FacebookBlue, Color.White, Icons.Default.Public)
        PlatformType.DIRECT -> Triple(VidGrabIndigo, Color.White, Icons.Default.OndemandVideo)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = 0.9f))
            .padding(horizontal = if (isSmall) 6.dp else 10.dp, vertical = if (isSmall) 3.dp else 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = platform.displayName,
                tint = textColor,
                modifier = Modifier.size(if (isSmall) 12.dp else 16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = platform.displayName,
                color = textColor,
                fontSize = if (isSmall) 11.sp else 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
