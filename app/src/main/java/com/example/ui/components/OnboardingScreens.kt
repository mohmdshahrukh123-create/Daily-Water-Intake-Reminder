package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.WaterViewModel
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingScreens(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    val currentStep by viewModel.onboardingStep.collectAsState()
    
    // Slide transition for steps
    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            // Mandate right to left horizontal sliding
            if (targetState > initialState) {
                slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            } else {
                slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "onboarding_navigation_slider"
    ) { step ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (step) {
                0 -> OnboardIntro(onNext = { viewModel.nextStep() })
                1 -> NotificationPermissionGate(viewModel = viewModel, onNext = { viewModel.nextStep() })
                2 -> GenderSelection(viewModel = viewModel, onNext = { viewModel.nextStep() }, onBack = { viewModel.prevStep() })
                3 -> BodyMetricsSelection(viewModel = viewModel, onNext = { viewModel.nextStep() }, onBack = { viewModel.prevStep() })
                4 -> ActivitySelection(viewModel = viewModel, onNext = { viewModel.nextStep() }, onBack = { viewModel.prevStep() })
                5 -> CycleSettingsSelection(viewModel = viewModel, onComplete = { viewModel.completeOnboarding() }, onBack = { viewModel.prevStep() })
            }
        }
    }
}

// 1. INTRO
@Composable
fun OnboardIntro(onNext: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_button")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "button_glow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboard_intro_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        
        // Large glass droplet design element
        Box(
            modifier = Modifier
                .size(160.dp)
                .shadow(16.dp, CircleShape, ambientColor = Color(0x3F3F8CFF))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE0F2FE), Color(0xFF67B5FB))
                    ),
                    shape = CircleShape
                )
                .border(2.dp, Color(0xCCFFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("💧", fontSize = 72.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Elite Hydration\nCompanion",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 40.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF0F62FE)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hi, I'm your personal hydration companion.\nLet's keep your body perfectly hydrated with intelligent schedules and beautiful real-time water simulation.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF475569),
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F62FE)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(60.dp)
                .testTag("lets_go_button")
                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF0F62FE))
                .rotate(0f) // trigger shadow update
                .rotate(0f)
        ) {
            Text(
                "LET'S GO",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// 2. PERMISSION GATE
@Composable
fun NotificationPermissionGate(
    viewModel: WaterViewModel,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var isPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            isPermissionGranted = granted
            if (granted) {
                onNext()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_gate_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFFEFF6FF), CircleShape)
                .border(2.dp, Color(0x803B82F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Notification Icon",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Stay Hydrated on Time! ⏰",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Glassmorphic explanatory card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x803B82F6), RoundedCornerShape(24.dp))
                .shadow(8.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A core feature of the Hydration Companion is smart periodic reminders based on your wakeup, workout, and sleep times.",
                    fontSize = 15.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Info, "Info", tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notification permissions are required.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onNext()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F62FE)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .testTag("grant_permission_button")
        ) {
            Text(
                if (isPermissionGranted) "CONTINUE" else "GRANT PERMISSION",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (!isPermissionGranted) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onNext) {
                Text("Dismiss & Enter anyway (Reminders disabled)", color = Color(0xFF64748B), fontSize = 14.sp)
            }
        }
    }
}

// 3. GENDER
@Composable
fun GenderSelection(
    viewModel: WaterViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val selectedGender by viewModel.gender.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gender_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Navigation Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, "Back", tint = Color(0xFF0F62FE), modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tell us about yourself",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Text(
            text = "This allows us to personalize your biological index formulas.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Male Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedGender == "Male") Color(0xFFE0F2FE) else Color(0xAAFFFFFF)
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(120.dp)
                .border(
                    width = if (selectedGender == "Male") 2.dp else 1.dp,
                    color = if (selectedGender == "Male") Color(0xFF0F62FE) else Color(0x4064748B),
                    shape = RoundedCornerShape(24.dp)
                )
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clickable { viewModel.updateGender("Male") }
                .testTag("gender_male_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFF3B82F6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Male, "Male", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Column {
                    Text("Male", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Biological base scale: 35ml/kg", fontSize = 12.sp, color = Color(0xFF475569))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Female Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedGender == "Female") Color(0xFFFCE7F3) else Color(0xAAFFFFFF)
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(120.dp)
                .border(
                    width = if (selectedGender == "Female") 2.dp else 1.dp,
                    color = if (selectedGender == "Female") Color(0xFFEC4899) else Color(0x4064748B),
                    shape = RoundedCornerShape(24.dp)
                )
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clickable { viewModel.updateGender("Female") }
                .testTag("gender_female_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFEC4899), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Female, "Female", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Column {
                    Text("Female", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Biological base scale: 30ml/kg", fontSize = 12.sp, color = Color(0xFF475569))
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F62FE)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .testTag("gender_continue_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("CONTINUE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, "Next")
            }
        }
    }
}

// 4. BODY METRICS
@Composable
fun BodyMetricsSelection(
    viewModel: WaterViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val unitImperial by viewModel.isImperial.collectAsState()
    val weightVal by viewModel.weight.collectAsState()
    val heightVal by viewModel.height.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("metrics_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, "Back", tint = Color(0xFF0F62FE), modifier = Modifier.size(36.dp))
            }
        }

        Text(
            text = "Your Metrics Scale",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // System Selector Tab Row
        TabRow(
            selectedTabIndex = if (unitImperial) 1 else 0,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
            containerColor = Color(0xFFF1F5F9),
            indicator = { Box(Modifier.fillMaxSize()) }
        ) {
            Tab(
                selected = !unitImperial,
                onClick = { viewModel.updateUnits(false) },
                text = { Text("Metric (kg, cm)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                selectedContentColor = Color(0xFF0F62FE),
                unselectedContentColor = Color(0xFF64748B)
            )
            Tab(
                selected = unitImperial,
                onClick = { viewModel.updateUnits(true) },
                text = { Text("Imperial (lbs, in)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                selectedContentColor = Color(0xFF0F62FE),
                unselectedContentColor = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Weight Section
        val displayWeight = if (unitImperial) Math.round(weightVal * 2.20462f) else Math.round(weightVal)
        val weightUnit = if (unitImperial) "lbs" else "kg"

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
            modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weight:", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Text("$displayWeight $weightUnit", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F62FE))
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Beautiful slider micro adjuster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledIconButton(
                        onClick = {
                            val newWeight = if (unitImperial) (weightVal - 1f / 2.2f) else (weightVal - 1f)
                            viewModel.updateWeight(newWeight.coerceAtLeast(30f))
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEFF6FF))
                    ) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F62FE))
                    }

                    Slider(
                        value = if (unitImperial) weightVal * 2.20462f else weightVal,
                        onValueChange = {
                            val computedKg = if (unitImperial) it / 2.20462f else it
                            viewModel.updateWeight(computedKg)
                        },
                        valueRange = if (unitImperial) 66f..330f else 30f..150f,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )

                    FilledIconButton(
                        onClick = {
                            val newWeight = if (unitImperial) (weightVal + 1f / 2.2f) else (weightVal + 1f)
                            viewModel.updateWeight(newWeight.coerceIn(30f, 150f))
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEFF6FF))
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F62FE))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Height Section
        val displayHeight = if (unitImperial) Math.round(heightVal * 0.3937f) else Math.round(heightVal)
        val heightUnit = if (unitImperial) "in" else "cm"

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
            modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Height:", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Text("$displayHeight $heightUnit", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F62FE))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledIconButton(
                        onClick = {
                            val newHeight = if (unitImperial) (heightVal - 1f / 0.3937f) else (heightVal - 1f)
                            viewModel.updateHeight(newHeight.coerceAtLeast(100f))
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEFF6FF))
                    ) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F62FE))
                    }

                    Slider(
                        value = if (unitImperial) heightVal * 0.3937f else heightVal,
                        onValueChange = {
                            val computedCm = if (unitImperial) it / 0.3937f else it
                            viewModel.updateHeight(computedCm)
                        },
                        valueRange = if (unitImperial) 39f..98f else 100f..250f,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )

                    FilledIconButton(
                        onClick = {
                            val newHeight = if (unitImperial) (heightVal + 1f / 0.3937f) else (heightVal + 1f)
                            viewModel.updateHeight(newHeight.coerceIn(100f, 250f))
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEFF6FF))
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F62FE))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F62FE)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .testTag("metrics_continue_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CONTINUE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, "Next")
            }
        }
    }
}

// 5. ACTIVITY
@Composable
fun ActivitySelection(
    viewModel: WaterViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val activeMinutes by viewModel.workoutMinutes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workout_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, "Back", tint = Color(0xFF0F62FE), modifier = Modifier.size(36.dp))
            }
        }

        Text(
            text = "Active Heavy Workouts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Text(
            text = "Exercising rapidly depletes hydration. Choose your average daily heavy workout speed minutes.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Big visual dial
        Box(
            modifier = Modifier
                .size(160.dp)
                .shadow(8.dp, CircleShape, ambientColor = Color(0x1F22C55E))
                .background(Color(0xFFF0FDF4), CircleShape)
                .border(2.dp, Color(0xFF22C55E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = Color(0xFF15803D),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "$activeMinutes",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF15803D)
                )
                Text(
                    "mins/day",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Workout categories badge depending on selected minutes
        val (speedLabel, speedColor) = when {
            activeMinutes == 0 -> Pair("Sedentary ☕", Color(0xFF64748B))
            activeMinutes <= 30 -> Pair("Light Action 🧘", Color(0xFF0284C7))
            activeMinutes <= 60 -> Pair("Healthy Active 🏃", Color(0xFF16A34A))
            else -> Pair("Elite Workout 🔥", Color(0xFFEA580C))
        }

        SuggestionChip(
            onClick = {},
            label = { Text(speedLabel, fontWeight = FontWeight.Bold, color = Color.White) },
            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = speedColor),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Slider(
            value = activeMinutes.toFloat(),
            onValueChange = { viewModel.updateWorkoutMinutes(it.toInt()) },
            valueRange = 0f..120f,
            steps = 23, // 5 min intervals
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F62FE)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .testTag("workout_continue_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CONTINUE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, "Next")
            }
        }
    }
}

// 6. DAILY CYCLE Day/Night transitions
@Composable
fun CycleSettingsSelection(
    viewModel: WaterViewModel,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val wakeH by viewModel.wakeHour.collectAsState()
    val wakeM by viewModel.wakeMinute.collectAsState()
    val bedH by viewModel.bedHour.collectAsState()
    val bedM by viewModel.bedMinute.collectAsState()

    // Flag representing whether we are on bedtime step (true = Night) or wakeup step (false = Day)
    var isNightStep by remember { mutableStateOf(false) }

    // Color animations for the Sun/Moon background card shift
    val transitionDuration = 1200
    val animatedBgStart by animateColorAsState(
        targetValue = if (isNightStep) Color(0xFF0A0F2C) else Color(0xFFFFECC8),
        animationSpec = tween(transitionDuration), label = "grad_start"
    )
    val animatedBgEnd by animateColorAsState(
        targetValue = if (isNightStep) Color(0xFF020412) else Color(0xFF87CEEB),
        animationSpec = tween(transitionDuration), label = "grad_end"
    )

    // Orbit animations for the Sun/Set
    val orbitAngle by animateFloatAsState(
        targetValue = if (isNightStep) 180f else 0f,
        animationSpec = tween(transitionDuration, easing = EaseInOutCubic), label = "sun_moon_angle"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cycle_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = {
                    if (isNightStep) {
                        isNightStep = false
                    } else {
                        onBack()
                    }
                }
            ) {
                Icon(Icons.Default.ChevronLeft, "Back", tint = Color(0xFF0F62FE), modifier = Modifier.size(36.dp))
            }
        }

        Text(
            text = if (isNightStep) "Night Cycle" else "Day Cycle",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Text(
            text = if (isNightStep) "Select your Bedtime below" else "Select your Wake-up time below",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // The Day/Night Animation Card Frame Container
        Card(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(280.dp)
                .shadow(16.dp, RoundedCornerShape(32.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(animatedBgStart, animatedBgEnd)
                        )
                    )
                    .padding(24.dp)
            ) {
                // Sun/Moon celestial drawer
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.TopCenter)
                ) {
                    val scaleWidth = size.width
                    val scaleHeight = size.height
                    
                    val radius = 100.dp.toPx()
                    val center = Offset(scaleWidth / 2f, scaleHeight + 20.dp.toPx())

                    // Calculate celestial angles based on transition state
                    // 0 degrees is Sun up, Moon down. 180 degrees is Moon up, Sun down.
                    val angleRad = Math.toRadians(orbitAngle.toDouble() - 90.5)

                    val sunX = center.x + radius * cos(angleRad).toFloat()
                    val sunY = center.y + radius * sin(angleRad).toFloat()

                    val moonX = center.x - radius * cos(angleRad).toFloat()
                    val moonY = center.y - radius * sin(angleRad).toFloat()

                    // Draw Orbit Path
                    drawCircle(
                        color = Color(0x1AFFFFFF),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw Sun
                    drawCircle(
                        color = Color(0xFFFFAE34),
                        radius = 24.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                    // Sun glow ring
                    drawCircle(
                        color = Color(0x33FFAE34),
                        radius = 34.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )

                    // Draw Moon (crisp glowing Silver crescent)
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        radius = 20.dp.toPx(),
                        center = Offset(moonX, moonY)
                    )
                    // Negative cut-out to form a crescent crescent moon look
                    drawCircle(
                        color = animatedBgStart,
                        radius = 18.dp.toPx(),
                        center = Offset(moonX - 6.dp.toPx(), moonY - 4.dp.toPx())
                    )
                    
                    // Draw mini stars if midnight step
                    if (orbitAngle > 90f) {
                        drawCircle(Color.White, 1.dp.toPx(), Offset(scaleWidth * 0.15f, 25.dp.toPx()))
                        drawCircle(Color.White, 1.5.dp.toPx(), Offset(scaleWidth * 0.3f, 40.dp.toPx()))
                        drawCircle(Color.White, 1.2.dp.toPx(), Offset(scaleWidth * 0.85f, 15.dp.toPx()))
                        drawCircle(Color.White, 1.dp.toPx() * 0.8f, Offset(scaleWidth * 0.7f, 35.dp.toPx()))
                    }
                }

                // Interactive Clock Time selection fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isNightStep) "Moon Sleep Mode 🛌" else "Daybreak Wakeup 🌅",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNightStep) Color(0xFF94A3B8) else Color(0xFF0369A1)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val currentHour = if (isNightStep) bedH else wakeH
                        val currentMin = if (isNightStep) bedM else wakeM
                        val displayPeriod = if (currentHour >= 12) "PM" else "AM"
                        val displayHour = when {
                            currentHour == 0 -> 12
                            currentHour > 12 -> currentHour - 12
                            else -> currentHour
                        }
                        val displayMinStr = String.format(Locale.getDefault(), "%02d", currentMin)

                        // Hours scroller selection button
                        Box(
                            modifier = Modifier
                                .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(12.dp))
                                .clickable {
                                    val nextHour = (currentHour + 1) % 24
                                    if (isNightStep) {
                                        viewModel.updateBedTime(nextHour, currentMin)
                                    } else {
                                        viewModel.updateWakeTime(nextHour, currentMin)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                String.format(Locale.getDefault(), "%2d", displayHour),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNightStep) Color.White else Color(0xFF1E293B)
                            )
                        }

                        Text(
                            ":",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightStep) Color.White else Color(0xFF1E293B)
                        )

                        // Minutes scroller selection button (increments by 15 mins)
                        Box(
                            modifier = Modifier
                                .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(12.dp))
                                .clickable {
                                    val nextMin = (currentMin + 15) % 60
                                    if (isNightStep) {
                                        viewModel.updateBedTime(currentHour, nextMin)
                                    } else {
                                        viewModel.updateWakeTime(currentHour, nextMin)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                displayMinStr,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNightStep) Color.White else Color(0xFF1E293B)
                            )
                        }

                        Text(
                            displayPeriod,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightStep) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Progress button
        Button(
            onClick = {
                if (!isNightStep) {
                    isNightStep = true
                } else {
                    onComplete()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isNightStep) Color(0xFF22C55E) else Color(0xFF0F62FE)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .testTag("cycle_continue_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isNightStep) "FINISH SETUP" else "CONFIRM WAKEU TIME",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!isNightStep) {
                    Icon(Icons.Default.ChevronRight, "Next")
                }
            }
        }
    }
}
