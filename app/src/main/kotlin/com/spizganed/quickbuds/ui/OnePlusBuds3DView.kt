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
 * Ultra-premium 3D interactive view for OnePlus Nord Buds 4 Pro and OnePlus Buds Pro 3.
 * Features:
 * - Dual-tone electroplated mirror chrome stems with realistic metallic reflection bands.
 * - Satin matte acoustic chamber with diffuse ambient occlusion.
 * - Ergonomic pebble charging case with articulated 3D clamshell lid (tap to open/close).
 * - Real-time 3D camera projection (orbit, drag, pitch & yaw with spring-back physics).
 * - Harmonic floating physics with dynamic height-scaled floor shadows.
 * - Interactive model toggle: OnePlus Nord Buds 4 Pro vs OnePlus Buds Pro 3.
 * - 3D soundwave rings for ANC / Transparency modes.
 */
class OnePlusBuds3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private fun dp(v: Float) = ThemeRes.dp(context, v).toFloat()

    // Model mode: 0 = OnePlus Nord Buds 4 Pro, 1 = OnePlus Buds Pro 3 (Flagship)
    var modelMode: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var connected: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var ancMode: String = "Off"
        set(value) {
            field = value
            invalidate()
        }

    var leftBattery: Int = 100
    var caseBattery: Int = 70
    var rightBattery: Int = 100
    var leftStatus: Int = 3
    var rightStatus: Int = 3

    var onComponentTapped: ((String) -> Unit)? = null

    // 3D Camera & Matrix
    private val camera = Camera()
    private val matrix3D = Matrix()

    // Orbit State
    private var orbitYaw = 0f
    private var orbitPitch = 12f
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDragging = false
    private var lastTouchTime = 0L

    // Case Lid State (1f = open, 0f = closed)
    var caseOpenProgress = 1f
        private set
    private var lidAnimator: ValueAnimator? = null

    // Hop Animations for earbud selection
    private var leftHop = 0f
    private var rightHop = 0f

    // Continuous Floating Animation
    private var animTime = 0f
    private val floatAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3400
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            animTime = (it.animatedValue as Float) * (2f * Math.PI.toFloat())
            postInvalidateOnAnimation()
        }
    }

    // Drawing Paints
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val chromePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private val rectF = RectF()
    private val stemPath = Path()

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
        val h = dp(230f).toInt()
        setMeasuredDimension(w, h)
    }

    fun toggleCaseLid() {
        val target = if (caseOpenProgress > 0.5f) 0f else 1f
        lidAnimator?.cancel()
        lidAnimator = ValueAnimator.ofFloat(caseOpenProgress, target).apply {
            duration = 440
            interpolator = if (target > 0.5f) OvershootInterpolator(1.25f) else DecelerateInterpolator()
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
            duration = 340
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
            duration = 340
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
                    orbitYaw = (orbitYaw + dx * 0.45f).coerceIn(-42f, 42f)
                    orbitPitch = (orbitPitch - dy * 0.35f).coerceIn(-12f, 35f)
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
                // Spring back
                ValueAnimator.ofFloat(orbitYaw, 0f).apply {
                    this.duration = 450
                    interpolator = OvershootInterpolator(1.15f)
                    addUpdateListener {
                        orbitYaw = it.animatedValue as Float
                        postInvalidateOnAnimation()
                    }
                    start()
                }
                ValueAnimator.ofFloat(orbitPitch, 12f).apply {
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
        val isLight = p.isLight

        // 1. Ambient Spotlight behind earbuds
        val spotlightColor = if (connected) (if (isLight) Color.argb(40, 0, 122, 255) else Color.argb(55, 30, 100, 255))
                             else Color.argb(15, 120, 130, 150)
        glowPaint.style = Paint.Style.FILL
        glowPaint.shader = RadialGradient(
            cx, cy + dp(20f), dp(130f),
            intArrayOf(spotlightColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy + dp(20f), dp(130f), glowPaint)
        glowPaint.shader = null

        // 2. Setup 3D Projection Matrix
        canvas.save()
        camera.save()
        camera.rotateX(orbitPitch)
        camera.rotateY(orbitYaw)
        camera.getMatrix(matrix3D)
        camera.restore()

        matrix3D.preTranslate(-cx, -cy)
        matrix3D.postTranslate(cx, cy)
        canvas.concat(matrix3D)

        // Levitation math
        val leftFloatY = sin(animTime) * dp(10f) - leftHop * dp(28f)
        val rightFloatY = sin(animTime + 1.3f) * dp(10f) - rightHop * dp(28f)
        val leftTilt = cos(animTime) * 3.5f
        val rightTilt = -cos(animTime + 1.3f) * 3.5f

        val leftX = cx - dp(76f)
        val rightX = cx + dp(76f)
        val budsY = cy - dp(18f)
        val caseY = cy + dp(32f)

        // 3. Dynamic Floor Drop Shadows
        drawShadow(canvas, leftX, caseY + dp(22f), dp(36f), 0.7f - (leftFloatY / dp(30f)))
        drawShadow(canvas, cx, caseY + dp(24f), dp(75f), 0.85f)
        drawShadow(canvas, rightX, caseY + dp(22f), dp(36f), 0.7f - (rightFloatY / dp(30f)))

        // 4. Acoustic Waves (ANC / Transparency)
        if (connected) {
            drawAcousticWaves(canvas, leftX, budsY + leftFloatY, true)
            drawAcousticWaves(canvas, rightX, budsY + rightFloatY, false)
        }

        // 5. Draw OnePlus Pebble Charging Case in Center
        drawPebbleCase(canvas, cx, caseY, isLight)

        // 6. Draw Left Earbud (Floating in 3D space)
        canvas.save()
        canvas.translate(leftX, budsY + leftFloatY)
        canvas.rotate(leftTilt)
        drawEarbud(canvas, true, isLight)
        canvas.restore()

        // 7. Draw Right Earbud (Floating in 3D space)
        canvas.save()
        canvas.translate(rightX, budsY + rightFloatY)
        canvas.rotate(rightTilt)
        drawEarbud(canvas, false, isLight)
        canvas.restore()

        canvas.restore()
    }

    private fun drawShadow(canvas: Canvas, x: Float, y: Float, radius: Float, intensity: Float) {
        val alpha = (intensity.coerceIn(0.2f, 1f) * 65).toInt()
        val shadowColor = Color.argb(alpha, 0, 0, 0)
        shadowPaint.shader = RadialGradient(
            x, y, radius,
            intArrayOf(shadowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        rectF.set(x - radius, y - radius * 0.36f, x + radius, y + radius * 0.36f)
        canvas.drawOval(rectF, shadowPaint)
        shadowPaint.shader = null
    }

    private fun drawAcousticWaves(canvas: Canvas, x: Float, y: Float, isLeft: Boolean) {
        if (ancMode == "Off") return
        val isAnc = ancMode.startsWith("ANC") || ancMode == "Smart"
        val waveColor = if (isAnc) Color.parseColor("#007AFF") else Color.parseColor("#10B981")
        wavePaint.strokeWidth = dp(1.5f)

        val pulse = (animTime / (2f * Math.PI.toFloat()))
        for (i in 0..2) {
            val progress = (pulse + i * 0.33f) % 1f
            val rad = dp(20f) + progress * dp(26f)
            val alpha = ((1f - progress) * 150).toInt()
            wavePaint.color = Color.argb(alpha, Color.red(waveColor), Color.green(waveColor), Color.blue(waveColor))
            rectF.set(x - rad, y - rad * 0.8f, x + rad, y + rad * 0.8f)
            val startAngle = if (isLeft) -60f else 120f
            canvas.drawArc(rectF, startAngle, 120f, false, wavePaint)
        }
    }

    /**
     * OnePlus Signature Ergonomic Pebble Case with 3D Clamshell Hinge.
     */
    private fun drawPebbleCase(canvas: Canvas, cx: Float, cy: Float, isLight: Boolean) {
        val caseW = dp(96f)
        val caseH = dp(62f)
        val cornerRad = dp(26f)

        // Case Base
        rectF.set(cx - caseW / 2, cy - caseH / 2, cx + caseW / 2, cy + caseH / 2)
        val baseGradient = LinearGradient(
            cx, cy - caseH / 2, cx, cy + caseH / 2,
            if (isLight) intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#E5E9F2"), Color.parseColor("#CBD5E1"))
            else intArrayOf(Color.parseColor("#262933"), Color.parseColor("#1A1C24"), Color.parseColor("#101217")),
            null, Shader.TileMode.CLAMP
        )
        bodyPaint.shader = baseGradient
        bodyPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(rectF, cornerRad, cornerRad, bodyPaint)

        // Polished Metallic Seam Rim
        strokePaint.color = if (isLight) Color.parseColor("#B0BEC5") else Color.parseColor("#475569")
        strokePaint.strokeWidth = dp(1.2f)
        canvas.drawRoundRect(rectF, cornerRad, cornerRad, strokePaint)

        // Interior Cradle Wells (when open)
        if (caseOpenProgress > 0.1f) {
            val alpha = (caseOpenProgress * 255).toInt()
            bodyPaint.shader = null
            bodyPaint.color = Color.argb(alpha, if (isLight) 210 else 18, if (isLight) 215 else 20, if (isLight) 225 else 26)

            // Left & Right wells
            rectF.set(cx - dp(34f), cy - dp(18f), cx - dp(8f), cy + dp(12f))
            canvas.drawRoundRect(rectF, dp(12f), dp(12f), bodyPaint)

            rectF.set(cx + dp(8f), cy - dp(18f), cx + dp(34f), cy + dp(12f))
            canvas.drawRoundRect(rectF, dp(12f), dp(12f), bodyPaint)

            // Dual Gold Pogo Charging Pins
            val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(alpha, 225, 185, 75)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx - dp(26f), cy - dp(4f), dp(1.8f), pinPaint)
            canvas.drawCircle(cx - dp(16f), cy - dp(4f), dp(1.8f), pinPaint)
            canvas.drawCircle(cx + dp(16f), cy - dp(4f), dp(1.8f), pinPaint)
            canvas.drawCircle(cx + dp(26f), cy - dp(4f), dp(1.8f), pinPaint)
        }

        // Center Front Battery LED Indicator
        val ledColor = when {
            !connected -> Color.parseColor("#64748B")
            caseBattery > 20 -> Color.parseColor("#10B981") // Green
            else -> Color.parseColor("#F59E0B")             // Amber
        }
        bodyPaint.shader = null
        bodyPaint.color = ledColor
        canvas.drawCircle(cx, cy + dp(17f), dp(2.2f), bodyPaint)
        glowPaint.color = Color.argb(85, Color.red(ledColor), Color.green(ledColor), Color.blue(ledColor))
        glowPaint.strokeWidth = dp(4f)
        canvas.drawCircle(cx, cy + dp(17f), dp(4f), glowPaint)

        // 3D Articulated Pebble Lid
        canvas.save()
        val hingeY = cy - caseH / 2 + dp(6f)
        canvas.translate(cx, hingeY)
        val lidCam = Camera()
        lidCam.rotateX(-caseOpenProgress * 92f)
        val lidMat = Matrix()
        lidCam.getMatrix(lidMat)
        canvas.concat(lidMat)
        canvas.translate(-cx, -hingeY)

        val lidRect = RectF(cx - caseW / 2, cy - caseH / 2, cx + caseW / 2, cy + dp(4f))
        bodyPaint.shader = LinearGradient(
            cx, cy - caseH / 2, cx, cy + dp(4f),
            if (isLight) intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#F1F5F9"), Color.parseColor("#E2E8F0"))
            else intArrayOf(Color.parseColor("#333745"), Color.parseColor("#222530")),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(lidRect, cornerRad, cornerRad, bodyPaint)
        strokePaint.color = if (isLight) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
        canvas.drawRoundRect(lidRect, cornerRad, cornerRad, strokePaint)

        // OnePlus Minimal Metallic Badge on Lid
        bodyPaint.shader = null
        bodyPaint.color = if (isLight) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
        rectF.set(cx - dp(14f), cy - dp(14f), cx + dp(14f), cy - dp(12.5f))
        canvas.drawRoundRect(rectF, dp(1f), dp(1f), bodyPaint)

        canvas.restore()
    }

    /**
     * OnePlus Earbud with Electroplated Mirror Chrome Stem and Matte Acoustic Chamber.
     */
    private fun drawEarbud(canvas: Canvas, isLeft: Boolean, isLight: Boolean) {
        val flip = if (isLeft) 1f else -1f
        canvas.scale(flip, 1f)

        val headCx = dp(4f)
        val headCy = -dp(14f)
        val headRadius = dp(17f)

        // 1. Acoustic Chamber (Matte Satin Obsidian/Charcoal or Pearl White)
        val headGradient = RadialGradient(
            headCx - dp(5f), headCy - dp(6f), headRadius * 1.5f,
            if (isLight) intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#E2E8F0"), Color.parseColor("#CBD5E1"))
            else intArrayOf(Color.parseColor("#475569"), Color.parseColor("#1E293B"), Color.parseColor("#0F172A")),
            null, Shader.TileMode.CLAMP
        )
        bodyPaint.shader = headGradient
        bodyPaint.style = Paint.Style.FILL
        canvas.drawCircle(headCx, headCy, headRadius, bodyPaint)

        // Specular highlight on matte chamber
        bodyPaint.shader = null
        bodyPaint.color = Color.argb(120, 255, 255, 255)
        canvas.drawCircle(headCx - dp(5f), headCy - dp(6f), dp(4.5f), bodyPaint)

        // 2. Translucent Silicone Ear Tip
        val tipX = headCx + dp(14f)
        val tipY = headCy - dp(1f)
        val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#94A3B8") else Color.parseColor("#334155")
            style = Paint.Style.FILL
        }
        rectF.set(tipX - dp(6f), tipY - dp(9.5f), tipX + dp(8f), tipY + dp(9.5f))
        canvas.drawOval(rectF, tipPaint)

        // Acoustic Mesh Filter
        val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#475569") else Color.parseColor("#0F172A")
        }
        canvas.drawCircle(tipX + dp(4f), tipY, dp(2f), meshPaint)

        // 3. Mirror-Finish Electroplated Chrome Stem (Liquid Metal Finish)
        stemPath.reset()
        val stemTop = headCy + dp(6f)
        val stemBottom = headCy + dp(46f)
        val stemLeft = -dp(4.5f)
        val stemRight = dp(6.5f)

        stemPath.moveTo(stemLeft, stemTop)
        stemPath.lineTo(stemRight, stemTop)
        stemPath.lineTo(stemRight - dp(1.5f), stemBottom - dp(4f))
        stemPath.quadTo(headCx, stemBottom, stemLeft + dp(1.5f), stemBottom - dp(4f))
        stemPath.close()

        // Multi-stop Metallic Chrome Gradient (giving that liquid chrome look!)
        val chromeGradient = LinearGradient(
            stemLeft, 0f, stemRight, 0f,
            intArrayOf(
                Color.parseColor("#94A3B8"),
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#CBD5E1"),
                Color.parseColor("#475569"),
                Color.parseColor("#E2E8F0"),
                Color.parseColor("#64748B")
            ),
            floatArrayOf(0f, 0.2f, 0.45f, 0.7f, 0.88f, 1f),
            Shader.TileMode.CLAMP
        )
        chromePaint.shader = chromeGradient
        chromePaint.style = Paint.Style.FILL
        canvas.drawPath(stemPath, chromePaint)

        // Chrome Edge Bevel
        strokePaint.color = Color.parseColor("#E2E8F0")
        strokePaint.strokeWidth = dp(0.8f)
        canvas.drawPath(stemPath, strokePaint)

        // 4. Capacitive Pinch / Squeeze Groove on Stem
        val grooveY = stemTop + dp(14f)
        val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 30, 41, 59)
            style = Paint.Style.FILL
        }
        rectF.set(headCx - dp(2f), grooveY, headCx + dp(2f), grooveY + dp(10f))
        canvas.drawRoundRect(rectF, dp(2f), dp(2f), groovePaint)

        // 5. Model-specific Accent:
        // Nord Buds 4 Pro: Electric Blue / Cyan LED Blade strip
        // Buds Pro 3: Gold Accent Rim on Head
        if (modelMode == 0 && connected) {
            // Cyan Blade Light (Nord Buds 4 Pro)
            glowPaint.color = Color.parseColor("#00E5FF")
            glowPaint.strokeWidth = dp(1.8f)
            canvas.drawLine(headCx - dp(0.5f), stemTop + dp(6f), headCx - dp(0.5f), stemBottom - dp(6f), glowPaint)
        } else if (modelMode == 1) {
            // Flagship Gold Mic Ring (OnePlus Buds Pro 3 Flagship)
            val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EAB308")
                style = Paint.Style.STROKE
                strokeWidth = dp(1.2f)
            }
            canvas.drawCircle(headCx, headCy, headRadius - dp(1f), goldPaint)
        } else if (modelMode == 2 && connected) {
            // Samsung Galaxy Blade Light (One UI 9)
            glowPaint.color = Color.parseColor("#3B82F6")
            glowPaint.strokeWidth = dp(2.2f)
            canvas.drawLine(headCx, stemTop + dp(4f), headCx, stemBottom - dp(4f), glowPaint)
        }

        // 6. Channel Stamp ('L' or 'R')
        textPaint.color = Color.parseColor("#64748B")
        textPaint.textSize = dp(9f)
        canvas.drawText(if (isLeft) "L" else "R", headCx, headCy + dp(3.5f), textPaint)
    }
}
