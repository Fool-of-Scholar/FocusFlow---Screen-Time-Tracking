package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PandaMascot(
    modifier: Modifier = Modifier,
    expression: String = "happy" // "happy", "sad", "focused", "sleepy"
) {
    // Idle bounce animation sequence
    val transition = rememberInfiniteTransition(label = "idle")
    val bounceOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Canvas(modifier = modifier.size(100.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + bounceOffset

        // Colors
        val pandaWhite = Color(0xFFF8F9FA)
        val pandaBlack = Color(0xFF212529)
        val cheekPink = Color(0xFFFFB6C1)
        val glassesTeal = Color(0xFF00C7AE)

        // 1. Ears (Left & Right)
        drawCircle(
            color = pandaBlack,
            radius = w * 0.18f,
            center = Offset(cx - w * 0.32f, cy - h * 0.28f)
        )
        drawCircle(
            color = pandaBlack,
            radius = w * 0.18f,
            center = Offset(cx + w * 0.32f, cy - h * 0.28f)
        )

        // 2. White Head Face Base
        drawCircle(
            color = pandaWhite,
            radius = w * 0.42f,
            center = Offset(cx, cy)
        )

        // Head contour line
        drawCircle(
            color = pandaBlack,
            radius = w * 0.42f,
            center = Offset(cx, cy),
            style = Stroke(width = 4f)
        )

        // 3. Eye Patches (Black blobs)
        // Left patch
        val leftPatchPath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    cx - w * 0.25f, cy - h * 0.12f,
                    cx - w * 0.05f, cy + h * 0.12f
                )
            )
        }
        drawPath(leftPatchPath, color = pandaBlack)

        // Right patch
        val rightPatchPath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    cx + w * 0.05f, cy - h * 0.12f,
                    cx + w * 0.25f, cy + h * 0.12f
                )
            )
        }
        drawPath(rightPatchPath, color = pandaBlack)

        // 4. Pupils / Eyebal design depending on expression
        when (expression.lowercase()) {
            "focused" -> {
                // Determined circular eyes + glowing tiny center dot
                drawCircle(color = Color.White, radius = w * 0.045f, center = Offset(cx - w * 0.14f, cy))
                drawCircle(color = Color.White, radius = w * 0.045f, center = Offset(cx + w * 0.14f, cy))

                // Inner bright teal center
                drawCircle(color = glassesTeal, radius = w * 0.02f, center = Offset(cx - w * 0.14f, cy))
                drawCircle(color = glassesTeal, radius = w * 0.02f, center = Offset(cx + w * 0.14f, cy))

                // Smart wire focus glasses overlay!
                drawCircle(
                    color = glassesTeal,
                    radius = w * 0.13f,
                    center = Offset(cx - w * 0.14f, cy),
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = glassesTeal,
                    radius = w * 0.13f,
                    center = Offset(cx + w * 0.14f, cy),
                    style = Stroke(width = 4f)
                )
                // Bridge
                drawLine(
                    color = glassesTeal,
                    start = Offset(cx - w * 0.01f, cy),
                    end = Offset(cx + w * 0.01f, cy),
                    strokeWidth = 4f
                )
            }
            "sad" -> {
                // Downturned sad arcs (U shape inverted)
                val leftSadPath = Path().apply {
                    moveTo(cx - w * 0.19f, cy + h * 0.03f)
                    quadraticBezierTo(
                        cx - w * 0.14f, cy - h * 0.05f,
                        cx - w * 0.09f, cy + h * 0.03f
                    )
                }
                drawPath(leftSadPath, color = Color.White, style = Stroke(width = 4.5f))

                val rightSadPath = Path().apply {
                    moveTo(cx + w * 0.09f, cy + h * 0.03f)
                    quadraticBezierTo(
                        cx + w * 0.14f, cy - h * 0.05f,
                        cx + w * 0.19f, cy + h * 0.03f
                    )
                }
                drawPath(rightSadPath, color = Color.White, style = Stroke(width = 4.5f))
            }
            "sleepy" -> {
                // Closed relaxing arcs (smile shaped eyes)
                val leftSleepPath = Path().apply {
                    moveTo(cx - w * 0.20f, cy - h * 0.01f)
                    quadraticBezierTo(
                        cx - w * 0.14f, cy + h * 0.05f,
                        cx - w * 0.08f, cy - h * 0.01f
                    )
                }
                drawPath(leftSleepPath, color = Color.White, style = Stroke(width = 4.5f))

                val rightSleepPath = Path().apply {
                    moveTo(cx + w * 0.08f, cy - h * 0.01f)
                    quadraticBezierTo(
                        cx + w * 0.14f, cy + h * 0.05f,
                        cx + w * 0.20f, cy - h * 0.01f
                    )
                }
                drawPath(rightSleepPath, color = Color.White, style = Stroke(width = 4.5f))

                // Zzz particle bubble
                drawArc(
                    color = glassesTeal,
                    startAngle = -20f,
                    sweepAngle = 40f,
                    useCenter = false,
                    topLeft = Offset(cx + w * 0.22f, cy - h * 0.45f),
                    size = Size(w * 0.1f, h * 0.1f),
                    style = Stroke(width = 3.5f)
                )
            }
            else -> { // "happy" / default
                // Big happy pupils looking up
                drawCircle(color = Color.White, radius = w * 0.06f, center = Offset(cx - w * 0.14f, cy - h * 0.01f))
                drawCircle(color = Color.White, radius = w * 0.06f, center = Offset(cx + w * 0.14f, cy - h * 0.01f))

                // Shimmer sparklers
                drawCircle(color = pandaBlack, radius = w * 0.03f, center = Offset(cx - w * 0.13f, cy - h * 0.02f))
                drawCircle(color = pandaBlack, radius = w * 0.03f, center = Offset(cx + w * 0.15f, cy - h * 0.02f))
                drawCircle(color = Color.White, radius = w * 0.012f, center = Offset(cx - w * 0.15f, cy - h * 0.03f))
                drawCircle(color = Color.White, radius = w * 0.012f, center = Offset(cx + w * 0.13f, cy - h * 0.03f))

                // Cheek blush
                drawCircle(color = cheekPink, radius = w * 0.045f, center = Offset(cx - w * 0.26f, cy + h * 0.11f))
                drawCircle(color = cheekPink, radius = w * 0.045f, center = Offset(cx + w * 0.26f, cy + h * 0.11f))
            }
        }

        // 5. Cute nose (small triangle shape pointing down)
        val nosePath = Path().apply {
            moveTo(cx - w * 0.035f, cy + h * 0.08f)
            lineTo(cx + w * 0.035f, cy + h * 0.08f)
            lineTo(cx, cy + h * 0.12f)
            close()
        }
        drawPath(nosePath, color = pandaBlack)

        // 6. Smile/Mouth (W-shape curved lines)
        if (expression != "sad") {
            val smilePath = Path().apply {
                moveTo(cx - w * 0.05f, cy + h * 0.16f)
                quadraticBezierTo(cx - w * 0.025f, cy + h * 0.21f, cx, cy + h * 0.17f)
                quadraticBezierTo(cx + w * 0.025f, cy + h * 0.21f, cx + w * 0.05f, cy + h * 0.16f)
            }
            drawPath(smilePath, color = pandaBlack, style = Stroke(width = 3.5f))
        } else {
            // Sad arc mouth
            val sadMouth = Path().apply {
                moveTo(cx - w * 0.04f, cy + h * 0.20f)
                quadraticBezierTo(cx, cy + h * 0.16f, cx + w * 0.04f, cy + h * 0.20f)
            }
            drawPath(sadMouth, color = pandaBlack, style = Stroke(width = 3.5f))
        }
    }
}
