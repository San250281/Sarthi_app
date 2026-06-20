package com.aistudio.saarthi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlin.math.sin
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.saarthi.data.*
import com.aistudio.saarthi.ui.theme.MyApplicationTheme
import com.aistudio.saarthi.ui.components.EmotionalJourneySection
import com.aistudio.saarthi.viewmodel.SaarthiState
import com.aistudio.saarthi.viewmodel.SaarthiViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    COMPANION,
    JOURNAL,
    PROFILE
}

class MainActivity : ComponentActivity(), PaymentResultListener {
    private val viewModel: SaarthiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Preload Razorpay checkout for faster dynamic loading
        try {
            Checkout.preload(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error preloading Razorpay: ${e.message}")
        }

        enableEdgeToEdge()
        setContent {
            val userProfile by viewModel.profile.collectAsStateWithLifecycle()
            val themeMode = userProfile?.themeMode ?: "Auto"
            
            // 6 AM to 4:59 PM is comforting daylight; 5 PM onwards and night represents twilight dark
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val isAdaptiveDark = hour !in 6..16
            
            val isDarkTheme = when (themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isAdaptiveDark
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                if (userProfile == null || userProfile?.passcodeHash.isNullOrBlank() || userProfile?.isLoggedIn == false) {
                    SecureAuthGate(viewModel = viewModel)
                } else {
                    SaarthiApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Toast.makeText(this, "Payment Successful: $razorpayPaymentId", Toast.LENGTH_LONG).show()
        viewModel.onRazorpayPaymentSuccess(razorpayPaymentId)
    }

    override fun onPaymentError(code: Int, response: String?) {
        val errorMsg = response ?: "Payment cancelled"
        Toast.makeText(this, "Payment Failed (Code $code): $errorMsg", Toast.LENGTH_LONG).show()
        viewModel.onRazorpayPaymentFailure(code, response)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaarthiApp(viewModel: SaarthiViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.COMPANION) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.COMPANION,
                    onClick = { selectedTab = AppTab.COMPANION },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Companion") },
                    label = { Text("Saarthi") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_companion")
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.JOURNAL,
                    onClick = { selectedTab = AppTab.JOURNAL },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Journal") },
                    label = { Text("Memories") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_journal")
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.PROFILE,
                    onClick = { selectedTab = AppTab.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Me") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                AppTab.COMPANION -> CompanionScreen(viewModel = viewModel)
                AppTab.JOURNAL -> JournalScreen(viewModel = viewModel)
                AppTab.PROFILE -> ProfileScreen(viewModel = viewModel)
            }
        }
    }
}

// ==========================================
// 1. COMPANION SCREEN (Active Voice/Text Chat)
// ==========================================
@Composable
fun CompanionScreen(viewModel: SaarthiViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val rawState by viewModel.companionState.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()
    val partialInput by viewModel.partialSpeechInput.collectAsStateWithLifecycle()
    val voiceAmplitude by viewModel.voiceAmplitude.collectAsStateWithLifecycle()
    val userProfile by viewModel.profile.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var companionMode by remember { mutableStateOf("chat") } // "chat" or "reflection"
    val listState = rememberLazyListState()

    // Permissions check
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required to speak to Saarthi!", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto scroll chat to end when messages arrive
    LaunchedEffect(messages.size, partialInput) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Trigger prompt generation when entering reflection tab
    LaunchedEffect(companionMode) {
        if (companionMode == "reflection") {
            viewModel.generatePersonalizedPrompts()
        }
    }

    // Screen layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Banner reflecting Time-Based Behavior
        TimeBannerSec(viewModel.getLocalTimeBasedTone())

        // Nightly Gratitude logging activation / invitation banner
        GratitudeCheckInBanner(viewModel = viewModel)

        // Custom Mode Selector Capsule
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color(0xFF1E2135),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Converse Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (companionMode == "chat") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { companionMode = "chat" }
                        .testTag("mode_chat_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Chat Mode",
                            tint = if (companionMode == "chat") MaterialTheme.colorScheme.onPrimary else Color(0xFF8A90A6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Converse",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (companionMode == "chat") MaterialTheme.colorScheme.onPrimary else Color(0xFF8A90A6)
                        )
                    }
                }

                // Reflection Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (companionMode == "reflection") MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { companionMode = "reflection" }
                        .testTag("mode_reflection_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Reflection Mode",
                            tint = if (companionMode == "reflection") MaterialTheme.colorScheme.onPrimary else Color(0xFF8A90A6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reflect & Summarize",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (companionMode == "reflection") MaterialTheme.colorScheme.onPrimary else Color(0xFF8A90A6)
                        )
                    }
                }
            }
        }

        if (companionMode == "chat") {
            // Wallet & Companion Selector Sticky Header
            WalletAndAgentSelectors(viewModel = viewModel)

            // Chat Conversation Logs
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart Logo",
                            tint = Color(0xFFFF9E3B),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Saarthi is ready to listen",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aap mujhse Hindi, English ya Hinglish me baat kar sakte hain. I will automatically adapt to you.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8A90A6)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(message = msg)
                    }

                    // Show partial speech input dynamically if listening
                    if (rawState == SaarthiState.LISTENING && partialInput.isNotBlank()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF23273A).copy(alpha = 0.5f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "... $partialInput",
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick suggestion chips pinned above the controller row
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val promptSuggestions = listOf(
                        "Feeling Stressed 😣",
                        "My Startup Idea 💡",
                        "Let's reflect on today"
                    )
                    promptSuggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = {
                                viewModel.sendMessage(suggestion)
                            },
                            label = { Text(suggestion, color = MaterialTheme.colorScheme.onSurface) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // Active animated Waveform section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Listening & Interaction Waveforms/Indicator
                SaarthiListeningIndicator(rawState, voiceAmplitude)

                // Current interaction status message
                val stateText = when (rawState) {
                    SaarthiState.LISTENING -> "Saarthi is listening... Speak now"
                    SaarthiState.THINKING -> "Thinking..."
                    SaarthiState.SPEAKING -> "Saarthi is speaking"
                    SaarthiState.IDLE -> "Tap mic to speak, or use keyboard"
                }
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (rawState == SaarthiState.LISTENING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Error alert indicator
                if (errorMsg != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF3333).copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMsg!!, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.clearError() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Control bar row: keyboard input, send action, speaker synthesis toggle, mic record
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Speaker volume output synthesis toggle
                    IconButton(
                        onClick = { viewModel.setTtsEnabled(!ttsEnabled) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("toggle_tts")
                    ) {
                        Icon(
                            imageVector = if (ttsEnabled) Icons.Default.Favorite else Icons.Default.Add, // Stand-ins for VolumeUp/Add
                            contentDescription = "Speech Toggle",
                            tint = if (ttsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // Keyboard fill text input field
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Search clarity or type...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("chat_input"),
                        shape = RoundedCornerShape(26.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    // Unified CTA trigger: Send or Mic toggle
                    if (inputText.isNotBlank()) {
                        FloatingActionButton(
                            onClick = {
                                viewModel.sendMessage(inputText, isVoice = false)
                                inputText = ""
                                keyboardController?.hide()
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("send_button"),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        // Mic Speech capturing action
                        FloatingActionButton(
                            onClick = {
                                if (rawState == SaarthiState.LISTENING) {
                                    viewModel.stopListening()
                                } else {
                                    val isPermissionGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (isPermissionGranted) {
                                        viewModel.startListening()
                                    } else {
                                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            containerColor = if (rawState == SaarthiState.LISTENING) Color(0xFFE53935) else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("voice_mic"),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh, // Using Refresh icon as dynamic voice controller
                                contentDescription = if (rawState == SaarthiState.LISTENING) "Stop Listening" else "Microphone Speech",
                                tint = if (rawState == SaarthiState.LISTENING) Color.White else MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }
        }
    } else {
        ReflectionModeScreen(viewModel = viewModel, modifier = Modifier.weight(1f))
    }
    }
}

// Banner Section representing current tone details
@Composable
fun TimeBannerSec(toneDetail: String) {
    val bannerName = when {
        toneDetail.contains("MORNING") -> "🌅 Morning Awakening"
        toneDetail.contains("AFTERNOON") -> "☀️ Afternoon Productivity"
        toneDetail.contains("EVENING") -> "☕ Evening Reflection"
        toneDetail.contains("NIGHT") -> "🌌 Quiet Night Companion"
        else -> "🕯️ Before Sleep Rest"
    }

    val bannerDesc = when {
        toneDetail.contains("MORNING") -> "Energetic, positive and goal-oriented."
        toneDetail.contains("AFTERNOON") -> "Supportive, peaceful, and pacing."
        toneDetail.contains("EVENING") -> "Reflective and welcoming back."
        toneDetail.contains("NIGHT") -> "Calming, comforting, and letting go."
        else -> "Resting, deep breaths, today is done."
    }

    val bannerColor = when {
        toneDetail.contains("MORNING") -> Color(0xFFFF9233).copy(alpha = 0.15f)
        toneDetail.contains("AFTERNOON") -> Color(0xFFFFC107).copy(alpha = 0.15f)
        toneDetail.contains("EVENING") -> Color(0xFF2196F3).copy(alpha = 0.15f)
        toneDetail.contains("NIGHT") -> Color(0xFF3F51B5).copy(alpha = 0.15f)
        else -> Color(0xFF9C27B0).copy(alpha = 0.15f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bannerColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bannerName,
                fontSize = 13.sp,
                color = Color(0xFFFF9E3B),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = bannerDesc,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Text(
            text = "GUIDE ACTIVE",
            fontSize = 9.sp,
            color = Color(0xFF26A69A),
            modifier = Modifier
                .border(1.dp, Color(0xFF26A69A), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun GratitudeCheckInBanner(viewModel: SaarthiViewModel) {
    val isActive by viewModel.isGratitudeLoggingActive.collectAsStateWithLifecycle()
    
    // Check if evening/night (6 PM to 5 AM)
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val isNightTime = hour in 18..23 || hour < 5
    
    if (isActive) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("gratitude_active_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌌", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nightly Gratitude Logging Active",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Write or speak what you are grateful for below. Saarthi is listening...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.cancelGratitudeCheckIn() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Cancel check-in",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    } else if (isNightTime) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("gratitude_prompt_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E2135).copy(alpha = 0.8f)
            ),
            border = BorderStroke(1.dp, Color(0xFF2C314B))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Log Your Nightly Gratitude",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Set a peaceful tone before sleeping.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A90A6)
                        )
                    }
                }
                
                Button(
                    onClick = { viewModel.triggerGratitudeCheckIn() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("start_gratitude_button")
                ) {
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Interactive custom dynamic Saarthi Listening & Waveform indicator
@Composable
fun SaarthiListeningIndicator(state: SaarthiState, voiceAmplitude: Float = 0f) {
    val infiniteTransition = rememberInfiniteTransition(label = "listening_ripple")

    // Continuous smooth rotation angle for rotation effects
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Warm breathing pulse for the core circular orb
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Outer warm glowing ripples radiating out for visual indicator feedback
    val rippleProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )
    val rippleProgress2 by infiniteTransition.animateFloat(
        initialValue = 0.33f,
        targetValue = 1.33f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )

    // Animated colors shifting depending on Saarthi activity state
    val coreColor by animateColorAsState(
        targetValue = when (state) {
            SaarthiState.LISTENING -> Color(0xFFFF9E3B) // Warm Golden
            SaarthiState.THINKING -> Color(0xFF26A69A)  // Calm Responsive Teal
            SaarthiState.SPEAKING -> Color(0xFFFFD180)  // Soothing Cream Yellow
            SaarthiState.IDLE -> Color(0xFF555B70)      // Soft Muted Slate Blue
        },
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "coreColor"
    )

    // Overall dynamic scale changes based on active states, vibrating dynamically when user is speaking
    val targetCoreScale = when (state) {
        SaarthiState.LISTENING -> 1.15f + (0.35f * voiceAmplitude)
        SaarthiState.THINKING -> 0.95f
        SaarthiState.SPEAKING -> 1.35f
        SaarthiState.IDLE -> 0.8f
    }
    val coreScaleModifier by animateFloatAsState(
        targetValue = targetCoreScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "stateScale"
    )

    // Responsive target wave amplitude responding in real-time to microphone volume
    val targetAmplitude = when (state) {
        SaarthiState.LISTENING -> 14.dp + (38.dp * voiceAmplitude)
        SaarthiState.THINKING -> 6.dp
        SaarthiState.SPEAKING -> 24.dp
        SaarthiState.IDLE -> 2.dp
    }
    val amplitudeDp by animateDpAsState(
        targetValue = targetAmplitude,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "amplitude"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Render concentric warm halo rings when actively listening or speaking
        if (state == SaarthiState.LISTENING || state == SaarthiState.SPEAKING) {
            // Ripple Ring 1
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer {
                        val scale = 1f + (rippleProgress1 * 1.6f) * coreScaleModifier
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - rippleProgress1.coerceAtMost(1f)) * 0.45f
                    }
                    .border(2.dp, coreColor, CircleShape)
            )

            // Ripple Ring 2
            val normalizedProgress2 = if (rippleProgress2 > 1f) rippleProgress2 - 1f else rippleProgress2
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer {
                        val scale = 1f + (normalizedProgress2 * 1.6f) * coreScaleModifier
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - normalizedProgress2.coerceAtMost(1f)) * 0.45f
                    }
                    .border(1.dp, coreColor, CircleShape)
            )
        }

        // Horizontal ambient generative voice-wave lines layered background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.35f }
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val points = 80
            val space = width / points
            val wavePhase = (rotationAngle * Math.PI / 180f).toFloat()

            val waveColors = when (state) {
                SaarthiState.LISTENING -> listOf(Color(0xFFFF9E3B), Color(0xFFFFD180))
                SaarthiState.THINKING -> listOf(Color(0xFF26A69A), Color(0xFF80CBC4))
                SaarthiState.SPEAKING -> listOf(Color(0xFFFFD180), Color(0xFFFFB74D))
                SaarthiState.IDLE -> listOf(Color(0xFF8A90A6).copy(alpha = 0.3f))
            }

            val amplitude = amplitudeDp.toPx()

            waveColors.forEachIndexed { index, color ->
                val path = Path()
                val phaseOffset = index * (Math.PI / 2.5f).toFloat() + wavePhase

                for (i in 0..points) {
                    val x = i * space
                    val dampening = sin(((i.toFloat() / points) * Math.PI)).toFloat()
                    val sine = sin(((i.toFloat() / 10f) - phaseOffset).toDouble()).toFloat()
                    val y = centerY + sine * amplitude * dampening

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Centered warm breathing core Orb
        Box(
            modifier = Modifier
                .size(68.dp)
                .graphicsLayer {
                    val scale = breathingScale * coreScaleModifier
                    scaleX = scale
                    scaleY = scale
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = 0.85f),
                            coreColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Elegant inner core shape
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                coreColor,
                                coreColor.copy(alpha = 0.75f)
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Color.White.copy(alpha = 0.6f)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Appropriate central state symbol indicating warm activity
                val stateIcon = when (state) {
                    SaarthiState.LISTENING -> Icons.Default.Favorite // heart shape representing caring active listening
                    SaarthiState.THINKING -> Icons.Default.Refresh  // rotation-indicative refresh icon
                    SaarthiState.SPEAKING -> Icons.Default.PlayArrow // speaking audio wave visualizer start
                    SaarthiState.IDLE -> Icons.Default.Person       // stand-by profile
                }

                Icon(
                    imageVector = stateIcon,
                    contentDescription = "Interaction Status Icon",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            if (state == SaarthiState.THINKING) {
                                rotationZ = rotationAngle * 1.5f
                            }
                        }
                )
            }
        }
    }
}

// Single Chat Message bubble layout
@Composable
fun ChatBubble(message: ChatMessage) {
    val isSaarthi = message.role == "saarthi"
    val alignLeft = isSaarthi

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (alignLeft) Arrangement.Start else Arrangement.End
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (alignLeft) Arrangement.Start else Arrangement.End,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (alignLeft) {
                // Circle label Avatar representing Saarthi
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "S", color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message text containers
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (alignLeft) 2.dp else 16.dp,
                            bottomEnd = if (alignLeft) 16.dp else 2.dp
                        )
                    )
                    .background(
                        if (alignLeft) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondary
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = if (alignLeft) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (message.isVoice) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val badgeColor = if (alignLeft) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Refresh, // representative mic/voice tag
                                contentDescription = "Voice prompt",
                                tint = badgeColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Voice input",
                                fontSize = 9.sp,
                                color = badgeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. JOURNAL SCREEN (Memory bank viewer)
// ==========================================
@Composable
fun JournalScreen(viewModel: SaarthiViewModel) {
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    var journalTab by remember { mutableStateOf("trends") } // "trends" or "insights"
    var filteredCategory by remember { mutableStateOf("All") }
    var showManualAddPrompt by remember { mutableStateOf(false) }

    val categories = listOf("All", "Name", "Goal", "Habit", "Interest", "Preference", "Milestone", "Gratitude")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saarthi's Journal",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Local Memory & Insight Tracker",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Button to add memory manually
            Button(
                onClick = { showManualAddPrompt = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Segment Switcher: Trends & insights
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subTabs = listOf("trends" to "📈 Journey Trends", "insights" to "🧠 Insights Bank")
            subTabs.forEach { (tabKey, label) ->
                val isSel = journalTab == tabKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { journalTab = tabKey }
                        .padding(vertical = 8.dp)
                        .testTag("journal_view_tab_$tabKey"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (journalTab == "trends") {
            // Render the brand-new interactive native line chart tracking clarity and confidence over time
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                EmotionalJourneySection(viewModel = viewModel)
            }
        } else {
            // Horizontal Category filtering bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(4).forEach { cat ->
                    val isSelected = cat == filteredCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { filteredCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.drop(4).forEach { cat ->
                    val isSelected = cat == filteredCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { filteredCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredList = if (filteredCategory == "All") memories else memories.filter { it.category == filteredCategory }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Empty Memory", tint = Color(0xFF8A90A6), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Memory bank is quiet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Saarthi automatically captures startup goals, interests, and habits as you talk. They will appear here dynamically!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { item ->
                        MemoryItemRow(item = item, onDelete = { viewModel.removeMemory(item.id) })
                    }
                }
            }
        }
    }

    // Modal dialog to add memory manually
    if (showManualAddPrompt) {
        var newCat by remember { mutableStateOf("Goal") }
        var newKey by remember { mutableStateOf("") }
        var newVal by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualAddPrompt = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Record New Insight", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Category Selection
                    Text("Category", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Goal", "Habit", "Interest", "Preference").forEach { c ->
                            val isSel = c == newCat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .clickable { newCat = c }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(c, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Concept (e.g. Fitness)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    TextField(
                        value = newVal,
                        onValueChange = { newVal = it },
                        label = { Text("Factual details to remember") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank() && newVal.isNotBlank()) {
                            viewModel.addManualMemory(newCat, newKey, newVal)
                            showManualAddPrompt = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddPrompt = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }
}

// Single Memory Card item display
@Composable
fun MemoryItemRow(item: MemoryItem, onDelete: () -> Unit) {
    val categoryIcon = when (item.category.lowercase(Locale.ROOT)) {
        "name" -> Icons.Default.Person
        "goal" -> Icons.Default.Star
        "habit" -> Icons.Default.Refresh
        "interest" -> Icons.Default.Favorite
        "gratitude" -> Icons.Default.Star
        else -> Icons.Default.Info
    }

    val iconColor = when (item.category.lowercase(Locale.ROOT)) {
        "goal" -> MaterialTheme.colorScheme.primary
        "habit" -> MaterialTheme.colorScheme.secondary
        "interest" -> Color(0xFFEC407A)
        "gratitude" -> Color(0xFFFFB300)
        else -> Color(0xFF42A5F5)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoryIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.key,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.category,
                    fontSize = 9.sp,
                    color = iconColor,
                    modifier = Modifier
                        .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.value,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

// ==========================================
// 3. PROFILE SCREEN (Mood selector & configurations)
// ==========================================
@Composable
fun ProfileScreen(viewModel: SaarthiViewModel) {
    val userProfile by viewModel.profile.collectAsStateWithLifecycle()
    val useDirectAudio by viewModel.useDirectAudioCapture.collectAsStateWithLifecycle()
    val profile = userProfile ?: UserProfile()
    var editNameText by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        editNameText = profile.userName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Self-Care & Profile",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure your voice guide companion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Self-Care Mood Check-In
        Text(
            text = "Current Mood Log",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Saarthi will adjust her supportive voice feedback instantly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val moodsList = listOf(
            "Calm 😌" to "Calm",
            "Stressed 😣" to "Stressed",
            "Sad 😢" to "Sad",
            "Excited 🤩" to "Excited",
            "Confused 😳" to "Confused",
            "Unmotivated 😴" to "Unmotivated"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodsList.take(3).forEach { (label, toneKey) ->
                    val isCur = profile.currentMood == toneKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.setMood(toneKey) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isCur) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodsList.drop(3).forEach { (label, toneKey) ->
                    val isCur = profile.currentMood == toneKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.setMood(toneKey) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isCur) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Username setting
        Text(
            text = "What should Saarthi call you?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = editNameText,
                onValueChange = { editNameText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("username_field"),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Button(
                onClick = {
                    if (editNameText.isNotBlank()) {
                        viewModel.saveProfileName(editNameText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Theme Mode settings block
        Text(
            text = "Theme & Atmosphere",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sync colors to the time of day or choose a static mood.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(10.dp))

        val themeOpts = listOf(
            "Auto" to "🌅 Adaptive",
            "Light" to "☀️ Daylight",
            "Dark" to "🌌 Twilight"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themeOpts.forEach { (optionKey, label) ->
                val isSelected = profile.themeMode == optionKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewModel.setThemeMode(optionKey) }
                        .padding(vertical = 12.dp)
                        .testTag("theme_opt_${optionKey.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // System actions card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Saarthi Companion Engine",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Mission: Help people move forward, one concise conversation at a time.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Direct Audio Capture (MediaRecorder)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Record with native MediaRecorder and send raw stream to Gemini for multi-modal listening.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                Switch(
                    checked = useDirectAudio,
                    onCheckedChange = { viewModel.setUseDirectAudioCapture(it) },
                    modifier = Modifier.testTag("direct_audio_switch")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Clear Chat History", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Reset all chats back to default.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                Button(
                    onClick = { viewModel.clearChatLogs() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Reset", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Premium Subscription & Talk-Time Credits Section
        var showProfilePlansDialog by remember { mutableStateOf(false) }

        Text(
            text = "Premium Subscription & Credits",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Unlock premium features, high-priority models and unlimited talk-time credits securely via Razorpay SDK.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "💳 Subscription Status",
                    tint = Color(0xFFFFCC00),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current Plan: ${profile.subscriptionPlan}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.subscriptionStatus == "Active") {
                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                        val expDate = sdf.format(java.util.Date(profile.subscriptionExpiry))
                        Text(
                            text = "Status: ACTIVE until $expDate",
                            fontSize = 11.sp,
                            color = Color(0xFF4CD964),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Status: Inactive (Free Tier)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Button(
                    onClick = { showProfilePlansDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2854FF)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("manage_sub_btn")
                ) {
                    Text("Manage", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showProfilePlansDialog) {
            RazorpayPlansDialog(
                viewModel = viewModel,
                onDismiss = { showProfilePlansDialog = false }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secure Login / Account Guard Section
        Text(
            text = "Security & Session Lock",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your voice chat logs and reflective journals are encrypted locally with SHA-256.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "🔒 Security",
                    tint = Color(0xFF00B2FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Encrypted Account: ${profile.userEmail.ifBlank { "Unregistered" }}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SHA-256 Hash Key Check: Active",
                        fontSize = 11.sp,
                        color = Color(0xFF4CD964),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Secure Lock Session", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Securely lock the vault and logout. Requires passcode to reopen.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                Button(
                    onClick = { viewModel.secureLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2854FF)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("logout_btn")
                ) {
                    Text("Lock App", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 4. REFLECTION MODE SCREEN
// ==========================================
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReflectionModeScreen(
    viewModel: SaarthiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val reflectionSummaries by viewModel.reflectionSummaries.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingReflection.collectAsStateWithLifecycle()
    val speechInput by viewModel.reflectionSpeechInput.collectAsStateWithLifecycle()
    val rawState by viewModel.companionState.collectAsStateWithLifecycle()
    val voiceAmplitude by viewModel.voiceAmplitude.collectAsStateWithLifecycle()

    var reflectionText by remember { mutableStateOf("") }
    val reflectivePrompts by viewModel.reflectivePrompts.collectAsStateWithLifecycle()
    val isGeneratingPrompts by viewModel.isGeneratingReflectivePrompts.collectAsStateWithLifecycle()

    // Sync input text with voice transcription updates
    LaunchedEffect(speechInput) {
        if (speechInput.isNotBlank()) {
            reflectionText = speechInput
        }
    }

    // Speech-to-text recording launcher
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startReflectionListening()
        } else {
            Toast.makeText(context, "Microphone permission is required to speak your thoughts!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
    ) {
        // Welcome and Intro Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reflection_intro_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E2135).copy(alpha = 0.6f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🧠 Quiet Space Reflection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dedicating some quiet time to speak or write down your deepest worries, moments of gratitude, or daily events. Saarthi will process your thoughts and generate key takeaways and emotional indicators.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A90A6)
                    )
                }
            }
        }

        // Personalized Reflective Prompts Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ Personalized Reflective Prompts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (isGeneratingPrompts) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Refining...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8A90A6)
                            )
                        }
                    } else {
                        Text(
                            text = "Refresh",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .clickable { viewModel.generatePersonalizedPrompts() }
                                .padding(4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reflectivePrompts.forEach { prompt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reflectionText = "Prompt: $prompt\n\nReflection: "
                                    viewModel.setReflectionSpeechInput("Prompt: $prompt\n\nReflection: ")
                                }
                                .testTag("reflective_prompt_item_${prompt.hashCode()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E2135).copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF2C314B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "💡",
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input Container Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2135)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Reflection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (reflectionText.isNotBlank()) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        reflectionText = ""
                                        viewModel.clearReflectionSpeechInput()
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = reflectionText,
                        onValueChange = {
                            reflectionText = it
                            viewModel.setReflectionSpeechInput(it)
                        },
                        placeholder = {
                            Text(
                                "Describe your feelings, what triggered them, or what is on your mind today... Type or use the voice assistant to speak freely.",
                                color = Color(0xFF8A90A6).copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("reflection_text_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2C314B),
                            focusedContainerColor = Color(0xFF151728),
                            unfocusedContainerColor = Color(0xFF151728)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Microphone Recording Handler
                        Button(
                            onClick = {
                                if (rawState == SaarthiState.LISTENING) {
                                    viewModel.stopListening()
                                } else {
                                    val isPermissionGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (isPermissionGranted) {
                                        viewModel.startReflectionListening()
                                    } else {
                                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reflection_mic_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (rawState == SaarthiState.LISTENING) Color(0xFFE53935) else Color(0xFF2C314B)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (rawState == SaarthiState.LISTENING) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                    contentDescription = "Speak Reflection Input",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (rawState == SaarthiState.LISTENING) "Stop Recording" else "Speak reflection",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Analyze Action Trigger
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.analyzeReflection(reflectionText) { success ->
                                    if (success) {
                                        reflectionText = ""
                                        viewModel.clearReflectionSpeechInput()
                                        Toast.makeText(context, "Summary generated successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = reflectionText.isNotBlank() && !isGenerating,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("analyze_reflection_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Analyze Reflection Action",
                                    tint = if (reflectionText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Analyze ✨",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (reflectionText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Voice Amplitude Visualizer
                    if (rawState == SaarthiState.LISTENING) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SaarthiListeningIndicator(rawState, voiceAmplitude)
                    }
                }
            }
        }

        // Generating insights progress layout
        if (isGenerating) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reflection_analysis_loader"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2135))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Synthesizing Reflection...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Saarthi is channeling cognitive insights to structure your sentiments, key takeaways, and next actions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8A90A6),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Past Reflection logs title bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reflexive Journey Logs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (reflectionSummaries.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFF5252),
                        modifier = Modifier
                            .clickable {
                                viewModel.clearAllReflections()
                            }
                            .padding(4.dp)
                    )
                }
            }
        }

        // Empty logs placeholder
        if (reflectionSummaries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty Reflection List",
                            tint = Color(0xFF2C314B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No structured reflections yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8A90A6)
                        )
                    }
                }
            }
        }

        // Render logs
        items(reflectionSummaries) { summary ->
            ReflectionSummaryCard(
                summary = summary,
                onDelete = { viewModel.removeReflection(summary) }
            )
        }
    }
}

@Composable
fun ReflectionSummaryCard(
    summary: ReflectionSummary,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val formattedDate = remember(summary.timestamp) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(summary.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("reflection_summary_card_${summary.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2135)),
        border = BorderStroke(1.dp, Color(0xFF2C314B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(ShapeDefaults.Medium)
                        .background(Color(0xFF23273A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = summary.mainMoodEmoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8A90A6)
                    )
                    Text(
                        text = if (summary.detectedFeelings.isBlank()) "Sentiment Analyzed" else summary.detectedFeelings.take(45) + if (summary.detectedFeelings.length > 45) "..." else "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Delete Button (touch targets >= 48.dp)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Reflection",
                        tint = Color(0xFFFF5252).copy(alpha = 0.8f)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF8A90A6)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (isExpanded) summary.originalText else summary.originalText.take(120) + if (summary.originalText.length > 120) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2C314B))
                Spacer(modifier = Modifier.height(12.dp))

                // Detected Feelings Section
                Text(
                    text = "Detected Feelings 💖",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary.detectedFeelings,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBCBFD1)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Key Takeaways Section
                Text(
                    text = "Core Takeaways 🎯",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9E3B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                summary.keyTakeaways.split("\n").forEach { bullet ->
                    if (bullet.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                color = Color(0xFFFF9E3B),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = bullet.removePrefix("•").removePrefix("-").trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFBCBFD1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recommended Next Steps
                Text(
                    text = "Actionable Steps ⚡",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4EE4A2)
                )
                Spacer(modifier = Modifier.height(4.dp))
                summary.actionableSteps.split("\n").forEach { step ->
                    if (step.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "→",
                                color = Color(0xFF4EE4A2),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = step.removePrefix("•").removePrefix("-").removePrefix("→").trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFBCBFD1)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// NEW: Multiple AI Companions and Razorpay Wallet Panel
// ==========================================
@Composable
fun WalletAndAgentSelectors(viewModel: com.aistudio.saarthi.viewmodel.SaarthiViewModel) {
    val walletBalance by viewModel.walletBalance.collectAsStateWithLifecycle()
    val selectedAgent by viewModel.selectedAgent.collectAsStateWithLifecycle()
    
    var showRazorpayPlans by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Row 1: Wallet Card with Add Credits Button
        Surface(
            color = Color(0xFF1E2135),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("wallet_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Wallet Icon",
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Saarthi Virtual Wallet",
                            fontSize = 11.sp,
                            color = Color(0xFF8A90A6)
                        )
                        Text(
                            text = "Balance: ₹$walletBalance",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Button(
                    onClick = { showRazorpayPlans = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2854FF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).testTag("add_money_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Icon",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Money", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Row 2: Title & Scrollable AI Agents selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Specialized Companion",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A90A6)
            )
            Text(
                text = "Active Rate: ₹${selectedAgent.pricePerMin}/min",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Horizontal list of Agents
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth().testTag("agents_row")
        ) {
            items(viewModel.agentsList) { agent ->
                val isSelected = agent.id == selectedAgent.id
                Surface(
                    color = if (isSelected) Color(0xFF2C314B) else Color(0xFF1E2135),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { viewModel.selectAgent(agent.id) }
                        .testTag("agent_card_${agent.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF2E334D)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(agent.avatarEmoji, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = agent.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = agent.description,
                            fontSize = 10.sp,
                            color = Color(0xFF8A90A6),
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Per-minute talking price shown clearly
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹${agent.pricePerMin}/min",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4CD964)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Talk", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Razorpay Dialog
    if (showRazorpayPlans) {
        RazorpayPlansDialog(
            viewModel = viewModel,
            onDismiss = { showRazorpayPlans = false }
        )
    }
}

// Helper function to invoke authentic Razorpay Android SDK checkout flow
fun startRazorpayCheckout(
    activity: ComponentActivity,
    price: Int,
    name: String,
    viewModel: com.aistudio.saarthi.viewmodel.SaarthiViewModel
) {
    val talkTimeVal = when (name) {
        "Weekly Explorer" -> 120
        "Monthly Companion" -> 450
        "Yearly Zen" -> 999999
        "Basic credits pack" -> 100
        "Mental peace booster" -> 220
        "Zen master pack" -> 600
        else -> price
    }
    viewModel.setPendingOrder(price = price, value = talkTimeVal, name = name)

    val co = Checkout()
    val apiKey = BuildConfig.RAZORPAY_API_KEY
    if (apiKey.isBlank() || apiKey == "rzp_test_placeholder_key_abc123") {
        Toast.makeText(activity, "No authentic Razorpay API key configured in user secrets. Initializing test launcher mode.", Toast.LENGTH_LONG).show()
    }
    co.setKeyID(apiKey)

    try {
        val options = org.json.JSONObject()
        options.put("name", "Saarthi Companion")
        options.put("description", "Bill: $name")
        options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
        options.put("theme.color", "#2854FF")
        options.put("currency", "INR")
        options.put("amount", (price * 100).toString()) // price in paise (e.g. ₹99 = 9900 paise)
        
        options.put("prefill.email", "san250281@gmail.com")
        options.put("prefill.contact", "9999999999")
        
        val retryObj = org.json.JSONObject()
        retryObj.put("enabled", true)
        retryObj.put("max_count", 3)
        options.put("retry", retryObj)

        co.open(activity, options)
    } catch (e: Exception) {
        Toast.makeText(activity, "Checkout initialization error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun RazorpayPlansDialog(viewModel: com.aistudio.saarthi.viewmodel.SaarthiViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedPlanPrice by remember { mutableStateOf<Int?>(null) }
    var selectedPlanValue by remember { mutableStateOf<Int?>(null) }
    var selectedPlanName by remember { mutableStateOf("") }
    
    var paymentStep by remember { mutableStateOf("plan_selection") } // "plan_selection", "gateway_option", "razorpay_gateway", "processing", "success"
    var paymentMethod by remember { mutableStateOf("") }
    var mockTxnId by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("Subscriptions") } // "Subscriptions" or "Top-up Credits"

    val subPlans = listOf(
        Triple(99, 120, "Weekly Explorer"),
        Triple(299, 450, "Monthly Companion"),
        Triple(999, 8888, "Yearly Zen")
    )

    val creditPlans = listOf(
        Triple(100, 100, "Basic credits pack"),
        Triple(220, 220, "Mental peace booster"),
        Triple(600, 600, "Zen master pack")
    )

    AlertDialog(
        onDismissRequest = { 
            if (paymentStep != "processing") { onDismiss() } 
        },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = paymentStep != "processing",
            dismissOnClickOutside = paymentStep != "processing"
        ),
        confirmButton = {},
        dismissButton = {},
        containerColor = Color(0xFF16192E),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Razorpay Secure",
                    tint = Color(0xFFFFCC00),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (paymentStep) {
                        "plan_selection" -> "Secure Razorpay Plans"
                        "gateway_option" -> "Checkout Options"
                        "razorpay_gateway" -> "Simulated Bank Portal"
                        "processing" -> "Razorpay Checkout"
                        else -> "Transaction Success!"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().testTag("razorpay_dialog_content")
            ) {
                when (paymentStep) {
                    "plan_selection" -> {
                        Text(
                            text = "Choose a premium subscription or top-up talk credits. Securely checkout using Razorpay.",
                            fontSize = 11.sp,
                            color = Color(0xFF8A90A6),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Tab selectors
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1123))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("Subscriptions", "Top-up Credits").forEach { TabName ->
                                val isSelected = TabName == activeTab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF2854FF) else Color.Transparent)
                                        .clickable { activeTab = TabName }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = TabName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF8A90A6)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val selectedList = if (activeTab == "Subscriptions") subPlans else creditPlans

                        selectedList.forEach { (price, value, name) ->
                            Surface(
                                color = Color(0xFF1E2135),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF2C314B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedPlanPrice = price
                                        selectedPlanValue = value
                                        selectedPlanName = name
                                        paymentStep = "gateway_option"
                                    }
                                    .testTag("plan_item_$price")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(
                                            text = if (activeTab == "Subscriptions") {
                                                if (name == "Yearly Zen") "UNLIMITED calling + full memory"
                                                else "Get $value minutes calling per month"
                                            } else {
                                                "Get ₹$value Voice Call credit instantly"
                                            },
                                            fontSize = 9.sp, 
                                            color = Color(0xFF4CD964), 
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2854FF))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("₹$price", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Standard talking rates apply.", fontSize = 10.sp, color = Color(0xFF8A90A6))
                            TextButton(onClick = { onDismiss() }) {
                                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }

                    "gateway_option" -> {
                        Surface(
                            color = Color(0xFF0F1123),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF233261)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Checkout Order Summary",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8A90A6)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(selectedPlanName ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Amount to Pay:", fontSize = 12.sp, color = Color.White)
                                    Text("₹${selectedPlanPrice ?: 0}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFF2C314B))
                                Spacer(modifier = Modifier.height(14.dp))

                                // Real Checkout Button
                                Button(
                                    onClick = {
                                        val activity = context as? ComponentActivity
                                        if (activity != null) {
                                            startRazorpayCheckout(
                                                activity = activity,
                                                price = selectedPlanPrice ?: 0,
                                                name = selectedPlanName,
                                                viewModel = viewModel
                                            )
                                            onDismiss() // Close plans picker, Razorpay overlay takes focus
                                        } else {
                                            Toast.makeText(context, "Activity context error", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2854FF)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("real_checkout_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = "Secure Checkout", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pay Securely via Razorpay SDK", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Simulator Button
                                OutlinedButton(
                                    onClick = {
                                        paymentMethod = "Sandbox Demo simulation"
                                        paymentStep = "processing"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF2C314B)),
                                    modifier = Modifier.fillMaxWidth().testTag("sim_checkout_btn")
                                ) {
                                    Text("Dev Live Payment Simulator", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color(0xFF00B2FF))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { paymentStep = "plan_selection" }) {
                                Text("Back to Plans", color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }

                    "razorpay_gateway" -> {
                        // Razorpay Authentic Theme checkout card
                        Surface(
                            color = Color(0xFF0F1123),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF233261)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(16.dp).background(Color(0xFF2854FF), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("R", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("razorpay", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text("SECURE GATEWAY", fontSize = 8.sp, color = Color(0xFF4CD964), fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFF2C314B))
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Text(selectedPlanName ?: "", fontSize = 11.sp, color = Color(0xFF8A90A6))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Amount to Pay:", fontSize = 13.sp, color = Color.White)
                                    Text("₹${selectedPlanPrice ?: 0}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Select simulated payment route:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A90A6))
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                val methods = listOf(
                                    "Google Pay / BHIM UPI" to Icons.Default.Favorite,
                                    "Razorpay Secure Credit / Debit Card" to Icons.Default.Lock,
                                    "Netbanking / Wallet Check" to Icons.Default.List
                                )
                                
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    methods.forEach { (label, icon) ->
                                        Surface(
                                            color = Color(0xFF1E2135),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    paymentMethod = label
                                                    paymentStep = "processing"
                                                }
                                                .testTag("method_${label.take(5)}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = Color(0xFF00B2FF),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { paymentStep = "plan_selection" }) {
                                Text("Back to Plans", color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }

                    "processing" -> {
                        var progress by remember { mutableStateOf(0f) }
                        LaunchedEffect(Unit) {
                            var elapsed = 0
                            while (elapsed < 15) {
                                kotlinx.coroutines.delay(100)
                                elapsed++
                                progress = elapsed / 15f
                            }
                            mockTxnId = "pay_" + (100000..999999).random() + "mock"
                            paymentStep = "success"
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = progress,
                                color = Color(0xFF2854FF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Securing bank handshake...", fontSize = 11.sp, color = Color(0xFF8A90A6))
                            Text("Authorizing ₹${selectedPlanPrice ?: 0} via Razorpay APIs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    "success" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success Icon",
                                tint = Color(0xFF4CD964),
                                modifier = Modifier.size(56.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Razorpay Simulated Payment Success!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Successfully processed transaction.", fontSize = 12.sp, color = Color(0xFF8A90A6))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Surface(
                                color = Color(0xFF1E2135),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Txn Reference ID:", fontSize = 9.sp, color = Color(0xFF8A90A6))
                                        Text(mockTxnId, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Selected Mode:", fontSize = 9.sp, color = Color(0xFF8A90A6))
                                        Text(paymentMethod, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    val isSub = selectedPlanName == "Weekly Explorer" || selectedPlanName == "Monthly Companion" || selectedPlanName == "Yearly Zen"
                                    if (isSub) {
                                        viewModel.purchaseSubscription(
                                            planName = selectedPlanName,
                                            price = selectedPlanPrice ?: 0,
                                            talkTimeCredits = selectedPlanValue ?: 0
                                        )
                                    } else {
                                        viewModel.addWalletBalance(selectedPlanValue ?: 0, selectedPlanName)
                                    }
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CD964)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("txn_success_ok")
                            ) {
                                Text("Complete Upgrade", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    )
}

// ==========================================
// NEW: Secure Passcode Lock & Auth Gateway System
// ==========================================
@Composable
fun SecureAuthGate(viewModel: SaarthiViewModel) {
    val userProfile by viewModel.profile.collectAsStateWithLifecycle()
    val isRegistered = userProfile != null && !userProfile!!.passcodeHash.isNullOrBlank()

    // Form attributes state
    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passcodeInput by remember { mutableStateOf("") }
    var showPasscode by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isSignUpMode by remember { mutableStateOf(!isRegistered) }

    // Synchronize signup mode if registration state changes
    LaunchedEffect(isRegistered) {
        isSignUpMode = !isRegistered
    }

    // Centered aesthetic layout with deep space visual elements
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C0E1E),
                        Color(0xFF131730),
                        Color(0xFF0F1123)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF16192E).copy(alpha = 0.95f))
                .border(BorderStroke(1.5.dp, Color(0xFF283464)), RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lock Badge Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2196F3).copy(alpha = 0.15f))
                    .border(2.dp, Color(0xFF2196F3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "🔒 Security Check",
                    tint = Color(0xFF00B2FF),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand / Feature Intro
            Text(
                text = if (isSignUpMode) "Secure Registration" else "Saarthi Personal Vault",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isSignUpMode) 
                    "Create your secure login details for on-device voice data encryption." 
                else 
                    "Unlock your personal journals and voice companions.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A90A6),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // --- Form Input Fields ---
            if (isSignUpMode) {
                // Name Input
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your Name", fontSize = 12.sp) },
                    leadingIcon = { 
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Name Icon", tint = Color(0xFF8A90A6)) 
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00B2FF),
                        unfocusedBorderColor = Color(0xFF283464),
                        focusedLabelColor = Color(0xFF00B2FF),
                        unfocusedLabelColor = Color(0xFF8A90A6)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_name_field")
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Email Input (In login mode, auto-fill if registered)
            LaunchedEffect(isSignUpMode, userProfile) {
                if (!isSignUpMode && userProfile != null) {
                    emailInput = userProfile!!.userEmail
                }
            }

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Email Address", fontSize = 12.sp) },
                leadingIcon = { 
                    Icon(imageVector = Icons.Default.Email, contentDescription = "Email Icon", tint = Color(0xFF8A90A6)) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00B2FF),
                    unfocusedBorderColor = Color(0xFF283464),
                    focusedLabelColor = Color(0xFF00B2FF),
                    unfocusedLabelColor = Color(0xFF8A90A6)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                enabled = isSignUpMode // Locker prevents email editing for locked device profiles
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Secure Passcode / Password Input
            OutlinedTextField(
                value = passcodeInput,
                onValueChange = { passcodeInput = it },
                label = { Text(if (isSignUpMode) "Enter Secure Passcode (PIN/Pass)" else "Secure Passcode", fontSize = 12.sp) },
                leadingIcon = { 
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Passcode Icon", tint = Color(0xFF8A90A6)) 
                },
                trailingIcon = {
                    IconButton(onClick = { showPasscode = !showPasscode }) {
                        Icon(
                            imageVector = if (showPasscode) Icons.Default.Favorite else Icons.Default.FavoriteBorder, // simple toggler indicators
                            contentDescription = "Toggle Visibility",
                            tint = Color(0xFF00B2FF)
                        )
                    }
                },
                visualTransformation = if (showPasscode) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00B2FF),
                    unfocusedBorderColor = Color(0xFF283464),
                    focusedLabelColor = Color(0xFF00B2FF),
                    unfocusedLabelColor = Color(0xFF8A90A6)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth().testTag("auth_passcode_field")
            )

            // Authentication Error Display
            if (authError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(authError!!, color = Color(0xFFFFD2D2), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Button(
                onClick = {
                    authError = null
                    if (isSignUpMode) {
                        if (nameInput.isBlank() || emailInput.isBlank() || passcodeInput.isBlank()) {
                            authError = "All fields are required."
                            return@Button
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                            authError = "Please enter a valid email."
                            return@Button
                        }
                        if (passcodeInput.length < 4) {
                            authError = "Passcode must be at least 4 characters."
                            return@Button
                        }

                        viewModel.secureSignUp(
                            name = nameInput,
                            email = emailInput,
                            passcode = passcodeInput,
                            onSuccess = {
                                // Registered
                            },
                            onError = { err ->
                                authError = err
                            }
                        )
                    } else {
                        if (emailInput.isBlank() || passcodeInput.isBlank()) {
                            authError = "Email and passcode are required."
                            return@Button
                        }

                        viewModel.secureLogin(
                            email = emailInput,
                            passcode = passcodeInput,
                            onSuccess = {
                                // Connected
                            },
                            onError = { err ->
                                authError = err
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00B2FF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_submit_btn")
            ) {
                Text(
                    text = if (isSignUpMode) "Register & Encrypt Account" else "Unlock Personal Space",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Toggle or Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRegistered) {
                    Text(
                        text = if (isSignUpMode) "Back to Login gate" else "Forgot passcode? Reset App",
                        color = Color(0xFF00B2FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                if (isSignUpMode) {
                                    isSignUpMode = false
                                    authError = null
                                } else {
                                    // Secure option to reset if passcode is lost
                                    viewModel.fullyResetSecureApp()
                                    authError = "Application has been fully reset. Please register a brand new profile."
                                }
                            }
                            .testTag("auth_mode_toggle")
                    )
                } else {
                    Text(
                        text = "🔒 Secured Locally utilizing SHA-256 Hashing.",
                        fontSize = 11.sp,
                        color = Color(0xFF4CD964),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
