package com.accessnav.app

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.accessnav.app.databinding.ActivityRouteBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class RouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        binding = ActivityRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val destination = intent.getStringExtra("destination") ?: "한양여자대학교 도서관"
        binding.tvDestination.text = destination

        setupMap()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRouteList.setOnClickListener {
            startActivity(Intent(this, RouteListActivity::class.java))
        }
        binding.btnStartNav.setOnClickListener {
            startActivity(Intent(this, RouteGuidanceActivity::class.java))
        }
    }

    private fun setupMap() {
        val start = GeoPoint(37.5565, 127.0452)
        val end   = GeoPoint(37.5573, 127.0466)

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.5)
            controller.setCenter(GeoPoint(37.5569, 127.0459))
        }

        // Route polyline
        val route = Polyline().apply {
            addPoint(start)
            addPoint(GeoPoint(37.5567, 127.0455))
            addPoint(GeoPoint(37.5570, 127.0460))
            addPoint(end)
            outlinePaint.color = android.graphics.Color.parseColor("#4CAF50")
            outlinePaint.strokeWidth = 10f
        }
        binding.mapView.overlays.add(route)

        // Start/End markers
        listOf(start to "출발", end to "도착").forEach { (pt, label) ->
            binding.mapView.overlays.add(Marker(binding.mapView).apply {
                position = pt; title = label
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
        }
    }

    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause()  { super.onPause();  binding.mapView.onPause() }
}
