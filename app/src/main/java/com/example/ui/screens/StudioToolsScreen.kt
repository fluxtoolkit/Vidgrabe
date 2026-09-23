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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.SavedMediaEntity
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VidGrabCyan
import com.example.ui.theme.VidGrabIndigo
import com.example.ui.theme.VidGrabPink
import com.example.ui.theme.VidGrabPurple
import com.example.ui.viewmodel.VidGrabViewModel
import java.util.Locale

@Composable
fun StudioToolsScreen(
    viewModel: VidGrabViewModel,
    onNavigateToDownloader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedMedia by viewModel.savedMedia.collectAsState()
    val trimTarget by viewModel.trimTarget.collectAsState()
    val trimStartMs by viewModel.trimStartMs.collectAsState()
    val trimEndMs by viewModel.trimEndMs.collectAsState()
    val isTrimming by viewModel.isTrimming.collectAsState()
    val trimProgress by viewModel.trimProgress.collectAsState()

    val convertTarget by viewModel.convertTarget.collectAsState()
    val convertFormat by viewModel.convertFormat.collectAsState()
    val isConverting by viewModel.isConverting.collectAsState()
    val convertProgress by viewModel.convertProgress.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Trimmer, 1 = Converter

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Title
        item {
            Text(
                text = "VidGrab Studio Tools",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Trim clips without quality loss & extract high-fidelity audio formats",
                color = Color.Gray,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tab Row
        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurfaceCard,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = VidGrabIndigo
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Video Trimmer", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Format Converter", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Trimmer Tool
        if (selectedTabIndex == 0) {
            item {
                TrimmerSection(
                    savedMedia = savedMedia,
                    selectedMedia = trimTarget,
                    trimStartMs = trimStartMs,
                    trimEndMs = trimEndMs,
                    isTrimming = isTrimming,
                    trimProgress = trimProgress,
                    onSelectMedia = { viewModel.openTrimmer(it) },
                    onStartChanged = { viewModel.setTrimStart(it) },
                    onEndChanged = { viewModel.setTrimEnd(it) },
                    onExecuteTrim = { viewModel.executeTrim() },
                    onNavigateToDownloader = onNavigateToDownloader
                )
            }
        } else {
            // Converter Tool
            item {
                ConverterSection(
                    savedMedia = savedMedia,
                    selectedMedia = convertTarget,
                    convertFormat = convertFormat,
                    isConverting = isConverting,
                    convertProgress = convertProgress,
                    onSelectMedia = { viewModel.openConverter(it) },
                    onSelectFormat = { viewModel.setConvertFormat(it) },
                    onExecuteConvert = { viewModel.executeConvert() },
                    onNavigateToDownloader = onNavigateToDownloader
                )
            }
        }
    }
}

@Composable
fun TrimmerSection(
    savedMedia: List<SavedMediaEntity>,
    selectedMedia: SavedMediaEntity?,
    trimStartMs: Long,
    trimEndMs: Long,
    isTrimming: Boolean,
    trimProgress: Float,
    onSelectMedia: (SavedMediaEntity) -> Unit,
    onStartChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
    onExecuteTrim: () -> Unit,
    onNavigateToDownloader: () -> Unit
) {
    var showMediaPicker by remember { mutableStateOf(false) }

    val videoOnlyList = savedMedia.filter {
        !it.format.equals("mp3", ignoreCase = true) && !it.format.equals("m4a", ignoreCase = true)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Step 1: Select Video
            Text(
                text = "1. Select Video to Trim",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showMediaPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trimmer_select_video_button")
                ) {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = VidGrabCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedMedia?.title ?: if (videoOnlyList.isNotEmpty()) "Choose from ${videoOnlyList.size} videos..." else "No videos in Library yet",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMediaPicker,
                    onDismissRequest = { showMediaPicker = false },
                    modifier = Modifier.background(DarkSurfaceVariant)
                ) {
                    if (videoOnlyList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No offline videos. Tap to download one.", color = Color.Gray) },
                            onClick = {
                                showMediaPicker = false
                                onNavigateToDownloader()
                            }
                        )
                    } else {
                        videoOnlyList.forEach { video ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(video.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("${video.platform} • ${formatTime(video.durationMs)}", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    onSelectMedia(video)
                                    showMediaPicker = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (selectedMedia == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a video from above or download a clip to start trimming.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Step 2: Configure Cut Points
                Text(
                    text = "2. Set Trim Interval",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Time readout card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Start Point", color = Color.Gray, fontSize = 11.sp)
                            Text(formatTime(trimStartMs), color = VidGrabCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Clip Length", color = Color.Gray, fontSize = 11.sp)
                            val cutDuration = (trimEndMs - trimStartMs).coerceAtLeast(0L)
                            Text(
                                text = formatTime(cutDuration),
                                color = VidGrabPink,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("End Point", color = Color.Gray, fontSize = 11.sp)
                            Text(formatTime(trimEndMs), color = VidGrabPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Time Scrubber with +/- buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Start:", color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.width(44.dp))
                    IconButton(
                        onClick = { onStartChanged(trimStartMs - 1000L) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "-1s", tint = Color.LightGray)
                    }

                    Slider(
                        value = trimStartMs.toFloat(),
                        onValueChange = { onStartChanged(it.toLong()) },
                        valueRange = 0f..selectedMedia.durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = VidGrabCyan,
                            activeTrackColor = VidGrabCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onStartChanged(trimStartMs + 1000L) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "+1s", tint = Color.LightGray)
                    }
                }

                // End Time Scrubber with +/- buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("End:", color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.width(44.dp))
                    IconButton(
                        onClick = { onEndChanged(trimEndMs - 1000L) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "-1s", tint = Color.LightGray)
                    }

                    Slider(
                        value = trimEndMs.toFloat(),
                        onValueChange = { onEndChanged(it.toLong()) },
                        valueRange = 0f..selectedMedia.durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = VidGrabPurple,
                            activeTrackColor = VidGrabPurple,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onEndChanged(trimEndMs + 1000L) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "+1s", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isTrimming) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { trimProgress },
                            color = VidGrabIndigo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Trimming & Repackaging lossless video stream... ${(trimProgress * 100).toInt()}%",
                            color = VidGrabCyan,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onExecuteTrim,
                        colors = ButtonDefaults.buttonColors(containerColor = VidGrabIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("execute_trim_button")
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trim & Save to Library", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConverterSection(
    savedMedia: List<SavedMediaEntity>,
    selectedMedia: SavedMediaEntity?,
    convertFormat: String,
    isConverting: Boolean,
    convertProgress: Float,
    onSelectMedia: (SavedMediaEntity) -> Unit,
    onSelectFormat: (String) -> Unit,
    onExecuteConvert: () -> Unit,
    onNavigateToDownloader: () -> Unit
) {
    var showMediaPicker by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Step 1: Select Video
            Text(
                text = "1. Select Source Media",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showMediaPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("converter_select_media_button")
                ) {
                    Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = VidGrabPink)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedMedia?.title ?: if (savedMedia.isNotEmpty()) "Choose from ${savedMedia.size} clips..." else "No clips in Library yet",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMediaPicker,
                    onDismissRequest = { showMediaPicker = false },
                    modifier = Modifier.background(DarkSurfaceVariant)
                ) {
                    if (savedMedia.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No clips in library. Tap to download one.", color = Color.Gray) },
                            onClick = {
                                showMediaPicker = false
                                onNavigateToDownloader()
                            }
                        )
                    } else {
                        savedMedia.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("${item.format.uppercase()} • ${formatTime(item.durationMs)}", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    onSelectMedia(item)
                                    showMediaPicker = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (selectedMedia == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Choose any saved video to extract its sound as an MP3 audio track or convert its format.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Step 2: Target Format Selection
                Text(
                    text = "2. Select Target Audio / Format",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val formats = listOf(
                    Triple("mp3", "MP3 Audio (320 kbps)", "High-compatibility audio stream"),
                    Triple("m4a", "M4A AAC Audio", "Lossless Apple & Android AAC audio"),
                    Triple("mp4", "MP4 Container", "Standard MPEG-4 video/audio container")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    formats.forEach { (fmt, title, desc) ->
                        val isSelected = convertFormat.equals(fmt, ignoreCase = true)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) VidGrabPurple.copy(alpha = 0.25f) else DarkSurfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) VidGrabPurple else DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFormat(fmt) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(desc, color = Color.Gray, fontSize = 11.sp)
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = VidGrabPurple)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isConverting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { convertProgress },
                            color = VidGrabPurple,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Extracting audio track & converting... ${(convertProgress * 100).toInt()}%",
                            color = VidGrabPink,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onExecuteConvert,
                        colors = ButtonDefaults.buttonColors(containerColor = VidGrabPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("execute_convert_button")
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extract & Convert to ${convertFormat.uppercase()}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
