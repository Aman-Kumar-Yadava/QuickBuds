package com.spizganed.quickbuds.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.spizganed.quickbuds.R

/**
 * OxygenOS / OnePlus style horseshoe battery gauges, exactly as shown in the screenshot:
 * Three horseshoe arc gauges (Left, Case, Right):
 * - Vivid emerald green battery arc (#10B981) sweeping around top
 * - High-res vector earbud/case glyph in center
 * - Clean label: "Left 100%", "Case 70%", "Right 100%"
 * - Dynamic charging bolt icon when charging
 */
class OnePlusHorseshoeGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private fun dp(v: Float) = ThemeRes.dp(context, v).toFloat()

    var connected: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    // Battery levels (-1 = unknown)
    var leftBattery: Int = 100
        set(value) { field = value; animateLevel(0, value) }
    var caseBattery: Int = 70
        set(value) { field = value; animateLevel(1, value) }
    var rightBattery: Int = 100
        set(value) { field = value; animateLevel(2, value) }

    // Charging states
    var chargingLeft: Boolean = false
        set(value) { field = value; invalidate() }
    var chargingCase: Boolean = false
        set(value) { field = value; invalidate() }
    var chargingRight: Boolean = false
        set(value) { field = value; invalidate() }

    // Wear status: 3/7 = wearing, 4/0 = in case
    var leftStatus: Int = 3
    var rightStatus: Int = 3

    var onGaugeTapped: ((Int) -> Unit)? = null

    private val animatedFractions = floatArrayOf(1f, 0.7f, 1f)
    private val animators = arrayOfNulls<ValueAnimator>(3)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val arcBounds = RectF()

    private val leftIcon = context.getDrawable(R.drawable.ic_bud_left)!!.mutate()
    private val caseIcon = context.getDrawable(R.drawable.ic_case)!!.mutate()
    private val rightIcon = context.getDrawable(R.drawable.ic_bud_right)!!.mutate()

    init {
        isClickable = true
    }

    private fun animateLevel(index: Int, target: Int) {
        val targetFraction = if (target < 0) 0f else (target / 100f).coerceIn(0f, 1f)
        animators[index]?.cancel()
        animators[index] = ValueAnimator.ofFloat(animatedFractions[index], targetFraction).apply {
            duration = 450
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animatedFractions[index] = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        // Plush height for horseshoe arcs + labels exactly like screenshot
        val h = dp(100f).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val colW = w / 3f

        val p = ThemeRes.palette(context)
        val isLight = p.isLight

        val levels = intArrayOf(leftBattery, caseBattery, rightBattery)
        val chargings = booleanArrayOf(chargingLeft, chargingCase, chargingRight)
        val names = arrayOf(
            context.getString(R.string.status_left),
            context.getString(R.string.status_case),
            context.getString(R.string.status_right)
        )

        val gaugeRadius = dp(27f)
        val strokeW = dp(5.5f)
        trackPaint.strokeWidth = strokeW
        gaugePaint.strokeWidth = strokeW

        // Horseshoe angles: starts at 140°, sweeps 260° across the top to 40°
        val startAngle = 140f
        val sweepMax = 260f

        val cy = dp(36f)

        for (i in 0..2) {
            val cx = colW * i + colW / 2f
            arcBounds.set(cx - gaugeRadius, cy - gaugeRadius, cx + gaugeRadius, cy + gaugeRadius)

            // 1. Background Track Arc (Soft Gray)
            trackPaint.color = if (isLight) Color.parseColor("#E5E7EB") else Color.parseColor("#272A36")
            canvas.drawArc(arcBounds, startAngle, sweepMax, false, trackPaint)

            // 2. Active Progress Arc (OnePlus Signature Emerald Green #10B981 / #00C853)
            val level = levels[i]
            val fraction = animatedFractions[i]
            if (connected && level >= 0 && fraction > 0f) {
                val gaugeColor = when {
                    level <= 20 -> Color.parseColor("#EF4444") // Red
                    level <= 40 -> Color.parseColor("#F59E0B") // Amber
                    else -> Color.parseColor("#10B981")        // Emerald Green (like screenshot)
                }
                gaugePaint.color = gaugeColor
                canvas.drawArc(arcBounds, startAngle, sweepMax * fraction, false, gaugePaint)
            }

            // 3. Center Earbud / Case Icon
            val icon = when (i) { 0 -> leftIcon; 1 -> caseIcon; else -> rightIcon }
            val iconSize = dp(if (i == 1) 22f else 18f)
            val iconColor = if (connected) (if (isLight) Color.parseColor("#1F2937") else Color.WHITE) else p.disabled
            icon.setTint(iconColor)
            icon.setBounds(
                (cx - iconSize / 2).toInt(),
                (cy - iconSize / 2).toInt(),
                (cx + iconSize / 2).toInt(),
                (cy + iconSize / 2).toInt()
            )
            icon.draw(canvas)

            // 4. Label below gauge (e.g. "Left 100%", "Case 70%", "Right 100%")
            val pctStr = if (!connected || level < 0) "—" else "$level%"
            val bolt = if (chargings[i]) " ⚡" else ""
            val fullLabel = "${names[i]} $pctStr$bolt"

            labelPaint.textSize = dp(13.5f)
            labelPaint.color = if (connected) (if (isLight) Color.parseColor("#111827") else Color.parseColor("#F3F4F6")) else p.textSecondary
            canvas.drawText(fullLabel, cx, cy + gaugeRadius + dp(18f), labelPaint)
        }
    }
}
