package com.accessnav.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.accessnav.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupClicks()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webMap.webViewClient = WebViewClient()
        binding.webMap.webChromeClient = WebChromeClient()

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

        binding.webMap.loadUrl("file:///android_asset/yj.html")
    }

    private fun setupClicks() {
        binding.btnClear.setOnClickListener {
            binding.etSearch.setText("")
        }

        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnFavorite.setOnClickListener {
            binding.webMap.evaluateJavascript(
                "window.filterMarkers && window.filterMarkers('favorite');",
                null
            )
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
    }

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
        binding.webMap.destroy()
        super.onDestroy()
    }
}