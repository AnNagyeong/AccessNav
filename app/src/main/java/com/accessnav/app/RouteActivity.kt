package com.accessnav.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.accessnav.app.databinding.ActivityRouteBinding
import org.json.JSONObject

class RouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val startName = intent.getStringExtra("startName") ?: "출발지"
        val destination = intent.getStringExtra("destination") ?: "목적지"
        val destinationAddress = intent.getStringExtra("destinationAddress") ?: "주소 정보 없음"
        val startLat = intent.getDoubleExtra("startLat", 37.56184)
        val startLng = intent.getDoubleExtra("startLng", 127.03811)
        val destinationLat = intent.getDoubleExtra("destinationLat", 37.5573)
        val destinationLng = intent.getDoubleExtra("destinationLng", 127.0466)

        binding.tvTopDestination.text = destination
        binding.tvDestination.text = "${destination}까지"

        setupSummary(destination, startLat, startLng, destinationLat, destinationLng)
        setupMap(startLat, startLng, destinationLat, destinationLng)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRouteList.setOnClickListener {
            startActivity(
                Intent(this, RouteListActivity::class.java).apply {
                    putExtra("startName", startName)
                    putExtra("destination", destination)
                    putExtra("destinationAddress", destinationAddress)
                }
            )
        }
        binding.btnStartNav.setOnClickListener {
            startActivity(Intent(this, RouteGuidanceActivity::class.java))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMap(
        startLat: Double,
        startLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ) {
        binding.webRouteMap.webViewClient = WebViewClient()
        binding.webRouteMap.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        val html = routeMapHtml(startLat, startLng, destinationLat, destinationLng)
        binding.webRouteMap.loadDataWithBaseURL(
            "http://127.0.0.1:3000/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun setupSummary(
        destination: String,
        startLat: Double,
        startLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ) {
        val distance = distanceMeters(startLat, startLng, destinationLat, destinationLng)
        val minutes = maxOf(1, Math.round(distance / 70.0).toInt())
        val danger = destination.length % 3 + 1

        binding.tvTime.text = "${minutes}분"
        binding.tvDistance.text = if (distance < 1000) {
            "${Math.round(distance)}m"
        } else {
            String.format("%.1fkm", distance / 1000.0)
        }
        binding.tvDanger.text = "${danger}곳"
    }

    private fun routeMapHtml(
        startLat: Double,
        startLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ): String {
        val points = routeShapePoints(startLat, startLng, destinationLat, destinationLng)
        val pointJson = JSONObject.quote(points.joinToString("|") { "${it.first},${it.second}" })

        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
              <style>
                html, body, #map { margin:0; width:100%; height:100%; overflow:hidden; background:#eef6fa; }
                .dot { width:14px; height:14px; border-radius:50%; background:#1f86ff; border:4px solid #fff; box-shadow:0 2px 8px rgba(0,0,0,.25); box-sizing:border-box; }
                .pin { width:34px; height:34px; border-radius:50% 50% 50% 0; background:#5aa7ff; transform:rotate(-45deg); box-shadow:0 2px 8px rgba(0,0,0,.22); }
                .pin:after { content:""; position:absolute; left:10px; top:10px; width:14px; height:14px; border-radius:50%; background:#fff; }
              </style>
            </head>
            <body>
              <div id="map"></div>
              <script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=80e48dd01aae3f043d16e5ad41071f5d&libraries=services"></script>
              <script>
                const startLat = $startLat;
                const startLng = $startLng;
                const endLat = $destinationLat;
                const endLng = $destinationLng;
                const routePoints = $pointJson.split("|").filter(Boolean).map(v => {
                  const [lat, lng] = v.split(",").map(Number);
                  return new kakao.maps.LatLng(lat, lng);
                });
                const map = new kakao.maps.Map(document.getElementById("map"), {
                  center: new kakao.maps.LatLng((startLat + endLat) / 2, (startLng + endLng) / 2),
                  level: 3
                });
                const start = new kakao.maps.LatLng(startLat, startLng);
                const end = new kakao.maps.LatLng(endLat, endLng);

                function overlay(position, className, content, xAnchor = .5, yAnchor = .5) {
                  const el = document.createElement("div");
                  el.className = className;
                  if (content) el.textContent = content;
                  const ov = new kakao.maps.CustomOverlay({ position, content: el, xAnchor, yAnchor, zIndex: 10 });
                  ov.setMap(map);
                  return ov;
                }

                const segmentColors = ["#48d10f", "#48d10f", "#ffcd33", "#f26a6a", "#48d10f"];
                for (let i = 0; i < routePoints.length - 1; i += 1) {
                  new kakao.maps.Polyline({
                    map,
                    path: [routePoints[i], routePoints[i + 1]],
                    strokeWeight: 7,
                    strokeColor: segmentColors[i] || "#48d10f",
                    strokeOpacity: .95,
                    strokeStyle: "solid"
                  });
                }
                overlay(start, "dot");
                overlay(end, "pin", "", .5, 1);
                map.setCenter(new kakao.maps.LatLng((startLat + endLat) / 2, (startLng + endLng) / 2));
                map.setLevel(3);
                setTimeout(() => {
                  map.relayout();
                  map.setCenter(new kakao.maps.LatLng((startLat + endLat) / 2, (startLng + endLng) / 2));
                }, 300);
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun routeShapePoints(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<Pair<Double, Double>> {
        val latStep = (endLat - startLat) / 5.0
        val lngStep = (endLng - startLng) / 5.0
        return listOf(
            startLat to startLng,
            startLat + latStep to startLng + lngStep,
            startLat + latStep * 2 to startLng + lngStep * 2,
            startLat + latStep * 3 to startLng + lngStep * 3,
            startLat + latStep * 4 to startLng + lngStep * 4,
            endLat to endLng
        )
    }

    private fun distanceMeters(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(startLat, startLng, endLat, endLng, result)
        return result[0].toDouble()
    }

    override fun onDestroy() {
        binding.webRouteMap.destroy()
        super.onDestroy()
    }
}
