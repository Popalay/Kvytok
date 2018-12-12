package com.popalay.kvytok

import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.Config.ALPHA_8
import android.graphics.Color.BLACK
import android.graphics.Color.TRANSPARENT
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PorterDuff.Mode.SRC_IN
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

open class TicketLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val textTicketNumber: TextView by bindView(R.id.text_ticket_number)
    private val imageQrCode: ImageView by bindView(R.id.image_qr_code)
    private val textTrainNumber: TextView by bindView(R.id.text_train_number)
    private val textCarNumber: TextView by bindView(R.id.text_car_number)
    private val textSeatNumber: TextView by bindView(R.id.text_seat_number)
    private val textName: TextView by bindView(R.id.text_name)

    private val mBackgroundPaint = Paint()
    private val mDividerPaint = Paint()
    private val mPath = Path()
    private var mDirty = true

    private val mRect = RectF()
    private val mRoundedCornerArc = RectF()

    private var mBackgroundColor: Int = 0

    private var mScallopHeight: Int = 0
    private var mScallopPositions: FloatArray = floatArrayOf()
    private var mScallopPositionsPercent: FloatArray = floatArrayOf()

    private var mScallopRadius: Int = 0

    private var mDividers: List<Pair<PointF, PointF>> = listOf()

    private var mShowDivider: Boolean = false
    private var mDividerDashLength: Int = 0
    private var mDividerDashGap: Int = 0
    private var mDividerWidth: Int = 0
    private var mDividerColor: Int = 0
    private var mCornerRadius: Int = 0
    private var mDividerPadding: Int = 0

    private var mShadow: Bitmap? = null
    private val mShadowPaint = Paint(ANTI_ALIAS_FLAG)
    private var mShadowBlurRadius = 0f

    init {
        init()
    }

    var ticket: Ticket? = null
        set(value) {
            field = value?.also {
                textTicketNumber.text = it.ticketNumber
                /*             textDeparture.text = it.departure
                             textDepartureDate.text = it.departureDate
                             textArrival.text = it.arrival
                             textArrivalDate.text = it.arrivalDate*/
                textTrainNumber.text = it.trainNumber
                textCarNumber.text = it.carNumber
                textSeatNumber.text = it.seatNumber
                textName.text = it.passenger
                imageQrCode.setImageBitmap(it.qrCode)
            }
            isVisible = value != null
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDirty) {
            doLayout()
        }
        if (mShadowBlurRadius > 0f && !isInEditMode) {
            canvas.drawBitmap(mShadow!!, 0f, mShadowBlurRadius / 2f, null)
        }
        canvas.drawPath(mPath, mBackgroundPaint)
        if (mShowDivider) {
            mDividers.forEach { (start, stop) ->
                canvas.drawLine(
                    start.x,
                    start.y,
                    stop.x,
                    stop.y,
                    mDividerPaint
                )
            }

        }
    }

    private fun doLayout() {
        val left = paddingLeft + mShadowBlurRadius
        val right = width.toFloat() - paddingRight.toFloat() - mShadowBlurRadius
        val top = paddingTop + mShadowBlurRadius / 2
        val bottom = height.toFloat() - paddingBottom.toFloat() - mShadowBlurRadius - mShadowBlurRadius / 2
        mPath.reset()
        mPath.arcTo(getTopLeftCornerRoundedArc(top, left), 180.0f, 90.0f, false)
        mPath.lineTo(left + mCornerRadius, top)

        mPath.lineTo(right - mCornerRadius, top)
        mPath.arcTo(getTopRightCornerRoundedArc(top, right), -90.0f, 90.0f, false)

        mScallopPositions
            .map { ((top + bottom) / it) - mScallopRadius }
            .forEach { offset ->
                mRect.set(
                    right - mScallopRadius,
                    top + offset,
                    right + mScallopRadius,
                    mScallopHeight + offset + top
                )
                mPath.arcTo(mRect, 270f, -180.0f, false)
            }

        mPath.arcTo(getBottomRightCornerRoundedArc(bottom, right), 0.0f, 90.0f, false)
        mPath.lineTo(right - mCornerRadius, bottom)

        mPath.lineTo(left + mCornerRadius, bottom)
        mPath.arcTo(getBottomLeftCornerRoundedArc(left, bottom), 90.0f, 90.0f, false)

        mScallopPositions
            .map { ((top + bottom) / it) - mScallopRadius }
            .forEach { offset ->
                mRect.set(
                    left - mScallopRadius,
                    top + offset,
                    left + mScallopRadius,
                    mScallopHeight + offset + top
                )
                mPath.arcTo(mRect, 90.0f, -180.0f, false)
            }

        mPath.close()

        mDividers = mScallopPositions
            .map { ((top + bottom) / it) - mScallopRadius }
            .map { offset -> generateDividers(top, left, right, offset) }

        generateShadow()
        mDirty = false
    }

    private fun generateDividers(
        top: Float,
        left: Float,
        right: Float,
        offset: Float
    ): Pair<PointF, PointF> {
        val startX = left + mScallopRadius.toFloat() + mDividerPadding.toFloat()
        val stratY = mScallopRadius.toFloat() + top + offset
        val stopX = right - mScallopRadius.toFloat() - mDividerPadding.toFloat()
        val stopY = mScallopRadius.toFloat() + top + offset
        return PointF(startX, stratY) to PointF(stopX, stopY)
    }

    private fun generateShadow() {
        if (!isInEditMode) {
            if (mShadowBlurRadius == 0f) return

            if (mShadow == null) {
                mShadow = Bitmap.createBitmap(width, height, ALPHA_8)
            } else {
                mShadow?.eraseColor(TRANSPARENT)
            }
            val c = Canvas(mShadow!!)
            c.drawPath(mPath, mShadowPaint)
            val rs = RenderScript.create(context)
            val blur = ScriptIntrinsicBlur.create(rs, Element.U8(rs))
            val input = Allocation.createFromBitmap(rs, mShadow)
            val output = Allocation.createTyped(rs, input.type)
            blur.setRadius(mShadowBlurRadius)
            blur.setInput(input)
            blur.forEach(output)
            output.copyTo(mShadow)
            input.destroy()
            output.destroy()
            blur.destroy()
        }
    }

    private fun init() {
        View.inflate(context, R.layout.widget_ticket, this)
        setBackgroundColor(Color.WHITE)

        mBackgroundColor = ContextCompat.getColor(context, android.R.color.white)
        mScallopRadius = 20f.px
        mScallopPositionsPercent = floatArrayOf(50f, 80f)
        mShowDivider = true
        mDividerWidth = 1f.px
        mDividerColor = ContextCompat.getColor(context, android.R.color.darker_gray)
        mDividerDashLength = 6f.px
        mDividerDashGap = 4f.px
        mCornerRadius = 4f.px
        mDividerPadding = 10f.px
        val elevation = 8f.px.toFloat()
        if (elevation > 0f) {
            setShadowBlurRadius(elevation)
        }

        mShadowPaint.colorFilter = PorterDuffColorFilter(BLACK, SRC_IN)
        mShadowPaint.alpha = 51 // 20%

        initElements()

        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    private fun initElements() {
        if (mDividerWidth > mScallopRadius) {
            mDividerWidth = mScallopRadius
            Log.w("TicketLayout", "You cannot apply divider width greater than scallop radius. Applying divider width to scallop radius.")
        }

        mScallopPositions = mScallopPositionsPercent.map { 100 / it }.toFloatArray()
        mScallopHeight = mScallopRadius * 2

        setBackgroundPaint()
        setDividerPaint()

        mDirty = true
        invalidate()
    }

    private fun setBackgroundPaint() {
        mBackgroundPaint.apply {
            alpha = 0
            isAntiAlias = true
            color = mBackgroundColor
            style = Paint.Style.FILL
        }
    }

    private fun setDividerPaint() {
        mDividerPaint.apply {
            alpha = 0
            isAntiAlias = true
            color = mDividerColor
            strokeWidth = mDividerWidth.toFloat()
            pathEffect = DashPathEffect(floatArrayOf(mDividerDashLength.toFloat(), mDividerDashGap.toFloat()), 0.0f)
        }
    }

    private fun getTopLeftCornerRoundedArc(top: Float, left: Float): RectF = mRoundedCornerArc.apply {
        set(left, top, left + mCornerRadius * 2, top + mCornerRadius * 2)
    }

    private fun getTopRightCornerRoundedArc(top: Float, right: Float): RectF = mRoundedCornerArc.apply {
        set(right - mCornerRadius * 2, top, right, top + mCornerRadius * 2)
    }

    private fun getBottomLeftCornerRoundedArc(left: Float, bottom: Float): RectF = mRoundedCornerArc.apply {
        set(left, bottom - mCornerRadius * 2, left + mCornerRadius * 2, bottom)
    }

    private fun getBottomRightCornerRoundedArc(bottom: Float, right: Float): RectF = mRoundedCornerArc.apply {
        set(right - mCornerRadius * 2, bottom - mCornerRadius * 2, right, bottom)
    }

    private fun setShadowBlurRadius(elevation: Float) {
        val maxElevation = 24f.px.toFloat()
        mShadowBlurRadius = Math.min(25f * (elevation / maxElevation), 25f)
    }
}