package com.github.androidtempley.sectorpicker

import android.os.Bundle
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var sectorPicker: SectorPicker
    private lateinit var seekBar: SeekBar
    private lateinit var textView: TextView

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

        Thread {
            while(true) {
                updateStatsText()

                Thread.sleep(500)
            }
        }.start()
    }

    private fun updateStatsText() {
        val marker1Pos = sectorPicker.getMarkerPosition(SectorPicker.MARKER_1)
        val marker2Pos = sectorPicker.getMarkerPosition(SectorPicker.MARKER_2)

        textView.text = String.format(getString(R.string.status), marker1Pos, marker2Pos)
    }
}