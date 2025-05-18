package com.szymo.lang

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var webView: WebView
    private lateinit var mic: ImageButton
    private lateinit var dialog: AlertDialog
    private lateinit var speechLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermission: ActivityResultLauncher<String>

    private var doubleBackToExitPressedOnce = false
    private var link = 1
    private var user = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mic = findViewById(R.id.mic)
        mic.visibility = View.INVISIBLE
        CheckFile()

        webView = findViewById(R.id.webview)
        webView.settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = true
            allowFileAccess = true
        }

        dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog).create().apply {
            setView(layoutInflater.inflate(R.layout.progresslayout, null))
            setCanceledOnTouchOutside(false)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                dialog.show()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                mic.visibility = if (url.contains("mowienie.php")) View.VISIBLE else View.GONE
                dialog.dismiss()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (error?.errorCode in listOf(-2, -6, -8)) {
                    webView.loadUrl("file:///android_asset/error.html")
                }
            }
        }

        webView.loadUrl("https://langapp.edu.pl/android/profile.php?user=$user")

        // Rozpoznawanie mowy – nowy launcher
        speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val recognizedText = matches?.get(0)?.replace("'", "\\'") ?: ""
                if (recognizedText.isNotEmpty()) {
                    webView.evaluateJavascript("check(\"$recognizedText\")") { returnValue ->
                        if (returnValue == "1") {
                            mic.visibility = View.GONE
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Nie rozpoznano mowy. Spróbuj ponownie.", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchSpeechRecognition()
            } else {
                Toast.makeText(this, "Potrzebne uprawnienie do mikrofonu.", Toast.LENGTH_SHORT).show()
            }
        }

        mic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermission.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                launchSpeechRecognition()
            }
        }

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener {
            dialog.show()
            when (it.itemId) {
                R.id.page_1 -> {
                    link = 1
                    webView.loadUrl("https://langapp.edu.pl/android/profil.php?user=$user")
                    true
                }
                R.id.page_2 -> {
                    link = 2
                    webView.loadUrl("https://langapp.edu.pl/android/zdania.php?user=$user")
                    true
                }
                R.id.page_3 -> {
                    link = 3
                    webView.loadUrl("https://langapp.edu.pl/android/slowka.php?user=$user")
                    true
                }
                R.id.page_4 -> {
                    link = 4
                    webView.loadUrl("https://langapp.edu.pl/android/biblioteka.php?user=$user")
                    true
                }
                R.id.page_5 -> {
                    link = 5
                    webView.loadUrl("https://langapp.edu.pl/android/wyjasnienia.html")
                    true
                }
                else -> false
            }
        }
    }

    private fun launchSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Zacznij mówić")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechLauncher.launch(intent)
    }

    private fun CheckFile() {
        val sharedPreference = getSharedPreferences("my", MODE_PRIVATE)
        val name = sharedPreference.getString("Name", "none")
        if (name == "none") {
            startActivity(Intent(this, createProfile::class.java))
        } else {
            user = name.toString()
        }
    }

    private fun checkUpdate() {
        val url = "https://langapp.edu.pl/android/json.php"
        val queue: RequestQueue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->
            try {
                val ver: String = response.getString("ver")
                val myVer: String = packageManager.getPackageInfo(packageName, 0).versionName
                if (ver != myVer) {
                    // Tworzymy dialog z pytaniem
                    AlertDialog.Builder(this)
                        .setTitle("Aktualizacja dostępna")
                        .setMessage("Dostępna jest nowa wersja aplikacji. Czy chcesz przejść do Sklepu Play, aby zaktualizować?")
                        .setPositiveButton("Tak") { dialog, _ ->
                            // Otwórz Sklep Play
                            val appPackageName = packageName // lub bezpiecznie podać konkretny package name
                            try {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=$appPackageName")
                                    )
                                )
                            } catch (e: ActivityNotFoundException) {
                                // Gdy Sklep Play nie jest zainstalowany, otwórz link w przeglądarce
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                                    )
                                )
                            }
                            dialog.dismiss()
                        }
                        .setNegativeButton("Nie") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { error ->
            Log.e("TAG", "RESPONSE IS $error")
        })

        queue.add(request)
    }

    override fun onResume() {
        super.onResume()
        CheckFile()
        checkUpdate()
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        val webUrl = webView.url
        when (link) {
            1 -> Toast.makeText(this, "Wciśnij wstecz szybko dwa razy aby zamknąć", Toast.LENGTH_LONG).show()
            2 -> if (webUrl == "https://langapp.edu.pl/android/texting.php?user=$user") bottomNav.selectedItemId = R.id.page_1 else bottomNav.selectedItemId = R.id.page_2
            3 -> if (webUrl == "https://langapp.edu.pl/android/wyjasnienia.html") bottomNav.selectedItemId = R.id.page_1 else bottomNav.selectedItemId = R.id.page_3
            4 -> if (webUrl == "https://langapp.edu.pl/android/other.php?user=$user") bottomNav.selectedItemId = R.id.page_1 else bottomNav.selectedItemId = R.id.page_4
            5 -> if (webUrl == "https://langapp.edu.pl/android/library.php?user=$user") bottomNav.selectedItemId = R.id.page_1 else bottomNav.selectedItemId = R.id.page_5
        }

        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }
}
