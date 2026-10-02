package com.spizganed.quickbuds.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.*

/**
 * Samsung One UI 9 style real-time 3D interactive view of wireless earbuds and charging case.
 * Features:
 * - Real-time 3D perspective projection via android.graphics.Camera and Matrix.
 * - Interactive touch orbiting (drag to tilt & rotate 3D view in space).
 * - Continuous smooth harmonic levitation / floating physics for left & right earbuds.
 * - 3D opening / closing clamshell lid on the charging case with smooth hinge rotation.
 * - Samsung Galaxy Buds signature glowing Blade Lights (LED strips).
 * - Dynamic 3D perspective floor drop-shadows responding to floating elevation.
 * - Acoustic soundwave / ANC isolation visualizer rings around the earbuds.
 * - Interactive tap detection for Left Bud, Right Bud, and Case lid toggle.
 */
class SamsungBuds3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Status inputs from connection manager / store
    var leftBattery = 85
    var caseBattery = 90
    var rightBattery = 85
    var leftStatus = 3   // 3 = in ear, 4 = in case, etc.
    var rightStatus = 3
    var connected = true
        set(value) {
            field = value
            invalidate()
        }
    var ancMode: String = "Off"
        set(value) {
            field = value
            invalidate()
        }

    // Callback when user taps a component
    var onComponentTapped: ((String) -> Unit)? = null

    // 3D Transforms
    private val camera = Camera()
    private val matrix3D = Matrix()

    // Interactive orbit state
    private var orbitYaw = 0f
    private var orbitPitch = 10f
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDragging = false
    private var lastTouchTime = 0L

    // Case lid open state: 1f = open, 0f = closed
    var caseOpenProgress = 1f
        private set
    private var lidAnimator: ValueAnimator? = null

    // Earbud selection hop animations
    private var leftHop = 0f
    private var rightHop = 0f

    // Continuous floating animation
    private var animTime = 0f
    private val floatAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3200
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            animTime = (it.animatedValue as Float) * (2f * Math.PI.toFloat())
            postInvalidateOnAnimation()
        }
    }

    private fun dp(v: Float) = ThemeRes.dp(context, v).toFloat()

    // Paints
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val rectF = RectF()
    private val tempPath = Path()

    init {
        floatAnimator.start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!floatAnimator.isRunning) floatAnimator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        floatAnimator.cancel()
        lidAnimator?.cancel()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        // Generous, plush stage height for One UI 9 3D showcase
        val h = dp(230f).toInt()
        setMeasuredDimension(w, h)
    }

    fun toggleCaseLid() {
        val target = if (caseOpenProgress > 0.5f) 0f else 1f
        lidAnimator?.cancel()
        lidAnimator = ValueAnimator.ofFloat(caseOpenProgress, target).apply {
            duration = 420
            interpolator = if (target > 0.5f) OvershootInterpolator(1.2f) else DecelerateInterpolator()
            addUpdateListener {
                caseOpenProgress = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
        Haptics.commit(this)
        onComponentTapped?.invoke("case")
    }

    fun hopLeftBud() {
        ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                leftHop = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
        Haptics.commit(this)
        onComponentTapped?.invoke("left")
    }

    fun hopRightBud() {
        ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                rightHop = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
        Haptics.commit(this)
        onComponentTapped?.invoke("right")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                lastTouchTime = System.currentTimeMillis()
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchDownX
                val dy = event.y - touchDownY
                if (!isDragging && (abs(dx) > dp(8f) || abs(dy) > dp(8f))) {
                    isDragging = true
                }
                if (isDragging) {
                    orbitYaw = (orbitYaw + dx * 0.4f).coerceIn(-40f, 40f)
                    orbitPitch = (orbitPitch - dy * 0.3f).coerceIn(-10f, 32f)
                    touchDownX = event.x
                    touchDownY = event.y
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                val duration = System.currentTimeMillis() - lastTouchTime
                if (!isDragging && duration < 350) {
                    handleTap(event.x, event.y)
                }
                // Spring orbit back to center
                ValueAnimator.ofFloat(orbitYaw, 0f).apply {
                    this.duration = 450
                    interpolator = OvershootInterpolator(1.1f)
                    addUpdateListener {
                        orbitYaw = it.animatedValue as Float
                        postInvalidateOnAnimation()
                    }
                    start()
                }
                ValueAnimator.ofFloat(orbitPitch, 10f).apply {
                    this.duration = 450
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        orbitPitch = it.animatedValue as Float
                        postInvalidateOnAnimation()
                    }
                    start()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTap(x: Float, y: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val colW = width / 3f

        if (x < cx - dp(45f)) {
            hopLeftBud()
        } else if (x > cx + dp(45f)) {
            hopRightBud()
        } else {
            toggleCaseLid()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        val p = ThemeRes.palette(context)
        val isDark = !p.isLight

        // 1. Subtle 3D Stage Pedestal Ambient Lighting in Background
        drawPedestalGlow(canvas, cx, cy + dp(40f), isDark, p.accent)

        // 2. Setup 3D Camera with Perspective
        canvas.save()
        camera.save()
        camera.rotateX(orbitPitch)
        camera.rotateY(orbitYaw)
        camera.getMatrix(matrix3D)
        camera.restore()

        matrix3D.preTranslate(-cx, -cy)
        matrix3D.postTranslate(cx, cy)
        canvas.concat(matrix3D)

        // Harmonic Floating Offsets
        val leftFloatY = sin(animTime) * dp(10f) - leftHop * dp(28f)
        val rightFloatY = sin(animTime + 1.2f) * dp(10f) - rightHop * dp(28f)
        val leftTilt = cos(animTime) * 3.5f
        val rightTilt = -cos(animTime + 1.2f) * 3.5f

        val leftInCase = (leftStatus == 4 || leftStatus == 0) && caseOpenProgress < 0.2f
        val rightInCase = (rightStatus == 4 || rightStatus == 0) && caseOpenProgress < 0.2f

        val leftX = cx - dp(74f)
        val rightX = cx + dp(74f)
        val budsY = cy - dp(18f)
        val caseY = cy + dp(28f)

        // 3. Dynamic Floor Shadows (perspective scaled)
        drawFloorShadow(canvas, leftX, caseY + dp(22f), dp(36f), 0.65f - (leftFloatY / dp(30f)))
        drawFloorShadow(canvas, cx, caseY + dp(24f), dp(72f), 0.85f)
        drawFloorShadow(canvas, rightX, caseY + dp(22f), dp(36f), 0.65f - (rightFloatY / dp(30f)))

        // 4. Draw Soundwaves (if ANC or Ambient active)
        if (connected) {
            drawAcousticWaves(canvas, leftX, budsY + leftFloatY, true, p.accent)
            drawAcousticWaves(canvas, rightX, budsY + rightFloatY, false, p.accent)
        }

        // 5. Draw 3D Charging Case in Center
        drawChargingCase(canvas, cx, caseY, isDark, p)

        // 6. Draw Left Earbud (Floating with 3D rotation)
        if (!leftInCase) {
            canvas.save()
            canvas.translate(leftX, budsY + leftFloatY)
            canvas.rotate(leftTilt)
            drawEarbud(canvas, true, isDark, p, leftBattery, leftStatus)
            canvas.restore()
        }

        // 7. Draw Right Earbud (Floating with 3D rotation)
        if (!rightInCase) {
            canvas.save()
            canvas.translate(rightX, budsY + rightFloatY)
            canvas.rotate(rightTilt)
            drawEarbud(canvas, false, isDark, p, rightBattery, rightStatus)
            canvas.restore()
        }

        canvas.restore()
    }

    /**
     * Soft radial stage spotlight glow behind 3D components.
     */
    private fun drawPedestalGlow(canvas: Canvas, cx: Float, cy: Float, isDark: Boolean, accentColor: Int) {
        val glowRadius = dp(140f)
        val glowColor = if (connected) Color.argb(35, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
                        else Color.argb(12, 120, 130, 150)
        val shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(glowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.style = Paint.Style.FILL
        glowPaint.shader = shader
        canvas.drawCircle(cx, cy, glowRadius, glowPaint)
        glowPaint.shader = null
    }

    /**
     * Dynamic soft drop shadow on virtual floor.
     */
    private fun drawFloorShadow(canvas: Canvas, x: Float, y: Float, radius: Float, intensity: Float) {
        val alpha = (intensity.coerceIn(0.2f, 1f) * 70).toInt()
        val shadowColor = Color.argb(alpha, 0, 0, 0)
        val shader = RadialGradient(
            x, y, radius,
            intArrayOf(shadowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        shadowPaint.shader = shader
        rectF.set(x - radius, y - radius * 0.35f, x + radius, y + radius * 0.35f)
        canvas.drawOval(rectF, shadowPaint)
        shadowPaint.shader = null
    }

    /**
     * 3D Acoustic / Soundwave Rings representing ANC or Ambient Sound.
     */
    private fun drawAcousticWaves(canvas: Canvas, x: Float, y: Float, isLeft: Boolean, accentColor: Int) {
        if (ancMode == "Off") return
        val isAnc = ancMode.startsWith("ANC") || ancMode == "Smart"
        val waveColor = if (isAnc) Color.argb(120, 43, 112, 247) else Color.argb(130, 0, 229, 255)
        wavePaint.color = waveColor
        wavePaint.strokeWidth = dp(1.4f)

        val pulse = (animTime / (2f * Math.PI.toFloat()))
        for (i in 0..2) {
            val progress = (pulse + i * 0.33f) % 1f
            val rad = dp(18f) + progress * dp(24f)
            val alpha = ((1f - progress) * 160).toInt()
            wavePaint.color = Color.argb(alpha, Color.red(waveColor), Color.green(waveColor), Color.blue(waveColor))
            rectF.set(x - rad, y - rad * 0.8f, x + rad, y + rad * 0.8f)
            val startAngle = if (isLeft) -60f else 120f
            canvas.drawArc(rectF, startAngle, 120f, false, wavePaint)
        }
    }

    /**
     * High-fidelity 3D Charging Case with animated clamshell lid.
     */
    private fun drawChargingCase(canvas: Canvas, cx: Float, cy: Float, isDark: Boolean, p: Palette) {
        val caseW = dp(92f)
        val caseH = dp(64f)
        val cornerRad = dp(24f)

        // Case Base Container
        rectF.set(cx - caseW / 2, cy - caseH / 2, cx + caseW / 2, cy + caseH / 2)
        val baseGradient = LinearGradient(
            cx, cy - caseH / 2, cx, cy + caseH / 2,
            if (isDark) intArrayOf(Color.parseColor("#262B3A"), Color.parseColor("#181B26"), Color.parseColor("#0E1017"))
            else intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#EAEFF8"), Color.parseColor("#D4DCEB")),
            null, Shader.TileMode.CLAMP
        )
        bodyPaint.shader = baseGradient
        bodyPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rectF, cornerRad, cornerRad, bodyPaint)

        // Case Outer Perimeter Micro-border
        strokePaint.color = if (isDark) Color.parseColor("#384157") else Color.parseColor("#CAD4E6")
        strokePaint.strokeWidth = dp(1.2f)
        canvas.drawRoundRect(rectF, cornerRad, cornerRad, strokePaint)

        // Interior Cradle Wells (when open)
        if (caseOpenProgress > 0.1f) {
            val cradleAlpha = (caseOpenProgress * 255).toInt()
            bodyPaint.shader = null
            bodyPaint.color = Color.argb(cradleAlpha, if (isDark) 12 else 200, if (isDark) 14 else 208, if (isDark) 20 else 220)

            // Left cradle slot
            rectF.set(cx - dp(32f), cy - dp(18f), cx - dp(8f), cy + dp(12f))
            canvas.drawRoundRect(rectF, dp(12f), dp(12f), bodyPaint)

            // Right cradle slot
            rectF.set(cx + dp(8f), cy - dp(18f), cx + dp(32f), cy + dp(12f))
            canvas.drawRoundRect(rectF, dp(12f), dp(12f), bodyPaint)

            // Gold charging contact pins in wells
            val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(cradleAlpha, 220, 180, 70)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx - dp(24f), cy - dp(4f), dp(1.8f), pinPaint)
            canvas.drawCircle(cx - dp(16f), cy - dp(4f), dp(1.8f), pinPaint)
            canvas.drawCircle(cx + dp(16f), cy - dp(4f), dp(1.8f), pinPaint)
            canvas.drawCircle(cx + dp(24f), cy - dp(4f), dp(1.8f), pinPaint)
        }

        // Case Status LED Light (One UI signature pill indicator)
        val ledColor = when {
            !connected -> Color.parseColor("#4B5563")
            caseBattery > 20 -> Color.parseColor("#10B981") // Green
            else -> Color.parseColor("#F59E0B")             // Amber
        }
        val ledY = cy + dp(18f)
        bodyPaint.shader = null
        bodyPaint.color = ledColor
        canvas.drawCircle(cx, ledY, dp(2.2f), bodyPaint)

        // Subtle LED Glow
        glowPaint.color = Color.argb(90, Color.red(ledColor), Color.green(ledColor), Color.blue(ledColor))
        glowPaint.strokeWidth = dp(4f)
        canvas.drawCircle(cx, ledY, dp(4f), glowPaint)

        // Animated 3D Clamshell Lid
        canvas.save()
        val hingeY = cy - caseH / 2 + dp(6f)
        // Rotate lid on 3D hinge axis
        canvas.translate(cx, hingeY)
        val lidCamera = Camera()
        lidCamera.rotateX(-caseOpenProgress * 92f)
        val lidMatrix = Matrix()
        lidCamera.getMatrix(lidMatrix)
        canvas.concat(lidMatrix)
        canvas.translate(-cx, -hingeY)

        val lidRect = RectF(cx - caseW / 2, cy - caseH / 2, cx + caseW / 2, cy + dp(4f))
        val lidGradient = LinearGradient(
            cx, cy - caseH / 2, cx, cy + dp(4f),
            if (isDark) intArrayOf(Color.parseColor("#2F3547"), Color.parseColor("#1C202C"))
            else intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#E4EBF7")),
            null, Shader.TileMode.CLAMP
        )
        bodyPaint.shader = lidGradient
        canvas.drawRoundRect(lidRect, cornerRad, cornerRad, bodyPaint)
        strokePaint.color = if (isDark) Color.parseColor("#444F69") else Color.parseColor("#CAD4E6")
        canvas.drawRoundRect(lidRect, cornerRad, cornerRad, strokePaint)

        // Samsung Logo / Brand Accent line on Lid
        bodyPaint.shader = null
        bodyPaint.color = if (isDark) Color.parseColor("#5A657C") else Color.parseColor("#9CA3AF")
        rectF.set(cx - dp(14f), cy - dp(14f), cx + dp(14f), cy - dp(12.5f))
        canvas.drawRoundRect(rectF, dp(1f), dp(1f), bodyPaint)

        canvas.restore()
    }

    /**
     * High-fidelity 3D Wireless Earbud with Samsung Blade Light.
     */
    private fun drawEarbud(canvas: Canvas, isLeft: Boolean, isDark: Boolean, p: Palette, battery: Int, status: Int) {
        val flip = if (isLeft) 1f else -1f
        canvas.scale(flip, 1f)

        // 1. Acoustic Earbud Body / Capsule Head
        val headCx = dp(4f)
        val headCy = -dp(14f)
        val headRadius = dp(16f)

        val headGradient = RadialGradient(
            headCx - dp(4f), headCy - dp(6f), headRadius * 1.4f,
            if (isDark) intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#C8D1E0"), Color.parseColor("#343A4A"), Color.parseColor("#181B24"))
            else intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#EBF0FA"), Color.parseColor("#B0BDD4")),
            null, Shader.TileMode.CLAMP
        )
        bodyPaint.shader = headGradient
        bodyPaint.style = Paint.Style.FILL
        canvas.drawCircle(headCx, headCy, headRadius, bodyPaint)

        // Specular Gloss Highlight on Head
        bodyPaint.shader = null
        bodyPaint.color = Color.argb(140, 255, 255, 255)
        canvas.drawCircle(headCx - dp(5f), headCy - dp(6f), dp(5f), bodyPaint)

        // 2. Silicone Ear Tip (Facing Inwards towards nose)
        val tipX = headCx + dp(13f)
        val tipY = headCy - dp(1f)
        val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#475266") else Color.parseColor("#94A3B8")
            style = Paint.Style.FILL
        }
        rectF.set(tipX - dp(6f), tipY - dp(9f), tipX + dp(8f), tipY + dp(9f))
        canvas.drawOval(rectF, tipPaint)

        // 3. Aerodynamic Blade Stem (Galaxy Buds3 Pro signature design)
        tempPath.reset()
        val stemTop = headCy + dp(6f)
        val stemBottom = headCy + dp(46f)
        val stemLeft = -dp(4f)
        val stemRight = dp(6f)

        tempPath.moveTo(stemLeft, stemTop)
        tempPath.lineTo(stemRight, stemTop)
        tempPath.lineTo(stemRight - dp(1.5f), stemBottom - dp(4f))
        tempPath.quadTo(headCx, stemBottom, stemLeft + dp(1.5f), stemBottom - dp(4f))
        tempPath.close()

        val stemGradient = LinearGradient(
            0f, stemTop, 0f, stemBottom,
            if (isDark) intArrayOf(Color.parseColor("#3B4357"), Color.parseColor("#222736"), Color.parseColor("#12151E"))
            else intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#DFE6F2"), Color.parseColor("#A8B5CC")),
            null, Shader.TileMode.CLAMP
        )
        bodyPaint.shader = stemGradient
        canvas.drawPath(tempPath, bodyPaint)

        // Stem Edge Highlight
        strokePaint.color = if (isDark) Color.parseColor("#505D7A") else Color.parseColor("#CCD6E8")
        strokePaint.strokeWidth = dp(1f)
        canvas.drawPath(tempPath, strokePaint)

        // 4. Samsung Signature Blade Light (Glowing Electric LED Strip)
        if (connected) {
            val bladeX = headCx - dp(1f)
            val bladeTop = stemTop + dp(6f)
            val bladeBottom = stemBottom - dp(6f)

            // Neon Outer Glow
            glowPaint.color = Color.argb(120, 0, 229, 255)
            glowPaint.strokeWidth = dp(4.5f)
            canvas.drawLine(bladeX, bladeTop, bladeX, bladeBottom, glowPaint)

            // Crisp Inner LED Core
            glowPaint.color = Color.parseColor("#2B70F7")
            glowPaint.strokeWidth = dp(2f)
            canvas.drawLine(bladeX, bladeTop, bladeX, bladeBottom, glowPaint)

            glowPaint.color = Color.WHITE
            glowPaint.strokeWidth = dp(0.8f)
            canvas.drawLine(bladeX, bladeTop, bladeX, bladeBottom, glowPaint)
        }

        // 5. Channel Indicator ('L' or 'R')
        textPaint.color = if (isDark) Color.parseColor("#8E99AD") else Color.parseColor("#64748B")
        textPaint.textSize = dp(9f)
        canvas.drawText(if (isLeft) "L" else "R", headCx - dp(1f), headCy + dp(3.5f), textPaint)
    }
}
