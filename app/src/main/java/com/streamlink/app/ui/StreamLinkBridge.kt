package com.streamlink.app.ui

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast

class StreamLinkBridge(
    private val context: Context,
    private val webView: WebView
) {

    // ---- Telegram Auth ----

    @JavascriptInterface
    fun sendTelegramCode(phoneNumber: String) {
        // Phase 2: TDLib integration will go here
        // For now simulate sending code
        webView.post {
            webView.evaluateJavascript(
                "window.onTelegramCodeSent && window.onTelegramCodeSent('$phoneNumber');",
                null
            )
        }
    }

    @JavascriptInterface
    fun verifyTelegramCode(code: String) {
        // Phase 2: TDLib integration will go here
        webView.post {
            webView.evaluateJavascript(
                "window.onTelegramVerified && window.onTelegramVerified(true);",
                null
            )
        }
    }

    // ---- Search ----

    @JavascriptInterface
    fun searchBot(botUsername: String, query: String) {
        // Phase 2: Send message to bot and get results
        webView.post {
            // Return mock results for now
            val mockResults = """[
                {"id":1,"title":"${query} - גרסה 1","year":"2023","quality":"4K"},
                {"id":2,"title":"${query} - גרסה 2","year":"2022","quality":"1080p"},
                {"id":3,"title":"${query} - Extended Cut","year":"2023","quality":"1080p"}
            ]"""
            webView.evaluateJavascript(
                "window.onSearchResults && window.onSearchResults($mockResults);",
                null
            )
        }
    }

    // ---- Download ----

    @JavascriptInterface
    fun downloadFile(fileId: String, fileName: String) {
        // Phase 2: Download via TDLib
        webView.post {
            webView.evaluateJavascript(
                "window.onDownloadStarted && window.onDownloadStarted('$fileId','$fileName');",
                null
            )
        }
    }

    @JavascriptInterface
    fun getDownloads(): String {
        // Return list of downloaded files
        return "[]"
    }

    // ---- Player ----

    @JavascriptInterface
    fun playFile(filePath: String, title: String) {
        // Phase 2: Launch ExoPlayer with filePath
        webView.post {
            Toast.makeText(context, "מפעיל: $title", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Storage ----

    @JavascriptInterface
    fun saveToLibrary(json: String) {
        // Phase 2: Save to Room database
    }

    @JavascriptInterface
    fun getLibrary(): String {
        // Phase 2: Get from Room database
        return "[]"
    }

    @JavascriptInterface
    fun showToast(message: String) {
        webView.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("StreamLink", message)
    }
}
