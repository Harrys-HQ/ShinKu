package com.shinku.reader.domain.ai

import okhttp3.Authenticator
import okhttp3.CookieJar
import okhttp3.OkHttpClient

fun OkHttpClient.forReaderContext(): OkHttpClient = newBuilder()
    .apply {
        interceptors().clear()
        networkInterceptors().clear()
    }
    .cookieJar(CookieJar.NO_COOKIES)
    .cache(null)
    .authenticator(Authenticator.NONE)
    .followRedirects(false)
    .followSslRedirects(false)
    .build()
