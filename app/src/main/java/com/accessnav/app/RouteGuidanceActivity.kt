package com.accessnav.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.accessnav.app.databinding.ActivityRouteGuidanceBinding

class RouteGuidanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteGuidanceBinding

    private val steps = listOf(
        RouteStep("#F44336", "한양여자대학교 디자인관 북문에서 출발", "입구로 이동"),
        RouteStep("#4CAF50", "방향을 유지하며 60m 이동", "남은 보도, 장애물 없음"),
        RouteStep("#FFC107", "우회전 후 40m 직진", "인근 공사 : 주의하여 이동"),
        RouteStep("#2196F3", "한양여자대학교 도서관 도착", "약 3분 소요")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteGuidanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        steps.forEach { step -> addStepView(step) }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnReport.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }
    }

    private fun addStepView(step: RouteStep) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_route_step, binding.stepsContainer, false)
        itemView.findViewById<View>(R.id.stepIndicator)
            .setBackgroundColor(Color.parseColor(step.color))
        itemView.findViewById<TextView>(R.id.tvStepTitle).text = step.title
        itemView.findViewById<TextView>(R.id.tvStepDesc).text = step.description

        val lp = itemView.layoutParams as LinearLayout.LayoutParams
        lp.bottomMargin = 12
        itemView.layoutParams = lp
        binding.stepsContainer.addView(itemView)
    }
}

data class RouteStep(val color: String, val title: String, val description: String)
