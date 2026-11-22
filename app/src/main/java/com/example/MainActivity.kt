package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.example.prayer.AzanNotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.components.MiniAudioPlayerBar
import com.example.ui.screens.HijriCalendarScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.QuranHomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsAndMonetizationScreen
import com.example.ui.screens.SurahReaderScreen
import com.example.ui.theme.AlQuranTheme
import com.example.ui.theme.LocalIsDarkTheme

object AppRoutes {
    const val QURAN = "quran"
    const val SEARCH = "search"
    const val PRAYER = "prayer"
    const val HIJRI = "hijri"
    const val SETTINGS = "settings"
    const val READER = "reader/{surahNomor}/{ayahNomor}"

    fun readerRoute(surahNomor: Int, ayahNomor: Int = 1): String = "reader/$surahNomor/$ayahNomor"
}

private data class TopNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    private val openPrayerTabTrigger = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            AlQuranTheme(darkModeSetting = settings.darkModeSetting) {
                AlQuranAppRoot(
                    viewModel = viewModel,
                    openPrayerTab = openPrayerTabTrigger.value,
                    onConsumeOpenPrayerTab = { openPrayerTabTrigger.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(AzanNotificationHelper.EXTRA_OPEN_PRAYER_TAB, false) == true) {
            AzanNotificationHelper.stopAzanAlertTone()
            openPrayerTabTrigger.value = true
        }
    }
}

@Composable
fun AlQuranAppRoot(
    viewModel: MainViewModel,
    openPrayerTab: Boolean = false,
    onConsumeOpenPrayerTab: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isDark = LocalIsDarkTheme.current

    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val downloadedCount by viewModel.downloadedSurahCount.collectAsStateWithLifecycle()
    val totalAyahCount by viewModel.totalAyahCount.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val readerState by viewModel.readerState.collectAsStateWithLifecycle()
    val locationMessage by viewModel.locationDetectMessage.collectAsStateWithLifecycle()

    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.id }.toSet() }

    LaunchedEffect(audioState.errorMessage) {
        val msg = audioState.errorMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearAudioError()
        }
    }

    val navItems = remember {
        listOf(
            TopNavItem(
                route = AppRoutes.QURAN,
                labelRes = R.string.nav_quran,
                selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
                unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
                testTag = "nav_quran"
            ),
            TopNavItem(
                route = AppRoutes.SEARCH,
                labelRes = R.string.nav_search,
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search,
                testTag = "nav_search"
            ),
            TopNavItem(
                route = AppRoutes.PRAYER,
                labelRes = R.string.nav_prayer,
                selectedIcon = Icons.Filled.AccessTime,
                unselectedIcon = Icons.Outlined.AccessTime,
                testTag = "nav_prayer"
            ),
            TopNavItem(
                route = AppRoutes.HIJRI,
                labelRes = R.string.nav_hijri,
                selectedIcon = Icons.Filled.CalendarMonth,
                unselectedIcon = Icons.Outlined.CalendarMonth,
                testTag = "nav_hijri"
            ),
            TopNavItem(
                route = AppRoutes.SETTINGS,
                labelRes = R.string.nav_settings,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings"
            )
        )
    }

    val isReaderScreen = currentDestination?.route?.startsWith("reader/") == true

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(openPrayerTab) {
        if (openPrayerTab) {
            navigateTopLevel(AppRoutes.PRAYER)
            onConsumeOpenPrayerTab()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                Column {
                    MiniAudioPlayerBar(
                        audioState = audioState,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onPrevious = viewModel::previousAyah,
                        onNext = viewModel::nextAyah,
                        onToggleRepeat = viewModel::toggleRepeatAyah,
                        onCycleSpeed = viewModel::cyclePlaybackSpeed,
                        onStop = viewModel::stopAudio
                    )
                    if (!useNavigationRail && !isReaderScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                val label = stringResource(item.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && !isReaderScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val label = stringResource(item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.QURAN,
                        modifier = Modifier.widthIn(max = 840.dp)
                    ) {
                        composable(AppRoutes.QURAN) {
                            QuranHomeScreen(
                                surahs = surahs,
                                bookmarks = bookmarks,
                                downloadedCount = downloadedCount,
                                syncProgress = syncProgress,
                                settings = settings,
                                onOpenSurah = { surahNo, ayahNo ->
                                    navController.navigate(AppRoutes.readerRoute(surahNo, ayahNo))
                                },
                                onStartFullDownload = viewModel::downloadAllSurahsOffline,
                                onCancelFullDownload = viewModel::cancelFullDownload,
                                onRemoveBookmark = viewModel::removeBookmark,
                                onToggleTheme = { viewModel.toggleQuickDarkMode(isDark) },
                                onOpenSearch = { navigateTopLevel(AppRoutes.SEARCH) },
                                onOpenPrayerTimes = { navigateTopLevel(AppRoutes.PRAYER) },
                                onOpenAdSenseSettings = { navigateTopLevel(AppRoutes.SETTINGS) }
                            )
                        }

                        composable(AppRoutes.SEARCH) {
                            SearchScreen(
                                searchState = searchState,
                                downloadedSurahCount = downloadedCount,
                                totalAyahCount = totalAyahCount,
                                syncProgress = syncProgress,
                                onQueryChange = viewModel::updateSearchQuery,
                                onOpenSurah = { surahNo, ayahNo ->
                                    navController.navigate(AppRoutes.readerRoute(surahNo, ayahNo))
                                },
                                onDownloadAllOffline = viewModel::downloadAllSurahsOffline
                            )
                        }

                        composable(AppRoutes.PRAYER) {
                            PrayerTimesScreen(
                                settings = settings,
                                locationMessage = locationMessage,
                                onClearLocationMessage = viewModel::clearLocationMessage,
                                onDetectLocation = { viewModel.detectLocationFromDevice(silent = false) },
                                onAutoRefreshLocationIfPermitted = { viewModel.detectLocationFromDevice(silent = true) },
                                onSelectCity = viewModel::selectCity,
                                onAzanToggle = viewModel::setAzanToggle,
                                onPreReminderToggle = viewModel::setAzanPreReminder,
                                onSoundModeChange = viewModel::setAzanSoundMode,
                                onTestAzanNotification = viewModel::sendTestAzanNotification,
                                onScheduleTestAlarm = viewModel::scheduleTestAzanAlarmIn5Seconds,
                                onRescheduleAlarms = viewModel::rescheduleAzanAlarms,
                                onOpenAdSenseSettings = { navigateTopLevel(AppRoutes.SETTINGS) }
                            )
                        }

                        composable(AppRoutes.HIJRI) {
                            HijriCalendarScreen(
                                settings = settings,
                                onAdjustHijri = viewModel::setHijriAdjustment
                            )
                        }

                        composable(AppRoutes.SETTINGS) {
                            SettingsAndMonetizationScreen(
                                settings = settings,
                                downloadedSurahCount = downloadedCount,
                                syncProgress = syncProgress,
                                onSetDarkMode = viewModel::setDarkMode,
                                onSetTajwidEnabled = viewModel::setTajwidEnabled,
                                onSetShowTransliteration = viewModel::setShowTransliteration,
                                onSetShowTranslation = viewModel::setShowTranslation,
                                onSetArabicFontSize = viewModel::setArabicFontSize,
                                onSelectQari = viewModel::setSelectedQari,
                                onDownloadAllOffline = viewModel::downloadAllSurahsOffline,
                                onCancelDownload = viewModel::cancelFullDownload,
                                onSaveAdSense = viewModel::saveAdSenseConfig
                            )
                        }

                        composable(
                            route = AppRoutes.READER,
                            arguments = listOf(
                                navArgument("surahNomor") { type = NavType.IntType },
                                navArgument("ayahNomor") { type = NavType.IntType }
                            )
                        ) { entry ->
                            val surahNomor = entry.arguments?.getInt("surahNomor") ?: 1
                            val ayahNomor = entry.arguments?.getInt("ayahNomor") ?: 1

                            LaunchedEffect(surahNomor) {
                                viewModel.openSurah(surahNomor)
                            }

                            SurahReaderScreen(
                                readerState = readerState,
                                initialAyah = ayahNomor,
                                bookmarkedIds = bookmarkedIds,
                                settings = settings,
                                audioState = audioState,
                                onBack = { navController.popBackStack() },
                                onNavigateSurah = { nextNo ->
                                    navController.navigate(AppRoutes.readerRoute(nextNo, 1)) {
                                        popUpTo(AppRoutes.READER) { inclusive = true }
                                    }
                                },
                                onRetryLoad = viewModel::retryLoadSurah,
                                onPlayFromAyah = viewModel::playSurahFrom,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onDownloadSurahAudio = viewModel::downloadCurrentSurahAudio,
                                onMarkLastRead = viewModel::markLastRead,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onToggleTajwid = viewModel::setTajwidEnabled,
                                onToggleTransliteration = viewModel::setShowTransliteration,
                                onToggleTranslation = viewModel::setShowTranslation,
                                onFontSizeChange = viewModel::setArabicFontSize,
                                onSelectQari = viewModel::setSelectedQari,
                                onToggleTheme = { viewModel.toggleQuickDarkMode(isDark) }
                            )
                        }
                    }
                }
            }
        }
    }
}
