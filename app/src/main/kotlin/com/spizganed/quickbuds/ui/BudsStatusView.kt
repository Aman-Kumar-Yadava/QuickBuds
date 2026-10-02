package com.spizganed.quickbuds.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.spizganed.quickbuds.R

/**
 * Samsung One UI 9 style earbuds showcase:
 * 1. 3D interactive animated view of Left Bud, Right Bud, and Charging Case.
 * 2. Three sleek Samsung One UI 9 battery capsules (Left, Case, Right) with
 *    circular battery level indicators, bold percentage text, and wear state badges.
 */
class BudsStatusView(context: Context) : LinearLayout(context) {

    private fun dp(v: Float) = ThemeRes.dp(context, v)
    private val p = ThemeRes.palette(context)

    val buds3DView: SamsungBuds3DView = SamsungBuds3DView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(220f))
    }

    var connected: Boolean = true
        set(value) {
            field = value
            buds3DView.connected = value
            updateCapsules()
        }

    private var leftLevel = -1
    private var caseLevel = -1
    private var rightLevel = -1
    private var leftStat = -1
    private var rightStat = -1

    // Three One UI 9 Capsules
    private lateinit var leftCapsule: LinearLayout
    private lateinit var caseCapsule: LinearLayout
    private lateinit var rightCapsule: LinearLayout

    private lateinit var leftPctText: TextView
    private lateinit var casePctText: TextView
    private lateinit var rightPctText: TextView

    private lateinit var leftBadgeText: TextView
    private lateinit var caseBadgeText: TextView
    private lateinit var rightBadgeText: TextView

    private lateinit var leftIconView: ImageView
    private lateinit var caseIconView: ImageView
    private lateinit var rightIconView: ImageView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        // 1. 3D Animated Earbuds & Case View
        addView(buds3DView)

        // 2. Battery Capsules Container
        val capsulesRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8f)
                bottomMargin = dp(4f)
                marginStart = dp(12f)
                marginEnd = dp(12f)
            }
        }

        leftCapsule = createCapsule(R.drawable.ic_bud_left, context.getString(R.string.status_left)) {
            buds3DView.hopLeftBud()
        }
        caseCapsule = createCapsule(R.drawable.ic_case, context.getString(R.string.status_case)) {
            buds3DView.toggleCaseLid()
        }
        rightCapsule = createCapsule(R.drawable.ic_bud_right, context.getString(R.string.status_right)) {
            buds3DView.hopRightBud()
        }

        leftPctText = leftCapsule.findViewWithTag("pct")
        casePctText = caseCapsule.findViewWithTag("pct")
        rightPctText = rightCapsule.findViewWithTag("pct")

        leftBadgeText = leftCapsule.findViewWithTag("badge")
        caseBadgeText = caseCapsule.findViewWithTag("badge")
        rightBadgeText = rightCapsule.findViewWithTag("badge")

        leftIconView = leftCapsule.findViewWithTag("icon")
        caseIconView = caseCapsule.findViewWithTag("icon")
        rightIconView = rightCapsule.findViewWithTag("icon")

        capsulesRow.addView(leftCapsule, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6f) })
        capsulesRow.addView(caseCapsule, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3f); marginEnd = dp(3f) })
        capsulesRow.addView(rightCapsule, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6f) })

        addView(capsulesRow)
    }

    private fun createCapsule(iconRes: Int, label: String, onClick: () -> Unit): LinearLayout {
        val capsule = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
            background = createCapsuleBackground()
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            ThemeRes.sinkOnPress(this)
        }

        // Top Row: Icon + Percentage
        val topRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        val icon = ImageView(context).apply {
            layoutParams = LayoutParams(dp(20f), dp(20f)).apply { marginEnd = dp(6f) }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(iconRes)
            tag = "icon"
        }
        topRow.addView(icon)

        val pct = TextView(context).apply {
            text = "—"
            textSize = 15f
            typeface = ThemeRes.bold(context)
            setTextColor(p.text)
            tag = "pct"
        }
        topRow.addView(pct)
        capsule.addView(topRow)

        // Bottom Row: Status Badge (e.g. "In ear", "In case", "Left")
        val badge = TextView(context).apply {
            text = label
            textSize = 11.5f
            typeface = ThemeRes.medium(context)
            setTextColor(p.textSecondary)
            tag = "badge"
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4f)
            }
        }
        capsule.addView(badge)

        return capsule
    }

    private fun createCapsuleBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20f).toFloat()
            setColor(if (p.isLight) Color.parseColor("#EBF0F8") else Color.parseColor("#181B26"))
            setStroke(dp(1f), if (p.isLight) Color.parseColor("#DDE3F0") else Color.parseColor("#262C3D"))
        }
    }

    fun setState(left: Int, case: Int, right: Int, leftStatus: Int, rightStatus: Int) {
        leftLevel = left
        caseLevel = case
        rightLevel = right
        leftStat = leftStatus
        rightStat = rightStatus

        buds3DView.leftBattery = left
        buds3DView.caseBattery = case
        buds3DView.rightBattery = right
        buds3DView.leftStatus = leftStatus
        buds3DView.rightStatus = rightStatus
        buds3DView.invalidate()

        updateCapsules()
    }

    private fun updateCapsules() {
        if (!connected) {
            leftPctText.text = "—"
            casePctText.text = "—"
            rightPctText.text = "—"

            leftBadgeText.text = context.getString(R.string.status_left)
            caseBadgeText.text = context.getString(R.string.status_case)
            rightBadgeText.text = context.getString(R.string.status_right)

            leftIconView.setColorFilter(p.disabled)
            caseIconView.setColorFilter(p.disabled)
            rightIconView.setColorFilter(p.disabled)
            return
        }

        leftPctText.text = if (leftLevel >= 0) "$leftLevel%" else "—"
        casePctText.text = if (caseLevel >= 0) "$caseLevel%" else "—"
        rightPctText.text = if (rightLevel >= 0) "$rightLevel%" else "—"

        leftBadgeText.text = wearLabel(leftStat) ?: context.getString(R.string.status_left)
        caseBadgeText.text = context.getString(R.string.status_case)
        rightBadgeText.text = wearLabel(rightStat) ?: context.getString(R.string.status_right)

        // Accent tint if in ear
        val leftInEar = leftStat == 3 || leftStat == 7
        val rightInEar = rightStat == 3 || rightStat == 7

        leftBadgeText.setTextColor(if (leftInEar) p.accent else p.textSecondary)
        rightBadgeText.setTextColor(if (rightInEar) p.accent else p.textSecondary)

        leftIconView.setColorFilter(if (leftInEar) p.accent else p.text)
        caseIconView.setColorFilter(p.text)
        rightIconView.setColorFilter(if (rightInEar) p.accent else p.text)
    }

    private fun wearLabel(status: Int) = when (status) {
        3, 7 -> context.getString(R.string.status_in_ear)
        4, 0 -> context.getString(R.string.status_in_case)
        -1 -> null
        else -> context.getString(R.string.status_out)
    }

    companion object {
        fun wearTint(p: Palette, isCase: Boolean, status: Int): Int = when {
            isCase || status == 3 || status == 7 -> p.text
            status == 4 || status == 0 -> Palette.withAlpha(p.textSecondary, 0.45f)
            else -> p.textSecondary
        }
    }
}
