package org.asr.studio.ui.diagnostics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.asr.studio.data.ImuData
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.*

/**
 * High-fidelity 3D Viewport with:
 * - Fusion 360-style ViewCube controls (TOP, FRONT, RIGHT, ISO).
 * - Fixed World Reference Planes with solid positive axes and DOTTED negative axes.
 * - Solid positive device arrows (+X Right, +Y Top/Notch, +Z Screen) and DOTTED negative device axes (-X, -Y, -Z).
 * - Mouse drag to orbit, mouse wheel scroll / buttons to zoom.
 */
@Composable
fun ThreeJsViewport(
    imu: ImuData?,
    modifier: Modifier = Modifier,
    onResetOrbit: (() -> Unit)? = null
) {
    var targetTheta by remember { mutableStateOf(0.7f) }
    var targetPhi by remember { mutableStateOf(1.1f) }
    var targetRadius by remember { mutableStateOf(3.4f) }

    val orbitTheta by animateFloatAsState(targetValue = targetTheta, animationSpec = tween(durationMillis = 250))
    val orbitPhi by animateFloatAsState(targetValue = targetPhi, animationSpec = tween(durationMillis = 250))
    val orbitRadius by animateFloatAsState(targetValue = targetRadius, animationSpec = tween(durationMillis = 150))

    val dashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }

    Box(modifier = modifier.background(Color(0xFF07080B), RoundedCornerShape(9.dp))) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        targetTheta -= dragAmount.x * 0.008f
                        targetPhi = (targetPhi - dragAmount.y * 0.008f).coerceIn(0.05f, 3.10f)
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (delta != 0f) {
                                    targetRadius = (targetRadius + delta * 0.28f).coerceIn(1.5f, 7.5f)
                                }
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0 || h <= 0) return@Canvas

            // ── Camera in Spherical Orbit (Three.js convention: Y is Up) ────────
            val camX = orbitRadius * sin(orbitPhi) * cos(orbitTheta)
            val camY = orbitRadius * cos(orbitPhi)
            val camZ = orbitRadius * sin(orbitPhi) * sin(orbitTheta)

            // Perspective Projection (FOV 40°) + View Matrix
            val projMat = Matrix4f().perspective(Math.toRadians(40.0).toFloat(), w / h, 0.1f, 50f)
            val viewMat = Matrix4f().lookAt(camX, camY, camZ, 0f, 0f, 0f, 0f, 1f, 0f)
            val vpMat = Matrix4f(projMat).mul(viewMat)

            val clipVec = Vector4f()
            fun project(worldX: Float, worldY: Float, worldZ: Float): Offset? {
                vpMat.transform(Vector4f(worldX, worldY, worldZ, 1f), clipVec)
                if (clipVec.w <= 0.05f) return null
                val ndcX = clipVec.x / clipVec.w
                val ndcY = clipVec.y / clipVec.w
                val sx = (ndcX * 0.5f + 0.5f) * w
                val sy = (1f - (ndcY * 0.5f + 0.5f)) * h
                return Offset(sx, sy)
            }

            // ── 1. Fixed Horizontal Ground Grid (Table Plane at Y = -1.4) ───────
            val gridY = -1.4f
            val gridExtent = 3.0f
            val gridDivs = 12
            val gridStep = (gridExtent * 2f) / gridDivs
            val gridColor = Color(0xFF161A24)

            for (i in 0..gridDivs) {
                val pos = -gridExtent + i * gridStep
                val isCenter = (i == gridDivs / 2)
                if (!isCenter) {
                    val p1z = project(pos, gridY, -gridExtent)
                    val p2z = project(pos, gridY, gridExtent)
                    if (p1z != null && p2z != null) drawLine(gridColor, p1z, p2z, strokeWidth = 1f)

                    val p1x = project(-gridExtent, gridY, pos)
                    val p2x = project(gridExtent, gridY, pos)
                    if (p1x != null && p2x != null) drawLine(gridColor, p1x, p2x, strokeWidth = 1f)
                }
            }

            // ── 2. Fixed World Reference Axes (Solid Positive, Dotted Negative) ──
            val originFloor = project(0f, gridY, 0f)

            // World East (+X Solid Red, -X Dotted Red)
            val eastEnd = project(gridExtent * 0.85f, gridY, 0f)
            val westEnd = project(-gridExtent * 0.85f, gridY, 0f)
            if (originFloor != null && eastEnd != null) {
                drawLine(Color(0xFFF87171), originFloor, eastEnd, strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
            if (originFloor != null && westEnd != null) {
                drawLine(Color(0xFFF87171).copy(alpha = 0.55f), originFloor, westEnd, strokeWidth = 1.5f, pathEffect = dashedEffect)
            }

            // World North (+Y ENU / -Z 3D Solid Green, -Y ENU / +Z 3D Dotted Green)
            val northEnd = project(0f, gridY, -gridExtent * 0.85f)
            val southEnd = project(0f, gridY, gridExtent * 0.85f)
            if (originFloor != null && northEnd != null) {
                drawLine(Color(0xFF4ADE80), originFloor, northEnd, strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
            if (originFloor != null && southEnd != null) {
                drawLine(Color(0xFF4ADE80).copy(alpha = 0.55f), originFloor, southEnd, strokeWidth = 1.5f, pathEffect = dashedEffect)
            }

            // World Up (+Z ENU / +Y 3D Solid Blue, -Z ENU / -Y 3D Dotted Blue)
            val upEnd = project(0f, gridY + 2.0f, 0f)
            val downEnd = project(0f, gridY - 0.8f, 0f)
            if (originFloor != null && upEnd != null) {
                drawLine(Color(0xFF60A5FA), originFloor, upEnd, strokeWidth = 2.0f, cap = StrokeCap.Round)
            }
            if (originFloor != null && downEnd != null) {
                drawLine(Color(0xFF60A5FA).copy(alpha = 0.45f), originFloor, downEnd, strokeWidth = 1.2f, pathEffect = dashedEffect)
            }

            // ── 3. Device Quaternion Transformation ──────────────────────────────
            val worldAlignQ = Quaternionf().rotateX(Math.toRadians(-90.0).toFloat())
            val androidQ = Quaternionf(
                (imu?.rotX ?: 0f),
                (imu?.rotY ?: 0f),
                (imu?.rotZ ?: 0f),
                imu?.rotW ?: 1f
            )
            val effectiveQ = Quaternionf(worldAlignQ).mul(androidQ)
            val modelMat = Matrix4f().rotation(effectiveQ)
            val mvpMat = Matrix4f(vpMat).mul(modelMat)

            fun projectModel(mx: Float, my: Float, mz: Float): Offset? {
                mvpMat.transform(Vector4f(mx, my, mz, 1f), clipVec)
                if (clipVec.w <= 0.05f) return null
                val ndcX = clipVec.x / clipVec.w
                val ndcY = clipVec.y / clipVec.w
                val sx = (ndcX * 0.5f + 0.5f) * w
                val sy = (1f - (ndcY * 0.5f + 0.5f)) * h
                return Offset(sx, sy)
            }

            // ── 4. Phone Body Dimensions (0.72w, 1.42h, 0.09d) ───────────────────
            val hx = 0.72f / 2f
            val hy = 1.42f / 2f
            val hz = 0.09f / 2f

            class BoxFace(val indices: IntArray, val normal: Vector3f, val color: Color, val borderColor: Color)

            val boxVerts = listOf(
                Vector3f(-hx, -hy, -hz), // 0 Back bottom-left
                Vector3f( hx, -hy, -hz), // 1 Back bottom-right
                Vector3f( hx,  hy, -hz), // 2 Back top-right
                Vector3f(-hx,  hy, -hz), // 3 Back top-left
                Vector3f(-hx, -hy,  hz), // 4 Front bottom-left
                Vector3f( hx, -hy,  hz), // 5 Front bottom-right
                Vector3f( hx,  hy,  hz), // 6 Front top-right
                Vector3f(-hx,  hy,  hz)  // 7 Front top-left
            )

            val lightDir = Vector3f(3f, 4f, 5f).normalize()
            val faces = listOf(
                BoxFace(intArrayOf(4, 5, 6, 7), Vector3f(0f, 0f, 1f),  Color(0xFF131722), Color(0xFF2A3144)), // Front (+Z)
                BoxFace(intArrayOf(1, 0, 3, 2), Vector3f(0f, 0f, -1f), Color(0xFF1E2330), Color(0xFF2E374D)), // Back (-Z)
                BoxFace(intArrayOf(5, 1, 2, 6), Vector3f(1f, 0f, 0f),  Color(0xFF171B26), Color(0xFF262E40)), // Right (+X)
                BoxFace(intArrayOf(0, 4, 7, 3), Vector3f(-1f, 0f, 0f), Color(0xFF171B26), Color(0xFF262E40)), // Left (-X)
                BoxFace(intArrayOf(7, 6, 2, 3), Vector3f(0f, 1f, 0f),  Color(0xFF202636), Color(0xFF333E57)), // Top (+Y)
                BoxFace(intArrayOf(0, 1, 5, 4), Vector3f(0f, -1f, 0f), Color(0xFF10131B), Color(0xFF1C2230))  // Bottom (-Y)
            )

            val worldNormal = Vector3f()
            val transformedVerts = boxVerts.map { v ->
                val tv = Vector3f()
                modelMat.transformPosition(v, tv)
                tv
            }

            // Draw shaded body faces with back-face culling
            for (face in faces) {
                modelMat.transformDirection(Vector3f(face.normal), worldNormal)
                val toCam = Vector3f(camX, camY, camZ).sub(transformedVerts[face.indices[0]]).normalize()
                val dotCam = worldNormal.dot(toCam)
                if (dotCam <= 0f) continue

                val dotLight = max(0f, worldNormal.dot(lightDir))
                val intensity = (0.45f + 0.55f * dotLight).coerceIn(0.2f, 1f)

                val faceColor = Color(
                    red = (face.color.red * intensity).coerceIn(0f, 1f),
                    green = (face.color.green * intensity).coerceIn(0f, 1f),
                    blue = (face.color.blue * intensity).coerceIn(0f, 1f),
                    alpha = 1f
                )

                val p0 = project(transformedVerts[face.indices[0]].x, transformedVerts[face.indices[0]].y, transformedVerts[face.indices[0]].z) ?: continue
                val p1 = project(transformedVerts[face.indices[1]].x, transformedVerts[face.indices[1]].y, transformedVerts[face.indices[1]].z) ?: continue
                val p2 = project(transformedVerts[face.indices[2]].x, transformedVerts[face.indices[2]].y, transformedVerts[face.indices[2]].z) ?: continue
                val p3 = project(transformedVerts[face.indices[3]].x, transformedVerts[face.indices[3]].y, transformedVerts[face.indices[3]].z) ?: continue

                val path = Path().apply {
                    moveTo(p0.x, p0.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
                }
                drawPath(path, faceColor)
                drawPath(path, face.borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f))
            }

            // ── 5. FRONT FACE: Glowing Teal OLED Screen (+Z) ─────────────────────
            val sx = 0.60f / 2f
            val sy = 1.24f / 2f
            val sz = hz + 0.001f

            val sp0 = projectModel(-sx, -sy, sz)
            val sp1 = projectModel( sx, -sy, sz)
            val sp2 = projectModel( sx,  sy, sz)
            val sp3 = projectModel(-sx,  sy, sz)

            if (sp0 != null && sp1 != null && sp2 != null && sp3 != null) {
                val cross = (sp1.x - sp0.x) * (sp2.y - sp0.y) - (sp1.y - sp0.y) * (sp2.x - sp0.x)
                if (cross > 0f) {
                    val screenPath = Path().apply {
                        moveTo(sp0.x, sp0.y); lineTo(sp1.x, sp1.y); lineTo(sp2.x, sp2.y); lineTo(sp3.x, sp3.y); close()
                    }
                    drawPath(screenPath, Color(0xFF0D3A34))
                    drawPath(screenPath, Color(0xFF5EEAD4).copy(alpha = 0.85f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.0f))

                    // Front Punch-Hole Selfie Camera
                    val camPoint = projectModel(0f, 0.54f, sz + 0.001f)
                    if (camPoint != null) {
                        drawCircle(Color(0xFF071412), 4.5f, camPoint)
                        drawCircle(Color(0xFF5EEAD4), 2.0f, camPoint)
                    }
                }
            }

            // ── 6. BACK FACE: 3-Lens Camera Bumper (-Z) ─────────────────────────
            val bz = -hz - 0.002f
            val bp0 = projectModel(-0.28f, 0.36f, bz)
            val bp1 = projectModel(-0.12f, 0.36f, bz)
            val bp2 = projectModel(-0.12f, 0.64f, bz)
            val bp3 = projectModel(-0.28f, 0.64f, bz)

            if (bp0 != null && bp1 != null && bp2 != null && bp3 != null) {
                val backCross = (bp1.x - bp0.x) * (bp2.y - bp0.y) - (bp1.y - bp0.y) * (bp2.x - bp0.x)
                if (backCross > 0f) {
                    val bumpPath = Path().apply {
                        moveTo(bp0.x, bp0.y); lineTo(bp1.x, bp1.y); lineTo(bp2.x, bp2.y); lineTo(bp3.x, bp3.y); close()
                    }
                    drawPath(bumpPath, Color(0xFF0F131D))
                    drawPath(bumpPath, Color(0xFF38BDF8).copy(alpha = 0.65f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f))

                    val lensY = listOf(0.42f, 0.50f, 0.58f)
                    for (ly in lensY) {
                        val lp = projectModel(-0.20f, ly, bz - 0.005f)
                        if (lp != null) {
                            drawCircle(Color(0xFF030712), 5.5f, lp)
                            drawCircle(Color(0xFF60A5FA), 3.0f, lp)
                            drawCircle(Color(0xFF93C5FD), 1.0f, lp)
                        }
                    }
                    val flashP = projectModel(-0.16f, 0.46f, bz - 0.003f)
                    if (flashP != null) drawCircle(Color(0xFFFFD166), 2.0f, flashP)
                }
            }

            // ── 7. BOTTOM EDGE: USB-C Charging Port (-Y) ────────────────────────
            val up0 = projectModel(-0.08f, -hy, 0f)
            val up1 = projectModel( 0.08f, -hy, 0f)
            if (up0 != null && up1 != null) {
                drawLine(Color(0xFF05070A), up0, up1, strokeWidth = 3.5f, cap = StrokeCap.Round)
            }

            // ── 8. 3D Device Body Axes (Solid Positive + Cone, Dotted Negative) ─
            val origin2D = projectModel(0f, 0f, 0f)

            fun drawPositiveArrow(dir: Vector3f, len: Float, color: Color) {
                if (origin2D == null) return
                val tip3D = Vector3f(dir).normalize().mul(len)
                val tip2D = projectModel(tip3D.x, tip3D.y, tip3D.z) ?: return

                val coneBase3D = Vector3f(dir).normalize().mul(len * 0.80f)
                val coneBase2D = projectModel(coneBase3D.x, coneBase3D.y, coneBase3D.z) ?: return

                drawLine(color, origin2D, coneBase2D, strokeWidth = 2.5f)

                val dx = tip2D.x - coneBase2D.x
                val dy = tip2D.y - coneBase2D.y
                val perpLen = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
                val coneWidth = 7.5f
                val px = -dy / perpLen * coneWidth
                val py =  dx / perpLen * coneWidth

                val conePath = Path().apply {
                    moveTo(tip2D.x, tip2D.y)
                    lineTo(coneBase2D.x + px, coneBase2D.y + py)
                    lineTo(coneBase2D.x - px, coneBase2D.y - py)
                    close()
                }
                drawPath(conePath, color)
            }

            fun drawNegativeAxis(dir: Vector3f, len: Float, color: Color) {
                if (origin2D == null) return
                val tip3D = Vector3f(dir).normalize().mul(len)
                val tip2D = projectModel(tip3D.x, tip3D.y, tip3D.z) ?: return
                drawLine(color.copy(alpha = 0.50f), origin2D, tip2D, strokeWidth = 1.5f, pathEffect = dashedEffect)
            }

            // +X Red (Right Edge), -X Dotted Red (Left Edge)
            drawPositiveArrow(Vector3f(1f, 0f, 0f), 1.05f, Color(0xFFF87171))
            drawNegativeAxis(Vector3f(-1f, 0f, 0f), 0.70f, Color(0xFFF87171))

            // +Y Green (Top Edge / Notch / LiDAR), -Y Dotted Green (Bottom Edge)
            drawPositiveArrow(Vector3f(0f, 1f, 0f), 1.05f, Color(0xFF4ADE80))
            drawNegativeAxis(Vector3f(0f, -1f, 0f), 0.70f, Color(0xFF4ADE80))

            // +Z Blue (Front Screen Outward), -Z Dotted Blue (Back Face Outward)
            drawPositiveArrow(Vector3f(0f, 0f, 1f), 1.05f, Color(0xFF60A5FA))
            drawNegativeAxis(Vector3f(0f, 0f, -1f), 0.70f, Color(0xFF60A5FA))

            // Gravity vector arrow (Pink #E879B9)
            if (imu != null) {
                val gx = imu.gravityX
                val gy = imu.gravityY
                val gz = imu.gravityZ
                val gMag = sqrt(gx * gx + gy * gy + gz * gz)
                if (gMag > 0.1f) {
                    drawPositiveArrow(Vector3f(gx / gMag, gy / gMag, gz / gMag), 1.25f, Color(0xFFE879B9))
                }
            }
        }

        // ── Fusion 360 Style ViewCube / Plane Navigation Toolbar (Top-Right) ─
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // View Plane Preset Pill Bar
            Row(
                modifier = Modifier
                    .background(Color(0xFF141824).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ViewPresetBtn("TOP") {
                    targetTheta = 1.5708f // Camera at South looking North: North is UP, East is RIGHT
                    targetPhi = 0.05f
                }
                ViewPresetBtn("FRONT") {
                    targetTheta = 1.5708f // Looking from South directly at the front
                    targetPhi = 1.5708f
                }
                ViewPresetBtn("RIGHT") {
                    targetTheta = 0f     // Looking from East at the right profile
                    targetPhi = 1.5708f
                }
                ViewPresetBtn("ISO") {
                    targetTheta = 1.15f
                    targetPhi = 1.10f
                    targetRadius = 3.4f
                }
            }

            // Zoom In / Out Toolbar (+ / −)
            Row(
                modifier = Modifier
                    .background(Color(0xFF141824).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ViewPresetBtn("+") {
                    targetRadius = (targetRadius - 0.6f).coerceIn(1.5f, 7.5f)
                }
                ViewPresetBtn("−") {
                    targetRadius = (targetRadius + 0.6f).coerceIn(1.5f, 7.5f)
                }
            }
        }

        // Bottom Ground Plane Legend
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp, 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "WORLD (Solid + / Dotted −):",
                color = Color(0xFF8B90A3),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text("X East", color = Color(0xFFF87171), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("Y North", color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("Z Up", color = Color(0xFF60A5FA), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ViewPresetBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFFE2E8F0),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
