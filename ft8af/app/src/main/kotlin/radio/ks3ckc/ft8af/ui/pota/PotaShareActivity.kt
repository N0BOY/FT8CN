package radio.ks3ckc.ft8af.ui.pota

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.k1af.ft8af.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import radio.ks3ckc.ft8af.pota.PotaSessionManager
import radio.ks3ckc.ft8af.pota.model.PotaActivation
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/** A bundled renderer shared with ft8af.app. The JS bridge never loads remote content. */
class PotaShareActivity : ComponentActivity() {
    private lateinit var web: WebView
    private var payload: JSONObject? = null
    private var publishedUrl: String? = null
    private val sharing = AtomicBoolean(false)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        // Use an inset-aware root: target SDK 36 enforces edge-to-edge.
        val root = android.widget.FrameLayout(this)
        root.setBackgroundColor(android.graphics.Color.rgb(7, 9, 15))
        root.addView(web)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(root)
        web.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                val name = request.url.path?.removePrefix("/activation/")
                if (request.url.scheme == "https" && request.url.host == "appassets.androidplatform.net" &&
                    request.url.path == "/activation/$name" && name in ASSETS
                ) {
                    val mime = when (name?.substringAfterLast('.')) {
                        "html" -> "text/html"
                        "css" -> "text/css"
                        "json" -> "application/json"
                        "svg" -> "image/svg+xml"
                        "woff2" -> "font/woff2"
                        else -> "application/javascript"
                    }
                    return WebResourceResponse(mime, "UTF-8", assets.open("activation/$name"))
                }
                return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(byteArrayOf()))
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (request.isForMainFrame && uri.scheme == "https" && uri.host == "ft8af.app") {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                }
                return true
            }
        }
        web.addJavascriptInterface(Bridge(), "FT8Share")
        web.loadUrl("https://appassets.androidplatform.net/activation/index.html")
    }

    private fun failed() {
        sharing.set(false)
        if (!isDestroyed) web.evaluateJavascript("shareFailed()", null)
    }

    private inner class Bridge {
        @JavascriptInterface
        fun ready() {
            lifecycleScope.launch {
                try {
                    val activation = PotaActivation(
                        id = intent.getLongExtra("id", -1),
                        parkRef = intent.getStringExtra("parks").orEmpty(),
                        operator = intent.getStringExtra("operator"),
                        startedAtMs = intent.getLongExtra("start", 0),
                        endedAtMs = intent.getLongExtra("end", 0),
                        qsoCount = 0,
                        notes = null,
                    )
                    payload = withContext(Dispatchers.IO) {
                        buildPotaSharePayload(activation, PotaSessionManager.getQsosForActivation(activation))
                    }
                    val json = JSONObject.quote(payload.toString()).replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
                    web.evaluateJavascript("showActivation(JSON.parse($json))", null)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failed()
                }
            }
        }

        @JavascriptInterface
        fun publish() {
            if (!sharing.compareAndSet(false, true)) return
            lifecycleScope.launch {
                try {
                    val body = payload?.toString() ?: error("Summary not loaded")
                    val url = publishedUrl ?: withContext(Dispatchers.IO) { publishSummary(body) }
                    publishedUrl = url
                    web.evaluateJavascript("sharePublished(${JSONObject.quote(url)})", null)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failed()
                }
            }
        }

        @JavascriptInterface
        fun image(dataUrl: String) {
            if (!sharing.get() || publishedUrl == null) return
            lifecycleScope.launch {
                try {
                    val file = withContext(Dispatchers.IO) {
                        require(dataUrl.startsWith("data:image/png;base64,") && dataUrl.length <= 8_000_000)
                        val bytes = Base64.decode(dataUrl.substringAfter(','), Base64.DEFAULT)
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        require(options.outWidth == 1080 && options.outHeight == 1350 && options.outMimeType == "image/png")
                        val directory = File(requireNotNull(externalCacheDir), "activation-shares").apply { mkdirs() }
                        // Keep recent attachments readable by receiving apps; remove only old generated PNGs.
                        directory.listFiles()?.filter { it.name.startsWith("ft8af-") && it.extension == "png" && it.lastModified() < System.currentTimeMillis() - 7 * 86_400_000L }?.forEach { it.delete() }
                        File.createTempFile("ft8af-", ".png", directory).apply { writeBytes(bytes) }
                    }
                    val uri = FileProvider.getUriForFile(this@PotaShareActivity, "radio.ks3ckc.ft8af.fileprovider", file)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, getString(R.string.pota_social_caption, publishedUrl))
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pota_social_share))
                        clipData = ClipData.newRawUri("POTA activation", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(send, getString(R.string.pota_social_share)))
                    sharing.set(false)
                    web.evaluateJavascript("shareFinished()", null)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failed()
                    Toast.makeText(this@PotaShareActivity, R.string.pota_social_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        web.removeJavascriptInterface("FT8Share")
        web.stopLoading()
        web.destroy()
        super.onDestroy()
    }

    companion object {
        private val ASSETS = setOf(
            "index.html", "summary.js", "summary.css", "qrcode.js", "world_land.json", "icon.svg",
            "geist-latin-wght-normal.woff2", "geist-mono-latin-wght-normal.woff2",
        )

        fun open(context: Context, activation: PotaActivation) {
            if (activation.isActive) return
            context.startActivity(Intent(context, PotaShareActivity::class.java).apply {
                putExtra("id", activation.id)
                putExtra("parks", activation.parkRef)
                putExtra("operator", activation.operator)
                putExtra("start", activation.startedAtMs)
                putExtra("end", activation.endedAtMs)
            })
        }

        private fun publishSummary(body: String): String {
            val connection = URL("https://ft8af.app/api/activations").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 20_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                check(connection.responseCode == 200) { "Publishing unavailable" }
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val url = JSONObject(response).getString("url")
                require(Regex("https://ft8af\\.app/activation\\?id=[a-f0-9]{32}").matches(url))
                return url
            } finally {
                connection.disconnect()
            }
        }
    }
}
