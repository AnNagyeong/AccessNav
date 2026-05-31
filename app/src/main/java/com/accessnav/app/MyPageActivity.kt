package com.accessnav.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.accessnav.app.databinding.ActivityMypageBinding

class MyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMypageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMypageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }
}
