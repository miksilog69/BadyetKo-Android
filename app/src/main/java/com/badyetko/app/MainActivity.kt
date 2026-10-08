package com.badyetko.app

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import java.io.File

class MainActivity : ComponentActivity() {
    companion object {
        private const val APP_URL = "https://liquid-budget-tracker.vercel.app/"
        private const val APP_HOST = "liquid-budget-tracker.vercel.app"
        private const val SUPABASE_HOST = "sgyleauirgtwlpzvhlup.supabase.co"
        private const val AUTH_CALLBACK = "badyetko://login-callback"
    }

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = fileCallback
        fileCallback = null
        if (callback == null) return@registerForActivityResult

        if (result.resultCode != RESULT_OK) {
            callback.onReceiveValue(null)
            return@registerForActivityResult
        }

        val data = result.data
        val uris = when {
            data?.clipData != null -> Array(data.clipData!!.itemCount) { i ->
                data.clipData!!.getItemAt(i).uri
            }
            data?.data != null -> arrayOf(data.data!!)
            else -> null
        }
        callback.onReceiveValue(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK

        webView = WebView(this)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        setContentView(webView)

        configureWebView()
        handleAuthCallback(intent?.data)

        if (savedInstanceState == null && !isAuthCallback(intent?.data)) {
            webView.loadUrl(APP_URL)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!handleAuthCallback(intent.data)) {
            val data = intent.data
            if (data != null && (data.scheme == "http" || data.scheme == "https")) {
                webView.loadUrl(data.toString())
            }
        }
    }

    private fun configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            userAgentString = userAgentString + " BadyetKoAndroid/1.1"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        webView.addJavascriptInterface(AndroidDownloadBridge(this), "BadyetKoAndroid")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback

                val intent = try {
                    fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    }
                } catch (_: Exception) {
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    }
                }

                return try {
                    filePicker.launch(intent)
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    Toast.makeText(this@MainActivity, "No file picker is available.", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request?.isForMainFrame != true) return false
                val uri = request.url ?: return false

                if (uri.host.equals(APP_HOST, ignoreCase = true)) {
                    return false
                }

                if (
                    uri.host.equals(SUPABASE_HOST, ignoreCase = true) &&
                    uri.path?.startsWith("/auth/v1/authorize") == true
                ) {
                    openAuthInBrowser(withAndroidRedirect(uri))
                    return true
                }

                if (uri.scheme == "mailto" || uri.scheme == "tel" || uri.scheme == "sms") {
                    return openExternal(uri)
                }

                if (uri.scheme == "http" || uri.scheme == "https") {
                    return openExternal(uri)
                }

                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectAndroidCompatibility()
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (url.startsWith("blob:", ignoreCase = true)) return@setDownloadListener
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                CookieManager.getInstance().getCookie(url)?.let {
                    request.addRequestHeader("Cookie", it)
                }
                val filename = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
                request.setTitle(filename)
                request.setDescription("Downloading from BadyetKo")
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "Downloading $filename", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, "Could not download this file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun injectAndroidCompatibility() {
        val js = """
            (() => {
              if (window.__badyetkoAndroidBridgeInstalled) return;
              window.__badyetkoAndroidBridgeInstalled = true;

              document.addEventListener('click', async (event) => {
                const a = event.target?.closest?.('a[download]');
                if (!a || !String(a.href || '').startsWith('blob:')) return;

                event.preventDefault();
                event.stopImmediatePropagation();

                try {
                  const response = await fetch(a.href);
                  const blob = await response.blob();
                  const reader = new FileReader();
                  reader.onloadend = () => {
                    if (typeof reader.result === 'string') {
                      window.BadyetKoAndroid?.saveBase64File(
                        a.download || 'BadyetKo-download',
                        reader.result
                      );
                    }
                  };
                  reader.readAsDataURL(blob);
                } catch (e) {
                  console.error('Android file export failed', e);
                }
              }, true);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun withAndroidRedirect(uri: Uri): Uri {
        val builder = uri.buildUpon().clearQuery()
        for (name in uri.queryParameterNames) {
            if (name == "redirect_to") continue
            for (value in uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value)
            }
        }
        builder.appendQueryParameter("redirect_to", AUTH_CALLBACK)
        return builder.build()
    }

    private fun openAuthInBrowser(uri: Uri) {
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(false)
                .build()
                .launchUrl(this, uri)
        } catch (_: Exception) {
            openExternal(uri)
        }
    }

    private fun openExternal(uri: Uri): Boolean {
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isAuthCallback(uri: Uri?): Boolean {
        return uri?.scheme == "badyetko" && uri.host == "login-callback"
    }

    private fun handleAuthCallback(uri: Uri?): Boolean {
        if (!isAuthCallback(uri)) return false

        val target = when {
            !uri?.fragment.isNullOrBlank() -> APP_URL + "#" + uri!!.fragment
            !uri?.query.isNullOrBlank() -> APP_URL + "?" + uri!!.query
            else -> APP_URL
        }
        webView.loadUrl(target)
        return true
    }

    private class AndroidDownloadBridge(
        private val context: Context
    ) {
        @JavascriptInterface
        fun saveBase64File(filename: String?, dataUrl: String?) {
            if (dataUrl.isNullOrBlank()) return

            try {
                val safeName = filename
                    ?.replace(Regex("[\\/:*?\"<>|]"), "_")
                    ?.takeIf { it.isNotBlank() }
                    ?: "BadyetKo-download"

                val raw = dataUrl.substringAfter(",", dataUrl)
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                val mime = dataUrl
                    .substringBefore(";")
                    .substringAfter("data:", "")
                    .ifBlank {
                        MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(safeName.substringAfterLast('.', ""))
                            ?: "application/octet-stream"
                    }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BadyetKo")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("Could not create download")
                    resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Toast.makeText(context, "Saved to Downloads/BadyetKo/$safeName", Toast.LENGTH_LONG).show()
                } else {
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, safeName)
                    file.writeBytes(bytes)
                    Toast.makeText(context, "Saved $safeName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not save exported file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        webView.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}
