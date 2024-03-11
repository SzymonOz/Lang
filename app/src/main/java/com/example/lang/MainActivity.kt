package com.example.lang

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.checkSelfPermission
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    lateinit var bottomNav : BottomNavigationView
    var user = ""
    lateinit var webView: WebView
    lateinit var mic: ImageButton
    lateinit var dialog : AlertDialog
    private var doubleBackToExitPressedOnce = false
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // do something
        }
    companion object {
        private const val REQUEST_CODE_STT = 1
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mic = findViewById(R.id.mic)
        mic.visibility = View.INVISIBLE
        CheckFile();

        webView = findViewById(R.id.webview)

        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE;
        webView.settings.domStorageEnabled = true;
        webView.settings.allowFileAccess = true
        webView.loadUrl("https://langapp.eu/profile.php?user="+user)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                dialogLoad()
            }
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val url = request?.url.toString()
                view?.loadUrl(url)
                return super.shouldOverrideUrlLoading(view, request)

            }
            override fun onPageFinished(view: WebView, url: String) {

                if(url.contains("https://langapp.eu/speech.php")){
                    mic.visibility = View.VISIBLE

                }else{
                    mic.visibility = View.GONE
                }
                super.onPageFinished(view, url)
                dialog.dismiss()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                val l = error?.errorCode
                if(l == -2 || l == -6 || l == -8){
                    webView.loadUrl("file:///android_asset/error.html")
                }
                super.onReceivedError(view, request, error)
            }

        }
        mic.setOnClickListener{
            if (checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED

            ){
                Toast.makeText(this, "Cos.", Toast.LENGTH_LONG).show()
                // Pass any permission you want while launching
                requestPermission.launch(Manifest.permission.RECORD_AUDIO)

            } else {
                val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                sttIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Zacznij mówić")
                try {
                    // Start the intent for a result, and pass in our request code.
                    startActivityForResult(sttIntent, REQUEST_CODE_STT)
                } catch (e: ActivityNotFoundException) {
                    // Handling error when the service is not available.
                    e.printStackTrace()
                    Toast.makeText(this, "Your device does not support STT.", Toast.LENGTH_LONG).show()
                }
            }

            //webView.evaluateJavascript("Test('Andrdoid-->Html')", null);


        }
        bottomNav = findViewById(R.id.bottom_navigation) as BottomNavigationView
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_1 -> {
                    webView.loadUrl("https://langapp.eu/profile.php?user="+user)
                    true
                }
                R.id.page_2 -> {
                    webView.loadUrl("https://langapp.eu/texting.php?user="+user)
                    true
                }
                R.id.page_3 -> {
                    webView.loadUrl("https://langapp.eu/wyjasnienia.html")
                    true
                }
                R.id.page_4 -> {
                    webView.loadUrl("https://langapp.eu/other.php?user="+user)
                    true
                }
                R.id.page_5 -> {
                    webView.loadUrl("https://langapp.eu/library.php?user="+user)
                    true
                }

                else -> {
                    false;
                }
            }
        }



    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // Handle the result for our request code.
            REQUEST_CODE_STT -> {
                // Safety checks to ensure data is available.
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Retrieve the result array.
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    // Ensure result array is not null or empty to avoid errors.
                    if (!result.isNullOrEmpty()) {
                        // Recognized text is in the first position.
                        val recognizedText = result[0].replace("'","\'")
                        // Do what you want with the recognized text.

                        webView.evaluateJavascript("check(\"$recognizedText\")") { returnValue ->
                            if(returnValue.equals("1")){
                                mic.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    fun dialogLoad() {

        dialog = AlertDialog.Builder(this,R.style.CustomAlertDialog)
            .create()
        val view = layoutInflater.inflate(R.layout.progresslayout,null)
        dialog.setView(view)

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }
    private fun CheckFile(){
        val sharedPreference =  getSharedPreferences("my", MODE_PRIVATE)
        val name = sharedPreference.getString("Name", "none")
        if(name.equals("none")){
            val intent = Intent(this, createProfile::class.java)
            startActivity(intent)
        }else{
            user = name.toString()
        }




    }
    private fun checkUpdate(){
        val url = "https://langapp.eu/json.php"
        val queue: RequestQueue = Volley.newRequestQueue(this)

        // on below line we are creating a variable for request
        // and initializing it with json object request
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->


            try {
                // on below line we are getting data from our response
                // and setting it in variables.
                val ver: String = response.getString("ver")
                val pInfo = packageManager.getPackageInfo(
                    packageName, 0
                )
                val my_ver: String = pInfo.versionName;
                if(!ver.equals(my_ver)){
                    Toast.makeText(this,"Dostępna jest nowa wersja, zajrzyj na https://langapp.eu",Toast.LENGTH_LONG).show();
                }



            } catch (e: Exception) {
                // on below line we are
                // handling our exception.
                e.printStackTrace()
            }

        }, { error ->
            // this method is called when we get
            // any error while fetching data from our API
            Log.e("TAG", "RESPONSE IS $error")
            // in this case we are simply displaying a toast message.

        })
        // at last we are adding
        // our request to our queue.
        queue.add(request)
    }
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Wciśnij dwa razy wstecz aby zamknąć", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }
    override fun onResume() {
        super.onResume()
        CheckFile()
        checkUpdate();
    }

}