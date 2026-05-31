package com.accessnav.app

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.accessnav.app.databinding.ActivityReportBinding

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.seekBarDanger.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvDangerPercent.text = "$progress%"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) = Unit

            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        })

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSubmit.setOnClickListener {
            Toast.makeText(this, "제보가 접수되었습니다. 감사합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnAddPhoto.setOnClickListener {
            Toast.makeText(this, "사진 선택 기능은 실제 기기에서 연결할 예정입니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
