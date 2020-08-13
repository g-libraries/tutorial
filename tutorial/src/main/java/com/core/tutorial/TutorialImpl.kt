package com.core.tutorial

import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Rect
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.core.basicextensions.applyGlobalLayoutListener
import com.core.common.interfaces.ITutorial
import kotlinx.android.synthetic.main.tutorial_overlay.view.*
import kotlin.math.hypot


/**
 * Base Tutorial implementation.
 * params - Default params, can modify
 * active - Tutorial active status
 * rootActivity - Root activity
 * rootView - Tutorial container
 */
abstract class TutorialImpl(
    val params: Params,
    var active: Boolean,
    private val rootActivity: Activity,
    val rootView: FrameLayout
) : ITutorial {

    data class Params(
        // Start and End gradient IDs
        var gradientColorIds: Pair<Int, Int> = Pair(
            R.color.tutorial_gradient_start,
            R.color.tutorial_gradient_end
        ),
        // Bottom nav icon bg gradiend ID
        var iconBGColorId: Int = R.color.tutorial_icon_bg,
        // Text layout ID and TextVies IDs
        var textLayoutId: Int = R.layout.tutorial_text,
        var textViewTitleId: Int = R.id.tutorial_text_title,
        var textViewMessageId: Int = R.id.tutorial_text_message,
        // Tutorial biases for background and central "hole" positioning
        var BIAS_ZERO: Float = 0F,
        var BIAS_SMALL: Float = 0.12F,
        var BIAS_LARGE: Float = 0.37F,
        var withBottomNav: Boolean = true,
        // Animation on/off, delay and duration
        var animEnabled: Boolean = false,
        var animDelay: Long = 150L,
        var animDuration: Long = 500L,
        var animInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    )

    private lateinit var titleTV: TextView
    private lateinit var messageTV: TextView
    private lateinit var bottomNavIconIV: ImageView

    protected var step: Int = 0

    // Used for Tutorial view positioning, used in Tutorial Strategies which can be implemented in app
    var x = 0f
    var y = 0f
    var radius = 0f

    // Must be inside attach() method to listen for Fragments lifecycle events, see implementation for reference
    abstract fun attachNavigationCallbacks(fragmentManager: FragmentManager)

    // Override to control Tutorial logic
    abstract fun startTutorial()

    abstract fun stopTutorial()
    abstract fun showStep()

    protected fun showTutorialView() {
        rootView.visibility = View.VISIBLE
    }

    protected fun hideTutorialView() {
        rootView.removeAllViews()
        rootView.visibility = View.GONE
    }

    /**
     * Create and show Tutorial view
     */
    protected fun createOverLay(
        lvId: Int,
        lvXY: Pair<Float, Float>,
        tvXY: Pair<Float, Float>,
        bgBiasHorVert: Pair<Float, Float>,
        textOnTop: Boolean,
        bottomNav: Boolean = false
    ) {
        // Clear old tutorial views
        rootView.removeAllViews()

        // Create new tutorial views
        val container = rootActivity.findViewById(android.R.id.content) as ViewGroup

        // Background: tint and circle with hole
        val overlayView = LayoutInflater.from(rootActivity)
            .inflate(R.layout.tutorial_overlay, container, false)

        overlayView.circle_background.centerX = x
        overlayView.circle_background.centerY = y
        overlayView.circle_background.radius = radius

        overlayView.circle_overlay.centerX = x
        overlayView.circle_overlay.centerY = y
        overlayView.circle_overlay.gradientCenterX =
            x + (rootView.measuredWidth * bgBiasHorVert.first)
        overlayView.circle_overlay.gradientCenterY =
            y + (rootView.measuredWidth * bgBiasHorVert.second)
        overlayView.circle_overlay.radius = radius
        overlayView.circle_overlay.gradientStartColorId = params.gradientColorIds.first
        overlayView.circle_overlay.gradientEndColorId = params.gradientColorIds.second

        // Line
        val lineView = ImageView(rootActivity)
        lineView.setImageDrawable(ContextCompat.getDrawable(rootActivity, lvId))
        lineView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lineView.x = lvXY.first
        lineView.y = lvXY.second

        // Text
        val textView =
            LayoutInflater.from(rootActivity).inflate(params.textLayoutId, container, false)

        titleTV = textView.findViewById(params.textViewTitleId)
        messageTV = textView.findViewById(params.textViewMessageId)

        textView.applyGlobalLayoutListener {
            it?.let {
                it.x = tvXY.first
                it.y =
                    tvXY.second + (if (textOnTop) it.measuredHeight / 2 else -it.measuredHeight / 2)
            }
        }

        // Bottom Navigation icon and background optional tweak
        // Icons to overlay hole, only for bottom nav tutorial items for "perfect" look
        val iconBGView = ImageView(rootActivity)
        iconBGView.setBackgroundColor(
            ContextCompat.getColor(
                rootActivity,
                params.iconBGColorId
            )
        )
        iconBGView.layoutParams = LinearLayout.LayoutParams(
            radius.toInt() * 3,
            radius.toInt() * 3
        )

        if (bottomNav) {
            iconBGView.applyGlobalLayoutListener {
                it?.let {
                    it.x = x - (it.measuredWidth / 2)
                    it.y = y - (it.measuredWidth / 2)
                }
            }
            bottomNavIconIV = ImageView(rootActivity)
            bottomNavIconIV.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            bottomNavIconIV.applyGlobalLayoutListener {
                it?.let {
                    it.x = x - (it.measuredWidth / 2)
                    it.y = y - (it.measuredWidth / 2)
                }
            }
            // Add bottom nav icon Tutorial view to layout
            rootView.addView(iconBGView)
            rootView.addView(bottomNavIconIV)
        }

        // Add all other Tutorial views
        rootView.addView(overlayView)
        rootView.addView(lineView)
        rootView.addView(textView)

        // Animations
        if (params.animEnabled) {
            overlayView.circle_overlay.showAnim(x, y, params)
        }
    }

    protected fun setTutorialText(titleId: Int, messageId: Int) {
        titleTV.text = rootActivity.getText(titleId)
        messageTV.text = rootActivity.getText(messageId)
    }

    protected fun setTutorialBottomNavIcon(icId: Int) {
        bottomNavIconIV.setImageDrawable(ContextCompat.getDrawable(rootActivity, icId))
    }

    // Check current target view position to show Tutorial on top of it
    protected fun measureView(view: View, bias: Float) {
        val rectangle = Rect()
        val window = rootActivity.window
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        val statusBarHeight = rectangle.top

        val pos = intArrayOf(0, 0)
        view.getLocationOnScreen(pos)

        x = (pos[0]).toFloat() + (view.measuredWidth / 2F) + (view.measuredWidth * bias)
        y = (pos[1].toFloat() - statusBarHeight) + (view.measuredHeight / 2F)
        radius = view.measuredHeight / 1.6F
    }

    // Strategy for Tutorial view appearance
    class ShowTutorialOverlayStrategy(private val tutorialOverlayStrategy: () -> Unit) {
        fun apply() = tutorialOverlayStrategy.invoke()
    }

    // Sample Default strategies - can use these or create your own
    val defaultShowBelowRight = {
        createOverLay(
            R.drawable.tutorial_line1,
            Pair(x + (radius * 1.25F), y),
            Pair((x / 2F), y + radius),
            Pair(
                params.BIAS_ZERO,
                params.BIAS_ZERO
            ),
            textOnTop = true
        )
    }

    val defaultShowAbove = {
        createOverLay(
            R.drawable.tutorial_line3,
            Pair((x - (radius * 2.7F)), y - (radius * 1.5F)),
            Pair((x - (radius * 3.2F)), y - (radius * 3F)),
            Pair(-params.BIAS_SMALL, -params.BIAS_SMALL),
            textOnTop = false
        )
    }

    val defaultShowBelowCenter = {
        createOverLay(
            R.drawable.tutorial_line2,
            Pair(x + (radius * 1.2F), y + (radius * 2F)),
            Pair((x / 3.5F), y + (radius * 2F)),
            Pair(-params.BIAS_SMALL, params.BIAS_SMALL),
            textOnTop = false
        )
    }

    val defaultShowAboveCenter = {
        createOverLay(
            R.drawable.tutorial_line2,
            Pair(x + (radius * 1.2F), y - (radius * 3F)),
            Pair((x / 3.5F), y - (radius * 3F)),
            Pair(-params.BIAS_SMALL, -params.BIAS_LARGE),
            textOnTop = false
        )
    }

    val defaultShowAboveLeft = {
        createOverLay(
            R.drawable.tutorial_line3,
            Pair((x - (radius * 3F)), y - (radius * 1.5F)),
            Pair((x - (radius * 4F)), y - (radius * 3F)),
            Pair(-params.BIAS_SMALL, -params.BIAS_SMALL),
            textOnTop = false
        )
    }

    val defaultShowOnBottomNav = {
        createOverLay(
            R.drawable.tutorial_line3,
            Pair((x - (radius * 2.7F)), y - (radius * 1.5F)),
            Pair((x - (radius * 3.2F)), y - (radius * 3F)),
            Pair(-params.BIAS_SMALL, -params.BIAS_SMALL),
            textOnTop = false,
            bottomNav = params.withBottomNav
        )
    }

    val defaultShowOnBottomNavLeft = {
        createOverLay(
            R.drawable.tutorial_line3,
            Pair((x - (radius * 3.5F)), y - (radius * 1.5F)),
            Pair((x - (radius * 4F)), y - (radius * 3F)),
            Pair(-params.BIAS_SMALL, -params.BIAS_SMALL),
            textOnTop = false,
            bottomNav = params.withBottomNav
        )
    }

    private fun View.hideAnim() {
        ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
            duration = 250
        }
    }

    private fun View.showAnim(x: Float, y: Float, params: Params) {
        this.alpha = 0f
        // alpha
        ObjectAnimator.ofFloat(this, "alpha", 0f, 255f).apply {
            startDelay = params.animDelay
            duration = params.animDuration
        }
        // circular reveal
        val finalRadius = hypot(x.toDouble(), y.toDouble()).toFloat()
        val anim =
            ViewAnimationUtils.createCircularReveal(this, x.toInt(), y.toInt(), 0f, finalRadius)
        anim.startDelay = params.animDelay
        anim.duration = params.animDuration
        anim.interpolator = params.animInterpolator
        anim.start()
    }
}