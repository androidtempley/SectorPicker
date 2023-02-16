package com.github.androidtempley.sectorpickerexample

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.androidtempley.sectorpicker.SectorPicker
import com.github.androidtempley.sectorpicker.SectorPickerEventListener

import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import android.graphics.Bitmap
import android.graphics.Canvas

import android.graphics.drawable.BitmapDrawable





class MainActivity : AppCompatActivity() {

    private lateinit var sectorPicker: SectorPicker
    private lateinit var seekBar: SeekBar
    private lateinit var textView: TextView
    private var centreButtonState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sectorPicker = findViewById(R.id.sectorPicker)
        seekBar = findViewById(R.id.seekBar)
        textView = findViewById(R.id.statsText)

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {  }

            override fun onStartTrackingTouch(seekBar: SeekBar) { }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                sectorPicker.numberOfPoints = progress
            }
        })

        sectorPicker.setEventListener(object: SectorPickerEventListener {
            override fun onMarkerMoved(marker: Int, position: Int) {
                updateStatsText()
            }
        })

        val bitmapFile =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_android_blue_24dp, null)?.let {
                drawableToBitmap(
                    it
                )
            }

        if (bitmapFile == null) {
            Log.e("MainActivity", "Bitmap is null...")
        }
        else {
            sectorPicker.setButtonBitmap(bitmapFile)
        }

        sectorPicker.setOnButtonClickListener {
            sectorPicker.setFullCircleMode(!centreButtonState)
            sectorPicker.setMarkerState(centreButtonState)
            centreButtonState = !centreButtonState
        }

        updateStatsText()
    }

    private fun updateStatsText() {
        val marker1Pos = sectorPicker.getMarkerPosition(SectorPicker.MARKER_1)
        val marker2Pos = sectorPicker.getMarkerPosition(SectorPicker.MARKER_2)

        textView.text = String.format(getString(R.string.status), marker1Pos, marker2Pos)
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap =
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)
        return bitmap
    }
}