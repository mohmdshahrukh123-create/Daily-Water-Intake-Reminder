package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WaterLog
import com.example.receiver.NotificationReceiver
import com.example.ui.BarItem
import com.example.ui.WaterViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val logs by viewModel.todayLogs.collectAsState()
    val totalLogged by viewModel.todaySum.collectAsState()
    val goal by viewModel.dailyGoal.collectAsState()
    val isOz by viewModel.isImperial.collectAsState()
    val currentGlass by viewModel.glassSize.collectAsState()
    val streak by viewModel.streak.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var activeAnalyticsTab by remember { mutableStateOf(0) } // 0 = Week, 1 = Month, 2 = Year

    // Continuous day boundary transition checks
    LaunchedEffect(Unit) {
        viewModel.checkDayTransition()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Sleek Header with customized icons and level badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2563EB), RoundedCornerShape(12.dp))
                            .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0x662563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💧", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            "Elite Hydrate",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        val levelLabel = when {
                            streak >= 15 -> "Level 5 • Master User"
                            streak >= 5 -> "Level 4 • Pro User"
                            streak >= 3 -> "Level 3 • Advanced User"
                            else -> "Level 2 • Elite User"
                        }
                        Text(
                            levelLabel.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Header interactive actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick celebrate streak meter
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFFECEB), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFFCDCB), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$streak Days",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C),
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFF1F5F9), CircleShape)
                            .shadow(2.dp, CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showSettings = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Scrollable central grid
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Water container mimicking the 3D bottle of Sleek Interface
                item {
                    val progressFloat = if (goal > 0) totalLogged.toFloat() / goal else 0f
                    val percentage = Math.round(progressFloat * 100f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x99FFFFFF), RoundedCornerShape(32.dp))
                            .border(1.dp, Color.White, RoundedCornerShape(32.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Ambient centered 3D bottle
                        WaterFluidView(
                            progress = progressFloat,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        // Goal Breakdown Metrics matched to Sleek Interface HTML template
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        if (isOz) "${Math.round(totalLogged * 0.0338f)}" else String.format("%,d", totalLogged),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        if (isOz) " / ${Math.round(goal * 0.0338f)} oz" else " / ${goal}ml",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                // Custom Tailwind-style double layer progress bar
                                Box(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(6.dp)
                                        .background(Color(0xFFE2E8F0), CircleShape)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressFloat.coerceIn(0f, 1f))
                                            .background(Color(0xFF3B82F6), CircleShape)
                                            .shadow(4.dp, CircleShape, spotColor = Color(0xFF2563EB))
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(0.7f)
                            ) {
                                val diff = maxOf(0, goal - totalLogged)
                                val diffLabel = if (isOz) "${Math.round(diff * 0.0338f)} oz" else "$diff ml"
                                Text(
                                    if (diff > 0) "+$diffLabel needed" else "Goal met! 🎉",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                                Text(
                                    "DAILY TARGET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // Section 2: Next Drink Timer Banner
                item {
                    val nextDrinkMessage = calculateNextDrinkScheduledText(
                        logs = logs,
                        goal = goal,
                        glassSize = currentGlass,
                        isOz = isOz,
                        viewModel = viewModel
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x99FFFFFF), RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x1F000000))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Rounded schedule frame
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "NEXT HYDRATION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )
                                    val sizeLabel = if (isOz) "${Math.round(currentGlass * 0.0338f)} oz" else "$currentGlass ml"
                                    Text(
                                        text = "$nextDrinkMessage • $sizeLabel",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Snooze/Postpone Action Styled as clean active pill tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color(0xFF2563EB))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.postponeAlarmBy20m()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "SNOOZE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // Section 3: Interactive Dynamic Cups
                item {
                    Column {
                        Text(
                            "Tap a Cup to Log Water",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val cups = listOf(100, 250, 500)
                            cups.forEach { ml ->
                                val active = currentGlass == ml
                                val ozVal = Math.round(ml * 0.0338f)
                                val volumeLabel = if (isOz) "$ozVal oz" else "$ml ml"
                                val iconType = when (ml) {
                                    100 -> "☕"
                                    250 -> "🥛"
                                    else -> "🍾"
                                }
                                val titleType = when (ml) {
                                    100 -> "Espresso"
                                    250 -> "Standard Glass"
                                    else -> "Sports Bottle"
                                }

                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (active) Color(0xFF2563EB) else Color(0x99FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .border(
                                            width = if (active) 0.dp else 1.dp,
                                            color = if (active) Color.Transparent else Color.White,
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .shadow(if (active) 12.dp else 2.dp, RoundedCornerShape(24.dp), spotColor = if (active) Color(0xFF2563EB) else Color(0x0A000000))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.updateGlassSize(ml)
                                            viewModel.logWater(ml)
                                        }
                                        .testTag("cup_${ml}ml_button")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = iconType, fontSize = 28.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            volumeLabel,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else Color(0xFF2563EB)
                                        )
                                        Text(
                                            titleType,
                                            fontSize = 9.sp,
                                            color = if (active) Color(0xCCFFFFFF) else Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Analytics Graphs
                item {
                    val allLogsList by viewModel.allLogs.collectAsState()
                    val barItems = when (activeAnalyticsTab) {
                        0 -> viewModel.getWeeklySummary(allLogsList)
                        1 -> viewModel.getMonthlySummary(allLogsList)
                        else -> viewModel.getYearlySummary(allLogsList)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x99FFFFFF), RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x0A000000))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Analytics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            // Tabs selector Row items
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("WEEK", "MONTH", "YEAR").forEachIndexed { index, title ->
                                    val active = activeAnalyticsTab == index
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (active) Color.White else Color.Transparent)
                                            .clickable { activeAnalyticsTab = index }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            title,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color(0xFF2563EB) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom animated bar renderer
                        BarChartComponent(items = barItems, maxGoal = goal.toFloat(), isImperial = isOz)
                    }
                }

                // Section 5: Today's Timeline
                item {
                    Text(
                        "Today's Timeline (Logged vs Scheduled)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (logs.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x0F0F62FE), RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🥤", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No water logged today yet.",
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Your schedules will fill up automatically.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    items(logs, key = { it.id }) { logItem ->
                        TimelineRowItem(log = logItem, isOz = isOz, onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteLog(logItem.id)
                        })
                    }
                }

                // Bottom padding inside LazyColumn
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Full Settings Dialog
        if (showSettings) {
            SettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettings = false }
            )
        }
    }
}

// ---------------------- SUB-COMPONENTS ----------------------

@Composable
fun TimelineRowItem(
    log: WaterLog,
    isOz: Boolean,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val displayTime = formatter.format(Date(log.timestamp))
    val volumeLabel = if (isOz) "${Math.round(log.amountMl * 0.0338f)} oz" else "${log.amountMl} ml"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x99FFFFFF), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White, RoundedCornerShape(16.dp))
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x05000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFF1F5F9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💧", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Drank $volumeLabel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        displayTime,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete log row",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun BarChartComponent(
    items: List<BarItem>,
    maxGoal: Float,
    isImperial: Boolean
) {
    val maxBarAmount = items.maxOfOrNull { it.amountMl }?.toFloat()?.coerceAtLeast(1000f) ?: 2500f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        items.forEach { item ->
            // Ratio calculations for responsive heights
            val heightRatio = (item.amountMl.toFloat() / maxBarAmount).coerceIn(0.02f, 1f)
            val animatedHeight by animateDpAsState(
                targetValue = (heightRatio * 120).dp,
                label = "chart_bar_height"
            )

            val hitGoal = item.amountMl >= maxGoal

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Bar Value Badge
                val displayMl = if (isImperial) "${Math.round(item.amountMl * 0.0338f)}" else "${item.amountMl}"
                Text(
                    text = displayMl,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hitGoal) Color(0xFF16A34A) else Color(0xFF2563EB)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // The bar segment
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(animatedHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                if (hitGoal) listOf(Color(0xFF4ADE80), Color(0xFF16A34A))
                                else listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
                            )
                        )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Label
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

// Settings dialog Composable
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    viewModel: WaterViewModel,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var isResetConfirmShown by remember { mutableStateOf(false) }

    // Read views parameters
    val isOz by viewModel.isImperial.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val height by viewModel.height.collectAsState()
    val workout by viewModel.workoutMinutes.collectAsState()
    val wakeH by viewModel.wakeHour.collectAsState()
    val wakeM by viewModel.wakeMinute.collectAsState()
    val bedH by viewModel.bedHour.collectAsState()
    val bedM by viewModel.bedMinute.collectAsState()
    val weatherBoost by viewModel.weatherAdjust.collectAsState()
    val sound by viewModel.soundEnabled.collectAsState()
    val vibe by viewModel.vibrateEnabled.collectAsState()
    val notify by viewModel.notificationsEnabled.collectAsState()
    val remindPostGoal by viewModel.remindAfterGoal.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x330F172A))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Glass card container
            Card(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consumed standard touch */ }
                    .shadow(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header row panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Advanced Settings",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F62FE)
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section A: Personal Metrics
                        Text("Personal Physical Specs", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF64748B))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Unit metrics Toggle switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Display Imperial System (lbs, oz)", fontSize = 14.sp, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                                    Switch(
                                        checked = isOz,
                                        onCheckedChange = { viewModel.updateUnits(it) }
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // Weight slider
                                Column {
                                    val wUnit = if (isOz) "lbs" else "kg"
                                    val displayW = if (isOz) Math.round(weight * 2.20462f) else Math.round(weight)
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Body Weight", fontSize = 14.sp, color = Color(0xFF334155))
                                        Text("$displayW $wUnit", fontWeight = FontWeight.Bold, color = Color(0xFF0F62FE))
                                    }
                                    Slider(
                                        value = if (isOz) weight * 2.20462f else weight,
                                        onValueChange = {
                                            viewModel.updateWeight(if (isOz) it / 2.20462f else it)
                                        },
                                        valueRange = if (isOz) 66f..330f else 30f..150f
                                    )
                                }

                                // Height slider
                                Column {
                                    val hUnit = if (isOz) "in" else "cm"
                                    val displayH = if (isOz) Math.round(height * 0.3937f) else Math.round(height)
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Body Height", fontSize = 14.sp, color = Color(0xFF334155))
                                        Text("$displayH $hUnit", fontWeight = FontWeight.Bold, color = Color(0xFF0F62FE))
                                    }
                                    Slider(
                                        value = if (isOz) height * 0.3937f else height,
                                        onValueChange = {
                                            viewModel.updateHeight(if (isOz) it / 0.3937f else it)
                                        },
                                        valueRange = if (isOz) 39f..98f else 100f..250f
                                    )
                                }

                                // Workout Slider
                                Column {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Heavy Workout Level", fontSize = 14.sp, color = Color(0xFF334155))
                                        Text("$workout mins/day", fontWeight = FontWeight.Bold, color = Color(0xFF0F62FE))
                                    }
                                    Slider(
                                        value = workout.toFloat(),
                                        onValueChange = { viewModel.updateWorkoutMinutes(it.toInt()) },
                                        valueRange = 0f..120f,
                                        steps = 23
                                    )
                                }
                            }
                        }

                        // Section B: Day Wake-Sleep cycle
                        Text("Clock Sleeping Cycle", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF64748B))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Wake Hour
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Wake-up Time", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                        Text("Standard morning cycle alerts begin", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Text(
                                        text = formatTime(wakeH, wakeM),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F62FE),
                                        modifier = Modifier
                                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                            .clickable {
                                                // Quick loop increment hour
                                                viewModel.updateWakeTime((wakeH + 1) % 24, wakeM)
                                            }
                                            .padding(8.dp)
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // Sleep Hour
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Bedtime Sleep", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                        Text("All hydration reminders halt automatically", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Text(
                                        text = formatTime(bedH, bedM),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F62FE),
                                        modifier = Modifier
                                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.updateBedTime((bedH + 1) % 24, bedM)
                                            }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }

                        // Section C: Reminder Engine Toggles
                        Text("Reminder Engine Configuration", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF64748B))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Master Notification toggle
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Sip Reminders", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                        Text("Disable standard drink alerts", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = notify, onCheckedChange = { viewModel.updateNotificationsEnabled(it) })
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // Remind after target reached
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Further Alerts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                        Text("Send reminders even after daily goal met", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Switch(checked = remindPostGoal, onCheckedChange = { viewModel.updateRemindAfterGoal(it) })
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // Play Sound
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Notification Sounds", fontSize = 14.sp, color = Color(0xFF334155))
                                    Switch(checked = sound, onCheckedChange = { viewModel.updateSoundEnabled(it) })
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // Play vibration
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Alert Vibrations", fontSize = 14.sp, color = Color(0xFF334155))
                                    Switch(checked = vibe, onCheckedChange = { viewModel.updateVibrateEnabled(it) })
                                }
                            }
                        }

                        // Section D: Weather Adjust Deep Feature
                        Text("Climatic Dynamic Modifiers", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF64748B))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Climatic Weather Adjust ☀️", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0369A1))
                                        Text("Increase intake target on hot/dry days (+400ml)", fontSize = 11.sp, color = Color(0xFF0369A1))
                                    }
                                    Switch(checked = weatherBoost, onCheckedChange = { viewModel.updateWeatherAdjust(it) })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Privacy Reset Button
                        if (!isResetConfirmShown) {
                            Button(
                                onClick = { isResetConfirmShown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                            ) {
                                Text("Reset All Data & Logs Wipes", color = Color(0xFFEF4444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, Color(0xFFFCA5A5), RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Are you absolutely sure?", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 14.sp)
                                    Text("This will wipe all historical drink tables, resets preferences metrics and returns to Onboarding immediately.", fontSize = 11.sp, color = Color(0xFF7F1D1D), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.resetAllData()
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("YES, RESET ALL")
                                        }

                                        TextButton(onClick = { isResetConfirmShown = false }) {
                                            Text("Cancel", color = Color(0xFF475569))
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
}

// Time calculations formatting helpers
private fun formatTime(hour: Int, minute: Int): String {
    val displayPeriod = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, displayPeriod)
}

// Precise countdown indicator computations
private fun calculateNextDrinkScheduledText(
    logs: List<WaterLog>,
    goal: Int,
    glassSize: Int,
    isOz: Boolean,
    viewModel: WaterViewModel
): String {
    val totalLogged = logs.sumOf { it.amountMl }
    if (totalLogged >= goal && !viewModel.remindAfterGoal.value) {
        return "Goal Met! Good Job 🏆"
    }

    val wakeH = viewModel.wakeHour.value
    val wakeM = viewModel.wakeMinute.value
    val bedH = viewModel.bedHour.value
    val bedM = viewModel.bedMinute.value

    val cal = Calendar.getInstance()
    val currHour = cal.get(Calendar.HOUR_OF_DAY)
    val currMin = cal.get(Calendar.MINUTE)
    val currMins = currHour * 60 + currMin
    val wakeMins = wakeH * 60 + wakeM
    val bedMins = bedH * 60 + bedM

    val displayGlass = if (isOz) "${Math.round(glassSize * 0.0338f)} oz" else "$glassSize ml"

    if (currMins < wakeMins) {
        val displayTimeStr = formatTime(wakeH, wakeM)
        return "Rest Status: Alarms begin morning at $displayTimeStr"
    } else if (currMins >= bedMins) {
        return "Rest Status: Wakeup scheduled tomorrow morning"
    }

    // Average dynamic intervals
    val totalActiveMins = if (bedMins > wakeMins) bedMins - wakeMins else (24 * 60 - wakeMins) + bedMins
    val totalGlassesNeeded = Math.ceil(goal.toDouble() / glassSize).toInt()
    val rawInterval = if (totalGlassesNeeded > 0) totalActiveMins / totalGlassesNeeded else 120
    val intervalMins = maxOf(30, rawInterval)

    // Calculate approximate next sip boundary.
    // e.g. based on last logs, or simply approximate timeline based on current time
    val lastItemTimestamp = logs.firstOrNull()?.timestamp ?: 0L
    val timeSinceLastMs = System.currentTimeMillis() - lastItemTimestamp
    val minsSinceLast = (timeSinceLastMs / (60 * 1000)).toInt()

    val remainderMins = if (lastItemTimestamp == 0L) {
        intervalMins
    } else {
        (intervalMins - minsSinceLast).coerceIn(1, intervalMins)
    }

    return "Next: ${remainderMins}m left - $displayGlass"
}
