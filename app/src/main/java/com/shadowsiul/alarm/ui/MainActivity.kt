package com.shadowsiul.alarm.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shadowsiul.alarm.AlarmApp
import com.shadowsiul.alarm.data.AlarmPreferences
import com.shadowsiul.alarm.ui.theme.AlarmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshUpcomingNotification()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNeededPermissions()

        setContent {
            AlarmTheme {
                val nav = rememberNavController()
                val vm: AlarmViewModel = viewModel()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        AlarmListScreen(
                            viewModel = vm,
                            onAdd = { nav.navigate("edit/0") },
                            onEdit = { id -> nav.navigate("edit/$id") },
                            onOptions = { nav.navigate("options") },
                        )
                    }
                    composable("options") {
                        OptionsScreen(
                            onBack = { nav.popBackStack() },
                            showUpcomingNotification = AlarmPreferences.showUpcomingNotification(this@MainActivity),
                            onShowUpcomingNotificationChange = vm::setShowUpcomingNotification,
                            onLanguageSelected = vm::setLanguage,
                        )
                    }
                    composable(
                        route = "edit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: 0L
                        EditAlarmScreen(
                            alarmId = id,
                            viewModel = vm,
                            onDone = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshUpcomingNotification()
    }

    private fun refreshUpcomingNotification() {
        lifecycleScope.launch(Dispatchers.IO) {
            (application as AlarmApp).repository.refreshUpcomingNotification()
        }
    }

    private fun requestNeededPermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }
    }
}
