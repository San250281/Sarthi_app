package com.aistudio.saarthi.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.saarthi.data.MoodRecord
import com.aistudio.saarthi.viewmodel.SaarthiViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EmotionalJourneySection(viewModel: SaarthiViewModel) {
    val records by viewModel.moodRecords.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()
    val growthReport by viewModel.growthReport.collectAsState()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    // Quick Check-In states
    var showCheckInForm by remember { mutableStateOf(false) }
    var inputMood by remember { mutableStateOf("Normal") }
    var claritySlider by remember { mutableFloatStateOf(6f) }
    var confidenceSlider by remember { mutableFloatStateOf(6f) }
    var noteText by remember { mutableStateOf("") }

    // Synchronize default selection to the last node
    LaunchedEffect(records) {
        if (records.isNotEmpty() && selectedIndex == null) {
            selectedIndex = records.size - 1
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section Title & Header Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dynamic Clarity Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Visualize emotional trends and cognitive confidence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = { showCheckInForm = !showCheckInForm },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCheckInForm) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("journey_check_in_btn")
                ) {
                    Text(
                        text = if (showCheckInForm) "Back to Trends" else "Log Check-In",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (showCheckInForm) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showCheckInForm) {
                // Interactive manual check-in form
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Real-time Emotional Check-In",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Mood log options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val moodOptions = listOf("Normal", "Excited", "Stressed", "Sad", "Confused", "Unmotivated")
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val availableWidth = maxWidth
                            val cellWidth = (availableWidth - 30.dp) / 6
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                moodOptions.forEach { mood ->
                                    val isSelected = mood == inputMood
                                    Box(
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable { inputMood = mood }
                                            .padding(vertical = 8.dp)
                                            .testTag("form_mood_$mood"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = getMoodEmoji(mood),
                                            fontSize = 18.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Clarity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cognitive Clarity",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${claritySlider.toInt()}/10",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = claritySlider,
                            onValueChange = { claritySlider = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Confidence Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Action Confidence",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${confidenceSlider.toInt()}/10",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = confidenceSlider,
                            onValueChange = { confidenceSlider = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }

                    // Log Short Note
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("How's your clarity changing? (optional note)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.addMoodCheckIn(
                                mood = inputMood,
                                confidence = confidenceSlider.toInt(),
                                clarity = claritySlider.toInt(),
                                notes = noteText
                            )
                            // reset
                            noteText = ""
                            showCheckInForm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_check_in_btn")
                    ) {
                        Text(
                            text = "Save Check-In Record",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Trends line chart and detail card
                if (records.size < 2) {
                    // Placeholder fallback if not enough points are pre-loaded
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, contentDescription = "Alert", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Awaiting logs...", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                } else {
                    // Render premium Canvas chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        EmotionalCanvasChart(
                            records = records,
                            selectedIndex = selectedIndex,
                            onPointSelected = { selectedIndex = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Legend indicators representing the two lines
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Spark Sparkle (Clarity)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Wave Shield (Confidence)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Premium Monthly Emotional Growth Report Button
                    Button(
                        onClick = { viewModel.generateGrowthReport() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("generate_growth_report_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "✨ Generate Emotional Growth Report",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 1. Generation Loader Dialog
                    if (isGeneratingReport) {
                        AlertDialog(
                            onDismissRequest = { /* Cannot dismiss during synthesis */ },
                            confirmButton = {},
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Analyzing Growth Patterns...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            text = {
                                Text(
                                    text = "Saarthi is parsing your emotional logs to identify your cognitive progression, resilience landmarks, and custom growth triggers.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            properties = androidx.compose.ui.window.DialogProperties(
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false
                            ),
                            modifier = Modifier.testTag("report_generation_loader")
                        )
                    }

                    // 2. Growth Report Display Dialog
                    if (growthReport != null) {
                        val reportText = growthReport ?: ""
                        val context = androidx.compose.ui.platform.LocalContext.current

                        AlertDialog(
                            onDismissRequest = { viewModel.clearGrowthReport() },
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📈 Monthly Growth Report",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearGrowthReport() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Close Report",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            item {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    reportText.split("\n").forEach { line ->
                                                        when {
                                                            line.startsWith("# ") -> {
                                                                Text(
                                                                    text = line.substring(2),
                                                                    style = MaterialTheme.typography.titleLarge,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                                )
                                                            }
                                                            line.startsWith("## ") -> {
                                                                Text(
                                                                    text = line.substring(3),
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    color = MaterialTheme.colorScheme.secondary,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                                                )
                                                            }
                                                            line.startsWith("### ") -> {
                                                                Text(
                                                                    text = line.substring(4),
                                                                    style = MaterialTheme.typography.titleSmall,
                                                                    color = MaterialTheme.colorScheme.tertiary,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                                )
                                                            }
                                                            line.startsWith("- ") || line.startsWith("* ") -> {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                                    verticalAlignment = Alignment.Top
                                                                ) {
                                                                    Text(
                                                                        text = "• ",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.padding(end = 4.dp)
                                                                    )
                                                                    Text(
                                                                        text = line.substring(2),
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }
                                                            }
                                                            else -> {
                                                                if (line.isNotBlank()) {
                                                                    Text(
                                                                        text = line,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                                                        modifier = Modifier.padding(vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Copy to Clipboard
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Saarthi Emotional Growth Report", reportText)
                                            clipboard.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(context, "Report copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).testTag("copy_report_clipboard_btn")
                                    ) {
                                        Text(text = "Copy Text", maxLines = 1)
                                    }

                                    // Share Report
                                    Button(
                                        onClick = {
                                            val sendIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, reportText)
                                                type = "text/plain"
                                            }
                                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Saarthi Growth Summary")
                                            context.startActivity(shareIntent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1f).testTag("share_report_btn")
                                    ) {
                                        Text(text = "Export / Share", maxLines = 1)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("growth_report_dialog")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail Card summarizing selected checkpoint details
                    val curIdx = selectedIndex
                    if (curIdx != null && curIdx in records.indices) {
                        val record = records[curIdx]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = getMoodEmoji(record.mood),
                                            fontSize = 20.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${record.mood} Vibe",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                    Text(
                                        text = sdf.format(Date(record.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("✨ CLARITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text("${record.clarity}/10 (Focused)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🛡️ CONFIDENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        Text("${record.confidence}/10 (Ready)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (record.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Text(
                                            text = "\"${record.notes}\"",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmotionalCanvasChart(
    records: List<MoodRecord>,
    selectedIndex: Int?,
    onPointSelected: (Int) -> Unit
) {
    val density = LocalDensity.current
    
    // Animate lines drawing fraction
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(records.size) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val markerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(records) {
                detectTapGestures { offset ->
                    // Find x coordinate of closest record point
                    val padLeft = with(density) { 32.dp.toPx() }
                    val padRight = with(density) { 16.dp.toPx() }
                    val plotWidth = size.width - padLeft - padRight
                    val spacing = plotWidth / (records.size - 1).coerceAtLeast(1)

                    var bestIdx = 0
                    var bestDist = Float.MAX_VALUE

                    for (i in records.indices) {
                        val ptX = padLeft + i * spacing
                        val dist = kotlin.math.abs(offset.x - ptX)
                        if (dist < bestDist) {
                            bestDist = dist
                            bestIdx = i
                        }
                    }

                    if (bestDist < spacing / 1.5f) {
                        onPointSelected(bestIdx)
                    }
                }
            }
    ) {
        val padLeft = 32.dp.toPx()
        val padRight = 16.dp.toPx()
        val padTop = 16.dp.toPx()
        val padBottom = 20.dp.toPx()

        val plotWidth = size.width - padLeft - padRight
        val plotHeight = size.height - padTop - padBottom

        // 1. Draw horizontal grid dashed lanes & text indicators
        val gridLines = 5
        for (i in 0 until gridLines) {
            val ratio = i / (gridLines - 1).toFloat()
            val gridY = padTop + ratio * plotHeight
            
            // Draw horizontal guide
            drawLine(
                color = gridColor,
                start = Offset(padLeft, gridY),
                end = Offset(size.width - padRight, gridY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        val n = records.size
        val xSpacing = plotWidth / (n - 1).coerceAtLeast(1)

        // Utility to compute coordinate positions
        fun getCoordX(index: Int): Float = padLeft + index * xSpacing
        // Map 1..10 to low..high height
        fun getCoordY(score: Int): Float {
            val norm = (score - 1) / 9f
            return padTop + (1f - norm) * plotHeight
        }

        // Draw background gradient fields under path transitions
        val pathClarityGrad = Path()
        val pathConfGrad = Path()

        // 2. Build Paths
        val clarityPath = Path()
        val confidencePath = Path()

        val progress = animationProgress.value

        if (n > 0) {
            val limitIndex = ((n - 1) * progress).toInt().coerceIn(0, n - 1)

            // Clarity curve build
            clarityPath.moveTo(getCoordX(0), getCoordY(records[0].clarity))
            pathClarityGrad.moveTo(getCoordX(0), padTop + plotHeight) // base
            pathClarityGrad.lineTo(getCoordX(0), getCoordY(records[0].clarity))

            for (i in 1..limitIndex) {
                val prevX = getCoordX(i - 1)
                val prevY = getCoordY(records[i - 1].clarity)
                val currX = getCoordX(i)
                val currY = getCoordY(records[i].clarity)

                // smooth Bezier control handles
                val cp1X = prevX + (currX - prevX) / 3f
                val cp2X = prevX + 2f * (currX - prevX) / 3f

                clarityPath.cubicTo(cp1X, prevY, cp2X, currY, currX, currY)
                pathClarityGrad.cubicTo(cp1X, prevY, cp2X, currY, currX, currY)
            }
            pathClarityGrad.lineTo(getCoordX(limitIndex), padTop + plotHeight)
            pathClarityGrad.close()

            // Confidence curve build
            confidencePath.moveTo(getCoordX(0), getCoordY(records[0].confidence))
            pathConfGrad.moveTo(getCoordX(0), padTop + plotHeight)
            pathConfGrad.lineTo(getCoordX(0), getCoordY(records[0].confidence))

            for (i in 1..limitIndex) {
                val prevX = getCoordX(i - 1)
                val prevY = getCoordY(records[i - 1].confidence)
                val currX = getCoordX(i)
                val currY = getCoordY(records[i].confidence)

                val cp1X = prevX + (currX - prevX) / 3f
                val cp2X = prevX + 2f * (currX - prevX) / 3f

                confidencePath.cubicTo(cp1X, prevY, cp2X, currY, currX, currY)
                pathConfGrad.cubicTo(cp1X, prevY, cp2X, currY, currX, currY)
            }
            pathConfGrad.lineTo(getCoordX(limitIndex), padTop + plotHeight)
            pathConfGrad.close()

            // 3. Render Area Gradients under curves
            drawPath(
                path = pathClarityGrad,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.22f), Color.Transparent),
                    startY = padTop,
                    endY = padTop + plotHeight
                )
            )

            drawPath(
                path = pathConfGrad,
                brush = Brush.verticalGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.18f), Color.Transparent),
                    startY = padTop,
                    endY = padTop + plotHeight
                )
            )

            // 4. Draw curve Bezier lines
            drawPath(
                path = clarityPath,
                color = primaryColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            drawPath(
                path = confidencePath,
                color = secondaryColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5. Draw point dots and high-contrast marker loops
            for (i in 0..limitIndex) {
                val x = getCoordX(i)
                val yClar = getCoordY(records[i].clarity)
                val yConf = getCoordY(records[i].confidence)

                val isCurrentSelection = i == selectedIndex

                // Draw points for clarity
                drawCircle(
                    color = primaryColor,
                    radius = if (isCurrentSelection) 6.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x, yClar)
                )

                // Highlight selected item with transparent aura halo
                if (isCurrentSelection) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = Offset(x, yClar)
                    )
                }

                // Draw confidence points
                drawCircle(
                    color = secondaryColor,
                    radius = if (isCurrentSelection) 6.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x, yConf)
                )

                if (isCurrentSelection) {
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = Offset(x, yConf)
                    )
                    
                    // Draw vertical marker line pointing at indices
                    drawLine(
                        color = markerColor,
                        start = Offset(x, padTop),
                        end = Offset(x, padTop + plotHeight),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }
    }
}

// Helpers
fun getMoodEmoji(mood: String): String {
    return when (mood.lowercase(Locale.ROOT)) {
        "excited" -> "🔥"
        "stressed" -> "🤯"
        "sad" -> "🩹"
        "confused" -> "🌀"
        "unmotivated" -> "💤"
        else -> "🌱" // Normal Vibe
    }
}
