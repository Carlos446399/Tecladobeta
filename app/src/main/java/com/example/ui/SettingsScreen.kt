package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.keyboard.data.AppPhrasesRepository
import com.example.keyboard.data.ClipboardHistoryRepository
import com.example.keyboard.data.KeyboardSettingsRepository
import com.example.keyboard.data.KeyboardStatsRepository
import com.example.keyboard.data.StatsSummary
import com.example.keyboard.data.TextShortcutRepository
import com.example.keyboard.data.UserDictionaryRepository
import com.example.keyboard.data.WordDictionary
import com.example.keyboard.model.AccentColorType
import com.example.keyboard.model.KeyShapeStyle
import com.example.keyboard.model.KeySizeMode
import com.example.keyboard.model.KeyboardLanguage
import com.example.keyboard.model.KeyboardSettings
import com.example.keyboard.model.KeyboardThemeType
import com.example.keyboard.ui.KeyboardMainView
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: KeyboardSettingsRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by repository.settings.collectAsStateWithLifecycle()

    val statsRepository = remember(context) { KeyboardStatsRepository.getInstance(context) }
    val clipboardRepository = remember(context) { ClipboardHistoryRepository.getInstance(context) }
    val shortcutRepository = remember(context) { TextShortcutRepository.getInstance(context) }
    val phrasesRepository = remember(context) { AppPhrasesRepository.getInstance(context) }
    val userDictionaryRepository = remember(context) { UserDictionaryRepository.getInstance(context) }

    val statsSummary by statsRepository.statsSummary.collectAsStateWithLifecycle(StatsSummary())
    val shortcutsList by shortcutRepository.allShortcutsFlow.collectAsStateWithLifecycle(emptyList())
    val phrasesList by phrasesRepository.allPhrasesFlow.collectAsStateWithLifecycle(emptyList())
    val userWordsList by userDictionaryRepository.allWordsFlow.collectAsStateWithLifecycle(emptyList())

    var showStatisticsScreen by remember { mutableStateOf(false) }
    var showShortcutsScreen by remember { mutableStateOf(false) }
    var showPhrasesScreen by remember { mutableStateOf(false) }
    var showPersonalDictionaryScreen by remember { mutableStateOf(false) }

    var isImeEnabledInSystem by remember { mutableStateOf(false) }
    var isImeSelectedAsDefault by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Permissão de microfone concedida para digitação por voz!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de microfone negada. A digitação por voz precisa do microfone.", Toast.LENGTH_LONG).show()
        }
    }

    if (showStatisticsScreen) {
        StatisticsScreen(
            statsRepository = statsRepository,
            onBack = { showStatisticsScreen = false },
            modifier = modifier
        )
        return
    }

    if (showShortcutsScreen) {
        TextShortcutsScreen(
            repository = shortcutRepository,
            onBack = { showShortcutsScreen = false },
            modifier = modifier
        )
        return
    }

    if (showPhrasesScreen) {
        AppPhrasesScreen(
            repository = phrasesRepository,
            onBack = { showPhrasesScreen = false },
            modifier = modifier
        )
        return
    }

    if (showPersonalDictionaryScreen) {
        PersonalDictionaryScreen(
            repository = userDictionaryRepository,
            onBack = { showPersonalDictionaryScreen = false },
            modifier = modifier
        )
        return
    }

    // Check system status for IME
    fun checkImeStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledList = imm?.enabledInputMethodList ?: emptyList()
        val packageName = context.packageName

        isImeEnabledInSystem = enabledList.any { it.packageName == packageName }

        val defaultIme = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: ""
        isImeSelectedAsDefault = defaultIme.contains(packageName)
    }

    // Auto-check on lifecycle resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkImeStatus()
                hasAudioPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-check background polling while not fully active
    LaunchedEffect(isImeEnabledInSystem, isImeSelectedAsDefault) {
        while (!isImeEnabledInSystem || !isImeSelectedAsDefault) {
            checkImeStatus()
            delay(1500)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF818CF8),
                                            Color(0xFF38BDF8),
                                            Color(0xFF34D399)
                                        )
                                    )
                                )
                                .padding(1.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "FLEXIKEY",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "AI NEURAL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                            Text(
                                text = "Privacidade Absoluta • 100% Offline",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // 1. Futuristic Pulse Status Hero Card
            item {
                FuturisticHeroStatusCard(
                    isImeEnabled = isImeEnabledInSystem,
                    isImeSelected = isImeSelectedAsDefault,
                    onEnableClicked = {
                        try {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Abra Configurações > Sistema > Idiomas e Teclado para ativar.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onSelectClicked = {
                        try {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Selecione o FLEXIKEY nas opções de entrada do sistema.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 2. Interactive Live Testing Playground (with Live Animated Keyboard Preview)
            item {
                InteractiveTestCard(
                    settings = settings,
                    repository = repository,
                    statsRepository = statsRepository,
                    clipboardRepository = clipboardRepository,
                    shortcutRepository = shortcutRepository,
                    phrasesRepository = phrasesRepository,
                    isImeEnabled = isImeEnabledInSystem,
                    isImeSelected = isImeSelectedAsDefault,
                    onEnableClicked = {
                        try {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Abra Configurações > Sistema > Idiomas e Teclado para ativar.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onSelectClicked = {
                        try {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Selecione o FLEXIKEY nas opções de entrada do sistema.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 3. Statistics Preview Dashboard Card
            item {
                StatisticsSummaryCard(
                    stats = statsSummary,
                    onOpenStatistics = { showStatisticsScreen = true }
                )
            }

            // 4. Text Shortcuts (Text Expansion) Navigation Card
            item {
                ActionNavigationCard(
                    icon = Icons.Default.AutoFixHigh,
                    title = stringResource(R.string.shortcuts_title),
                    subtitle = stringResource(R.string.shortcuts_desc),
                    badge = "${shortcutsList.count { it.isEnabled }} ativos",
                    onClick = { showShortcutsScreen = true },
                    testTag = "shortcuts_nav_card"
                )
            }

            // 5. App-Specific Quick Phrases Navigation Card
            item {
                ActionNavigationCard(
                    icon = Icons.Default.Quickreply,
                    title = stringResource(R.string.phrases_title),
                    subtitle = stringResource(R.string.phrases_desc),
                    badge = "${phrasesList.size} frases",
                    onClick = { showPhrasesScreen = true },
                    testTag = "phrases_nav_card"
                )
            }

            // 6. Personal Dictionary Navigation Card
            item {
                ActionNavigationCard(
                    icon = Icons.Default.Spellcheck,
                    title = stringResource(R.string.personal_dict_title),
                    subtitle = stringResource(R.string.personal_dict_desc),
                    badge = "${userWordsList.size} palavras",
                    onClick = { showPersonalDictionaryScreen = true },
                    testTag = "personal_dict_nav_card"
                )
            }

            // 7. Theme & Colors Customization Card
            item {
                ThemeCustomizationCard(
                    settings = settings,
                    onThemeChange = { repository.setThemeType(it) },
                    onAccentChange = { repository.setAccentColorType(it) },
                    onKeyShapeChange = { repository.setKeyShapeStyle(it) },
                    onAdaptiveThemeToggle = { repository.setAdaptiveThemeEnabled(it) },
                    onKeySeparationToggle = { repository.setKeySeparationEnabled(it) },
                    onKeySizeModeChange = { repository.setKeySizeMode(it) }
                )
            }

            // 8. Typing & Language Preferences (including Autocorrect & Space Autocorrect)
            item {
                TypingPreferencesCard(
                    settings = settings,
                    hasAudioPermission = hasAudioPermission,
                    onRequestAudioPermission = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onSuggestionsToggle = { repository.setSuggestionsEnabled(it) },
                    onAutoCorrectToggle = { repository.setAutoCorrectEnabled(it) },
                    onSpaceAutoCorrectToggle = { repository.setSpaceAutoCorrectEnabled(it) },
                    onAutoLanguageDetectionToggle = { repository.setAutoLanguageDetectionEnabled(it) },
                    onConversationTranslateToggle = { repository.setConversationTranslateEnabled(it) },
                    onNumberRowToggle = { repository.setShowNumberRow(it) },
                    onEmojiKeyToggle = { repository.setShowEmojiKey(it) },
                    onLanguageSelect = { repository.setLanguage(it) }
                )
            }

            // 9. Haptic & Sound Feedback Preferences
            item {
                FeedbackPreferencesCard(
                    settings = settings,
                    onHapticToggle = { repository.setHapticEnabled(it) },
                    onVibrationDurationChange = { repository.setVibrationDuration(it) },
                    onSoundToggle = { repository.setSoundEnabled(it) }
                )
            }

            // 10. Privacy & Architecture Card
            item {
                PrivacyInfoCard()
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FuturisticHeroStatusCard(
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    onEnableClicked: () -> Unit,
    onSelectClicked: () -> Unit
) {
    val isFullyActive = isImeEnabled && isImeSelected

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val activeColor = if (isFullyActive) Color(0xFF34D399) else Color(0xFFFBBF24)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x181E293B)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    if (isFullyActive) listOf(
                        Color(0x5534D399),
                        Color(0x3338BDF8),
                        Color(0x55818CF8)
                    ) else listOf(
                        Color(0x77FBBF24),
                        Color(0x44F43F5E),
                        Color(0x44818CF8)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .testTag("hero_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E1B4B).copy(alpha = 0.45f),
                            Color(0xFF0F172A).copy(alpha = 0.65f),
                            Color(0xFF090D16).copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Animated Pulse Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(activeColor.copy(alpha = pulseAlpha * 0.35f))
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(activeColor)
                        )
                    }

                    Column {
                        Text(
                            text = if (isFullyActive) "STATUS: ATIVO E OPERACIONAL" else "STATUS: ATIVAÇÃO NECESSÁRIA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = activeColor
                        )
                        Text(
                            text = if (isFullyActive) "Motor inteligente pronto em todos os apps" else "Complete as 2 etapas abaixo para utilizar",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22818CF8))
                        .border(1.dp, Color(0x44818CF8), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isFullyActive) "READY" else "CONFIG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8)
                    )
                }
            }

            if (!isFullyActive) {
                HorizontalDivider(color = Color(0x33FFFFFF))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepItem(
                        stepNumber = "1",
                        title = stringResource(R.string.step_enable_ime),
                        description = stringResource(R.string.step_enable_ime_desc),
                        isCompleted = isImeEnabled,
                        buttonText = if (isImeEnabled) "Ativado no Sistema ✓" else "1. Ativar Teclado",
                        onButtonClick = onEnableClicked
                    )

                    StepItem(
                        stepNumber = "2",
                        title = stringResource(R.string.step_select_ime),
                        description = stringResource(R.string.step_select_ime_desc),
                        isCompleted = isImeSelected,
                        buttonText = if (isImeSelected) "Teclado Padrão Ativo ✓" else "2. Definir como Padrão",
                        onButtonClick = onSelectClicked,
                        enabled = isImeEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x181E293B)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x22818CF8), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22818CF8))
                    .border(1.dp, Color(0x44818CF8), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2238BDF8))
                            .border(0.5.dp, Color(0x5538BDF8), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    buttonText: String,
    onButtonClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted)
                        Color(0xFF34D399)
                    else
                        Color(0x33818CF8)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = stepNumber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF818CF8)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            Button(
                onClick = onButtonClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) Color(0x3334D399) else Color(0xFF818CF8),
                    contentColor = if (isCompleted) Color(0xFF34D399) else Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(top = 4.dp).testTag("step_btn_$stepNumber")
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatisticsSummaryCard(
    stats: StatsSummary,
    onOpenStatistics: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x181E293B)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x22818CF8), RoundedCornerShape(18.dp))
            .clickable { onOpenStatistics() }
            .testTag("stats_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.stats_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ver detalhes",
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Words
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22818CF8))
                        .border(0.5.dp, Color(0x44818CF8), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Palavras",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stats.totalWords}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Speed (WPM)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x2238BDF8))
                        .border(0.5.dp, Color(0x4438BDF8), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Velocidade",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stats.averageWpm} PPM",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Streak
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x2234D399))
                        .border(0.5.dp, Color(0x4434D399), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Sequência",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stats.currentStreakDays}d 🔥",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveTestCard(
    settings: KeyboardSettings,
    repository: KeyboardSettingsRepository,
    statsRepository: KeyboardStatsRepository,
    clipboardRepository: ClipboardHistoryRepository,
    shortcutRepository: TextShortcutRepository,
    phrasesRepository: AppPhrasesRepository,
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    onEnableClicked: () -> Unit,
    onSelectClicked: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val recentClips by clipboardRepository.recentClips.collectAsStateWithLifecycle(emptyList())
    val allPhrases by phrasesRepository.allPhrasesFlow.collectAsStateWithLifecycle(emptyList())
    val currentPhrases = remember(allPhrases) {
        allPhrases.filter { it.appCategory == AppPhrasesRepository.CATEGORY_GENERAL || it.appCategory == AppPhrasesRepository.CATEGORY_MESSAGING }.map { it.phrase }
    }

    var showKeyboardSimulator by remember { mutableStateOf(true) }
    var isSimulatorIncognito by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    // Simulator Autocorrect Undo state
    data class SimulatorUndoState(val originalWord: String, val committedText: String)
    var lastSimulatorUndo by remember { mutableStateOf<SimulatorUndoState?>(null) }
    val simulatorWordHistory = remember { ArrayDeque<String>(6) }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    fun playFeedback() {
        if (settings.hapticEnabled && vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            settings.vibrationDurationMs.coerceIn(5L, 100L),
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(settings.vibrationDurationMs.coerceIn(5L, 100L))
                }
            } catch (_: Exception) {
            }
        }
        if (settings.soundEnabled && audioManager != null) {
            try {
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.35f)
            } catch (_: Exception) {
            }
        }
    }

    fun recordSimulatorWord(word: String) {
        statsRepository.recordTypedWord(1, isSecure = false)
        if (word.length >= 2) {
            if (simulatorWordHistory.size >= 5) {
                simulatorWordHistory.removeFirst()
            }
            simulatorWordHistory.addLast(word)
            if (settings.autoLanguageDetectionEnabled) {
                val detected = WordDictionary.detectLanguageFromWords(simulatorWordHistory.toList())
                if (detected != null && detected != settings.language) {
                    repository.setLanguage(detected)
                }
            }
        }
    }

    fun processWordEndInSimulator(separator: String) {
        playFeedback()
        val currentText = textFieldValue.text
        val cursor = textFieldValue.selection.min.coerceIn(0, currentText.length)
        val textBefore = currentText.substring(0, cursor)
        val textAfter = currentText.substring(cursor)

        val rawWord = textBefore.takeLastWhile { it.isLetterOrDigit() }

        if (rawWord.isNotEmpty()) {
            // 1. Text Expansion check
            val expansion = shortcutRepository.getExpansion(rawWord)
            if (expansion != null) {
                val base = textBefore.dropLast(rawWord.length)
                val committed = "$expansion$separator"
                val newText = base + committed + textAfter
                val newPos = base.length + committed.length
                textFieldValue = TextFieldValue(newText, TextRange(newPos))
                lastSimulatorUndo = SimulatorUndoState(originalWord = rawWord, committedText = committed)
                recordSimulatorWord(expansion)
                return
            }

            // 2. Autocorrect check
            val shouldAutoCorrect = if (separator == " ") settings.spaceAutoCorrectEnabled else true
            if (settings.autoCorrectEnabled && shouldAutoCorrect) {
                val corrected = WordDictionary.getAutocorrectWord(rawWord, settings.language)
                if (corrected != null && corrected != rawWord) {
                    val base = textBefore.dropLast(rawWord.length)
                    val committed = "$corrected$separator"
                    val newText = base + committed + textAfter
                    val newPos = base.length + committed.length
                    textFieldValue = TextFieldValue(newText, TextRange(newPos))
                    lastSimulatorUndo = SimulatorUndoState(originalWord = rawWord, committedText = committed)
                    recordSimulatorWord(corrected)
                    return
                }
            }

            recordSimulatorWord(rawWord)
        }

        lastSimulatorUndo = null
        val newText = textBefore + separator + textAfter
        val newPos = textBefore.length + separator.length
        textFieldValue = TextFieldValue(newText, TextRange(newPos))
    }

    fun handleCharTyped(char: String) {
        playFeedback()
        val isPunctuation = char in listOf(".", ",", "!", "?", ";", ":")
        if (isPunctuation) {
            processWordEndInSimulator(char)
            return
        }

        lastSimulatorUndo = null
        val currentText = textFieldValue.text
        val sel = textFieldValue.selection
        val start = sel.min.coerceIn(0, currentText.length)
        val end = sel.max.coerceIn(0, currentText.length)
        val newText = currentText.substring(0, start) + char + currentText.substring(end)
        val newPos = start + char.length
        textFieldValue = TextFieldValue(newText, TextRange(newPos))
        statsRepository.recordTypedChar(char, isSecure = false)
    }

    fun handleBackspace() {
        playFeedback()

        // Autocorrect / shortcut Undo on backspace
        val undo = lastSimulatorUndo
        if (undo != null) {
            lastSimulatorUndo = null
            val currentText = textFieldValue.text
            val cursor = textFieldValue.selection.min.coerceIn(0, currentText.length)
            val textBefore = currentText.substring(0, cursor)
            val textAfter = currentText.substring(cursor)

            if (textBefore.endsWith(undo.committedText)) {
                val base = textBefore.dropLast(undo.committedText.length)
                val newText = base + undo.originalWord + textAfter
                val newPos = base.length + undo.originalWord.length
                textFieldValue = TextFieldValue(newText, TextRange(newPos))
                return
            }
        }

        lastSimulatorUndo = null
        val currentText = textFieldValue.text
        val sel = textFieldValue.selection
        val start = sel.min.coerceIn(0, currentText.length)
        val end = sel.max.coerceIn(0, currentText.length)
        if (start != end) {
            val newText = currentText.substring(0, start) + currentText.substring(end)
            textFieldValue = TextFieldValue(newText, TextRange(start))
        } else if (start > 0) {
            val newText = currentText.substring(0, start - 1) + currentText.substring(start)
            textFieldValue = TextFieldValue(newText, TextRange(start - 1))
        }
    }

    fun handleSpace() {
        processWordEndInSimulator(" ")
    }

    fun handleEnter() {
        playFeedback()
        lastSimulatorUndo = null
        val currentText = textFieldValue.text
        val sel = textFieldValue.selection
        val start = sel.min.coerceIn(0, currentText.length)
        val end = sel.max.coerceIn(0, currentText.length)
        val newText = currentText.substring(0, start) + "\n" + currentText.substring(end)
        textFieldValue = TextFieldValue(newText, TextRange(start + 1))
        statsRepository.recordTypedWord(1, isSecure = false)
    }

    fun handleSuggestion(word: String) {
        playFeedback()
        lastSimulatorUndo = null
        val currentText = textFieldValue.text
        val sel = textFieldValue.selection
        val start = sel.min.coerceIn(0, currentText.length)
        val end = sel.max.coerceIn(0, currentText.length)

        if (start != end) {
            // Replace selected word or phrase directly
            val newText = currentText.substring(0, start) + word + currentText.substring(end)
            val newPos = start + word.length
            textFieldValue = TextFieldValue(newText, TextRange(newPos))
        } else {
            val textBefore = currentText.substring(0, start)
            val textAfter = currentText.substring(start)
            val prefixLen = textBefore.takeLastWhile { it.isLetterOrDigit() }.length
            val baseBefore = textBefore.dropLast(prefixLen)
            val newText = baseBefore + word + " " + textAfter
            val newPos = baseBefore.length + word.length + 1
            textFieldValue = TextFieldValue(newText, TextRange(newPos))
        }
        recordSimulatorWord(word)
    }

    fun handleChip(phrase: String) {
        playFeedback()
        lastSimulatorUndo = null
        val currentText = textFieldValue.text
        val cursor = textFieldValue.selection.min.coerceIn(0, currentText.length)
        val textBefore = currentText.substring(0, cursor)
        val textAfter = currentText.substring(cursor)
        val spaceNeeded = if (textBefore.isNotEmpty() && !textBefore.endsWith(" ")) " " else ""
        val newText = textBefore + spaceNeeded + phrase + " " + textAfter
        val newPos = textBefore.length + spaceNeeded.length + phrase.length + 1
        textFieldValue = TextFieldValue(newText, TextRange(newPos))
        recordSimulatorWord(phrase)
    }

    // Extract suggestions based on cursor word or active text selection
    val currentSuggestions = remember(textFieldValue.text, textFieldValue.selection, settings.language) {
        val sel = textFieldValue.selection
        val start = sel.min.coerceIn(0, textFieldValue.text.length)
        val end = sel.max.coerceIn(0, textFieldValue.text.length)
        if (start != end) {
            val selectedText = textFieldValue.text.substring(start, end).trim()
            val list = mutableListOf<String>()
            val phraseCorrection = WordDictionary.getPhraseOrWordCorrection(selectedText, settings.language)
            if (phraseCorrection != null && !phraseCorrection.equals(selectedText, ignoreCase = true)) {
                list.add(phraseCorrection)
            }
            if (!selectedText.contains(" ")) {
                val autoCorrect = WordDictionary.getAutocorrectWord(selectedText, settings.language)
                if (autoCorrect != null && !list.contains(autoCorrect)) {
                    list.add(autoCorrect)
                }
                val dictSuggestions = WordDictionary.getSuggestions(selectedText, settings.language)
                for (s in dictSuggestions) {
                    if (!list.contains(s)) list.add(s)
                }
            }
            list
        } else {
            val word = textFieldValue.text.substring(0, start).takeLastWhile { it.isLetterOrDigit() }
            WordDictionary.getSuggestions(word, settings.language)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("interactive_test_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.test_keyboard_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { showKeyboardSimulator = !showKeyboardSimulator },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (showKeyboardSimulator) "Ocultar Demo" else "Mostrar Demo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // System status banner inside test card
            if (!isImeEnabled || !isImeSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Teclado não está ativo como padrão no sistema.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isImeEnabled) {
                                Button(
                                    onClick = onEnableClicked,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Text("1. Ativar no Sistema", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (!isImeSelected) {
                                Button(
                                    onClick = onSelectClicked,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isImeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Text("2. Selecionar Padrão", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Active status pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Teclado ativado no sistema e pronto para uso!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier.fillMaxWidth().testTag("test_input_field"),
                placeholder = { Text(stringResource(R.string.test_keyboard_hint)) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                trailingIcon = {
                    if (textFieldValue.text.isNotEmpty()) {
                        IconButton(onClick = {
                            textFieldValue = TextFieldValue("")
                            lastSimulatorUndo = null
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpar"
                            )
                        }
                    }
                },
                minLines = 2,
                maxLines = 4
            )

            // Quick phrase chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("Olá! 😀", "Testando teclado", "vlw", "obrigado", "Ótimo dia!")) { sample ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { handleChip(sample) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ $sample",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // In-App Interactive Keyboard Simulator
            AnimatedVisibility(visible = showKeyboardSimulator) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF818CF8).copy(alpha = 0.5f),
                                    Color(0xFF38BDF8).copy(alpha = 0.3f),
                                    Color(0xFF34D399).copy(alpha = 0.4f)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x331E293B))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSimulatorIncognito) Color(0xFF94A3B8) else Color(0xFF34D399))
                                    )
                                    Text(
                                        text = if (isSimulatorIncognito) "MODO INCÓGNITO ATIVO" else "PRÉVIA AO VIVO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp,
                                        color = if (isSimulatorIncognito) Color(0xFFCBD5E1) else Color(0xFF38BDF8)
                                    )
                                }
                                Text(
                                    text = "${settings.language.displayName} • ${settings.themeType.displayName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        KeyboardMainView(
                            settings = settings,
                            imeAction = EditorInfo.IME_ACTION_DONE,
                            suggestions = currentSuggestions,
                            quickPhrases = currentPhrases,
                            quickPhraseCategoryBadge = "Geral",
                            isSecureFieldActive = false,
                            isIncognitoActive = isSimulatorIncognito,
                            onToggleIncognito = { isSimulatorIncognito = !isSimulatorIncognito },
                            clipboardClips = recentClips,
                            onCharTyped = { char -> handleCharTyped(char) },
                            onBackspace = { handleBackspace() },
                            onSpace = { handleSpace() },
                            onEnter = { handleEnter() },
                            onSuggestionSelected = { word -> handleSuggestion(word) },
                            onQuickPhraseSelected = { phrase -> handleChip(phrase) },
                            onLanguageSwitched = {
                                playFeedback()
                                val nextLang = if (settings.language == KeyboardLanguage.PT_BR) {
                                    KeyboardLanguage.EN_US
                                } else {
                                    KeyboardLanguage.PT_BR
                                }
                                repository.setLanguage(nextLang)
                            },
                            onDeleteClip = { clipId ->
                                clipboardRepository.deleteClip(clipId)
                            },
                            onClearAllClips = {
                                clipboardRepository.clearAll()
                            },
                            onStickerSelected = { sticker ->
                                handleChip("[Figurinha: ${sticker.title} ${sticker.emoji}]")
                            },
                            onOpenOcr = {
                                val intent = Intent(context, com.example.ocr.OcrScannerActivity::class.java)
                                context.startActivity(intent)
                            },
                            onOpenSettings = {
                                // Already in settings
                            },
                            onHideKeyboard = {
                                showKeyboardSimulator = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCustomizationCard(
    settings: KeyboardSettings,
    onThemeChange: (KeyboardThemeType) -> Unit,
    onAccentChange: (AccentColorType) -> Unit,
    onKeyShapeChange: (KeyShapeStyle) -> Unit,
    onAdaptiveThemeToggle: (Boolean) -> Unit,
    onKeySeparationToggle: (Boolean) -> Unit,
    onKeySizeModeChange: (KeySizeMode) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x181E293B)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x22818CF8), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.pref_theme_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Theme Selection Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Esquema de Cores & Estética",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(KeyboardThemeType.entries) { theme ->
                        val isSelected = settings.themeType == theme
                        FilterChip(
                            selected = isSelected,
                            onClick = { onThemeChange(theme) },
                            label = { Text(theme.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x33818CF8),
                                selectedLabelColor = Color(0xFF38BDF8),
                                containerColor = Color(0x110F172A)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = Color(0xFF38BDF8),
                                borderColor = Color(0x33818CF8)
                            ),
                            modifier = Modifier.testTag("theme_${theme.id}")
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x33818CF8))

            // Adaptive Theme Switch
            PreferenceSwitchRow(
                icon = Icons.Default.Palette,
                title = "Tema Adaptativo ao Sistema",
                subtitle = "Alterna dinamicamente entre modos Claro e Escuro de acordo com as preferências globais do Android.",
                checked = settings.adaptiveThemeEnabled,
                onCheckedChange = onAdaptiveThemeToggle
            )

            HorizontalDivider(color = Color(0x33818CF8))

            // Key Size Mode Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Altura e Tamanho das Teclas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeySizeMode.entries.forEach { mode ->
                        val isSelected = settings.keySizeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x33818CF8) else Color(0x110F172A))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0x33818CF8),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onKeySizeModeChange(mode) }
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                                .testTag("key_size_${mode.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${mode.heightDp}dp",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x33818CF8))

            // Key Separation Switch
            PreferenceSwitchRow(
                icon = Icons.Default.Keyboard,
                title = "Separação Visual das Teclas",
                subtitle = "Exibe contorno, espaçamento e relevo destacados entre as teclas individuais.",
                checked = settings.keySeparationEnabled,
                onCheckedChange = onKeySeparationToggle
            )

            HorizontalDivider(color = Color(0x33818CF8))

            // Key Shape Style Selection (Round, Square, Borderless, Rounded)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Formato e Estilo das Teclas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyShapeStyle.entries.chunked(2).forEach { rowStyles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowStyles.forEach { style ->
                                val isSelected = settings.keyShapeStyle == style
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0x28818CF8) else Color(0x110F172A)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color(0x33818CF8),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onKeyShapeChange(style) }
                                        .testTag("shape_${style.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Visual shape preview tile
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp, 20.dp)
                                                    .clip(RoundedCornerShape(style.cornerRadiusDp.dp))
                                                    .background(if (isSelected) Color(0xFF38BDF8) else Color(0x55818CF8))
                                                    .then(
                                                        if (style.hasElevation) Modifier.border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(style.cornerRadiusDp.dp))
                                                        else Modifier
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "A",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color(0xFF0F172A) else Color.White
                                                )
                                            }

                                            Text(
                                                text = style.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = style.description,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x33818CF8))

            // Accent Color Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cor de Destaque",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AccentColorType.entries) { accent ->
                        val isSelected = settings.accentColorType == accent
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accent.color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable { onAccentChange(accent) }
                                .testTag("accent_${accent.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = accent.displayName,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingPreferencesCard(
    settings: KeyboardSettings,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onSuggestionsToggle: (Boolean) -> Unit,
    onAutoCorrectToggle: (Boolean) -> Unit,
    onSpaceAutoCorrectToggle: (Boolean) -> Unit,
    onAutoLanguageDetectionToggle: (Boolean) -> Unit,
    onConversationTranslateToggle: (Boolean) -> Unit,
    onNumberRowToggle: (Boolean) -> Unit,
    onEmojiKeyToggle: (Boolean) -> Unit,
    onLanguageSelect: (KeyboardLanguage) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x181E293B)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22818CF8), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Spellcheck,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Digitação e Idioma",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Language Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.pref_language_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KeyboardLanguage.entries.forEach { lang ->
                        val isSelected = settings.language == lang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0x33818CF8)
                                    else Color(0x110F172A)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0x33818CF8),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onLanguageSelect(lang) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x33818CF8))

            // Auto Language Detection switch
            PreferenceSwitchRow(
                icon = Icons.Default.Language,
                title = stringResource(R.string.pref_auto_lang_title),
                subtitle = stringResource(R.string.pref_auto_lang_desc),
                checked = settings.autoLanguageDetectionEnabled,
                onCheckedChange = onAutoLanguageDetectionToggle
            )

            HorizontalDivider(color = Color(0x33818CF8))

            // Conversation Translate switch
            PreferenceSwitchRow(
                icon = Icons.Default.Translate,
                title = "Tradução de Conversa em Tempo Real",
                subtitle = "Traduz automaticamente a entrada de texto e mensagens diretamente pela barra superior do teclado.",
                checked = settings.conversationTranslateEnabled,
                onCheckedChange = onConversationTranslateToggle
            )

            HorizontalDivider(color = Color(0x33818CF8))

            // Voice Input Permission Card/Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hasAudioPermission) Color(0x1534D399) else Color(0x22F59E0B))
                    .border(
                        1.dp,
                        if (hasAudioPermission) Color(0x4434D399) else Color(0x66F59E0B),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (!hasAudioPermission) {
                            onRequestAudioPermission()
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (hasAudioPermission) Color(0x3334D399) else Color(0x33F59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microfone",
                        tint = if (hasAudioPermission) Color(0xFF34D399) else Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Digitação por Voz (Microfone)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasAudioPermission) {
                            "Permissão concedida. Toque no ícone de microfone no teclado para falar."
                        } else {
                            "Permissão necessária para o microfone. Toque para conceder acesso."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }

                if (!hasAudioPermission) {
                    FilledTonalButton(
                        onClick = onRequestAudioPermission,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Permitir", fontSize = 11.sp)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Concedido",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0x33818CF8))

            // Suggestions switch
            PreferenceSwitchRow(
                icon = Icons.Default.Spellcheck,
                title = stringResource(R.string.pref_suggestions_title),
                subtitle = stringResource(R.string.pref_suggestions_desc),
                checked = settings.suggestionsEnabled,
                onCheckedChange = onSuggestionsToggle
            )

            HorizontalDivider(color = Color(0x33818CF8))

            // Autocorrect switch
            PreferenceSwitchRow(
                icon = Icons.Default.AutoFixHigh,
                title = stringResource(R.string.pref_autocorrect_title),
                subtitle = stringResource(R.string.pref_autocorrect_desc),
                checked = settings.autoCorrectEnabled,
                onCheckedChange = onAutoCorrectToggle
            )

            // Space Autocorrect switch
            AnimatedVisibility(visible = settings.autoCorrectEnabled) {
                Column {
                    HorizontalDivider(color = Color(0x33818CF8))
                    PreferenceSwitchRow(
                        icon = Icons.Default.AutoFixHigh,
                        title = "Autocorreção ao teclar espaço",
                        subtitle = "Substitui automaticamente ao teclar espaço. Se desativado, selecione a palavra ou frase para aplicar a correção na barra.",
                        checked = settings.spaceAutoCorrectEnabled,
                        onCheckedChange = onSpaceAutoCorrectToggle
                    )
                }
            }

            HorizontalDivider(color = Color(0x33818CF8))

            // Number row switch
            PreferenceSwitchRow(
                icon = Icons.Default.Numbers,
                title = stringResource(R.string.pref_number_row_title),
                subtitle = stringResource(R.string.pref_number_row_desc),
                checked = settings.showNumberRow,
                onCheckedChange = onNumberRowToggle
            )

            HorizontalDivider(color = Color(0x33818CF8))

            // Emoji key switch
            PreferenceSwitchRow(
                icon = Icons.Default.EmojiEmotions,
                title = stringResource(R.string.pref_emoji_key_title),
                subtitle = stringResource(R.string.pref_emoji_key_desc),
                checked = settings.showEmojiKey,
                onCheckedChange = onEmojiKeyToggle
            )
        }
    }
}

@Composable
private fun FeedbackPreferencesCard(
    settings: KeyboardSettings,
    onHapticToggle: (Boolean) -> Unit,
    onVibrationDurationChange: (Long) -> Unit,
    onSoundToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x181E293B)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22818CF8), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Resposta Tátil e Som",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Haptic Switch
            PreferenceSwitchRow(
                icon = Icons.Default.TouchApp,
                title = stringResource(R.string.pref_haptic_title),
                subtitle = stringResource(R.string.pref_haptic_desc),
                checked = settings.hapticEnabled,
                onCheckedChange = onHapticToggle
            )

            // Vibration Duration Slider (when haptic is enabled)
            AnimatedVisibility(visible = settings.hapticEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intensidade da Vibração",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${settings.vibrationDurationMs} ms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Slider(
                        value = settings.vibrationDurationMs.toFloat(),
                        onValueChange = { onVibrationDurationChange(it.toLong()) },
                        valueRange = 5f..80f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth().testTag("vibration_slider")
                    )
                }
            }

            HorizontalDivider(color = Color(0x33818CF8))

            // Sound Switch
            PreferenceSwitchRow(
                icon = Icons.Default.VolumeUp,
                title = stringResource(R.string.pref_sound_title),
                subtitle = stringResource(R.string.pref_sound_desc),
                checked = settings.soundEnabled,
                onCheckedChange = onSoundToggle
            )
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun PrivacyInfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0x181E293B)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22818CF8), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Privacidade e Segurança Offline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "O FlexiKey funciona 100% offline no seu dispositivo. Use o Modo Incógnito manual na barra de sugestões ou entre em campos de senha para suspender imediatamente o registro de histórico, estatísticas e novas palavras.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
