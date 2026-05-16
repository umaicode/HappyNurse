// IV 백 + drip chamber + 똑똑 떨어지는 방울 애니메이션 (세련된 톤)
// - 잔량은 백 안의 액체 높이로 표현 (그라데이션 + 메니스커스)
// - 방울: 표면 장력으로 매달림 → 분리 → 자유낙하 → chamber 바닥 squash
// - chamber 우측에 gtt/min 라벨을 함께 렌더 (Canvas 내부 nativeCanvas 로 텍스트)
package com.happynurse.presentation.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

@Composable
fun IvDripAnimation(
    fillRatio: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6EB296),
    animate: Boolean = true,
    cycleMillis: Int? = null,
    gttPerMin: Int? = null,
) {
    // 한 사이클(=한 방울 떨어지는 주기) 계산:
    // - cycleMillis 가 주어지면 우선 사용
    // - 없으면 gtt/min 기준 60/gtt 초 (예: 20gtt → 3.0s, 60gtt → 1.0s)
    // - 둘 다 없으면 기본 1500ms
    val effectiveCycleMs = cycleMillis
        ?: gttPerMin?.takeIf { it > 0 }?.let { (60_000.0 / it).toInt().coerceIn(400, 4000) }
        ?: 1500
    val transition = rememberInfiniteTransition(label = "iv-drip")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(effectiveCycleMs, easing = LinearEasing),
        ),
        label = "iv-drip-progress",
    )
    // 출렁임(메니스커스) 전용 — 점적 사이클과 독립된 느린 사인 위상
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
        ),
        label = "iv-wave-phase",
    )
    val t = if (animate) progress else 0f
    val wp = if (animate) wavePhase else 0f
    val ratio = fillRatio.coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 백을 컨테이너 폭의 58% 차지 → 가로 중앙 정렬
        val bagW = w * 0.58f
        val bagX = (w - bagW) / 2f
        // 위쪽에 hanger/port 가 들어갈 여유 공간을 살짝 남김
        val bagTop = h * (12f / 130f)
        val bagBottom = h * (90f / 130f)
        val bagH = bagBottom - bagTop
        val cx = bagX + bagW / 2f

        // chamber: 백 바로 아래 중앙
        val chW = w * 0.18f
        val chTop = bagBottom + h * 0.05f
        val chBot = chTop + h * 0.16f
        val chCx = cx
        val chLeft = chCx - chW / 2f

        val cornerR = bagW * 0.16f

        // ── 상단: 미니멀 hanger 고리 + 행잉 포트 ─────────
        // 행잉 고리 (얇은 원호 — 백 위 고정점)
        val hangerCx = cx
        val hangerCy = bagTop - h * 0.058f
        val hangerR = bagW * 0.06f
        drawCircle(
            color = Color(0xFFCBD5E1),
            radius = hangerR,
            center = Offset(hangerCx, hangerCy),
            style = Stroke(width = 1.1f),
        )
        // 고리에서 포트로 내려가는 짧은 끈
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(hangerCx, hangerCy + hangerR),
            end = Offset(hangerCx, bagTop - 0.5f),
            strokeWidth = 1.0f,
        )
        // 행잉 포트 (백 상단 중앙에 살짝 박힌 작은 직사각형)
        val portW = bagW * 0.22f
        val portH = h * 0.022f
        drawRoundRect(
            color = Color(0xFFD8DEE6),
            topLeft = Offset(cx - portW / 2f, bagTop - portH * 0.55f),
            size = Size(portW, portH),
            cornerRadius = CornerRadius(portH * 0.45f, portH * 0.45f),
        )

        // ── 백 본체: 유리 그라데이션 + 가장자리 테두리 ────
        val glassBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFCFEFF),
                Color(0xFFF1F5F9),
                Color(0xFFE7EEF5),
            ),
            start = Offset(bagX, bagTop),
            end = Offset(bagX + bagW, bagBottom),
        )
        drawRoundRect(
            brush = glassBrush,
            topLeft = Offset(bagX, bagTop),
            size = Size(bagW, bagH),
            cornerRadius = CornerRadius(cornerR, cornerR),
        )
        drawRoundRect(
            color = Color(0xFFD8DEE6),
            topLeft = Offset(bagX, bagTop),
            size = Size(bagW, bagH),
            cornerRadius = CornerRadius(cornerR, cornerR),
            style = Stroke(width = 1.1f),
        )

        // ── 백 좌측 하이라이트(유리 반사) ─────────────────
        drawRoundRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(bagX + bagW * 0.08f, bagTop + bagH * 0.06f),
            size = Size(bagW * 0.06f, bagH * 0.78f),
            cornerRadius = CornerRadius(bagW * 0.03f, bagW * 0.03f),
        )

        // ── 액체: vertical gradient + 직선 표면 + 미세 표면 하이라이트 ─
        val fluidH = bagH * ratio
        val fluidY = bagBottom - fluidH
        if (fluidH > 1f) {
            val inset = 1.2f
            val bottomCorner = (cornerR - inset).coerceAtLeast(0f)
            val left = bagX + inset
            val right = bagX + bagW - inset
            val top = fluidY
            val bottom = bagBottom - inset * 0.4f

            // 메니스커스(수면 출렁임) — 좌우 끝의 높이가 사인파로 위아래 진동, 중앙은 반대 위상
            val width = right - left
            val waveAmp = (width * 0.02f).coerceAtMost(fluidH * 0.18f)
            val leftWaveDy = kotlin.math.sin(wp.toDouble()).toFloat() * waveAmp
            val rightWaveDy = kotlin.math.sin(wp + Math.PI.toFloat()).toFloat() * waveAmp
            val midDy = kotlin.math.sin(wp + Math.PI.toFloat() / 2f).toFloat() * waveAmp * 0.5f
            val leftTopY = top + leftWaveDy
            val rightTopY = top + rightWaveDy
            val midX = (left + right) / 2f
            val midTopY = top + midDy

            val fluidPath = Path().apply {
                moveTo(left, leftTopY)
                // 좌→중앙→우 를 두 개의 quadratic bezier 로 연결해 부드러운 물결
                quadraticBezierTo(left + width * 0.25f, leftTopY + (midTopY - leftTopY) * 1.4f, midX, midTopY)
                quadraticBezierTo(left + width * 0.75f, midTopY + (rightTopY - midTopY) * 1.4f, right, rightTopY)
                lineTo(right, bottom - bottomCorner)
                quadraticBezierTo(right, bottom, right - bottomCorner, bottom)
                lineTo(left + bottomCorner, bottom)
                quadraticBezierTo(left, bottom, left, bottom - bottomCorner)
                close()
            }

            // (1) 베이스 vertical: 상단 옅음 → 하단 진함 (깊이감)
            val baseBrush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.45f),
                    color.copy(alpha = 0.72f),
                    color.copy(alpha = 0.90f),
                    color.copy(alpha = 0.96f),
                ),
                startY = top,
                endY = bottom,
            )
            drawPath(fluidPath, brush = baseBrush)

            // (2) 좌측 빛 / 우측 그늘 — horizontal 보조 그라데이션
            // 투명한 흰빛이 왼쪽에서 미세하게 들어오고, 오른쪽은 약하게 어두워지는 입체감.
            val sideBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.04f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.04f),
                    Color.Black.copy(alpha = 0.10f),
                ),
                startX = left,
                endX = right,
            )
            drawPath(fluidPath, brush = sideBrush)

            // (3) 표면 광택(specular) — 표면 바로 아래 6~10% 영역에 밝은 띠
            if (fluidH > 6f) {
                val surfaceTop = minOf(leftTopY, rightTopY, midTopY)
                val specTop = surfaceTop
                val specBot = surfaceTop + (bottom - surfaceTop).coerceAtMost(fluidH) * 0.18f
                val specPath = Path().apply {
                    moveTo(left, specTop)
                    lineTo(right, specTop)
                    lineTo(right, specBot)
                    lineTo(left, specBot)
                    close()
                }
                val specBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    startY = specTop,
                    endY = specBot,
                )
                drawPath(specPath, brush = specBrush)
            }

            // (4) 표면 직하 가는 흰 라인 — 수면 반사. 메니스커스 곡선 따라 흔들림.
            if (fluidH > 4f) {
                val phase = kotlin.math.sin(t * 2f * Math.PI).toFloat()
                val shift = phase * (right - left) * 0.05f
                val hLeft = left + (right - left) * 0.20f + shift
                val hRight = right - (right - left) * 0.20f + shift
                // 중앙 부근 메니스커스 높이에 라인을 붙임
                val lineY = midTopY + 1.4f
                drawLine(
                    color = Color.White.copy(alpha = (0.50f + phase * 0.15f).coerceIn(0.30f, 0.75f)),
                    start = Offset(hLeft, lineY),
                    end = Offset(hRight, lineY),
                    strokeWidth = 0.9f,
                )
            }
        }

        // ── connector neck (백→chamber) ───────────────────
        val neckTopHalf = bagW * 0.05f
        val neckBotHalf = chW * 0.32f
        val neckPath = Path().apply {
            moveTo(cx - neckTopHalf, bagBottom)
            lineTo(cx - neckBotHalf, chTop)
            lineTo(cx + neckBotHalf, chTop)
            lineTo(cx + neckTopHalf, bagBottom)
            close()
        }
        drawPath(neckPath, color = Color(0xFFD8DEE6))

        // ── drip chamber: 유리 원기둥 ─────────────────────
        val chamberBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFF4F7FA).copy(alpha = 0.85f),
            ),
            start = Offset(chLeft, chTop),
            end = Offset(chLeft + chW, chTop),
        )
        drawRoundRect(
            brush = chamberBrush,
            topLeft = Offset(chLeft, chTop),
            size = Size(chW, chBot - chTop),
            cornerRadius = CornerRadius(chW * 0.18f, chW * 0.18f),
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(chLeft, chTop),
            size = Size(chW, chBot - chTop),
            cornerRadius = CornerRadius(chW * 0.18f, chW * 0.18f),
            style = Stroke(width = 0.9f),
        )

        // ── 똑똑 떨어지는 방울 ────────────────────────────
        val dropTopY = chTop + (chBot - chTop) * 0.20f
        val dropBotY = chBot - (chBot - chTop) * 0.26f
        val baseR = chW * 0.18f

        if (ratio > 0f) {
            val (cy, rx, ry, alpha) = computeDropFrame(t, dropTopY, dropBotY, baseR)
            if (alpha > 0.01f) {
                drawOval(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(chCx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                )
                // 방울 하이라이트(낙하 중인 방울에만 작게)
                if (alpha > 0.6f && ry > rx * 0.9f) {
                    drawOval(
                        color = Color.White.copy(alpha = alpha * 0.45f),
                        topLeft = Offset(chCx - rx * 0.45f, cy - ry * 0.55f),
                        size = Size(rx * 0.5f, ry * 0.35f),
                    )
                }
            }
        }

        // ── 풀(pool) — 충돌 시 잔물결 ────────────────────
        val poolBaseRx = chW * 0.36f
        val poolBaseRy = (chBot - chTop) * 0.10f
        val poolScale = poolRipple(t)
        val poolRx = poolBaseRx * poolScale
        val poolRy = poolBaseRy * poolScale
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.7f)),
                startY = chBot - poolRy * 1.4f - 2f,
                endY = chBot - 1f,
            ),
            topLeft = Offset(chCx - poolRx, chBot - poolRy * 1.4f - 1f),
            size = Size(poolRx * 2f, poolRy * 2f),
        )

        // ── outflow tube ──────────────────────────────────
        drawLine(
            color = color.copy(alpha = 0.45f),
            start = Offset(chCx, chBot),
            end = Offset(chCx, h - 2f),
            strokeWidth = 2.4f,
        )

        // ── gtt/min 라벨: 수액 백 중앙에 항상 고정 렌더 ─────
        if (gttPerMin != null && gttPerMin > 0) {
            val labelCenterX = cx
            val labelCenterY = (bagTop + bagBottom) / 2f

            val textColor = Color(0xFF606062).toArgb()
            val strokeColor = Color(0xFFC7C7CB).copy(alpha = 0.50f).toArgb()
            val numberStrokePaint = Paint().apply {
                isAntiAlias = true
                this.color = strokeColor
                textSize = h * 0.23f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
                strokeJoin = Paint.Join.ROUND
            }
            val numberPaint = Paint().apply {
                isAntiAlias = true
                this.color = textColor
                textSize = h * 0.23f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val unitStrokePaint = Paint().apply {
                isAntiAlias = true
                this.color = strokeColor
                textSize = h * 0.085f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                style = Paint.Style.STROKE
                strokeWidth = 2.0f
                strokeJoin = Paint.Join.ROUND
            }
            val unitPaint = Paint().apply {
                isAntiAlias = true
                this.color = textColor
                textSize = h * 0.085f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val numberStr = gttPerMin.toString()
            val numberFm = numberPaint.fontMetrics
            val numberH = numberFm.descent - numberFm.ascent
            val unitFm = unitPaint.fontMetrics
            val unitH = unitFm.descent - unitFm.ascent

            val totalH = numberH + unitH * 0.8f
            val numberBaselineY = labelCenterY - totalH / 2f - numberFm.ascent
            val unitBaselineY = numberBaselineY + numberFm.descent + unitH * 0.15f - unitFm.ascent

            drawContext.canvas.nativeCanvas.apply {
                drawText(numberStr, labelCenterX, numberBaselineY, numberStrokePaint)
                drawText(numberStr, labelCenterX, numberBaselineY, numberPaint)
                drawText("gtt/min", labelCenterX, unitBaselineY, unitStrokePaint)
                drawText("gtt/min", labelCenterX, unitBaselineY, unitPaint)
            }
        }
    }
}

/** keyTimes piecewise easing — JSX IVBag 의 keySplines 단순화 */
private fun computeDropFrame(
    t: Float,
    topY: Float,
    botY: Float,
    baseR: Float,
): FloatArray {
    val total = botY - topY
    return when {
        // 0 ~ 0.30 : 표면 장력으로 형성 (아주 천천히 자라남)
        t < 0.30f -> {
            val k = t / 0.30f
            val grow = easeOutCubic(k)
            floatArrayOf(
                topY + 0.06f * total,
                baseR * (0.55f + 0.45f * grow),
                baseR * (0.55f + 0.55f * grow),
                (k * 4.5f).coerceAtMost(1f),
            )
        }
        // 0.30 ~ 0.42 : 분리 직전, 길게 늘어짐
        t < 0.42f -> {
            val k = (t - 0.30f) / 0.12f
            floatArrayOf(
                topY + 0.06f * total + 0.06f * total * k,
                baseR * (1.0f - 0.06f * k),
                baseR * (1.10f + 0.32f * k),
                1f,
            )
        }
        // 0.42 ~ 0.88 : 자유낙하 (ease-in 가속)
        t < 0.88f -> {
            val k = (t - 0.42f) / 0.46f
            val fall = easeInQuad(k)
            floatArrayOf(
                topY + (0.12f + 0.82f * fall) * total,
                baseR * (1.0f - 0.18f * fall),
                baseR * (1.42f + 0.06f * fall),
                1f,
            )
        }
        // 0.88 ~ 0.95 : 바닥 충돌 squash
        t < 0.95f -> {
            val k = (t - 0.88f) / 0.07f
            val ease = 1f - (1f - k) * (1f - k)  // easeOutQuad
            floatArrayOf(
                botY - baseR * 0.2f,
                baseR * (0.85f + 0.55f * ease),
                baseR * (1.45f - 1.10f * ease),
                1f - 0.55f * ease,
            )
        }
        // 0.95 ~ 1 : 페이드아웃
        else -> {
            val k = (t - 0.95f) / 0.05f
            floatArrayOf(
                botY - baseR * 0.2f,
                baseR * 1.4f,
                baseR * 0.35f,
                (0.45f * (1f - k)).coerceAtLeast(0f),
            )
        }
    }
}

/** 풀 잔물결: 평상시 1.0 → 충돌 직후 부드럽게 부풀고 복귀 */
private fun poolRipple(t: Float): Float = when {
    t < 0.88f -> 1f
    t < 0.95f -> {
        val k = (t - 0.88f) / 0.07f
        1f + easeOutCubic(k) * 0.22f
    }
    t < 1.0f -> {
        val k = (t - 0.95f) / 0.05f
        1.22f - easeOutCubic(k) * 0.20f
    }
    else -> 1.02f
}

private fun easeOutCubic(x: Float): Float {
    val inv = 1f - x
    return 1f - inv * inv * inv
}

private fun easeInQuad(x: Float): Float = x * x
