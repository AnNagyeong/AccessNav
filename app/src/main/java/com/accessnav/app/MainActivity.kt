package com.accessnav.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.accessnav.app.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Demo POIs (Hanyang Women's University area)
    private val pois = listOf(
        POI("한양여자대학교 도서관", 37.5573, 127.0466, POIType.DESTINATION),
        POI("장애인 화장실", 37.5568, 127.0455, POIType.RESTROOM),
        POI("엘리베이터", 37.5575, 127.0470, POIType.ELEVATOR),
        POI("버스 정류장", 37.5565, 127.0460, POIType.BUS),
        POI("베리어프리 카페", 37.5570, 127.0450, POIType.BARRIER_FREE)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()
        setupMap()
        setupSearchBar()
        setupFilterChips()
        setupBottomNav()
    }

    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1)
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(37.5570, 127.0460))
        }
        addMarkers()
    }

    private fun addMarkers() {
        pois.forEach { poi ->
            val marker = Marker(binding.mapView).apply {
                position = GeoPoint(poi.lat, poi.lng)
                title = poi.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            binding.mapView.overlays.add(marker)
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val dest = binding.etSearch.text.toString()
                if (dest.isNotBlank()) openRoute(dest)
                true
            } else false
        }
        binding.btnFilter.setOnClickListener { showFilterDialog() }
    }

    private fun setupFilterChips() {
        val categories = listOf("택시 정류장", "편의점", "기계")
        categories.forEach { label ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                chipCornerRadius = 24f
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                else -> true
            }
        }
    }

    private fun openRoute(destination: String) {
        startActivity(Intent(this, RouteActivity::class.java).apply {
            putExtra("destination", destination)
        })
    }

    private fun showFilterDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_filter, null)
        view.findViewById<android.widget.ImageView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}

data class POI(val name: String, val lat: Double, val lng: Double, val type: POIType)
enum class POIType { DESTINATION, RESTROOM, ELEVATOR, BUS, BARRIER_FREE }
