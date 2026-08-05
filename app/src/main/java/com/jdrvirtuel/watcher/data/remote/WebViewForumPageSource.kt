package com.jdrvirtuel.watcher.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jdrvirtuel.watcher.domain.model.FetchResult
import com.jdrvirtuel.watcher.domain.repository.ForumPageSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONTokener
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class WebViewForumPageSource @Inject constructor(
    @ApplicationContext private val context: Context
) : ForumPageSource {

    override suspend fun fetchHtml(url: String): FetchResult = withContext(Dispatchers.Main) {
        withTimeoutOrNull(WebViewConstants.FETCH_TIMEOUT_MS) {
            var result = executeFetch(url)

            // Si challenge détecté, on attend et on réessaie une fois
            if (result is FetchResult.ChallengeRequired) {
                delay(WebViewConstants.CHALLENGE_RETRY_DELAY_MS)
                result = executeFetch(url)
            }
            
            if (result is FetchResult.Success) {
                CookieManager.getInstance().flush()
            }
            
            result
        } ?: FetchResult.Error("Dépassement du délai de récupération (30s)")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun executeFetch(url: String): FetchResult = suspendCancellableCoroutine { continuation ->
        val webView = WebView(context)

        val cleanup = {
            webView.stopLoading()
            webView.webViewClient = WebViewClient() // Impossible de mettre à null sur certaines versions
            webView.webChromeClient = null
            webView.destroy()
        }

        continuation.invokeOnCancellation {
            cleanup()
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = WebViewConstants.USER_AGENT
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.postDelayed({
                    if (continuation.isActive) {
                        view.evaluateJavascript(
                            "(function(){return document.documentElement.outerHTML;})();",
                            object : ValueCallback<String> {
                                override fun onReceiveValue(value: String?) {
                                    if (continuation.isActive) {
                                        val html = if (value != null && value != "null") {
                                            try {
                                                JSONTokener(value).nextValue().toString()
                                            } catch (e: Exception) {
                                                value
                                            }
                                        } else {
                                            ""
                                        }
                                        val result = classifyHtml(html)
                                        continuation.resume(result)
                                        cleanup()
                                    }
                                }
                            }
                        )
                    }
                }, WebViewConstants.PAGE_SETTLE_DELAY_MS)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true && continuation.isActive) {
                    continuation.resume(FetchResult.Error(error?.description?.toString() ?: "Erreur réseau inconnue"))
                    cleanup()
                }
            }
        }

        webView.loadUrl(url)
    }

    private fun classifyHtml(html: String): FetchResult {
        return when {
            html.isEmpty() -> FetchResult.Error("Le contenu récupéré est vide")
            html.contains(WebViewConstants.SUCCESS_MARKER) -> FetchResult.Success(html)
            html.contains("cf-turnstile") || 
            html.contains("challenge-platform") || 
            html.contains("Just a moment") || 
            html.contains("cf_chl") -> FetchResult.ChallengeRequired
            else -> FetchResult.Error("Structure de page inconnue (marqueur 'topictitle' absent)")
        }
    }
}
