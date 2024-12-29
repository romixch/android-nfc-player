package ch.romix.nfcplayer

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.Credentials
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

fun createNextcloudImageLoader(context: Context, nextcloud: Nextcloud): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = {
                        OkHttpClient.Builder()
                            .addInterceptor(RequestHeaderInterceptor(nextcloud))
                            .build()
                    }
                )
            )
        }
        .build()
}

private class RequestHeaderInterceptor(val nextcloud: Nextcloud) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val headers = Headers.Builder()
            .set("Authorization", Credentials.basic(nextcloud.user, nextcloud.password))
            .build()
        val request = chain.request().newBuilder()
            .headers(headers)
            .build()
        val response = chain.proceed(request)
        println("Loaded image again")
        return response
    }
}
