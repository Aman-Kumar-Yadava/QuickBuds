package com.spizganed.quickbuds.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Samsung One UI 9 style noise control segmented switcher:
 * Deep 24dp squircle container holding equal-width segments (Noise cancelling, Off, Ambient sound, Adaptive).
 * The active segment pill smoothly slides into place with Samsung One UI electric blue fill and haptics.
 */
class AncSegmentedView(
    context: Context,
    labels: List<String>,
    iconRes: List<Int> = emptyList()
) : View(context) {

    var onSegmentTapped: ((Int) -> Unit)? = null

    var selected: Int = -1
        set(v) {
            if (v == field) return
            val from = field
            field = v
            anim?.cancel()
            if (width == 0 || from < 0 || v < 0) {
                pos = v.toFloat()
                invalidate()
                return
            }
            anim = ValueAnimator.ofFloat(pos, v.toFloat()).apply {
                duration = 240
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    pos = it.animatedValue as Float
                    postInvalidateOnAnimation()
                }
                start()
            }
        }

    private var pos = -1f
    private var anim: ValueAnimator? = null

    private fun dp(v: Float) = ThemeRes.dp(context, v).toFloat()

    private val p = ThemeRes.palette(context)
    private val labels = labels.toMutableList()
    private val icons: MutableList<Drawable> = iconRes.map { context.getDrawable(it)!!.mutate() }.toMutableList()

    fun setSegment(i: Int, label: String, iconRes: Int) {
        if (i !in labels.indices) return
        labels[i] = label
        if (i in icons.indices) icons[i] = context.getDrawable(iconRes)!!.mutate()
        invalidate()
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(if (iconRes.isEmpty()) 14f else 12f)
        textAlign = Paint.Align.CENTER
        typeface = ThemeRes.medium(context)
    }
    private val box = RectF()
    private val capBounds = Rect()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // One UI 9 generous touch target: 68dp for mode switcher, 50dp for text-only
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            dp(if (icons.isEmpty()) 50f else 68f).toInt()
        )
    }

    private fun drawTrack(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val e = dp(0.5f)
        box.set(e, e, w - e, h - e)
        val cornerRad = dp(24f)

        // Samsung One UI 9 soft container surface
        trackPaint.color = if (p.isLight) Color.parseColor("#EBF0F8") else Color.parseColor("#181B26")
        canvas.drawRoundRect(box, cornerRad, cornerRad, trackPaint)

        // Micro-border
        strokePaint.color = if (p.isLight) Color.parseColor("#DDE3F0") else Color.parseColor("#262C3D")
        canvas.drawRoundRect(box, cornerRad, cornerRad, strokePaint)
    }

    private fun drawActivePill(canvas: Canvas) {
        val h = height.toFloat()
        if (pos >= 0f) {
            val inset = dp(5f)
            val segW = (width - inset * 2) / labels.size
            val left = inset + segW * pos
            val right = left + segW
            box.set(left, inset, right, h - inset)
            val pillRad = dp(20f)

            // One UI Signature Vibrant Blue Pill
            pillPaint.shader = LinearGradient(
                left, inset, right, h - inset,
                intArrayOf(Color.parseColor("#387DF8"), Color.parseColor("#2262E6")),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(box, pillRad, pillRad, pillPaint)
            pillPaint.shader = null
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawTrack(canvas)
        drawActivePill(canvas)

        val h = height.toFloat()
        val inset = dp(5f)
        val segW = (width - inset * 2) / labels.size
        val iconSize = dp(24f)

        textPaint.getTextBounds("H", 0, 1, capBounds)
        val capH = capBounds.height().toFloat()
        val iconTop = Math.round((h - (iconSize + dp(4f) + capH)) / 2).toFloat()
        val baseline = if (icons.isEmpty()) h / 2 + capH / 2 else iconTop + iconSize + dp(5f) + capH

        labels.forEachIndexed { i, label ->
            val closeness = if (pos < 0f) 0f else (1f - kotlin.math.abs(pos - i)).coerceIn(0f, 1f)
            val isSelected = closeness > 0.5f

            val textColor = if (isSelected) Color.WHITE else p.textSecondary
            val cx = inset + segW * i + segW / 2

            icons.getOrNull(i)?.run {
                setTint(textColor)
                setBounds(
                    (cx - iconSize / 2).toInt(), iconTop.toInt(),
                    (cx + iconSize / 2).toInt(), (iconTop + iconSize).toInt()
                )
                draw(canvas)
            }

            textPaint.color = textColor
            textPaint.typeface = if (isSelected) ThemeRes.bold(context) else ThemeRes.medium(context)
            canvas.drawText(label, cx, baseline, textPaint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!isEnabled) return false
        if (e.actionMasked == MotionEvent.ACTION_UP) {
            val i = ((e.x / width) * labels.size).toInt().coerceIn(0, labels.size - 1)
            performClick()
            Haptics.commit(this)
            onSegmentTapped?.invoke(i)
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
