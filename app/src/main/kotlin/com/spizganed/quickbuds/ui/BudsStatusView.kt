package com.spizganed.quickbuds.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.spizganed.quickbuds.R

/**
 * OxygenOS / OnePlus Showcase:
 * 1. Model switch chip (OnePlus Nord Buds 4 Pro / OnePlus Buds Pro 3).
 * 2. Ultra-premium 3D animated interactive Earbuds & Pebble Case (OnePlusBuds3DView).
 * 3. Exact screenshot Horseshoe Battery Gauges (OnePlusHorseshoeGaugeView) with Left, Case, Right.
 */
class BudsStatusView(context: Context) : LinearLayout(context) {

    private fun dp(v: Float) = ThemeRes.dp(context, v)
    private val p = ThemeRes.palette(context)

    val buds3DView: OnePlusBuds3DView = OnePlusBuds3DView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(230f))
    }

    val horseshoeGaugeView: OnePlusHorseshoeGaugeView = OnePlusHorseshoeGaugeView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6f)
            bottomMargin = dp(4f)
        }
    }

    var connected: Boolean = true
        set(value) {
            field = value
            buds3DView.connected = value
            horseshoeGaugeView.connected = value
        }

    private val modelChipText: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        // 1. Model Switch Chip: OnePlus Nord Buds 4 Pro vs Buds Pro 3
        val chipContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(6f), dp(14f), dp(6f))
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6f)
                bottomMargin = dp(2f)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16f).toFloat()
                setColor(if (p.isLight) Color.parseColor("#E5E9F2") else Color.parseColor("#1E2230"))
            }
            isClickable = true
            isFocusable = true
            ThemeRes.sinkOnPress(this)
            setOnClickListener {
                buds3DView.modelMode = (buds3DView.modelMode + 1) % 3
                updateModelText()
                Haptics.commit(it)
            }
        }

        val chipIcon = ImageView(context).apply {
            layoutParams = LayoutParams(dp(16f), dp(16f)).apply { marginEnd = dp(6f) }
            setImageResource(R.drawable.ic_earbud)
            setColorFilter(if (p.isLight) Color.parseColor("#007AFF") else Color.parseColor("#38BDF8"))
        }
        chipContainer.addView(chipIcon)

        modelChipText = TextView(context).apply {
            text = "OnePlus Nord Buds 4 Pro ▾"
            textSize = 12.5f
            typeface = ThemeRes.medium(context)
            setTextColor(if (p.isLight) Color.parseColor("#1F2937") else Color.WHITE)
        }
        chipContainer.addView(modelChipText)
        addView(chipContainer)

        // 2. 3D Earbuds & Case View
        addView(buds3DView)

        // 3. Horseshoe Battery Gauges (Left, Case, Right)
        addView(horseshoeGaugeView)

        // Tapping gauge triggers corresponding 3D hop/lid
        horseshoeGaugeView.onGaugeTapped = { slot ->
            when (slot) {
                0 -> buds3DView.hopLeftBud()
                1 -> buds3DView.toggleCaseLid()
                2 -> buds3DView.hopRightBud()
            }
        }
    }

    private fun updateModelText() {
        modelChipText.text = when (buds3DView.modelMode) {
            0 -> "OnePlus Nord Buds 4 Pro ▾"
            1 -> "OnePlus Buds Pro 3 (Flagship) ▾"
            else -> "Galaxy Buds3 Pro (One UI 9) ▾"
        }
    }

    fun setState(left: Int, case: Int, right: Int, leftStatus: Int, rightStatus: Int) {
        buds3DView.leftBattery = left
        buds3DView.caseBattery = case
        buds3DView.rightBattery = right
        buds3DView.leftStatus = leftStatus
        buds3DView.rightStatus = rightStatus
        buds3DView.invalidate()

        horseshoeGaugeView.leftBattery = left
        horseshoeGaugeView.caseBattery = case
        horseshoeGaugeView.rightBattery = right
        horseshoeGaugeView.leftStatus = leftStatus
        horseshoeGaugeView.rightStatus = rightStatus
        horseshoeGaugeView.chargingLeft = leftStatus == 4 || leftStatus == 0
        horseshoeGaugeView.chargingRight = rightStatus == 4 || rightStatus == 0
    }

    companion object {
        fun wearTint(p: Palette, isCase: Boolean, status: Int): Int = when {
            isCase || status == 3 || status == 7 -> p.text
            status == 4 || status == 0 -> Palette.withAlpha(p.textSecondary, 0.45f)
            else -> p.textSecondary
        }
    }
}
