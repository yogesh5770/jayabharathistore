package com.jayabharathistore.app.di

import com.jayabharathistore.app.data.api.TwilioApi
import com.jayabharathistore.app.data.api.OrderApi
import com.jayabharathistore.app.data.api.GitHubApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // For Emulator: use 10.0.2.2 to access host's localhost
    // For Real Device: use Cloudflare Worker URL
    private const val BASE_URL = "https://twilio-backend.yogesh234456.workers.dev/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "JayabharathiApp/1.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    @javax.inject.Named("TwilioRetrofit")
    fun provideTwilioRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://twilio-backend.yogesh234456.workers.dev/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @javax.inject.Named("FunctionsRetrofit")
    fun provideFunctionsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://us-central1-jayabhararthi-st.cloudfunctions.net/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @javax.inject.Named("GitHubRetrofit")
    fun provideGitHubRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTwilioApi(@javax.inject.Named("TwilioRetrofit") retrofit: Retrofit): TwilioApi {
        return retrofit.create(TwilioApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOrderApi(@javax.inject.Named("FunctionsRetrofit") retrofit: Retrofit): OrderApi {
        return retrofit.create(OrderApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGitHubApi(@javax.inject.Named("GitHubRetrofit") retrofit: Retrofit): GitHubApi {
        return retrofit.create(GitHubApi::class.java)
    }
}
