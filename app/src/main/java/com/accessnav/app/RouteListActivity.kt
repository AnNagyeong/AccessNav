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

    private val segments = listOf(
        RouteSegment("한양여자대학교 정문 도착",
            "걸어서 30분, 보도블록이 이어지는 경로", "#4CAF50"),
        RouteSegment("한양여자대학교 도서관 후문 도착",
            "걸어서 30%, 보도블록에서 이어지는 경로", "#FFC107"),
        RouteSegment("한양여자대학교 도서관 후문 도착",
            "걸어서 50%, 최단거리", "#F44336")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteListBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}

data class RouteSegment(val title: String, val description: String, val color: String)
