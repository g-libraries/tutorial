package com.core.tutorial

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.graphics.Shader.TileMode
import android.support.v4.content.ContextCompat


class CircleGradientHoleView : HoleView {
    private var bitmap: Bitmap? = null
    var gradientCenterX: Float = 0F
    var gradientCenterY: Float = 0F

    var gradientStartColorId: Int = android.R.color.black
    var gradientEndColorId: Int = android.R.color.white

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (bitmap == null) {
            createWindowFrame()
        }
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)
    }

    private fun createWindowFrame() {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val osCanvas = Canvas(bitmap!!)

        val outerRectangle = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Draw background
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = ContextCompat.getColor(context, android.R.color.transparent)
        osCanvas.drawRect(outerRectangle, paint)

        // Draw gradient circle
        paint.color = Color.BLACK
        paint.strokeWidth = 10F
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.shader = LinearGradient(
            width / 3f, height / 3f, width.toFloat(),
            height.toFloat(), ContextCompat.getColor(context, gradientStartColorId), ContextCompat.getColor(context, gradientEndColorId), TileMode.MIRROR
        )
        osCanvas.drawCircle(if (gradientCenterX != 0F) gradientCenterX else centerX,
            if (gradientCenterY != 0F) gradientCenterY else centerY,
            height / 3F, paint)
    }

    override fun isInEditMode(): Boolean {
        return true
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        bitmap = null
    }
}
