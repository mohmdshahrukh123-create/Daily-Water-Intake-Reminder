package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

@Composable
fun WaterFluidView(
    progress: Float, // 0.0 to 1.0 (or greater)
    modifier: Modifier = Modifier
) {
    // Wave animation offsets
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    
    val waveOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave_1_offset"
    )

    val waveOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (-2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave_2_offset"
    )

    // Animated bubble heights
    val bubbleTranslation by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "bubble_float"
    )

    // Coerced progress to animate liquid rise smoothly
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "liquid_rise"
    )

    Box(
        modifier = modifier
            .padding(12.dp)
            .size(width = 175.dp, height = 250.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp, topStart = 40.dp, topEnd = 40.dp),
                spotColor = Color(0x33000000)
            )
            .background(Color(0x33FFFFFF), RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp, topStart = 40.dp, topEnd = 40.dp))
            .border(
                width = 4.dp,
                color = Color.White,
                shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp, topStart = 40.dp, topEnd = 40.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 38.dp, bottomEnd = 38.dp, topStart = 38.dp, topEnd = 38.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate liquid level (0 progres is at bottom, 1 is at top)
            val waterLevel = height * (1f - animatedProgress)
            val waveAmplitude = (12.dp.toPx() * (1f - (animatedProgress - 0.5f) * (animatedProgress - 0.5f) * 4).coerceIn(0.2f, 1f))

            // Draw deep back wave (darker, semi-transparent)
            if (animatedProgress > 0.005f) {
                val backWavePath = Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, waterLevel)

                    for (x in 0..width.toInt() step 6) {
                        val relativeX = x.toFloat()
                        // Sinusoidal profile
                        val angle = (relativeX / width * 2 * Math.PI).toFloat() + waveOffset2
                        val y = waterLevel + sin(angle) * waveAmplitude * 0.7f
                        lineTo(relativeX, y)
                    }

                    lineTo(width, height)
                    close()
                }

                drawPath(
                    path = backWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xAA3B82F6),
                            Color(0xEE1E40AF)
                        )
                    )
                )

                // Render dynamic oxygen/water bubbles floating upwards from bottom
                val bubblePoints = listOf(
                    Pair(0.2f, 0.9f),
                    Pair(0.35f, 0.45f),
                    Pair(0.5f, 0.7f),
                    Pair(0.65f, 0.35f),
                    Pair(0.8f, 0.85f),
                    Pair(0.45f, 0.2f)
                )

                bubblePoints.forEach { (xRatio, startYRatio) ->
                    val bubbleX = xRatio * width + sin((waveOffset1 + startYRatio * 10f)) * 10.dp.toPx()
                    val bubbleY = waterLevel + (height - waterLevel) * ((startYRatio + bubbleTranslation) % 1.0f)
                    
                    // Only draw bubbles below active liquid surface
                    if (bubbleY > waterLevel + 10.dp.toPx() && bubbleY < height - 20.dp.toPx()) {
                        val bubbleRadius = (3.dp.toPx() + (startYRatio * 2.dp.toPx()))
                        drawCircle(
                            color = Color(0xB3FFFFFF),
                            radius = bubbleRadius,
                            center = Offset(bubbleX, bubbleY)
                        )
                    }
                }

                // Draw front wave (crispy brand blue, full opacity)
                val frontWavePath = Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, waterLevel)

                    for (x in 0..width.toInt() step 6) {
                        val relativeX = x.toFloat()
                        val angle = (relativeX / width * 2.5 * Math.PI).toFloat() + waveOffset1
                        // Add slightly different amplitude to emphasize parallax 3D
                        val y = waterLevel + sin(angle) * waveAmplitude
                        lineTo(relativeX, y)
                    }

                    lineTo(width, height)
                    close()
                }

                drawPath(
                    path = frontWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6366F1), // Indigo blue transition
                            Color(0xFF3B82F6), // Fresh Blue
                            Color(0xFF1D4ED8)  // Deep Blue
                        )
                    )
                )
            }

            // Draw glossy highlight and inner glass reflection (3D Depth)
            val glassOutlinePath = Path().apply {
                moveTo(2.dp.toPx(), 2.dp.toPx())
                lineTo(width - 2.dp.toPx(), 2.dp.toPx())
                lineTo(width - 2.dp.toPx(), height - 2.dp.toPx())
                lineTo(2.dp.toPx(), height - 2.dp.toPx())
                close()
            }

            // Draw reflections and container side shadows
            drawPath(
                path = glassOutlinePath,
                color = Color(0x33FFFFFF),
                style = Stroke(width = 3.dp.toPx())
            )

            // Dynamic diagonal highlight reflection strip
            drawImageHighlight(width, height)
        }

        // Floating percentage label overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            val percentage = Math.round(progress * 100f)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$percentage",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0x33000000),
                            offset = Offset(0f, 4f),
                            blurRadius = 8f
                        )
                    )
                )
                Text(
                    text = "%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xCCFFFFFF),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = "REACHED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xCCFFFFFF),
                letterSpacing = 1.5.sp,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0x22000000),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawImageHighlight(width: Float, height: Float) {
    // Specular glossy reflex on the list
    val glossPath = Path().apply {
        moveTo(width * 0.15f, 5.dp.toPx())
        quadraticTo(
            width * 0.08f, height * 0.5f,
            width * 0.15f, height - 15.dp.toPx()
        )
        lineTo(width * 0.22f, height - 15.dp.toPx())
        quadraticTo(
            width * 0.16f, height * 0.5f,
            width * 0.22f, 5.dp.toPx()
        )
        close()
    }
    drawPath(
        path = glossPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0x40FFFFFF),
                Color(0x05FFFFFF)
            )
        )
    )
}
