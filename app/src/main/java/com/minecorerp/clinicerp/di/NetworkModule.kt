package com.minecorerp.clinicerp.di

import com.minecorerp.clinicerp.data.local.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val cookieInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val sessionId = runBlocking { sessionManager.sessionId.firstOrNull() }
            val server = runBlocking { sessionManager.serverUrl.firstOrNull() }?.toHttpUrlOrNull()
            val trustedOrigin = server != null && originalRequest.url.scheme == server.scheme &&
                originalRequest.url.host == server.host && originalRequest.url.port == server.port
            val requestBuilder = originalRequest.newBuilder()
            if (sessionId != null && trustedOrigin) {
                requestBuilder.header("Cookie", "session_id=$sessionId")
            }
            
            val response = chain.proceed(requestBuilder.build())
            
            // Capture session_id from Set-Cookie header if present
            val cookies = response.headers("Set-Cookie")
            val sessionCookie = cookies.find { it.contains("session_id=", ignoreCase = true) }
            if (sessionCookie != null && trustedOrigin) {
                val sid = sessionCookie.substringAfter("session_id=").substringBefore(";")
                if (sid != sessionId) {
                    runBlocking { sessionManager.updateSessionId(sid) }
                }
            }
            
            response
        }

        return OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(cookieInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://example.odoo.com/") 
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideOdooService(retrofit: Retrofit): com.minecorerp.clinicerp.data.remote.OdooService {
        return retrofit.create(com.minecorerp.clinicerp.data.remote.OdooService::class.java)
    }
}
