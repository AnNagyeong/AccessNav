package com.accessnav.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.accessnav.app.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "AccessNavMain"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val DEFAULT_LAT = 37.56184
        private const val DEFAULT_LNG = 127.03811
    }

    private lateinit var binding: ActivityMainBinding
    private var startPlace: SearchPlace? = null
    private var endPlace: SearchPlace? = null
    private val searchHandler = Handler(Looper.getMainLooper())
    private var pendingSearch: Runnable? = null
    private var suppressSearchWatcher = false
    private var lastSearchWasExplicit = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupClicks()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webMap.webViewClient = WebViewClient()
        binding.webMap.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(TAG, "WebView: ${consoleMessage.message()}")
                return true
            }
        }

        binding.webMap.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(true)
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        binding.webMap.addJavascriptInterface(MapBridge(), "AndroidBridge")
        val mapHtml = assets.open("yj.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        binding.webMap.loadDataWithBaseURL(
            "http://127.0.0.1:3000/",
            mapHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun setupClicks() {
        binding.btnClear.setOnClickListener {
            binding.etSearch.setText("")
            hideSearchResults()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (suppressSearchWatcher) return

                pendingSearch?.let(searchHandler::removeCallbacks)
                val keyword = s?.toString()?.trim().orEmpty()

                if (keyword.length < 2) {
                    hideSearchResults()
                    return
                }

                pendingSearch = Runnable { searchKeyword(explicit = false) }
                searchHandler.postDelayed(pendingSearch!!, 350)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            val isEnterUp = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_UP
            if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnterUp) {
                searchKeyword(explicit = true)
                true
            } else {
                false
            }
        }

        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnCurrentLocation.setOnClickListener {
            moveToCurrentLocation()
        }

        binding.btnFavorite.setOnClickListener {
            openFavorites()
        }

        binding.btnZoomIn.setOnClickListener {
            binding.webMap.evaluateJavascript(
                "window.zoomIn && window.zoomIn();",
                null
            )
        }

        binding.btnZoomOut.setOnClickListener {
            binding.webMap.evaluateJavascript(
                "window.zoomOut && window.zoomOut();",
                null
            )
        }

        binding.chipTaxi.setOnClickListener {
            binding.webMap.evaluateJavascript(
                "window.filterMarkers && window.filterMarkers('taxi');",
                null
            )
        }

        binding.chipStore.setOnClickListener {
            binding.webMap.evaluateJavascript(
                "window.filterMarkers && window.filterMarkers('store');",
                null
            )
        }

        binding.chipCafe.setOnClickListener {
            binding.webMap.evaluateJavascript(
                "window.filterMarkers && window.filterMarkers('cafe');",
                null
            )
        }

        binding.btnMenu.setOnClickListener {
            binding.webMap.evaluateJavascript(
                "window.filterMarkers && window.filterMarkers('all');",
                null
            )
        }

        binding.navHome.setOnClickListener {
            binding.webMap.evaluateJavascript(
                "window.filterMarkers && window.filterMarkers('all');",
                null
            )
        }

        binding.navLocation.setOnClickListener {
            moveToCurrentLocation()
        }

        binding.navCamera.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        binding.navBookmark.setOnClickListener {
            openFavorites()
        }

        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }
    }

    private fun openFavorites() {
        startActivity(Intent(this, FavoritesActivity::class.java))
    }

    private fun moveToCurrentLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastLocation = getLastKnownLocation(locationManager)

        if (lastLocation != null) {
            showCurrentLocation(lastLocation)
            return
        }

        requestCurrentLocation(locationManager)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(locationManager: LocationManager): Location? {
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider ->
                runCatching {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.getLastKnownLocation(provider)
                    } else {
                        null
                    }
                }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation(locationManager: LocationManager) {
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { provider ->
                runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
            }

        if (provider == null) {
            Toast.makeText(this, "기기의 위치 서비스를 켜고 다시 눌러주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "현재 위치를 확인하는 중입니다.", Toast.LENGTH_SHORT).show()
        locationManager.requestSingleUpdate(
            provider,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    showCurrentLocation(location)
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun showCurrentLocation(location: Location) {
        if (!isInsideKoreaMapArea(location.latitude, location.longitude)) {
            Toast.makeText(
                this,
                "현재 위치가 카카오맵 표시 범위 밖이라 기본 위치로 이동합니다.",
                Toast.LENGTH_SHORT
            ).show()
            moveMapTo(DEFAULT_LAT, DEFAULT_LNG)
            return
        }

        moveMapTo(location.latitude, location.longitude)
    }

    private fun moveMapTo(latitude: Double, longitude: Double) {
        binding.webMap.evaluateJavascript(
            "window.showCurrentLocation && window.showCurrentLocation($latitude, $longitude);",
            null
        )
    }

    private fun isInsideKoreaMapArea(latitude: Double, longitude: Double): Boolean {
        return latitude in 33.0..39.5 && longitude in 124.0..132.0
    }

    private fun searchKeyword(explicit: Boolean) {
        val keyword = binding.etSearch.text.toString().trim()
        if (keyword.isEmpty()) return

        lastSearchWasExplicit = explicit
        val quotedKeyword = JSONObject.quote(keyword)
        binding.webMap.evaluateJavascript(
            "window.searchPlaces && window.searchPlaces($quotedKeyword);",
            null
        )
    }

    private fun renderSearchResults(results: List<SearchPlace>) {
        binding.resultList.removeAllViews()

        if (results.isEmpty()) {
            hideSearchResults()
            if (lastSearchWasExplicit) {
                Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        results.take(8).forEach { place ->
            binding.resultList.addView(createResultRow(place))
        }

        binding.resultScroll.visibility = View.VISIBLE
    }

    private fun createResultRow(place: SearchPlace): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { selectPlace(place) }
        }

        row.addView(TextView(this).apply {
            text = place.name
            setTextColor(getColor(R.color.black))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })

        row.addView(TextView(this).apply {
            text = place.address.ifBlank { "주소 정보 없음" }
            setTextColor(getColor(R.color.text_secondary))
            textSize = 12f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })

        return row
    }

    private fun selectPlace(place: SearchPlace) {
        when {
            startPlace == null -> {
                startPlace = place
                showRoutePoint("start", place)
                Toast.makeText(this, "출발지 설정: ${place.name}", Toast.LENGTH_SHORT).show()
            }
            endPlace == null -> {
                endPlace = place
                showRoutePoint("end", place)
                Toast.makeText(this, "목적지 설정: ${place.name}", Toast.LENGTH_SHORT).show()
                openRoutePage()
            }
            else -> {
                endPlace = place
                showRoutePoint("end", place)
                Toast.makeText(this, "목적지 변경: ${place.name}", Toast.LENGTH_SHORT).show()
                openRoutePage()
            }
        }

        suppressSearchWatcher = true
        binding.etSearch.setText(place.name)
        binding.etSearch.setSelection(binding.etSearch.text.length)
        suppressSearchWatcher = false
        hideSearchResults()

        binding.webMap.evaluateJavascript(
            "window.moveTo && window.moveTo(${place.lat}, ${place.lng}, 3);",
            null
        )
    }

    private fun showRoutePoint(type: String, place: SearchPlace) {
        val quotedType = JSONObject.quote(type)
        val quotedName = JSONObject.quote(place.name)
        binding.webMap.evaluateJavascript(
            "window.setRoutePoint && window.setRoutePoint($quotedType, ${place.lat}, ${place.lng}, $quotedName);",
            null
        )
    }

    private fun openRoutePage() {
        val start = startPlace ?: return
        val end = endPlace ?: return

        startActivity(
            Intent(this, RouteActivity::class.java).apply {
                putExtra("startName", start.name)
                putExtra("startAddress", start.address)
                putExtra("startLat", start.lat)
                putExtra("startLng", start.lng)
                putExtra("destination", end.name)
                putExtra("destinationAddress", end.address)
                putExtra("destinationLat", end.lat)
                putExtra("destinationLng", end.lng)
            }
        )
    }

    private fun hideSearchResults() {
        binding.resultScroll.visibility = View.GONE
        binding.resultList.removeAllViews()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showFilterDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_filter, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        view.findViewById<ImageView>(R.id.btnCloseFilter).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroy() {
        pendingSearch?.let(searchHandler::removeCallbacks)
        binding.webMap.destroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) return

        if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            moveToCurrentLocation()
        } else {
            Toast.makeText(this, "현재 위치를 보려면 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    inner class MapBridge {
        @JavascriptInterface
        fun onSearchResults(json: String) {
            Log.d(TAG, "onSearchResults: $json")
            val places = runCatching {
                val array = JSONArray(json)
                List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    SearchPlace(
                        name = item.optString("name"),
                        address = item.optString("address"),
                        lat = item.optDouble("lat"),
                        lng = item.optDouble("lng")
                    )
                }.filter { it.name.isNotBlank() }
            }.getOrElse {
                emptyList()
            }

            runOnUiThread {
                renderSearchResults(places)
            }
        }
    }

    data class SearchPlace(
        val name: String,
        val address: String,
        val lat: Double,
        val lng: Double
    )
}
