package com.szymo.lang

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
    private lateinit var requestPermission: ActivityResultLauncher<String>
    private lateinit var textSpeech: TextView

    private var doubleBackToExitPressedOnce = false
    private var link = 1
    private var user = ""
    private var update_status = false;

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mic = findViewById(R.id.mic)
        textSpeech = findViewById(R.id.textSpeech)
        mic.visibility = View.INVISIBLE
        textSpeech.visibility = View.INVISIBLE
        CheckFile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            createDynamicShortcut()
        }
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
                textSpeech.visibility = if (url.contains("mowienie.php")) View.VISIBLE else View.GONE
                textSpeech.text = "Kliknij na ikonkę mikrofonu aby rozpocząć rozpoznawanie mowy";
                dialog.dismiss()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (error?.errorCode in listOf(-2, -6, -8)) {
                    webView.loadUrl("file:///android_asset/error.html")
                }
            }
        }

        webView.loadUrl("https://langapp.edu.pl/android/profil.php?user=$user")


        requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                initializeSpeechRecognizer()
                launchSpeechRecognition()
            } else {
                Toast.makeText(this, "Potrzebne uprawnienie do mikrofonu.", Toast.LENGTH_SHORT).show()
            }
        }

        mic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermission.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                initializeSpeechRecognizer()
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
        if (intent.getBooleanExtra("from_shortcut", false)) {
            bottomNav.selectedItemId = R.id.page_2
            webView.loadUrl("https://langapp.edu.pl/android/losowe_zadanie.php?user=$user")
        }
    }
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createDynamicShortcut() {
        val shortcutManager = getSystemService(ShortcutManager::class.java)

        val shortcutIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("from_shortcut", true)
        }

        val shortcut = ShortcutInfo.Builder(this, "shortcut_slowka")
            .setShortLabel("Losowe zadanie")
            .setLongLabel("Wykonaj losowe zadanie")
            .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher)) // lub inna ikonka
            .setIntent(shortcutIntent)
            .build()

        shortcutManager.dynamicShortcuts = listOf(shortcut)
    }
    private fun initializeSpeechRecognizer() {
        if (!::speechRecognizer.isInitialized) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    textSpeech.text = "Słucham..."
                    textSpeech.visibility = View.VISIBLE
                }

                override fun onBeginningOfSpeech() {
                    textSpeech.text = "Mów teraz..."
                    textSpeech.visibility = View.VISIBLE
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    textSpeech.text = partial?.get(0) ?: ""
                    textSpeech.visibility = View.VISIBLE
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.get(0)?.replace("'", "\\'") ?: ""
                    textSpeech.text = "Rozpoznałem : ${matches?.get(0) ?: ""}"
                    textSpeech.visibility = View.VISIBLE

                    if (recognizedText.isNotEmpty()) {
                        webView.evaluateJavascript("check(\"$recognizedText\")") { returnValue ->
                            if (returnValue == "1") {
                                mic.visibility = View.GONE
                                textSpeech.visibility = View.GONE
                            }
                        }
                    }
                }

                override fun onEndOfSpeech() {
                    textSpeech.text = "Przetwarzam..."
                    textSpeech.visibility = View.VISIBLE
                }

                override fun onError(error: Int) {
                    textSpeech.text = "Błąd rozpoznawania"
                    Toast.makeText(applicationContext, "Błąd rozpoznawania mowy", Toast.LENGTH_SHORT).show()
                }

                // Pozostałe override – mogą pozostać puste:
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

        }
    }

    private fun launchSpeechRecognition() {
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        speechRecognizer.startListening(speechIntent)
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
                    AlertDialog.Builder(this)
                        .setTitle("Aktualizacja dostępna")
                        .setMessage("Dostępna jest nowa wersja aplikacji. Czy chcesz przejść do Sklepu Play, aby zaktualizować?")
                        .setPositiveButton("Tak") { dialog, _ ->
                            val appPackageName = packageName
                            try {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=$appPackageName")
                                    )
                                )
                            } catch (e: ActivityNotFoundException) {
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
        if (intent.getBooleanExtra("from_shortcut", false)) {
            bottomNav.selectedItemId = R.id.page_2
            webView.loadUrl("https://langapp.edu.pl/android/losowe_zadanie.php?user=$user")
        }
        if (!update_status) {
            update_status = true
            checkUpdate()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }


}
