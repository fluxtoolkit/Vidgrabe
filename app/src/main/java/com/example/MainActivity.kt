package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DownloadStatus
import com.example.ui.components.OfflinePlayerModal
import com.example.ui.screens.BatchQueueScreen
import com.example.ui.screens.DownloaderScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.StudioToolsScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.VidGrabIndigo
import com.example.ui.theme.VidGrabTheme
import com.example.ui.viewmodel.VidGrabViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VidGrabTheme {
                VidGrabApp()
            }
        }
    }
}

@Composable
fun VidGrabApp(viewModel: VidGrabViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val playingMedia by viewModel.playingMedia.collectAsState()
    val downloadQueue by viewModel.downloadQueue.collectAsState()
    val savedMedia by viewModel.savedMedia.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    val activeDownloadsCount = downloadQueue.count { it.status == DownloadStatus.DOWNLOADING.name || it.status == DownloadStatus.QUEUED.name }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                // Tab 0: Downloader
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Grab Video",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Grab", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VidGrabIndigo,
                        selectedTextColor = VidGrabIndigo,
                        indicatorColor = VidGrabIndigo.copy(alpha = 0.2f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_downloader")
                )

                // Tab 1: Batch Queue
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        if (activeDownloadsCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = VidGrabIndigo) {
                                        Text("$activeDownloadsCount", color = Color.White)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Queue,
                                    contentDescription = "Batch Downloads",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Queue,
                                contentDescription = "Batch Downloads",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = { Text("Batch", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VidGrabIndigo,
                        selectedTextColor = VidGrabIndigo,
                        indicatorColor = VidGrabIndigo.copy(alpha = 0.2f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_batch")
                )

                // Tab 2: Offline Library
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        if (savedMedia.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = Color(0xFF374151)) {
                                        Text("${savedMedia.size}", color = Color.LightGray)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "Offline Library",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Offline Library",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = { Text("Library", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VidGrabIndigo,
                        selectedTextColor = VidGrabIndigo,
                        indicatorColor = VidGrabIndigo.copy(alpha = 0.2f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_library")
                )

                // Tab 3: Studio Tools
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Studio Tools",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Studio", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VidGrabIndigo,
                        selectedTextColor = VidGrabIndigo,
                        indicatorColor = VidGrabIndigo.copy(alpha = 0.2f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_tab_studio")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DownloaderScreen(
                    viewModel = viewModel,
                    onNavigateToBatch = { selectedTab = 1 }
                )
                1 -> BatchQueueScreen(
                    viewModel = viewModel,
                    onNavigateToLibrary = { selectedTab = 2 }
                )
                2 -> LibraryScreen(
                    viewModel = viewModel,
                    onNavigateToDownloader = { selectedTab = 0 },
                    onOpenTrimmer = { media ->
                        viewModel.openTrimmer(media)
                        selectedTab = 3
                    },
                    onOpenConverter = { media ->
                        viewModel.openConverter(media)
                        selectedTab = 3
                    }
                )
                3 -> StudioToolsScreen(
                    viewModel = viewModel,
                    onNavigateToDownloader = { selectedTab = 0 }
                )
            }
        }

        // Fullscreen Offline Media Player Overlay
        playingMedia?.let { media ->
            OfflinePlayerModal(
                media = media,
                onClose = { viewModel.closePlayer() },
                onTrim = {
                    viewModel.closePlayer()
                    viewModel.openTrimmer(it)
                    selectedTab = 3
                },
                onShare = { viewModel.shareMedia(it) }
            )
        }
    }
}

// Backwards compatibility for existing GreetingScreenshotTest
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun VidGrabAppPreview() {
    VidGrabTheme {
        VidGrabApp()
    }
}
