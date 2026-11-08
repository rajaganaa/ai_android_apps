package com.example.clipsaver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clipsaver.download.TaskState
import com.example.clipsaver.ui.ClipSaverTheme
import com.example.clipsaver.ui.DownloadsScreen
import com.example.clipsaver.ui.DownloadsViewModel
import com.example.clipsaver.ui.HomeScreen
import com.example.clipsaver.ui.HomeViewModel
import com.example.clipsaver.ui.SettingsScreen
import com.example.clipsaver.ui.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val homeVm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only handle a share on a fresh start, not after rotation.
        if (savedInstanceState == null) handleIntent(intent)
        setContent { ClipSaverTheme { AppRoot(homeVm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            homeVm.onSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))
        }
    }
}

@Composable
private fun AppRoot(homeVm: HomeViewModel) {
    val downloadsVm: DownloadsViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()

    val ui by homeVm.ui.collectAsStateWithLifecycle()
    val tasks by downloadsVm.tasks.collectAsStateWithLifecycle()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val settingsUi by settingsVm.ui.collectAsStateWithLifecycle()
    val engine by settingsVm.engineState.collectAsStateWithLifecycle()
    val activeCount = tasks.count { it.state == TaskState.RUNNING || it.state == TaskState.QUEUED }

    // Ask once for permission to show the download progress notification (Android 13+).
    val ctx = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = ui.tab == 0, onClick = { homeVm.selectTab(0) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = ui.tab == 1, onClick = { homeVm.selectTab(1) },
                    icon = {
                        BadgedBox(badge = { if (activeCount > 0) Badge { Text("$activeCount") } }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        }
                    },
                    label = { Text("Downloads") },
                )
                NavigationBarItem(
                    selected = ui.tab == 2, onClick = { homeVm.selectTab(2) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                )
            }
        },
    ) { padding ->
        val m = Modifier.padding(padding)
        when (ui.tab) {
            0 -> HomeScreen(ui, engine, homeVm, m)
            1 -> DownloadsScreen(tasks, downloadsVm, m)
            else -> SettingsScreen(settings, settingsUi, engine, settingsVm, m)
        }
    }
}
