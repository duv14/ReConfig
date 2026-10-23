/* ReConfig by duv14. See LICENSE-RECONFIG.txt and ATTRIBUTIONS.md. */
package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import org.polyfrost.compose.mc.McFontQueue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin

/** Articulated skin preview. Rendering stops when this composable leaves the screen. */
@Composable
fun PlayerSkinPreview(username: String, uuid: String, modifier: Modifier = Modifier, tag: String = "", emoji: String = "") {
    var yaw by remember(username, uuid) { mutableFloatStateOf(12f) }
    var frame by remember(username, uuid) { mutableStateOf<ImageBitmap?>(null) }
    var loaded by remember(username, uuid) { mutableStateOf(false) }
    var animated by remember { mutableStateOf(true) }
    var walking by remember { mutableStateOf(false) }
    var waveStart by remember { mutableLongStateOf(0L) }
    LaunchedEffect(username, uuid) {
        loaded = false
        frame = null
        val texture = OfficialHeads.loadSkin(username, uuid)
        if (texture == null) { loaded = true; return@LaunchedEffect }
        val atlas = withContext(Dispatchers.Default) { ImageIO.read(ByteArrayInputStream(texture.png)) }
        if (atlas == null) { loaded = true; return@LaunchedEffect }
        var seconds = 0.0
        var last = System.nanoTime()
        var lastYaw = Float.NaN
        var wasMoving = true
        try {
            while (isActive) {
                val now = System.nanoTime()
                if (animated) seconds += ((now - last) / 1_000_000_000.0).coerceIn(0.0, .1)
                last = now
                val wave = if (waveStart == 0L) -1.0 else (now - waveStart) / 1_000_000_000.0
                val moving = animated || wave in 0.0..2.2
                val currentYaw = yaw
                if (moving || wasMoving || currentYaw != lastYaw || frame == null) {
                    val image = withContext(Dispatchers.Default) {
                        val rendered = SkinModelRenderer.renderFrame(atlas, texture.slim, currentYaw.toDouble(), seconds, walking && animated, wave)
                        try { rendered.toComposeImageBitmap() } finally { rendered.flush() }
                    }
                    frame = image
                    loaded = true
                    lastYaw = currentYaw
                }
                wasMoving = moving
                delay(50) // cap at 20fps; paused previews do no raster work until rotated
            }
        } finally { atlas.flush() }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MinecraftNametag(username, tag, emoji)
        val rendered = frame
        Box(Modifier.fillMaxWidth().height(385.dp).pointerInput(username, uuid) {
            detectDragGestures { change, drag -> change.consume(); yaw = (yaw + drag.x * .6f) % 360f }
        }, contentAlignment = Alignment.Center) {
            if (rendered != null) Image(bitmap = rendered, contentDescription = "Animated 3D Minecraft skin of $username. Drag to rotate.",
                modifier = Modifier.fillMaxSize(), filterQuality = FilterQuality.None)
            else Text(if (loaded) "Skin preview unavailable" else "Loading your skin…", color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SkinControl(if (walking) "Stand" else "Walk") { walking = !walking; animated = true }
            SkinControl("Wave") { waveStart = System.nanoTime() }
            SkinControl(if (animated) "Pause" else "Animate") { animated = !animated }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SkinControl("↶") { yaw -= 30f }
            SkinControl("Reset view") { yaw = 12f; walking = false }
            SkinControl("↷") { yaw += 30f }
        }
        Text("Drag to rotate", color = LocalTheme.current.textColorSecondary.copy(.65f), fontSize = 10.sp)
    }
}

@Composable
private fun SkinControl(label: String, click: () -> Unit) {
    val theme = LocalTheme.current
    Text(label, color = theme.textColorSecondary, fontSize = 11.sp,
        modifier = Modifier.background(UiColor.White.copy(.04f), RoundedCornerShape(8.dp))
            .border(1.dp, UiColor.White.copy(.055f), RoundedCornerShape(8.dp))
            .onClick(rememberInteractionSource(), click).padding(horizontal = 10.dp, vertical = 8.dp))
}

/** Uses Minecraft's live glyph renderer so the name matches the in-game pixel font. */
@Composable
private fun MinecraftNametag(username: String, tag: String, emoji: String) {
    val density = LocalDensity.current.density
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val nameScale = minOf(2.65f, (maxWidth.value - if (tag.isEmpty()) 28f else 96f) / McFontQueue.measureTextWidth(username, 1f)).coerceAtLeast(1f)
        val scale = nameScale * density
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.background(UiColor(0xEF202124), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (McFontQueue.renderer != null) {
                    Canvas(Modifier.size((McFontQueue.measureTextWidth(username, scale) / density).dp, (McFontQueue.measureTextHeight(scale) / density).dp)) {
                        drawIntoCanvas { canvas -> McFontQueue.renderer?.invoke(canvas.nativeCanvas, username, 0f, 0f, 0xFFADB0BE.toInt(), false, scale) }
                    }
                } else BasicText(username, style = TextStyle(color = UiColor(0xFFADB0BE), fontSize = 22.sp, fontFamily = FontFamily.Monospace))
            }
            if (tag.isNotEmpty()) Row(Modifier.background(UiColor(0xEF303136), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (emoji.isNotEmpty()) Icon("/assets/reconfig/emojis/$emoji.png", modifier = Modifier.size(18.dp))
                Text(tag, color = UiColor(0xFFE4E5EC), fontSize = 13.sp)
            }
        }
    }
}

/** Orthographic projection with face culling, depth sorting and skin-atlas UV coordinates. */
internal object SkinModelRenderer {
    private data class Point(val x: Double, val y: Double, val z: Double)
    private data class Face(val points: List<Point>, val u: Int, val v: Int, val width: Int, val height: Int, val mirror: Boolean)
    private data class VisibleFace(val face: Face, val points: List<Point>, val depth: Double, val light: Float)

    fun render(png: ByteArray, slim: Boolean, yawDegrees: Double): ByteArray {
        val atlas = ImageIO.read(ByteArrayInputStream(png)) ?: error("Invalid skin")
        val frame = renderFrame(atlas, slim, yawDegrees)
        return try { ByteArrayOutputStream().use { output -> check(ImageIO.write(frame, "png", output)); output.toByteArray() } }
        finally { atlas.flush(); frame.flush() }
    }

    fun renderFrame(skin: BufferedImage, slim: Boolean, yawDegrees: Double, seconds: Double = 0.0, walking: Boolean = false, wave: Double = -1.0): BufferedImage {
        require(skin.width == 64 && skin.height in listOf(32, 64))
        val faces = mutableListOf<Face>()
        fun cube(x: Double, y: Double, w: Int, h: Int, d: Int, u: Int, v: Int, expand: Double = 0.0, mirror: Boolean = false, swing: Double = 0.0, roll: Double = 0.0, turn: Double = 0.0) {
            val left = x - w / 2.0 - expand; val right = x + w / 2.0 + expand
            val top = y - expand; val bottom = y + h + expand
            val front = d / 2.0 + expand; val back = -d / 2.0 - expand
            fun p(xp: Double, yp: Double, zp: Double): Point {
                val px = xp - x; val py = yp - y
                val ry = py * cos(swing) - zp * sin(swing)
                val rz = py * sin(swing) + zp * cos(swing)
                val rx = px * cos(roll) - ry * sin(roll)
                val rotatedY = px * sin(roll) + ry * cos(roll)
                return Point(x + rx * cos(turn) + rz * sin(turn), y + rotatedY - 16.0, -rx * sin(turn) + rz * cos(turn))
            }
            fun face(points: List<Point>, tx: Int, ty: Int, tw: Int, th: Int) { faces += Face(points, tx, ty, tw, th, mirror) }
            face(listOf(p(left,top,front),p(right,top,front),p(right,bottom,front),p(left,bottom,front)),u+d,v+d,w,h)
            face(listOf(p(right,top,back),p(left,top,back),p(left,bottom,back),p(right,bottom,back)),u+2*d+w,v+d,w,h)
            face(listOf(p(right,top,front),p(right,top,back),p(right,bottom,back),p(right,bottom,front)),u+d+w,v+d,d,h)
            face(listOf(p(left,top,back),p(left,top,front),p(left,bottom,front),p(left,bottom,back)),u,v+d,d,h)
            face(listOf(p(left,top,back),p(right,top,back),p(right,top,front),p(left,top,front)),u+d,v,w,d)
            face(listOf(p(left,bottom,front),p(right,bottom,front),p(right,bottom,back),p(left,bottom,back)),u+d+w,v,w,d)
        }
        val stride = if (walking) sin(seconds * 6.0) * .55 else 0.0
        val idle = sin(seconds * 1.8) * .035
        val waving = wave in 0.0..2.2
        val waveBlend = if (waving) (sin((wave / 2.2) * Math.PI) * 4).coerceIn(0.0, 1.0) else 0.0
        val armRoll = (2.85 + sin(wave * 13.0) * .10) * waveBlend
        val headTurn = sin(seconds * .65) * .07
        val modern = skin.height == 64
        val arm = if (slim && modern) 3 else 4
        cube(0.0,0.0,8,8,8,0,0,turn=headTurn)
        cube(0.0,8.0,8,12,4,16,16)
        cube(-4.0-arm/2.0,8.0,arm,12,4,40,16,swing=stride+idle,roll=armRoll+.03)
        cube(4.0+arm/2.0,8.0,arm,12,4,if(modern)32 else 40,if(modern)48 else 16,mirror=!modern,swing=-stride-idle,roll=-.03)
        cube(-2.0,20.0,4,12,4,0,16,swing=-stride)
        cube(2.0,20.0,4,12,4,if(modern)16 else 0,if(modern)48 else 16,mirror=!modern,swing=stride)
        val hat = modern || (32 until 64).any { x -> (0 until 32).any { y -> skin.getRGB(x,y) ushr 24 < 128 } }
        if(hat) cube(0.0,0.0,8,8,8,32,0,.5,turn=headTurn)
        if(modern) {
            cube(0.0,8.0,8,12,4,16,32,.25)
            cube(-4.0-arm/2.0,8.0,arm,12,4,40,32,.25,swing=stride+idle,roll=armRoll+.03)
            cube(4.0+arm/2.0,8.0,arm,12,4,48,48,.25,swing=-stride-idle,roll=-.03)
            cube(-2.0,20.0,4,12,4,0,32,.25,swing=-stride)
            cube(2.0,20.0,4,12,4,0,48,.25,swing=stride)
        }
        val yaw = Math.toRadians(yawDegrees); val pitch = -.10
        val breathing = sin(seconds * 1.8) * .12
        fun rotate(p: Point): Point {
            val x = p.x*cos(yaw)+p.z*sin(yaw)
            val z = -p.x*sin(yaw)+p.z*cos(yaw)
            return Point(x,(p.y+breathing)*cos(pitch)-z*sin(pitch),(p.y+breathing)*sin(pitch)+z*cos(pitch))
        }
        val visible = faces.mapNotNull { face ->
            val points = face.points.map(::rotate)
            val a=points[0]; val b=points[1]; val c=points[3]
            val normalZ=(b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x)
            if(normalZ <= .00001) null else {
                val normalX=(b.y-a.y)*(c.z-a.z)-(b.z-a.z)*(c.y-a.y)
                val normalY=(b.z-a.z)*(c.x-a.x)-(b.x-a.x)*(c.z-a.z)
                val length=kotlin.math.sqrt(normalX*normalX+normalY*normalY+normalZ*normalZ)
                val light=(.72+.22*normalZ/length-.12*normalY/length).coerceIn(.5,1.0).toFloat()
                VisibleFace(face,points,points.sumOf { it.z }/4,light)
            }
        }.sortedBy { it.depth }
        val image = BufferedImage(256,400,BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            graphics.color=Color(0,0,0,22); graphics.fillOval(53,370,150,16)
            for(entry in visible) {
                val face=entry.face
                val tile=BufferedImage(face.width,face.height,BufferedImage.TYPE_INT_ARGB)
                for(y in 0 until face.height) for(x in 0 until face.width) {
                    val rgba=skin.getRGB(face.u+if(face.mirror)face.width-1-x else x,face.v+y)
                    val r=((rgba ushr 16 and 255)*entry.light).toInt()
                    val g=((rgba ushr 8 and 255)*entry.light).toInt()
                    val b=((rgba and 255)*entry.light).toInt()
                    tile.setRGB(x,y,(rgba and -0x1000000) or (r shl 16) or (g shl 8) or b)
                }
                val p0=entry.points[0]; val p1=entry.points[1]; val p3=entry.points[3]
                val scale=10.4-.6*waveBlend
                val transform=AffineTransform((p1.x-p0.x)*scale/face.width,(p1.y-p0.y)*scale/face.width,(p3.x-p0.x)*scale/face.height,(p3.y-p0.y)*scale/face.height,128+p0.x*scale,198+16*(10.4-scale)+p0.y*scale)
                graphics.composite=AlphaComposite.SrcOver
                graphics.drawImage(tile,transform,null)
                tile.flush()
            }
        } finally { graphics.dispose() }
        return image
    }
}
