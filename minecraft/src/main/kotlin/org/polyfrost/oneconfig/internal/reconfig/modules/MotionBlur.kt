/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules

import net.minecraft.client.Minecraft
import org.jetbrains.skia.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.polyfrost.oneconfig.internal.ui.compose.opengl.StoredGLState
import org.slf4j.LoggerFactory

/** Temporal world-frame accumulation, executed before the HUD is drawn. OpenGL only. */
object MotionBlur {
    private val log = LoggerFactory.getLogger("ReConfig/MotionBlur")
    private val gl = StoredGLState(330)
    private var source: Surface? = null
    private var backend: BackendRenderTarget? = null
    private var current: Surface? = null
    private var history: Surface? = null
    private var framebuffer = -1
    private var world: Any? = null
    private var lastTime = 0L
    private var seeded = false
    private var failed = false
    private val copy = Paint().apply { blendMode = BlendMode.SRC }
    private val opaque = Paint().apply { color = 0xFF000000.toInt(); blendMode = BlendMode.PLUS }
    private val blend = Paint()

    @JvmStatic fun reset() {
        history?.close(); history = null
        current?.close(); current = null
        source?.close(); source = null
        backend?.close(); backend = null
        framebuffer = -1; seeded = false; lastTime = 0; world = null
    }

    @JvmStatic fun draw() {
        val mc = Minecraft.getInstance()
        if (!ModuleAccess.enabled("motion_blur") || mc.level == null || mc.screen != null ||
            ModuleAccess.number("motion_blur", "strength", 35f) <= 0f) {
            reset(); failed = false; return
        }
        if (failed || !SkiaCtx.isReady || SkiaCtx.isVulkanMode) return
        val target = mc.mainRenderTarget
        val fbo = RenderTargetFbo.getFboId(target)
        if (fbo <= 0 || target.width <= 0 || target.height <= 0) return
        val oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        gl.capture()
        try {
            SkiaCtx.directContext.resetGLAll()
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
            if (framebuffer != fbo || source?.width != target.width || source?.height != target.height || world !== mc.level) {
                reset()
                backend = BackendRenderTarget.makeGL(target.width, target.height, 0, 8, fbo, FramebufferFormat.GR_GL_RGBA8)
                source = Surface.makeFromBackendRenderTarget(SkiaCtx.directContext, backend!!,
                    SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB, null)
                val info = ImageInfo.makeN32Premul(target.width, target.height)
                current = Surface.makeRenderTarget(SkiaCtx.directContext, false, info, 0, SurfaceOrigin.BOTTOM_LEFT, null)
                history = Surface.makeRenderTarget(SkiaCtx.directContext, false, info, 0, SurfaceOrigin.BOTTOM_LEFT, null)
                framebuffer = fbo; world = mc.level
            }
            val src = source ?: return
            val nowFrame = current ?: return
            val previous = history ?: return
            src.notifyContentWillChange(ContentChangeMode.RETAIN)
            src.draw(nowFrame.canvas, 0, 0, copy)
            // Minecraft's world alpha is not guaranteed opaque; normalize it before accumulation.
            nowFrame.canvas.drawRect(Rect.makeXYWH(0f, 0f, target.width.toFloat(), target.height.toFloat()), opaque)
            val now = System.nanoTime()
            val dt = if (lastTime == 0L) 1.0 / 60.0 else ((now - lastTime) / 1e9).coerceIn(.001, .25)
            val retention = EffectMath.retention(ModuleAccess.number("motion_blur", "strength", 35f), ModuleAccess.number("motion_blur", "responsiveness", 70f), dt)
            if (!seeded || now - lastTime > 250_000_000L) nowFrame.draw(previous.canvas, 0, 0, copy)
            else {
                blend.color = ((255 * (1 - retention)).toInt().coerceIn(1, 255) shl 24) or 0xFFFFFF
                nowFrame.draw(previous.canvas, 0, 0, blend)
            }
            previous.draw(src.canvas, 0, 0, copy)
            seeded = true; lastTime = now
            SkiaCtx.directContext.flushAndSubmit(src, false)
        } catch (error: Exception) {
            reset(); failed = true
            log.error("Motion blur disabled after a rendering failure; toggle the module off/on to retry", error)
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead)
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw)
            gl.restore()
        }
    }
}
