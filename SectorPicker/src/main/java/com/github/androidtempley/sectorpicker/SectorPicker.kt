package com.github.androidtempley.sectorpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SectorPicker(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    companion object {
        const val MARKER_1 = 1
        const val MARKER_2 = 2
        const val CLOCKWISE = true
        const val ANTICLOCKWISE = !CLOCKWISE
        private const val ENABLE_LOGGING = false
    }

    private var listener: SectorPickerEventListener? = null

    private var mNumPoints: Int
    var numberOfPoints: Int
        get() = mNumPoints
        set(points) {
            mNumPoints = points
            recalculatePoints()

            if(mMarker1.pointIdx >= mNumPoints) {
                mMarker1.pointIdx = mNumPoints - 1
                listener?.onMarkerMoved(MARKER_1, mMarker1.pointIdx)
            }
            if(mMarker2.pointIdx >= mNumPoints) {
                mMarker2.pointIdx = mNumPoints - 1
                listener?.onMarkerMoved(MARKER_2, mMarker2.pointIdx)
            }

            invalidate()
            requestLayout()
        }
    private var mPointsColor: Int
    private var mPointsRadius: Float
    private var mFillColor: Int
    var fillDirection = CLOCKWISE

    private var mMarker1: Marker
    private var mMarker2: Marker
    private var mPoints = mutableListOf<Point>()
    private var mSectors = mutableListOf<Point>()

    private var mRadius = 0f
    private var mCenterX = 0f
    private var mCenterY = 0f
    private var mFillBounds: RectF? = null

    init {
        val ta = context.theme.obtainStyledAttributes(attrs, R.styleable.SectorPicker, 0, 0)
        try {
            mNumPoints = ta.getInteger(R.styleable.SectorPicker_numPoints, 4)
            mPointsColor = ta.getColor(R.styleable.SectorPicker_pointColor, Color.BLACK)
            mPointsRadius = ta.getDimension(R.styleable.SectorPicker_pointRadius, 0f)
            mFillColor = ta.getColor(R.styleable.SectorPicker_fillColor, Color.LTGRAY)
            mMarker1 = Marker(ta.getColor(R.styleable.SectorPicker_marker1Color, Color.RED),
                ta.getDimension(R.styleable.SectorPicker_markerRadius, 0f),
                ta.getDimension(R.styleable.SectorPicker_markerLineWidth, 0f),
                ta.getInteger(R.styleable.SectorPicker_marker1StartPosition, 0))
            mMarker2 = Marker(ta.getColor(R.styleable.SectorPicker_marker2Color, Color.BLUE),
                ta.getDimension(R.styleable.SectorPicker_markerRadius, 0f),
                ta.getDimension(R.styleable.SectorPicker_markerLineWidth, 0f),
                ta.getInteger(R.styleable.SectorPicker_marker2StartPosition, 0))

            if(mMarker1.pointIdx >= mNumPoints)
                throw IndexOutOfBoundsException("Marker 1 position out of bounds, position ${mMarker1.pointIdx}, number of points $mNumPoints")
            else if (mMarker2.pointIdx >= mNumPoints)
                throw IndexOutOfBoundsException("Marker 2 position out of bounds, position ${mMarker2.pointIdx}, number of points $mNumPoints")
        } finally {
            ta.recycle()
        }
    }

    fun getMarkerPosition(marker: Int): Int {
        return marker.let {
            when (it) {
                MARKER_1 -> mMarker1.pointIdx
                MARKER_2 -> mMarker2.pointIdx
                else -> -1
            }
        }
    }

    fun setMarkerPosition(marker: Int, position: Int) {
        when(marker) {
            MARKER_1 -> mMarker1.pointIdx = if(position < mNumPoints) position else mNumPoints - 1
            MARKER_2 -> mMarker2.pointIdx = if(position < mNumPoints) position else mNumPoints - 1
            else   ->   return
        }

        listener?.onMarkerMoved(marker, position)

        invalidate()
        requestLayout()
    }

    fun setEventListener(listener: SectorPickerEventListener) { this.listener = listener }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Account for padding
        var xpad = (paddingLeft + paddingRight).toFloat()
        var ypad = (paddingTop + paddingBottom).toFloat()

        // Account for markers
        xpad += java.lang.Float.max(mMarker1.radius, mMarker2.radius)
        ypad += java.lang.Float.max(mMarker1.radius, mMarker2.radius)

        val ww = w.toFloat() - xpad
        val hh = h.toFloat() - ypad

        // Figure out how big we can make the circle of points.
        mRadius = min(ww, hh) / 2

        // Calculate the circle center and bounds
        mCenterX = w.toFloat() / 2
        mCenterY = h.toFloat() / 2
        mFillBounds = RectF().apply {
            top = mCenterY - mRadius
            bottom = mCenterY + mRadius
            left = mCenterX - mRadius
            right = mCenterX + mRadius
        }

        recalculatePoints()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            val marker1Pos = if(mMarker1.pointIdx < mNumPoints) {
                mMarker1.pointIdx
            } else {
                mNumPoints - 1
            }
            val marker2Pos = if(mMarker2.pointIdx < mNumPoints) {
                mMarker2.pointIdx
            } else {
                mNumPoints - 1
            }

            // Draw fill - always fill Marker 1 -> Marker 2, clockwise
            val start = ((if (fillDirection) marker1Pos else marker2Pos) * 360f / mNumPoints)
            var sweep = ((if(fillDirection) marker2Pos else marker1Pos) * 360f / mNumPoints) - start
            if(sweep < 0)
                sweep += 360
            drawArc(mFillBounds!!, start - 90, sweep, true, fillPaint)

            // Draw points
            for(i in 0 until mNumPoints)  {
                drawCircle(mPoints[i].xPos, mPoints[i].yPos, mPointsRadius, pointPaint)
            }

            // Draw Markers, Marker 1 on top of Marker 2
            drawCircle(mPoints[marker2Pos].xPos, mPoints[marker2Pos].yPos, mMarker2.radius, mMarker2.paint)
            drawCircle(mPoints[marker1Pos].xPos, mPoints[marker1Pos].yPos, mMarker1.radius, mMarker1.paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility") // Not a clickable view?
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event).let { result ->
            if(!result) {
                when(event?.action) {
                    MotionEvent.ACTION_UP -> {
                        if(mMarker1.isMoving) {
                            mMarker1.isMoving = false       // Clear moving flag
                            listener?.onMarkerMoved(MARKER_1, mMarker1.pointIdx)
                        }
                        if(mMarker2.isMoving) {
                            mMarker2.isMoving = false       // Clear moving flag
                            listener?.onMarkerMoved(MARKER_2, mMarker2.pointIdx)
                        }
                        true
                    }
                    else -> false
                }
            } else true
        }
    }

    private val gestureListener = object: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            val touchMargin = 1.2f          // Apply 20% increase to marker size as additional touch area
            e?.apply {
                // Check position on down event and return false if not within either Marker
                // Check Marker 1
                var xMin = mPoints[mMarker1.pointIdx].xPos - mMarker1.radius * touchMargin
                var xMax = mPoints[mMarker1.pointIdx].xPos + mMarker1.radius * touchMargin
                var yMin = mPoints[mMarker1.pointIdx].yPos - mMarker1.radius * touchMargin
                var yMax = mPoints[mMarker1.pointIdx].yPos + mMarker1.radius * touchMargin

                if (x in xMin..xMax && y in yMin..yMax) {
                    SectorPickerLog("onDown", "Marker1 Down")
                    mMarker1.isMoving = true
                    return true
                }

                // Check Marker 2
                xMin = mPoints[mMarker2.pointIdx].xPos - mMarker2.radius * touchMargin
                xMax = mPoints[mMarker2.pointIdx].xPos + mMarker2.radius * touchMargin
                yMin = mPoints[mMarker2.pointIdx].yPos - mMarker2.radius * touchMargin
                yMax = mPoints[mMarker2.pointIdx].yPos + mMarker2.radius * touchMargin

                return if(x in xMin..xMax && y in yMin..yMax) {
                    SectorPickerLog("onDown", "Marker2 Down")
                    mMarker2.isMoving = true
                    true
                } else {
                    SectorPickerLog("onDown", "Neither marker")
                    false
                }
            }
            return false
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            // Calculate best position for marker after motion ends
            SectorPickerLog("onScroll", "Moving from ${e1!!.x},${e1.y} to ${e2!!.x},${e2.y}")

            // Consider the circle split into mNumPoints sectors, with ach point at the centre of each arc
            // Test each sector to see if scroll end is within
            // If so, and is a different sector to the current positionof the marker, update the view
            val relPoint = Point(e2.x - mCenterX, e2.y - mCenterY)

            for(i in 0 until mNumPoints) {
                val vectorPoint1 = Point(mSectors[i].xPos - mCenterX, mSectors[i].yPos - mCenterY)
                val vectorPoint2 = Point(mSectors[(i + 1) % mNumPoints].xPos - mCenterX, mSectors[(i + 1) % mNumPoints].yPos - mCenterY)
                // Point is within sector if it is clockwise of the first vector, and anti-clockwise of the second vector
                if(!isVectorClockwise(vectorPoint1, relPoint) && isVectorClockwise(vectorPoint2, relPoint))
                {
                    SectorPickerLog("Sector", i.toString())
                    when {
                        mMarker1.isMoving -> {
                            mMarker1.pointIdx = i
                        }
                        mMarker2.isMoving -> {
                            mMarker2.pointIdx = i
                        }
                        else -> return false
                    }

                    invalidate()
                    requestLayout()
                    return true
                }
            }

            return false
        }

        /* Don't support any other gestures */
    }

    private val gestureDetector: GestureDetector = GestureDetector(getContext(), gestureListener)

    private val fillPaint = Paint(0).apply {
        color = mFillColor
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mPointsColor
        style = Paint.Style.FILL_AND_STROKE
    }

    private fun recalculatePoints() {
        mPoints.clear()
        mSectors.clear()

        // Calculate points positions
        for(i in 0 until mNumPoints) {
            var angle = i * 360 / mNumPoints
            angle -= 90     // Normalise to 12 o'clock rather than the geometric default of 3

            // Calculate x and y points on the circle
            val xPos = mCenterX + mRadius * cos(angle * Math.PI / 180)
            val yPos = mCenterY + mRadius * sin(angle * Math.PI / 180)

            mPoints.add(Point(xPos.toFloat(), yPos.toFloat()))
        }

        // Calculate sector boundaries
        for(i in 0 until mNumPoints) {
            var angle = ((i * 360) - 180) / mNumPoints
            angle -= 90

            // Calculate x and y points on the circle, relative to the centre
            val xPos = mCenterX + mRadius * cos(angle * Math.PI / 180)
            val yPos = mCenterY + mRadius * sin(angle * Math.PI / 180)

            mSectors.add(Point(xPos.toFloat(), yPos.toFloat()))
        }
    }

    private fun isVectorClockwise(v1: Point, v2: Point): Boolean {
        return ((-v1.xPos * v2.yPos) + (v1.yPos * v2.xPos)) > 0
    }

    @Suppress("FunctionName")
    private fun SectorPickerLog(tag: String, msg: String) {
        if(ENABLE_LOGGING)
            Log.d("SectorPicker $tag", msg)
    }

    class Marker(mColor: Int, mRadius: Float, mLineWidth: Float, mPointIdx: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mColor
            style = Paint.Style.STROKE
            strokeWidth = mLineWidth
        }
        val radius = mRadius
        var pointIdx = mPointIdx
        var isMoving = false
    }

    class Point(mXPos: Float, mYPos: Float) {
        val xPos = mXPos
        val yPos = mYPos
    }
}