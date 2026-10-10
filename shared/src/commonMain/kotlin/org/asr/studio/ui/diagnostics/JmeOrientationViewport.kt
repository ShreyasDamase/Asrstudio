package org.asr.studio.ui.diagnostics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CancellationException
import org.asr.studio.data.ImuData
// Pin this in build.gradle.kts so the import is deterministic:
//   compose.resources { packageOfResClass = "org.asr.studio.generated.resources" }
import org.asr.studio.generated.resources.Res
import org.asr.studio.ui.theme.StudioColors
import org.asr.studio.ui.theme.StudioRadii
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val Background = Color(0.075f, 0.085f, 0.095f)
private const val MODEL_PATH = "files/android_phone_ros.obj" // src/commonMain/composeResources/files/

private fun brighten(color: Color, amount: Float = 0.42f) = Color(
    red = color.red + (1f - color.red) * amount,
    green = color.green + (1f - color.green) * amount,
    blue = color.blue + (1f - color.blue) * amount,
    alpha = color.alpha,
)

/**
 * Pure-Compose 3D orientation viewport (commonMain: Desktop, Android, Web).
 * No JAWT, SwingPanel, GL context or JVM-only API. Loads the OBJ through Compose Resources,
 * rotates it by the Android rotation-vector quaternion (device -> ENU world, Z up),
 * and draws flat-shaded triangles with back-face culling + painter's sort.
 * Input: drag = orbit, pinch / wheel = zoom.
 */
@Composable
fun JmeOrientationViewport(imu: ImuData?, modifier: Modifier = Modifier) {
    var mesh by remember { mutableStateOf<PhoneMesh?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val latestImu by rememberUpdatedState(imu)
    var pose by remember { mutableStateOf(Quat.IDENTITY) }
    var azimuth by remember { mutableStateOf(0.6f) }
    var elevation by remember { mutableStateOf(0.45f) }
    var distance by remember { mutableStateOf(3.2f) }
    var orientationAvailable by remember { mutableStateOf(false) }
    var orientationSource by remember { mutableStateOf("unavailable") }
    var selectedView by remember { mutableStateOf("Reset view") }

    LaunchedEffect(Unit) {
        try {
            mesh = parsePhoneObj(Res.readBytes(MODEL_PATH).decodeToString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            loadError = e.message ?: e.toString()
        }
    }

    // Same exponential smoothing the jME version used (rate 12/s).
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1e9f
            last = now
            val sample = latestImu
            val quaternion = sample?.let { Quat(it.rotX, it.rotY, it.rotZ, it.rotW).normalizedOrNull() }
            val target = quaternion ?: sample?.let { Quat.fromGravity(it.gravityX, it.gravityY, it.gravityZ) }
            orientationAvailable = target != null
            orientationSource = when {
                quaternion != null -> "Rotation vector"
                target != null -> "Gravity tilt · heading unavailable"
                else -> "Waiting for IMU"
            }
            if (target != null) {
                pose = pose.nlerp(target, (1f - exp(-if (quaternion != null) 12f * dt else 24f * dt)).coerceIn(0f, 1f))
            }
        }
    }

    Column(modifier.background(Background)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ViewPresetButton("Top", selectedView == "Top") {
                    selectedView = "Top"; azimuth = 0f; elevation = 1.35f; distance = 3.6f
                }
                ViewPresetButton("Front", selectedView == "Front") {
                    selectedView = "Front"; azimuth = 0f; elevation = 0.08f; distance = 3.2f
                }
                ViewPresetButton("Left", selectedView == "Left") {
                    selectedView = "Left"; azimuth = -1.5707964f; elevation = 0.08f; distance = 3.2f
                }
                ViewPresetButton("Right", selectedView == "Right") {
                    selectedView = "Right"; azimuth = 1.5707964f; elevation = 0.08f; distance = 3.2f
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewPresetButton("Bottom", selectedView == "Bottom") {
                    selectedView = "Bottom"; azimuth = 0f; elevation = -1.35f; distance = 3.6f
                }
                ViewPresetButton("Back", selectedView == "Back") {
                    selectedView = "Back"; azimuth = 3.1415927f; elevation = 0.08f; distance = 3.2f
                }
                ViewPresetButton("Reset view", selectedView == "Reset view") {
                    selectedView = "Reset view"; azimuth = 0.6f; elevation = 0.45f; distance = 3.2f
                }
                Spacer(Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = orientationSource,
                    color = if (orientationAvailable) StudioColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        val m = mesh
        when {
            loadError != null -> Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("Could not load $MODEL_PATH: $loadError", color = Color.White.copy(alpha = 0.6f))
            }
            m == null -> Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("Loading model…", color = Color.White.copy(alpha = 0.6f))
            }
            else -> BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // One pointer = orbit (mouse drag / one finger); two pointers = pinch zoom.
                            detectTransformGestures { _, pan, zoom, _ ->
                                azimuth -= pan.x * 0.01f
                                elevation = (elevation + pan.y * 0.01f).coerceIn(-1.3f, 1.3f)
                                if (zoom != 1f) distance = (distance / zoom).coerceIn(2.2f, 7f)
                                selectedView = ""
                            }
                        }
                        .pointerInput(Unit) {
                            // Mouse wheel (desktop / web).
                            awaitPointerEventScope {
                                while (true) {
                                    val e = awaitPointerEvent()
                                    if (e.type == PointerEventType.Scroll) {
                                        val dy = e.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                        distance = (distance + dy * 0.25f).coerceIn(2.2f, 7f)
                                        selectedView = ""
                                        e.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                ) {
                    renderScene(m, pose, azimuth, elevation, distance)
                }

                val floorZ = -0.9f
                val labels = listOf(
                    ViewAxisLabel("E", 1.72f, 0f, floorZ, StudioColors.axisX),
                    ViewAxisLabel("W", -1.72f, 0f, floorZ, brighten(StudioColors.axisX)),
                    ViewAxisLabel("N", 0f, 1.72f, floorZ, StudioColors.success),
                    ViewAxisLabel("S", 0f, -1.72f, floorZ, brighten(StudioColors.success)),
                    ViewAxisLabel("Z ↑", 0f, 0f, 1.55f, StudioColors.axisZ),
                )
                labels.forEach { label ->
                    val position = projectWorldPoint(widthPx, heightPx, azimuth, elevation, distance, label.x, label.y, label.z)
                    if (position != null) {
                        Text(
                            text = label.text,
                            color = label.color,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.offset {
                                val x = clampOverlayPosition(position.x, 4.dp.toPx(), widthPx - 56.dp.toPx())
                                val y = clampOverlayPosition(position.y, 4.dp.toPx(), heightPx - 24.dp.toPx())
                                IntOffset((x - 4.dp.toPx()).roundToInt(), (y - 4.dp.toPx()).roundToInt())
                            }.background(Background.copy(alpha = 0.92f)).padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }

                // Device-frame axes share the IMU quaternion used by the mesh. Place labels at
                // their transformed tips so the triad reads as attached to the phone, not the floor.
                val deviceAxes = listOf(
                    DeviceAxisLabel("X", 0.82f, StudioColors.axisX),
                    DeviceAxisLabel("Y", 1.05f, StudioColors.success),
                    DeviceAxisLabel("Z", 0.62f, StudioColors.axisZ),
                )
                deviceAxes.forEach { axis ->
                    val point = rotateDeviceVector(pose, axis.x * axis.length, axis.y * axis.length, axis.z * axis.length)
                    val position = projectWorldPoint(widthPx, heightPx, azimuth, elevation, distance, point.x, point.y, point.z)
                    if (position != null) {
                        Text(
                            text = axis.name,
                            color = axis.color,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.offset {
                                val x = clampOverlayPosition(position.x + 5.dp.toPx(), 4.dp.toPx(), widthPx - 30.dp.toPx())
                                val y = clampOverlayPosition(position.y - 8.dp.toPx(), 4.dp.toPx(), heightPx - 24.dp.toPx())
                                IntOffset(x.roundToInt(), y.roundToInt())
                            }.background(Background.copy(alpha = 0.96f)).padding(horizontal = 3.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewPresetButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.height(30.dp),
            shape = StudioRadii.controlShape,
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
            colors = colors,
        ) { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.height(30.dp),
            shape = StudioRadii.controlShape,
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        ) { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) }
    }
}

private fun projectWorldPoint(
    width: Float,
    height: Float,
    azimuth: Float,
    elevation: Float,
    distance: Float,
    x: Float,
    y: Float,
    z: Float,
): Offset? {
    val ce = cos(elevation)
    val camX = distance * sin(azimuth) * ce
    val camY = -distance * cos(azimuth) * ce
    val camZ = distance * sin(elevation)
    val fx = -camX / distance
    val fy = -camY / distance
    val fz = -camZ / distance
    var rx = fy
    var ry = -fx
    val rightLength = sqrt(rx * rx + ry * ry)
    if (rightLength < 1e-5f) return null
    rx /= rightLength
    ry /= rightLength
    val ux = ry * fz
    val uy = -rx * fz
    val uz = rx * fy - ry * fx
    val dx = x - camX
    val dy = y - camY
    val dz = z - camZ
    val depth = dx * fx + dy * fy + dz * fz
    if (depth < 0.1f) return null
    val scale = height * 1.2f / depth
    val projectedX = width / 2f + (dx * rx + dy * ry) * scale
    val projectedY = height / 2f - (dx * ux + dy * uy + dz * uz) * scale
    return if (projectedX.isFinite() && projectedY.isFinite()) Offset(projectedX, projectedY) else null
}

// ---------------------------------------------------------------------------------------------
// Math
// ---------------------------------------------------------------------------------------------

private data class Quat(val x: Float, val y: Float, val z: Float, val w: Float) {
    fun normalizedOrNull(): Quat? {
        val n = sqrt(x * x + y * y + z * z + w * w)
        return if (x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite() && n > 1e-4f) {
            Quat(x / n, y / n, z / n, w / n)
        } else null
    }

    fun normalized(): Quat {
        return normalizedOrNull() ?: IDENTITY
    }

    fun nlerp(t: Quat, b: Float): Quat {
        val s = if (x * t.x + y * t.y + z * t.z + w * t.w < 0f) -1f else 1f
        return Quat(
            x + (s * t.x - x) * b,
            y + (s * t.y - y) * b,
            z + (s * t.z - z) * b,
            w + (s * t.w - w) * b,
        ).normalized()
    }

    companion object {
        val IDENTITY = Quat(0f, 0f, 0f, 1f)

        /** Estimate phone tilt by aligning Android's stationary gravity vector with world-up; gravity has no yaw. */
        fun fromGravity(gx: Float, gy: Float, gz: Float): Quat? {
            if (!gx.isFinite() || !gy.isFinite() || !gz.isFinite()) return null
            val length = sqrt(gx * gx + gy * gy + gz * gz)
            if (length < 5f) return null // ignore free-fall / invalid samples
            val x = gx / length
            val y = gy / length
            val z = gz / length
            // Android reports the support/gravity vector toward device-up at rest, so align it to world-up.
            // The antiparallel case has no unique shortest axis; choose device X deterministically.
            if (z < -0.9999f) return Quat(1f, 0f, 0f, 0f)
            val qx = y
            val qy = -x
            val qz = 0f
            val qw = 1f + z
            return Quat(qx, qy, qz, qw).normalizedOrNull()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Rendering
// ---------------------------------------------------------------------------------------------

private fun DrawScope.renderScene(mesh: PhoneMesh, q: Quat, az: Float, el: Float, dist: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val focal = size.height * 1.2f

    // Camera orbiting the origin; world is ENU, Z up.
    val ce = cos(el)
    val camX = dist * sin(az) * ce
    val camY = -dist * cos(az) * ce
    val camZ = dist * sin(el)
    val fx = -camX / dist
    val fy = -camY / dist
    val fz = -camZ / dist
    var rx = fy
    var ry = -fx
    val rl = sqrt(rx * rx + ry * ry)
    rx /= rl
    ry /= rl
    val ux = ry * fz
    val uy = -rx * fz
    val uz = rx * fy - ry * fx

    val tmp = FloatArray(3)
    fun project(x: Float, y: Float, z: Float) {
        val dx = x - camX
        val dy = y - camY
        val dz = z - camZ
        val depth = dx * fx + dy * fy + dz * fz
        val s = focal / depth
        tmp[0] = cx + (dx * rx + dy * ry) * s
        tmp[1] = cy - (dx * ux + dy * uy + dz * uz) * s
        tmp[2] = depth
    }

    fun line(ax: Float, ay: Float, azz: Float, bx: Float, by: Float, bz: Float, color: Color, width: Float) {
        project(ax, ay, azz)
        val x0 = tmp[0]; val y0 = tmp[1]; val d0 = tmp[2]
        project(bx, by, bz)
        if (d0 < 0.1f || tmp[2] < 0.1f) return
        drawLine(color, Offset(x0, y0), Offset(tmp[0], tmp[1]), strokeWidth = width)
    }

    fun arrowHead(ax: Float, ay: Float, azz: Float, bx: Float, by: Float, bz: Float, color: Color) {
        project(ax, ay, azz)
        val x0 = tmp[0]; val y0 = tmp[1]; val d0 = tmp[2]
        project(bx, by, bz)
        val x1 = tmp[0]; val y1 = tmp[1]; val d1 = tmp[2]
        if (d0 < 0.1f || d1 < 0.1f) return
        val dx = x1 - x0; val dy = y1 - y0
        val length = sqrt(dx * dx + dy * dy)
        if (length < 1f) return
        val dirX = dx / length; val dirY = dy / length
        val baseX = x1 - dirX * 13f; val baseY = y1 - dirY * 13f
        val head = Path().apply {
            moveTo(x1, y1)
            lineTo(baseX - dirY * 5f, baseY + dirX * 5f)
            lineTo(baseX + dirY * 5f, baseY - dirX * 5f)
            close()
        }
        drawPath(head, color)
    }

    // Floor grid + world axes (X east, Y north, Z up).
    val floor = -0.9f
    val grid = Color(0.18f, 0.22f, 0.25f)
    for (i in -8..8) {
        val v = i * 0.25f
        line(v, -2f, floor, v, 2f, floor, grid, 1f)
        line(-2f, v, floor, 2f, v, floor, grid, 1f)
    }
    line(-1.6f, 0f, floor, 0f, 0f, floor, brighten(StudioColors.axisX), 2f)
    line(0f, 0f, floor, 1.6f, 0f, floor, StudioColors.axisX, 2f)
    line(0f, -1.6f, floor, 0f, 0f, floor, brighten(StudioColors.success), 2f)
    line(0f, 0f, floor, 0f, 1.6f, floor, StudioColors.success, 2f)
    line(0f, 0f, floor, 0f, 0f, 1.5f, StudioColors.axisZ, 2f)
    line(-0.09f, 0f, 1.29f, 0f, 0f, 1.5f, StudioColors.axisZ, 2f)
    line(0.09f, 0f, 1.29f, 0f, 0f, 1.5f, StudioColors.axisZ, 2f)

    // Rotation matrix from quaternion (device -> world).
    val x = q.x; val y = q.y; val z = q.z; val w = q.w
    val m00 = 1 - 2 * (y * y + z * z); val m01 = 2 * (x * y - z * w); val m02 = 2 * (x * z + y * w)
    val m10 = 2 * (x * y + z * w); val m11 = 1 - 2 * (x * x + z * z); val m12 = 2 * (y * z - x * w)
    val m20 = 2 * (x * z - y * w); val m21 = 2 * (y * z + x * w); val m22 = 1 - 2 * (x * x + y * y)

    val n = mesh.v.size / 3
    val wx = FloatArray(n); val wy = FloatArray(n); val wz = FloatArray(n)
    val sx = FloatArray(n); val sy = FloatArray(n); val sd = FloatArray(n)
    for (i in 0 until n) {
        val mx = mesh.v[i * 3]; val my = mesh.v[i * 3 + 1]; val mz = mesh.v[i * 3 + 2]
        wx[i] = m00 * mx + m01 * my + m02 * mz
        wy[i] = m10 * mx + m11 * my + m12 * mz
        wz[i] = m20 * mx + m21 * my + m22 * mz
        project(wx[i], wy[i], wz[i])
        sx[i] = tmp[0]; sy[i] = tmp[1]; sd[i] = tmp[2]
    }

    // Light fixed relative to the camera (upper-left, slightly toward viewer).
    val lx = -0.35f * rx + 0.6f * ux - 0.7f * fx
    val ly = -0.35f * ry + 0.6f * uy - 0.7f * fy
    val lz = 0.6f * uz - 0.7f * fz

    val triCount = mesh.tri.size / 3
    val visible = BooleanArray(triCount)
    val depthT = FloatArray(triCount)
    val shadeT = FloatArray(triCount)
    for (t in 0 until triCount) {
        val a = mesh.tri[t * 3]; val b = mesh.tri[t * 3 + 1]; val c = mesh.tri[t * 3 + 2]
        if (sd[a] < 0.1f || sd[b] < 0.1f || sd[c] < 0.1f) continue
        val nx0 = mesh.triNormal[t * 3]; val ny0 = mesh.triNormal[t * 3 + 1]; val nz0 = mesh.triNormal[t * 3 + 2]
        val nx = m00 * nx0 + m01 * ny0 + m02 * nz0
        val ny = m10 * nx0 + m11 * ny0 + m12 * nz0
        val nz = m20 * nx0 + m21 * ny0 + m22 * nz0
        if (nx * (camX - wx[a]) + ny * (camY - wy[a]) + nz * (camZ - wz[a]) <= 0f) continue
        visible[t] = true
        depthT[t] = (sd[a] + sd[b] + sd[c]) / 3f
        shadeT[t] = 0.4f + 0.6f * max(0f, nx * lx + ny * ly + nz * lz)
    }

    val path = Path()
    val cameraDeviceZ = m02 * camX + m12 * camY + m22 * camZ
    val cameraOnBackSide = cameraDeviceZ < mesh.backZ
    fun drawLayer(layer: Int) {
        val order = (0 until triCount).filter { visible[it] && mesh.triLayer[it] == layer }
            .sortedByDescending { depthT[it] }
        for (t in order) {
            val a = mesh.tri[t * 3]; val b = mesh.tri[t * 3 + 1]; val c = mesh.tri[t * 3 + 2]
            val base = if (layer == LAYER_BUMP) Color(0.035f, 0.04f, 0.045f) else mesh.colors[mesh.triMat[t]]
            val s = shadeT[t]
            val color = Color(base.red * s, base.green * s, base.blue * s)
            path.reset()
            path.moveTo(sx[a], sy[a]); path.lineTo(sx[b], sy[b]); path.lineTo(sx[c], sy[c]); path.close()
            drawPath(path, color)
            drawPath(path, color, style = Stroke(width = 0.8f))
        }
    }

    // Explicit layers prevent triangle-center sorting from painting rear camera bumps over the
    // front screen. The USB recess is painted first so the shell naturally occludes its interior.
    drawLayer(LAYER_PORT)
    if (!cameraOnBackSide) drawLayer(LAYER_BUMP)
    drawLayer(LAYER_SHELL)
    if (cameraOnBackSide) drawLayer(LAYER_BUMP)

    // iPhone-style Dynamic Island: a pill fixed to the center of the front glass near its top edge.
    if (!cameraOnBackSide) {
        val islandWidth = 0.34f
        val islandHeight = 0.072f
        val radius = islandHeight / 2f
        val islandY = mesh.topY - 0.13f
        val islandZ = mesh.frontZ + 0.002f
        val points = ArrayList<Offset>(18)
        for (step in 0..8) {
            val angle = (-90f + step * 180f / 8f) * 0.0174532925f
            points += Offset(islandWidth / 2f - radius + radius * cos(angle), islandY + radius * sin(angle))
        }
        for (step in 0..8) {
            val angle = (90f + step * 180f / 8f) * 0.0174532925f
            points += Offset(-islandWidth / 2f + radius + radius * cos(angle), islandY + radius * sin(angle))
        }
        path.reset()
        points.forEachIndexed { index, point ->
            val wx = m00 * point.x + m01 * point.y + m02 * islandZ
            val wy = m10 * point.x + m11 * point.y + m12 * islandZ
            val wz = m20 * point.x + m21 * point.y + m22 * islandZ
            project(wx, wy, wz)
            if (index == 0) path.moveTo(tmp[0], tmp[1]) else path.lineTo(tmp[0], tmp[1])
        }
        path.close()
        drawPath(path, Color(0.012f, 0.014f, 0.018f))

        // Small lens glint on the right side makes the feature read as a front camera island.
        val lensX = 0.105f
        val lensY = islandY
        val lensWorldX = m00 * lensX + m01 * lensY + m02 * (islandZ + 0.001f)
        val lensWorldY = m10 * lensX + m11 * lensY + m12 * (islandZ + 0.001f)
        val lensWorldZ = m20 * lensX + m21 * lensY + m22 * (islandZ + 0.001f)
        project(lensWorldX, lensWorldY, lensWorldZ)
        val lensCenter = Offset(tmp[0], tmp[1])
        val lensDepth = tmp[2]
        project(
            m00 * (lensX + 0.012f) + m01 * lensY + m02 * (islandZ + 0.001f),
            m10 * (lensX + 0.012f) + m11 * lensY + m12 * (islandZ + 0.001f),
            m20 * (lensX + 0.012f) + m21 * lensY + m22 * (islandZ + 0.001f),
        )
        if (lensDepth > 0.1f && tmp[2] > 0.1f) {
            val lensRadius = (tmp[0] - lensCenter.x).let { sqrt(it * it + (tmp[1] - lensCenter.y) * (tmp[1] - lensCenter.y)) }
            drawCircle(Color(0.12f, 0.17f, 0.23f), radius = lensRadius, center = lensCenter)
        }
    }

    // Odom-style phone-frame triad: all three arrows begin at the model origin and rotate with IMU.
    val axes = listOf(
        Triple(StudioColors.axisX, 0.82f, 0f),
        Triple(StudioColors.success, 0f, 1.05f),
        Triple(StudioColors.axisZ, 0f, 0f),
    )
    val origin = floatArrayOf(0f, 0f, 0f)
    for ((index, axis) in axes.withIndex()) {
        val localX = if (index == 0) axis.second else 0f
        val localY = if (index == 1) axis.second else 0f
        val localZ = if (index == 2) 0.62f else 0f
        val endX = m00 * localX + m01 * localY + m02 * localZ
        val endY = m10 * localX + m11 * localY + m12 * localZ
        val endZ = m20 * localX + m21 * localY + m22 * localZ
        line(origin[0], origin[1], origin[2], endX, endY, endZ, axis.first, 3f)
        arrowHead(origin[0], origin[1], origin[2], endX, endY, endZ, axis.first)
    }
}

// ---------------------------------------------------------------------------------------------
// OBJ parsing (pure Kotlin, no java.*)
// ---------------------------------------------------------------------------------------------

private class PhoneMesh(
    val v: FloatArray,          // centered + scaled xyz
    val tri: IntArray,          // 3 vertex indices per triangle
    val triNormal: FloatArray,  // 3 floats per triangle, from the file's vn
    val triMat: IntArray,       // material index per triangle
    val triLayer: IntArray,
    val backZ: Float,
    val frontZ: Float,
    val topY: Float,
    val colors: List<Color>,
)

private val materialNames = listOf("PhoneWhite", "PhoneBlack", "ScreenBlack")
private val materialColors = listOf(
    Color(0.92f, 0.92f, 0.92f),
    Color(0.16f, 0.16f, 0.17f),
    Color(0.10f, 0.12f, 0.15f),
)

private data class ViewAxisLabel(val text: String, val x: Float, val y: Float, val z: Float, val color: Color)
private data class DeviceAxisLabel(val name: String, val length: Float, val color: Color) {
    val x: Float get() = when (name) { "X" -> 1f; else -> 0f }
    val y: Float get() = when (name) { "Y" -> 1f; else -> 0f }
    val z: Float get() = when (name) { "Z" -> 1f; else -> 0f }
}

private data class Vec3(val x: Float, val y: Float, val z: Float)

private fun clampOverlayPosition(value: Float, minimum: Float, maximum: Float): Float =
    value.coerceIn(minimum, maximum.coerceAtLeast(minimum))

private fun rotateDeviceVector(q: Quat, x: Float, y: Float, z: Float): Vec3 {
    val xx = q.x; val yy = q.y; val zz = q.z; val ww = q.w
    return Vec3(
        (1 - 2 * (yy * yy + zz * zz)) * x + 2 * (xx * yy - zz * ww) * y + 2 * (xx * zz + yy * ww) * z,
        2 * (xx * yy + zz * ww) * x + (1 - 2 * (xx * xx + zz * zz)) * y + 2 * (yy * zz - xx * ww) * z,
        2 * (xx * zz - yy * ww) * x + 2 * (yy * zz + xx * ww) * y + (1 - 2 * (xx * xx + yy * yy)) * z,
    )
}

private const val LAYER_PORT = 0
private const val LAYER_BUMP = 1
private const val LAYER_SHELL = 2

private fun parsePhoneObj(text: String): PhoneMesh {
    val ws = Regex("\\s+")
    val pos = ArrayList<Float>()
    val nrm = ArrayList<Float>()
    val tri = ArrayList<Int>()
    val triN = ArrayList<Float>()
    val triM = ArrayList<Int>()
    val triLayer = ArrayList<Int>()
    var mat = 0

    for (raw in text.lineSequence()) {
        val line = raw.trim()
        when {
            line.startsWith("v ") -> line.split(ws).let { pos += it[1].toFloat(); pos += it[2].toFloat(); pos += it[3].toFloat() }
            line.startsWith("vn ") -> line.split(ws).let { nrm += it[1].toFloat(); nrm += it[2].toFloat(); nrm += it[3].toFloat() }
            line.startsWith("usemtl ") -> mat = materialNames.indexOf(line.substring(7).trim()).coerceAtLeast(0)
            line.startsWith("f ") -> {
                val corners = line.split(ws).drop(1).map { it.split('/') }
                val vi = corners.map { it[0].toInt() - 1 }
                val ni = corners.first().getOrNull(2)?.takeIf { it.isNotEmpty() }?.toInt()?.minus(1)
                for (k in 1 until vi.size - 1) { // fan triangulation
                    val a = vi[0]; val b = vi[k]; val c = vi[k + 1]
                    tri += a; tri += b; tri += c
                    triM += mat
                    if (ni != null) {
                        triN += nrm[ni * 3]; triN += nrm[ni * 3 + 1]; triN += nrm[ni * 3 + 2]
                    } else {
                        val e1x = pos[b * 3] - pos[a * 3]; val e1y = pos[b * 3 + 1] - pos[a * 3 + 1]; val e1z = pos[b * 3 + 2] - pos[a * 3 + 2]
                        val e2x = pos[c * 3] - pos[a * 3]; val e2y = pos[c * 3 + 1] - pos[a * 3 + 1]; val e2z = pos[c * 3 + 2] - pos[a * 3 + 2]
                        triN += e1y * e2z - e1z * e2y; triN += e1z * e2x - e1x * e2z; triN += e1x * e2y - e1y * e2x
                    }
                }
            }
        }
    }

    fun isPort(i: Int): Boolean {
        val px = pos[i * 3]; val py = pos[i * 3 + 1]; val pz = pos[i * 3 + 2]
        return abs(px) <= 0.4175f && py <= -7.799f && pz in 0.2515f..0.5085f
    }
    fun isBehindBack(i: Int) = pos[i * 3 + 2] < -0.001f
    var rawBackZ = Float.NEGATIVE_INFINITY
    var rawFrontZ = Float.NEGATIVE_INFINITY
    repeat(triM.size) { t ->
        val a = tri[t * 3]; val b = tri[t * 3 + 1]; val c = tri[t * 3 + 2]
        val layer = when {
            isPort(a) && isPort(b) && isPort(c) -> LAYER_PORT
            isBehindBack(a) || isBehindBack(b) || isBehindBack(c) -> LAYER_BUMP
            else -> LAYER_SHELL
        }
        triLayer += layer
        if (layer == LAYER_BUMP) rawBackZ = max(rawBackZ, max(pos[a * 3 + 2], max(pos[b * 3 + 2], pos[c * 3 + 2])))
        if (triM[t] == 2) rawFrontZ = max(rawFrontZ, max(pos[a * 3 + 2], max(pos[b * 3 + 2], pos[c * 3 + 2])))
    }
    if (rawBackZ == Float.NEGATIVE_INFINITY) rawBackZ = 0f
    if (rawFrontZ == Float.NEGATIVE_INFINITY) rawFrontZ = 0f

    // Center on the bounding box; 0.1 matches the old jME model.scale(0.1f).
    val verts = pos.toFloatArray()
    val mid = FloatArray(3)
    val maxBound = FloatArray(3)
    for (axis in 0..2) {
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (i in axis until verts.size step 3) { lo = minOf(lo, verts[i]); hi = maxOf(hi, verts[i]) }
        mid[axis] = (lo + hi) / 2f
        maxBound[axis] = hi
    }
    for (i in verts.indices) verts[i] = (verts[i] - mid[i % 3]) * 0.1f

    return PhoneMesh(
        verts, tri.toIntArray(), triN.toFloatArray(), triM.toIntArray(), triLayer.toIntArray(),
        (rawBackZ - mid[2]) * 0.1f, (rawFrontZ - mid[2]) * 0.1f,
        (maxBound[1] - mid[1]) * 0.1f, materialColors,
    )
}
