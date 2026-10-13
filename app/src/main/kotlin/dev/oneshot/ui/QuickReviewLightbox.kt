package dev.oneshot.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.oneshot.camera.HapticManager
import dev.oneshot.camera.MediaThumbnail
import dev.oneshot.ui.theme.OneShotTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Optical Ground Glass Quick Review Lightbox (BRAND.md Darkroom Aesthetics)
 * Enables instant in-app inspection of the most recently captured photo, motion photo, or video.
 */
@Composable
fun QuickReviewLightbox(
    media: MediaThumbnail?,
    onDismiss: () -> Unit,
    onOpenSystemGallery: () -> Unit,
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
) {
    if (media == null) return

    val dateFormatted = rememberDateString(media.dateAddedSec)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF0101010))
            .clickable(onClick = {
                hapticManager.tick()
                onDismiss()
            })
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}, // Prevent dismiss when tapping content
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Title & Metadata & Close Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (media.isVideo) "底片回放 · 动态影像" else "底片回放 · 决定性瞬间",
                        color = OneShotTheme.AmberGrain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OneShotTheme.MonospaceFont
                    )
                    Text(
                        text = "${media.displayName}  |  $dateFormatted",
                        color = OneShotTheme.TitaniumGray,
                        fontSize = 10.sp,
                        fontFamily = OneShotTheme.MonospaceFont
                    )
                }

                // Close Pill
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OneShotTheme.ChassisCard)
                        .border(1.dp, OneShotTheme.TitaniumBorder, CircleShape)
                        .clickable {
                            hapticManager.tick()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = OneShotTheme.FilmSilver,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Central Optical Media Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF000000))
                    .border(1.dp, OneShotTheme.TitaniumBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (media.bitmap != null) {
                    Image(
                        bitmap = media.bitmap.asImageBitmap(),
                        contentDescription = "底片微缩预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "底片加载中...",
                        color = OneShotTheme.TitaniumGray,
                        fontSize = 12.sp,
                        fontFamily = OneShotTheme.MonospaceFont
                    )
                }

                if (media.isVideo) {
                    // Video Play Badge Center
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC121212))
                            .border(1.dp, OneShotTheme.AmberGrain, RoundedCornerShape(20.dp))
                            .clickable {
                                hapticManager.tick()
                                onOpenSystemGallery()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "▶", color = OneShotTheme.AmberGrain, fontSize = 14.sp)
                            Text(
                                text = "调起播放器",
                                color = OneShotTheme.FilmSilver,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Bottom Action Bar: Open in Gallery & Return to Viewfinder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary Button: Back to Viewfinder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OneShotTheme.ChassisCard)
                        .border(1.dp, OneShotTheme.TitaniumBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            hapticManager.tick()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "返回取景器",
                        color = OneShotTheme.FilmSilver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Primary Button: Open System Gallery
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OneShotTheme.AmberGrainMuted)
                        .border(1.dp, OneShotTheme.AmberGrain, RoundedCornerShape(12.dp))
                        .clickable {
                            hapticManager.tick()
                            onOpenSystemGallery()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "调起系统相册",
                        color = OneShotTheme.AmberGrain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberDateString(epochSec: Long): String {
    if (epochSec <= 0) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epochSec * 1000L))
}
