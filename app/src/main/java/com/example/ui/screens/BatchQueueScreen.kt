package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.PlatformType
import com.example.data.model.VideoQuality
import com.example.ui.components.PlatformBadge
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VidGrabCyan
import com.example.ui.theme.VidGrabIndigo
import com.example.ui.theme.VidGrabPink
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.VidGrabViewModel
import java.util.Locale

@Composable
fun BatchQueueScreen(
    viewModel: VidGrabViewModel,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadQueue by viewModel.downloadQueue.collectAsState()
    val batchUrlsText by viewModel.batchUrlsText.collectAsState()
    val batchQuality by viewModel.batchQuality.collectAsState()

    var isAddingBatchExpanded by remember { mutableStateOf(false) }

    val activeCount = downloadQueue.count { it.status == DownloadStatus.DOWNLOADING.name }
    val queuedCount = downloadQueue.count { it.status == DownloadStatus.QUEUED.name }
    val completedCount = downloadQueue.count { it.status == DownloadStatus.COMPLETED.name }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Title Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Batch Downloads",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Queue & download multiple video clips simultaneously",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = { isAddingBatchExpanded = !isAddingBatchExpanded },
                    colors = ButtonDefaults.buttonColors(containerColor = VidGrabIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add Links",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAddingBatchExpanded) "Hide" else "Add Links", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Multi-URL Input Section (Expandable or always visible)
        item {
            AnimatedVisibility(visible = isAddingBatchExpanded || downloadQueue.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste Multiple Video Links",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = { viewModel.loadSampleBatch() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("load_sample_batch_button")
                            ) {
                                Text("Load Sample Batch", fontSize = 11.sp, color = VidGrabCyan)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = batchUrlsText,
                            onValueChange = { viewModel.setBatchUrlsText(it) },
                            placeholder = {
                                Text(
                                    "Paste one URL per line...\nhttps://youtube.com/...\nhttps://tiktok.com/...\nhttps://instagram.com/...",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VidGrabIndigo,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            minLines = 4,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("batch_urls_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Quality for Batch
                        Text(
                            text = "Batch Quality Target:",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(VideoQuality.Q_1080P, VideoQuality.Q_720P, VideoQuality.AUDIO_MP3).forEach { q ->
                                val isSelected = batchQuality == q
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) VidGrabIndigo else DarkSurfaceVariant)
                                        .border(1.dp, if (isSelected) VidGrabIndigo else DarkBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setBatchQuality(q) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (q.isAudioOnly) "MP3 Audio" else q.label.substringBefore(" "),
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.startBatchDownload()
                                isAddingBatchExpanded = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VidGrabIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_batch_button")
                        ) {
                            Icon(imageVector = Icons.Default.Queue, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val count = batchUrlsText.lines().filter { it.isNotBlank() }.size
                            Text(
                                text = if (count > 0) "Queue $count Downloads" else "Start Batch Download",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Summary Bar & Global Controls
        if (downloadQueue.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Queue Status",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$activeCount Downloading", color = VidGrabCyan, fontSize = 11.sp)
                                Text(" • ", color = Color.Gray, fontSize = 11.sp)
                                Text("$queuedCount Queued", color = WarningAmber, fontSize = 11.sp)
                                Text(" • ", color = Color.Gray, fontSize = 11.sp)
                                Text("$completedCount Done", color = SuccessGreen, fontSize = 11.sp)
                            }
                        }

                        Row {
                            if (activeCount > 0) {
                                IconButton(onClick = { viewModel.pauseAllDownloads() }) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause All", tint = WarningAmber)
                                }
                            } else if (queuedCount > 0) {
                                IconButton(onClick = { viewModel.resumeAllDownloads() }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume All", tint = SuccessGreen)
                                }
                            }
                            if (completedCount > 0) {
                                IconButton(onClick = { viewModel.clearCompletedDownloads() }) {
                                    Icon(Icons.Default.ClearAll, contentDescription = "Clear Completed", tint = Color.LightGray)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // List of Download Items
        if (downloadQueue.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Queue,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Download Queue is Empty",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paste multiple links or load our sample batch to test simultaneous downloading.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.loadSampleBatch()
                                isAddingBatchExpanded = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VidGrabIndigo)
                        ) {
                            Text("Try Sample Batch Queue")
                        }
                    }
                }
            }
        } else {
            items(downloadQueue, key = { it.id }) { item ->
                DownloadQueueItemCard(
                    item = item,
                    onPause = { viewModel.pauseDownload(item.id) },
                    onResume = { viewModel.resumeDownload(item.id) },
                    onCancel = { viewModel.cancelDownload(item.id) },
                    onViewInLibrary = onNavigateToLibrary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun DownloadQueueItemCard(
    item: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onViewInLibrary: () -> Unit
) {
    val platformType = PlatformType.values().find {
        it.displayName.equals(item.platform, ignoreCase = true)
    } ?: PlatformType.DIRECT

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.status == DownloadStatus.DOWNLOADING.name) VidGrabIndigo else DarkBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    PlatformBadge(platform = platformType, isSmall = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge
                StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when (item.status) {
                    DownloadStatus.COMPLETED.name -> SuccessGreen
                    DownloadStatus.FAILED.name -> ErrorRed
                    DownloadStatus.PAUSED.name -> WarningAmber
                    else -> VidGrabIndigo
                },
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress details (percentage, speed, MBs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${formatBytes(item.bytesDownloaded)} / ${formatBytes(item.totalBytes)}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                if (item.status == DownloadStatus.DOWNLOADING.name && item.speedBytesPerSec > 0) {
                    Text(
                        text = "${formatBytes(item.speedBytesPerSec)}/s",
                        color = VidGrabCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (item.status == DownloadStatus.FAILED.name) {
                    Text(
                        text = item.errorMessage ?: "Failed",
                        color = ErrorRed,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.quality} • ${item.format.uppercase()}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Row {
                    when (item.status) {
                        DownloadStatus.DOWNLOADING.name -> {
                            IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = WarningAmber, modifier = Modifier.size(18.dp))
                            }
                        }
                        DownloadStatus.PAUSED.name -> {
                            IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                        DownloadStatus.COMPLETED.name -> {
                            IconButton(onClick = onViewInLibrary, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Folder, contentDescription = "View", tint = VidGrabCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, icon) = when (status) {
        DownloadStatus.DOWNLOADING.name -> Pair(VidGrabCyan, Icons.Default.PlayArrow)
        DownloadStatus.QUEUED.name -> Pair(WarningAmber, Icons.Default.HourglassTop)
        DownloadStatus.PAUSED.name -> Pair(Color.LightGray, Icons.Default.Pause)
        DownloadStatus.COMPLETED.name -> Pair(SuccessGreen, Icons.Default.CheckCircle)
        DownloadStatus.FAILED.name -> Pair(ErrorRed, Icons.Default.Error)
        else -> Pair(Color.Gray, Icons.Default.HourglassTop)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
        else -> "$bytes B"
    }
}
