package com.accessnav.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.accessnav.app.databinding.ActivityRouteListBinding

class RouteListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destination = intent.getStringExtra("destination") ?: "목적지"
        val segments = routeSegments(destination)

        binding.tvRouteSummary.text = "${destination}까지 추천 경로 ${segments.size}개"
        binding.rvRouteList.layoutManager = LinearLayoutManager(this)
        binding.rvRouteList.adapter = SegmentAdapter(segments)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnStartNav.setOnClickListener {
            startActivity(Intent(this, RouteGuidanceActivity::class.java))
        }
    }

    inner class SegmentAdapter(private val items: List<RouteSegment>) :
        RecyclerView.Adapter<SegmentAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val border: View = v.findViewById(R.id.topBorder)
            val title: TextView = v.findViewById(R.id.tvSegmentTitle)
            val desc: TextView = v.findViewById(R.id.tvSegmentDesc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_route_segment, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.border.setBackgroundColor(android.graphics.Color.parseColor(item.color))
            holder.title.text = item.title
            holder.desc.text = item.description
        }

        override fun getItemCount() = items.size
    }

    private fun routeSegments(destination: String) = listOf(
        RouteSegment(
            "${destination} 정문 경로",
            "3분 · 100m · 안전 구간 20%, 주의 구간 80%",
            "#49D11A"
        ),
        RouteSegment(
            "${destination} 최단 경로",
            "2분 · 90m · 위험 구간 2곳 포함",
            "#F26A6A"
        )
    )
}

data class RouteSegment(val title: String, val description: String, val color: String)
