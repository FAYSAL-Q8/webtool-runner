/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.webkit.URLUtil

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.CalendarContract
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast

class ToolAppBridge(
    private val context: Context,
    private val onDownloadSaved: (String) -> Unit = {}
) {
    var onToggleDesktopModeRequested: (() -> Unit)? = null

    @JavascriptInterface
    fun toggleDesktopMode() {
        Handler(Looper.getMainLooper()).post {
            onToggleDesktopModeRequested?.invoke()
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        try {
            val ms = durationMs.coerceIn(5, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(ms)
                }
            }
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun shareText(title: String, text: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(sendIntent, title.ifEmpty { "Share" }).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun copyToClipboard(label: String, text: String): Boolean {
        return try {
            Handler(Looper.getMainLooper()).post {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText(label.ifEmpty { "ToolCraft" }, text)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun addToCalendar(title: String, description: String, beginTimeMillis: Long, endTimeMillis: Long): Boolean {
        return try {
            Handler(Looper.getMainLooper()).post {
                val now = System.currentTimeMillis()
                val begin = if (beginTimeMillis > 0) beginTimeMillis else now + 3600000L
                val end = if (endTimeMillis > 0 && endTimeMillis >= begin) endTimeMillis else begin + 3600000L
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title.ifEmpty { "Task Deadline" })
                    putExtra(CalendarContract.Events.DESCRIPTION, description)
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    Toast.makeText(context, "Opening Calendar...", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Intent.ACTION_EDIT).apply {
                        type = "vnd.android.cursor.item/event"
                        putExtra("title", title)
                        putExtra("description", description)
                        putExtra("beginTime", begin)
                        putExtra("endTime", end)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """{"os":"Android","release":"${Build.VERSION.RELEASE}","sdk":${Build.VERSION.SDK_INT},"model":"${Build.MODEL}","brand":"${Build.BRAND}","connected":true}"""
    }

    @JavascriptInterface
    fun isConnected(): Boolean = true

    @JavascriptInterface
    fun openAuthorProfile(): Boolean {
        return try {
            Handler(Looper.getMainLooper()).post {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/FAYSAL-Q8")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun startStreamDownload(streamId: String, filename: String, mimeType: String, totalBytes: Long): Boolean {
        return FileDownloadManager.startStreamDownload(context, streamId, filename, mimeType, totalBytes)
    }

    @JavascriptInterface
    fun appendStreamChunk(streamId: String, chunkBase64: String): Boolean {
        return FileDownloadManager.appendStreamChunk(streamId, chunkBase64)
    }

    @JavascriptInterface
    fun finishStreamDownload(streamId: String): Boolean {
        return FileDownloadManager.finishStreamDownload(context, streamId) { success, path, _ ->
            if (success) {
                onDownloadSaved(path)
            }
        }
    }

    @JavascriptInterface
    fun cancelStreamDownload(streamId: String) {
        FileDownloadManager.cancelStreamDownload(context, streamId)
    }

    @JavascriptInterface
    fun saveBase64(dataUriOrBase64: String, filename: String, mimeType: String) {
        FileDownloadManager.saveBase64Data(context, dataUriOrBase64, filename, mimeType) { success, path, _ ->
            if (success) {
                onDownloadSaved(path)
            }
        }
    }

    @JavascriptInterface
    fun saveText(content: String, filename: String, mimeType: String) {
        FileDownloadManager.saveText(context, content, filename, mimeType) { success, path, _ ->
            if (success) {
                onDownloadSaved(path)
            }
        }
    }

    @JavascriptInterface
    fun saveData(key: String, value: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences("tool_bridge_data", Context.MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun getData(key: String): String {
        return try {
            val prefs = context.getSharedPreferences("tool_bridge_data", Context.MODE_PRIVATE)
            prefs.getString(key, "") ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}

class ToolWebChromeClient(
    private val onProgress: (Int) -> Unit = {},
    private val onConsoleLog: (ConsoleLogEntry) -> Unit = {},
    private val onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean = { _, _ -> false },
    private val onRequestWebPermission: (PermissionRequest) -> Unit = { it.deny() },
    private val onRequestGeolocation: (String, GeolocationPermissions.Callback) -> Unit = { _, cb -> cb.invoke("", false, false) }
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgress(newProgress)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage != null) {
            val level = when (consoleMessage.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                ConsoleMessage.MessageLevel.WARNING -> "WARN"
                ConsoleMessage.MessageLevel.TIP -> "INFO"
                else -> "LOG"
            }
            val entry = ConsoleLogEntry(
                level = level,
                message = consoleMessage.message() ?: "",
                lineNumber = consoleMessage.lineNumber(),
                sourceId = consoleMessage.sourceId() ?: ""
            )
            onConsoleLog(entry)
        }
        return true
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.let {
            onRequestWebPermission(it)
        }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        if (origin != null && callback != null) {
            onRequestGeolocation(origin, callback)
        } else {
            callback?.invoke(origin, false, false)
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onFileChooser(filePathCallback, fileChooserParams)
    }
}

class ToolWebViewClient(
    private val onPageFinishedAction: (WebView) -> Unit = {}
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: ""
        if (url.startsWith("https://github.com/FAYSAL-Q8", ignoreCase = true) ||
            url.contains("github.com/FAYSAL-Q8", ignoreCase = true)) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                view?.context?.startActivity(intent)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:") || url.startsWith("about:")) {
            return false
        }
        return false
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.let { webView ->
            onPageFinishedAction(webView)
        }
    }
}

object WebEngineHelper {

    const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    fun configureWebView(webView: WebView, onToggleDesktopMode: (() -> Unit)? = null) {
        webView.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
            setBackgroundColor(0xFF090D16.toInt())

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            val bridge = ToolAppBridge(context)
            bridge.onToggleDesktopModeRequested = onToggleDesktopMode
            addJavascriptInterface(bridge, "AndroidApp")
            addJavascriptInterface(bridge, "AndroidBridge")
            addJavascriptInterface(bridge, "AndroidDownloader")

            setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                if (url.startsWith("blob:") || url.startsWith("data:")) {
                    val safeFilename = filename.replace("'", "\\'")
                    val safeMime = (mimetype ?: "").replace("'", "\\'")
                    val js = """
                        (function() {
                            try {
                                fetch('$url').then(function(r){ return r.blob(); }).then(function(b){
                                    if (typeof _tcStreamBlob === 'function') {
                                        _tcStreamBlob(b, '$safeFilename', b.type || '$safeMime');
                                    } else {
                                        var reader = new FileReader();
                                        reader.onloadend = function(){
                                            window.AndroidDownloader && window.AndroidDownloader.saveBase64(reader.result, '$safeFilename', b.type || '$safeMime');
                                        };
                                        reader.readAsDataURL(b);
                                    }
                                }).catch(function(err){
                                    console.error('Blob download error: ' + err);
                                });
                            } catch(e) {
                                console.error('Download trigger error: ' + e);
                            }
                        })();
                    """.trimIndent()
                    evaluateJavascript(js, null)
                } else if (url.startsWith("http://") || url.startsWith("https://")) {
                    FileDownloadManager.downloadHttp(context, url, userAgent, contentDisposition, mimetype, filename)
                }
            }
        }
    }

    fun applyDesktopMode(webView: WebView, isDesktop: Boolean) {
        webView.settings.apply {
            userAgentString = if (isDesktop) DESKTOP_USER_AGENT else null
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        val js = if (isDesktop) {
            """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=1280, initial-scale=0.75, maximum-scale=3.0, user-scalable=yes';
                document.documentElement.classList.add('desktop-mode');
                document.body.classList.add('desktop-mode');
                var btn = document.getElementById('desktopToggleBtn');
                if (btn) { btn.textContent = '📱 Mobile'; btn.classList.add('active'); }
                window.dispatchEvent(new Event('resize'));
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (meta) {
                    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
                }
                document.documentElement.classList.remove('desktop-mode');
                document.body.classList.remove('desktop-mode');
                var btn = document.getElementById('desktopToggleBtn');
                if (btn) { btn.textContent = '🖥️ Desktop'; btn.classList.remove('active'); }
                window.dispatchEvent(new Event('resize'));
            })();
            """.trimIndent()
        }
        webView.evaluateJavascript(js, null)
    }

    fun loadToolHtml(webView: WebView, toolId: Long, htmlContent: String) {
        val downloadHookScript = """
            <script>
            (function() {
                if (window._toolCraftDownloadHooked) return;
                window._toolCraftDownloadHooked = true;

                window._tcStreamBlob = function(blob, filename, mimeType) {
                    filename = filename || 'downloaded_file';
                    var effectiveMime = mimeType || blob.type || 'application/octet-stream';
                    var total = blob.size || 0;
                    var streamId = 'str_' + Date.now() + '_' + Math.floor(Math.random() * 1000000);
                    var CHUNK_SIZE = 256 * 1024; // 256KB safe IPC chunk

                    if (window.AndroidDownloader && window.AndroidDownloader.startStreamDownload) {
                        var started = window.AndroidDownloader.startStreamDownload(streamId, filename, effectiveMime, total);
                        if (!started) return;

                        var offset = 0;
                        function sendNextChunk() {
                            if (offset >= total) {
                                window.AndroidDownloader.finishStreamDownload(streamId);
                                return;
                            }
                            var sliceEnd = Math.min(offset + CHUNK_SIZE, total);
                            var slice = blob.slice(offset, sliceEnd);
                            var reader = new FileReader();
                            reader.onload = function(evt) {
                                var res = evt.target.result || '';
                                var comma = res.indexOf(',');
                                var base64Data = (comma >= 0) ? res.substring(comma + 1) : res;
                                window.AndroidDownloader.appendStreamChunk(streamId, base64Data);
                                offset = sliceEnd;
                                setTimeout(sendNextChunk, 0);
                            };
                            reader.onerror = function() {
                                window.AndroidDownloader.cancelStreamDownload(streamId);
                            };
                            reader.readAsDataURL(slice);
                        }
                        sendNextChunk();
                    } else {
                        var reader = new FileReader();
                        reader.onloadend = function(){
                            window.AndroidDownloader && window.AndroidDownloader.saveBase64(reader.result, filename, effectiveMime);
                        };
                        reader.readAsDataURL(blob);
                    }
                };

                window.downloadFile = function(dataOrBlob, filename, mimeType) {
                    filename = filename || 'downloaded_file';
                    mimeType = mimeType || 'application/octet-stream';
                    if (dataOrBlob instanceof Blob) {
                        window._tcStreamBlob(dataOrBlob, filename, mimeType);
                    } else if (typeof dataOrBlob === 'string') {
                        if (dataOrBlob.startsWith('blob:') || dataOrBlob.startsWith('data:')) {
                            fetch(dataOrBlob)
                                .then(function(r) { return r.blob(); })
                                .then(function(b) { window._tcStreamBlob(b, filename, mimeType); })
                                .catch(function() {
                                    window.AndroidDownloader && window.AndroidDownloader.saveBase64(dataOrBlob, filename, mimeType);
                                });
                        } else {
                            window.AndroidDownloader && window.AndroidDownloader.saveText(dataOrBlob, filename, mimeType);
                        }
                    }
                };

                window.Android = {
                    showToast: function(msg) {
                        if (window.AndroidApp && window.AndroidApp.showToast) {
                            window.AndroidApp.showToast(String(msg));
                        } else {
                            console.log('[Android Toast]', msg);
                        }
                    },
                    vibrate: function(ms) {
                        if (window.AndroidApp && window.AndroidApp.vibrate) {
                            window.AndroidApp.vibrate(Number(ms) || 20);
                        } else if (navigator.vibrate) {
                            navigator.vibrate(Number(ms) || 20);
                        }
                    },
                    share: function(title, text) {
                        if (window.AndroidApp && window.AndroidApp.shareText) {
                            window.AndroidApp.shareText(title || '', text || '');
                        } else if (navigator.share) {
                            navigator.share({ title: title, text: text }).catch(function(){});
                        }
                    },
                    copy: function(text, label) {
                        if (window.AndroidApp && window.AndroidApp.copyToClipboard) {
                            return window.AndroidApp.copyToClipboard(label || 'ToolCraft', text || '');
                        } else if (navigator.clipboard) {
                            navigator.clipboard.writeText(text).then(function() {
                                window.Android.showToast('Copied to clipboard');
                            });
                            return true;
                        }
                        return false;
                    },
                    downloadFile: function(dataOrBlob, filename, mimeType) {
                        window.downloadFile(dataOrBlob, filename, mimeType);
                    },
                    addToCalendar: function(title, description, beginTimeMillis, endTimeMillis) {
                        if (window.AndroidApp && window.AndroidApp.addToCalendar) {
                            return window.AndroidApp.addToCalendar(title || '', description || '', Number(beginTimeMillis) || 0, Number(endTimeMillis) || 0);
                        } else {
                            console.log('[Android Calendar]', title, description, beginTimeMillis);
                            return false;
                        }
                    },
                    getDeviceInfo: function() {
                        if (window.AndroidApp && window.AndroidApp.getDeviceInfo) {
                            try { return JSON.parse(window.AndroidApp.getDeviceInfo()); } catch(e){}
                        }
                        return { os: 'Android', connected: true };
                    },
                    isConnected: function() {
                        return !!(window.AndroidApp && window.AndroidApp.isConnected && window.AndroidApp.isConnected());
                    },
                    saveData: function(key, value) {
                        if (window.AndroidApp && window.AndroidApp.saveData) {
                            return window.AndroidApp.saveData(String(key), String(value));
                        } else {
                            try { localStorage.setItem(String(key), String(value)); return true; } catch(e){ return false; }
                        }
                    },
                    getData: function(key) {
                        if (window.AndroidApp && window.AndroidApp.getData) {
                            return window.AndroidApp.getData(String(key));
                        } else {
                            try { return localStorage.getItem(String(key)) || ''; } catch(e){ return ''; }
                        }
                    }
                };

                window.AndroidBridge = window.Android;

                document.addEventListener('click', function(e) {
                    var el = e.target;
                    while (el && el.tagName !== 'A') { el = el.parentElement; }
                    if (el && (el.hasAttribute('download') || el.download)) {
                        var href = el.getAttribute('href') || el.href;
                        var filename = el.getAttribute('download') || el.download || 'downloaded_file';
                        if (href && (href.startsWith('blob:') || href.startsWith('data:'))) {
                            e.preventDefault();
                            e.stopPropagation();
                            fetch(href)
                                .then(function(r) { return r.blob(); })
                                .then(function(b) { window._tcStreamBlob(b, filename, b.type || 'application/octet-stream'); })
                                .catch(function(err) { console.error(err); });
                        }
                    }
                }, true);
            })();
            </script>
        """.trimIndent()

        val instrumentedHtml = instrumentHtml(htmlContent, toolId)

        val originBaseUrl = "https://tool-$toolId.internal.runner/"
        webView.loadDataWithBaseURL(
            originBaseUrl,
            instrumentedHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    fun instrumentHtml(htmlContent: String, toolId: Long): String {
        val downloadHookScript = """
            <script>
            (function() {
                if (window._toolCraftDownloadHooked) return;
                window._toolCraftDownloadHooked = true;

                window._tcStreamBlob = function(blob, filename, mimeType) {
                    filename = filename || 'downloaded_file';
                    var effectiveMime = mimeType || blob.type || 'application/octet-stream';
                    var total = blob.size || 0;
                    var streamId = 'str_' + Date.now() + '_' + Math.floor(Math.random() * 1000000);
                    var CHUNK_SIZE = 256 * 1024; // 256KB safe IPC chunk

                    if (window.AndroidDownloader && window.AndroidDownloader.startStreamDownload) {
                        var started = window.AndroidDownloader.startStreamDownload(streamId, filename, effectiveMime, total);
                        if (!started) return;

                        var offset = 0;
                        function sendNextChunk() {
                            if (offset >= total) {
                                window.AndroidDownloader.finishStreamDownload(streamId);
                                return;
                            }
                            var sliceEnd = Math.min(offset + CHUNK_SIZE, total);
                            var slice = blob.slice(offset, sliceEnd);
                            var reader = new FileReader();
                            reader.onload = function(evt) {
                                var res = evt.target.result || '';
                                var comma = res.indexOf(',');
                                var base64Data = (comma >= 0) ? res.substring(comma + 1) : res;
                                window.AndroidDownloader.appendStreamChunk(streamId, base64Data);
                                offset = sliceEnd;
                                setTimeout(sendNextChunk, 0);
                            };
                            reader.onerror = function() {
                                window.AndroidDownloader.cancelStreamDownload(streamId);
                            };
                            reader.readAsDataURL(slice);
                        }
                        sendNextChunk();
                    } else {
                        var reader = new FileReader();
                        reader.onloadend = function(){
                            window.AndroidDownloader && window.AndroidDownloader.saveBase64(reader.result, filename, effectiveMime);
                        };
                        reader.readAsDataURL(blob);
                    }
                };

                window.downloadFile = function(dataOrBlob, filename, mimeType) {
                    filename = filename || 'downloaded_file';
                    mimeType = mimeType || 'application/octet-stream';
                    if (dataOrBlob instanceof Blob) {
                        window._tcStreamBlob(dataOrBlob, filename, mimeType);
                    } else if (typeof dataOrBlob === 'string') {
                        if (dataOrBlob.startsWith('blob:') || dataOrBlob.startsWith('data:')) {
                            fetch(dataOrBlob)
                                .then(function(r) { return r.blob(); })
                                .then(function(b) { window._tcStreamBlob(b, filename, mimeType); })
                                .catch(function() {
                                    window.AndroidDownloader && window.AndroidDownloader.saveBase64(dataOrBlob, filename, mimeType);
                                });
                        } else {
                            window.AndroidDownloader && window.AndroidDownloader.saveText(dataOrBlob, filename, mimeType);
                        }
                    }
                };

                window.Android = {
                    showToast: function(msg) {
                        if (window.AndroidApp && window.AndroidApp.showToast) {
                            window.AndroidApp.showToast(String(msg));
                        } else {
                            console.log('[Android Toast]', msg);
                        }
                    },
                    vibrate: function(ms) {
                        if (window.AndroidApp && window.AndroidApp.vibrate) {
                            window.AndroidApp.vibrate(Number(ms) || 20);
                        } else if (navigator.vibrate) {
                            navigator.vibrate(Number(ms) || 20);
                        }
                    },
                    share: function(title, text) {
                        if (window.AndroidApp && window.AndroidApp.shareText) {
                            window.AndroidApp.shareText(title || '', text || '');
                        } else if (navigator.share) {
                            navigator.share({ title: title, text: text }).catch(function(){});
                        }
                    },
                    copy: function(text, label) {
                        if (window.AndroidApp && window.AndroidApp.copyToClipboard) {
                            return window.AndroidApp.copyToClipboard(label || 'ToolCraft', text || '');
                        } else if (navigator.clipboard) {
                            navigator.clipboard.writeText(text).then(function() {
                                window.Android.showToast('Copied to clipboard');
                            });
                            return true;
                        }
                        return false;
                    },
                    downloadFile: function(dataOrBlob, filename, mimeType) {
                        window.downloadFile(dataOrBlob, filename, mimeType);
                    },
                    addToCalendar: function(title, description, beginTimeMillis, endTimeMillis) {
                        if (window.AndroidApp && window.AndroidApp.addToCalendar) {
                            return window.AndroidApp.addToCalendar(title || '', description || '', Number(beginTimeMillis) || 0, Number(endTimeMillis) || 0);
                        } else {
                            console.log('[Android Calendar]', title, description, beginTimeMillis);
                            return false;
                        }
                    },
                    getDeviceInfo: function() {
                        if (window.AndroidApp && window.AndroidApp.getDeviceInfo) {
                            try { return JSON.parse(window.AndroidApp.getDeviceInfo()); } catch(e){}
                        }
                        return { os: 'Android', connected: true };
                    },
                    isConnected: function() {
                        return !!(window.AndroidApp && window.AndroidApp.isConnected && window.AndroidApp.isConnected());
                    },
                    saveData: function(key, value) {
                        if (window.AndroidApp && window.AndroidApp.saveData) {
                            window.AndroidApp.saveData(String(key), String(value));
                        } else {
                            try { localStorage.setItem(String(key), String(value)); } catch(e){}
                        }
                    },
                    getData: function(key) {
                        if (window.AndroidApp && window.AndroidApp.getData) {
                            return window.AndroidApp.getData(String(key));
                        } else {
                            try { return localStorage.getItem(String(key)) || ''; } catch(e){ return ''; }
                        }
                    }
                };

                window.AndroidBridge = window.Android;

                document.addEventListener('click', function(e) {
                    var el = e.target;
                    while (el && el.tagName !== 'A') { el = el.parentElement; }
                    if (el && (el.hasAttribute('download') || el.download)) {
                        var href = el.getAttribute('href') || el.href;
                        var filename = el.getAttribute('download') || el.download || 'downloaded_file';
                        if (href && (href.startsWith('blob:') || href.startsWith('data:'))) {
                            e.preventDefault();
                            e.stopPropagation();
                            fetch(href)
                                .then(function(r) { return r.blob(); })
                                .then(function(b) { window._tcStreamBlob(b, filename, b.type || 'application/octet-stream'); })
                                .catch(function(err) { console.error(err); });
                        }
                    }
                }, true);
            })();
            </script>
        """.trimIndent()

        return if (htmlContent.contains("<head>", ignoreCase = true)) {
            htmlContent.replaceFirst("<head>", "<head>\n$downloadHookScript", ignoreCase = true)
        } else if (htmlContent.contains("<html>", ignoreCase = true)) {
            htmlContent.replaceFirst("<html>", "<html>\n$downloadHookScript", ignoreCase = true)
        } else {
            downloadHookScript + "\n" + htmlContent
        }
    }

    fun loadTool(webView: WebView, tool: com.example.data.model.Tool) {
        if (tool.isMultiFile && tool.projectDir != null) {
            val projectDir = com.example.engine.ZipToolManager.getToolProjectDir(webView.context, tool.projectDir)
            if (tool.isBuiltIn) {
                val slug = tool.projectDir.removePrefix("default_").replace('_', '-')
                val zipName = "$slug.zip"
                try {
                    val stream = webView.context.assets.open("default_tools/$zipName")
                    stream.use { input ->
                        projectDir.mkdirs()
                        java.util.zip.ZipInputStream(input).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val name = entry.name.replace('\\', '/')
                                if (!name.startsWith("__MACOSX") && !name.contains(".DS_Store")) {
                                    val dest = java.io.File(projectDir, name)
                                    if (entry.isDirectory) {
                                        dest.mkdirs()
                                    } else {
                                        dest.parentFile?.mkdirs()
                                        java.io.FileOutputStream(dest).use { fos ->
                                            zis.copyTo(fos)
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            val entryFile = java.io.File(projectDir, tool.entryPoint)
            val rawContent = if (entryFile.exists()) entryFile.readText(Charsets.UTF_8) else tool.htmlCode
            val instrumentedHtml = instrumentHtml(rawContent, tool.id)
            val originBaseUrl = "file://${projectDir.absolutePath}/"
            webView.loadDataWithBaseURL(
                originBaseUrl,
                instrumentedHtml,
                "text/html",
                "UTF-8",
                null
            )
        } else {
            loadToolHtml(webView, tool.id, tool.htmlCode)
        }
    }

    fun wipeToolStorage(webView: WebView, toolId: Long, onComplete: () -> Unit = {}) {
        val origin = "https://tool-$toolId.internal.runner/"
        try {
            // Delete specific origin storage in WebStorage
            WebStorage.getInstance().deleteOrigin(origin)
            
            // Clear Cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()

            // Run JavaScript to clear localStorage, sessionStorage, and IndexedDB
            val jsCleanup = """
                (function() {
                    try { window.localStorage.clear(); } catch(e){}
                    try { window.sessionStorage.clear(); } catch(e){}
                    try {
                        if (window.indexedDB && window.indexedDB.databases) {
                            window.indexedDB.databases().then(function(dbs) {
                                dbs.forEach(function(db) { window.indexedDB.deleteDatabase(db.name); });
                            });
                        }
                    } catch(e){}
                })();
            """.trimIndent()
            webView.evaluateJavascript(jsCleanup, null)

            // Clear cache
            webView.clearCache(true)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            onComplete()
        }
    }

    fun captureThumbnail(webView: WebView, onBitmapCaptured: (Bitmap) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        // Wait 400ms for dynamic render / canvas initial frames to finish
        handler.postDelayed({
            try {
                val width = webView.width
                val height = webView.height
                if (width > 0 && height > 0) {
                    val targetWidth = 480
                    val targetHeight = (height.toFloat() / width.toFloat() * targetWidth).toInt().coerceIn(320, 640)
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val scaleX = targetWidth.toFloat() / width.toFloat()
                    val scaleY = targetHeight.toFloat() / height.toFloat()
                    canvas.scale(scaleX, scaleY)
                    webView.draw(canvas)
                    onBitmapCaptured(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }
}
