package com.informatika.bondoman.di

import com.informatika.bondoman.BuildConfig
import com.informatika.bondoman.model.remote.service.AuthService
import com.informatika.bondoman.model.remote.service.TransactionService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

val networkModule = module {
    // Dependency: HttpLoggingInterceptor
    single<Interceptor> {
        HttpLoggingInterceptor { message -> Timber.tag("OkHttp").d(message) }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    // Dependency: OkHttpClient
    single {
        OkHttpClient.Builder()
            .addInterceptor(get<Interceptor>())
            .build()
    }

    // Dependency: Retrofit
    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }

    // Dependency: ApiService
    single<AuthService> { get<Retrofit>().create(AuthService::class.java) }

    // Dependency: TransactionService
    single<TransactionService> { get<Retrofit>().create(TransactionService::class.java) }
}