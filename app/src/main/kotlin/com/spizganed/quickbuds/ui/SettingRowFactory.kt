package com.spizganed.quickbuds.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.spizganed.quickbuds.R

/**
 * Builds the rows inside Samsung One UI 9 style settings cards:
 * - 40x40dp squircle icon container with vibrant category tint.
 * - Inset dividers (starting under text column, 68dp inset from start).
 * - Plush 26dp rounded squircle cards.
 * - One UI 9 smooth toggles, chevrons, and typography.
 */
object SettingRowFactory {

    fun build(
        context: Context,
        iconRes: Int,
        titleRes: Int,
        subtitleRes: Int,
        trailing: View?,
        value: View? = null,
        minHeightDp: Float = 66f,
        leading: View? = null,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        val dp = { v: Float -> ThemeRes.dp(context, v) }
        val p = ThemeRes.palette(context)

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            minimumHeight = dp(minHeightDp)
            setPadding(dp(16f), dp(10f), dp(16f), dp(10f))
            if (onClick != null) {
                background = ThemeRes.ripple(context)
                setOnClickListener { onClick() }
            }
        }

        if (leading != null) {
            row.addView(leading, (leading.layoutParams as? LinearLayout.LayoutParams
                ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            ).apply { marginEnd = dp(14f) })
        }

        // Samsung One UI 9 Squircle Icon Badge Container
        if (iconRes != 0) {
            val (badgeBg, badgeIconColor) = getCategoryBadgeColors(iconRes, p.isLight)
            val iconContainer = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40f), dp(40f)).apply {
                    marginEnd = dp(14f)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(13f).toFloat()
                    setColor(badgeBg)
                }
                val iv = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(22f), dp(22f), Gravity.CENTER)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(ThemeRes.tint(context, iconRes, badgeIconColor))
                    contentDescription = ""
                    tag = ICON_TAG
                }
                addView(iv)
            }
            row.addView(iconContainer)
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            tag = TEXT_TAG
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textColumn.addView(TextView(context).apply {
            if (titleRes != 0) setText(titleRes)
            tag = TITLE_TAG
            setTextColor(p.text)
            textSize = 15.5f
            typeface = ThemeRes.medium(context)
        })
        if (subtitleRes != 0) {
            textColumn.addView(TextView(context).apply {
                setText(subtitleRes)
                setTextColor(p.textSecondary)
                textSize = 12.5f
                setPadding(0, dp(2f), 0, 0)
                tag = SUBTITLE_TAG
            })
        }
        row.addView(textColumn)

        if (value != null) row.addView(value)
        if (trailing != null) {
            row.addView(FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(52f), LinearLayout.LayoutParams.WRAP_CONTENT)
                addView(trailing, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.END or Gravity.CENTER_VERTICAL
                ))
            })
        }
        return row
    }

    private fun getCategoryBadgeColors(iconRes: Int, isLight: Boolean): Pair<Int, Int> {
        return when (iconRes) {
            R.drawable.ic_equalizer -> if (isLight) Color.parseColor("#EFEAFF") to Color.parseColor("#7C3AED")
                                       else Color.parseColor("#261B40") to Color.parseColor("#A78BFA")
            R.drawable.ic_spatial -> if (isLight) Color.parseColor("#E6F0FF") to Color.parseColor("#2563EB")
                                     else Color.parseColor("#142340") to Color.parseColor("#60A5FA")
            R.drawable.ic_earbud, R.drawable.ic_gesture -> if (isLight) Color.parseColor("#FFF4E5") to Color.parseColor("#D97706")
                                                           else Color.parseColor("#382310") to Color.parseColor("#FBBF24")
            R.drawable.ic_low_latency, R.drawable.ic_bolt -> if (isLight) Color.parseColor("#E6F9F0") to Color.parseColor("#059669")
                                                             else Color.parseColor("#102C20") to Color.parseColor("#34D399")
            R.drawable.ic_hearing -> if (isLight) Color.parseColor("#E0F7FA") to Color.parseColor("#0891B2")
                                     else Color.parseColor("#0F2833") to Color.parseColor("#38BDF8")
            R.drawable.ic_devices -> if (isLight) Color.parseColor("#EDE9FE") to Color.parseColor("#6366F1")
                                     else Color.parseColor("#1D1C3E") to Color.parseColor("#818CF8")
            R.drawable.ic_find_buds -> if (isLight) Color.parseColor("#FCE7F3") to Color.parseColor("#DB2777")
                                       else Color.parseColor("#3B1629") to Color.parseColor("#F472B6")
            else -> if (isLight) Color.parseColor("#EEF2F6") to Color.parseColor("#475569")
                    else Color.parseColor("#1F2432") to Color.parseColor("#94A3B8")
        }
    }

    fun subtitle(context: Context, row: LinearLayout): TextView {
        row.findViewWithTag<TextView>(SUBTITLE_TAG)?.let { return it }
        val column = row.findViewWithTag<LinearLayout>(TEXT_TAG)
        return TextView(context).apply {
            setTextColor(ThemeRes.color(context, R.attr.appColorTextSecondary))
            textSize = 12.5f
            setPadding(0, ThemeRes.dp(context, 2f), 0, 0)
            tag = SUBTITLE_TAG
            column.addView(this)
        }
    }

    const val SUBTITLE_TAG = "setting_row_subtitle"
    const val TITLE_TAG = "setting_row_title"
    const val TEXT_TAG = "setting_row_text"
    const val ICON_TAG = "setting_row_icon"

    fun buildSwitch(context: Context, checked: Boolean): Switch {
        val (thumb, track) = ThemeRes.switchTints(context)
        return object : Switch(context) {
            override fun performClick(): Boolean = super.performClick().also { Haptics.commit(this) }
        }.apply {
            isChecked = checked
            text = ""
            showText = false
            thumbTintList = thumb
            trackTintList = track
            trackTintMode = PorterDuff.Mode.SRC_IN
            background = null
        }
    }

    fun buildChevron(context: Context): ImageView {
        val dp = { v: Float -> ThemeRes.dp(context, v) }
        return ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(20f), dp(20f))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(
                ThemeRes.tint(context, R.drawable.ic_chevron_right, ThemeRes.color(context, R.attr.appColorTextSecondary))
            )
            contentDescription = ""
        }
    }

    fun buildValue(context: Context, text: String): TextView {
        val dp = { v: Float -> ThemeRes.dp(context, v) }
        return TextView(context).apply {
            setText(text)
            setTextColor(ThemeRes.color(context, R.attr.appColorTextSecondary))
            textSize = 13f
            gravity = Gravity.END
            setPadding(dp(8f), 0, 0, 0)
            maxWidth = (context.resources.displayMetrics.widthPixels * 0.55f).toInt()
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
    }

    /**
     * Samsung One UI 9 Inset Divider (starts under text column, 70dp from start).
     */
    fun buildDivider(context: Context): View = View(context).apply {
        val outline = ThemeRes.color(context, R.attr.appColorOutline)
        val dp = { v: Float -> ThemeRes.dp(context, v) }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1f)
        ).apply {
            marginStart = dp(70f) // Inset past the icon badge
            marginEnd = dp(16f)
        }
        setBackgroundColor(outline)
    }

    /**
     * Samsung One UI 9 Plush Squircle Card: 26dp radius.
     */
    fun card(context: Context, radiusDp: Float = 26f): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = ThemeRes.card(context, radiusDp)
        clipToOutline = true
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    const val SPLIT_RADIUS = 18f

    fun splitList(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    fun addSplit(list: LinearLayout, row: View) {
        val c = card(list.context, SPLIT_RADIUS)
        (c.layoutParams as LinearLayout.LayoutParams).topMargin = if (list.childCount > 0) ThemeRes.dp(list.context, 10f) else 0
        c.addView(row)
        list.addView(c)
    }

    fun addRow(card: LinearLayout, row: View) {
        if (card.childCount > 0) card.addView(buildDivider(card.context))
        card.addView(row)
    }

    fun screen(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        )
        ThemeRes.screenPadding(this)
    }

    fun title(context: Context, titleRes: Int): TextView = TextView(context).apply {
        setText(titleRes)
        setTextColor(ThemeRes.color(context, R.attr.appColorTextPrimary))
        textSize = 28f
        typeface = ThemeRes.bold(context)
        setPadding(ThemeRes.dp(context, 4f), ThemeRes.dp(context, 14f), 0, ThemeRes.dp(context, 18f))
    }

    fun section(context: Context, titleRes: Int): TextView = sectionLabel(context, titleRes)

    fun sectionLabel(context: Context, titleRes: Int): TextView = TextView(context).apply {
        setText(titleRes)
        setTextColor(ThemeRes.color(context, R.attr.appColorAccent))
        textSize = 13.5f
        typeface = ThemeRes.bold(context)
        val dp = { v: Float -> ThemeRes.dp(context, v) }
        setPadding(dp(8f), dp(18f), 0, dp(8f))
    }

    fun iconButton(context: Context, iconRes: Int, descRes: Int, onClick: () -> Unit): android.widget.ImageButton {
        val dp = { v: Float -> ThemeRes.dp(context, v) }
        return android.widget.ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44f), dp(44f))
            setPadding(dp(11f), dp(11f), dp(11f), dp(11f))
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = ThemeRes.ripple(context, ThemeRes.iconButton(context))
            setImageDrawable(ThemeRes.tint(context, iconRes, ThemeRes.color(context, R.attr.appColorAccent)))
            if (descRes != 0) contentDescription = context.getString(descRes)
            setOnClickListener { onClick() }
            ThemeRes.sinkOnPress(this)
        }
    }
}
