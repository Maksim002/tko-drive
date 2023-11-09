package ru.telecor.gm.mobile.droid.di.providers

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.telecor.gm.mobile.droid.BuildConfig
import ru.telecor.gm.mobile.droid.di.ServerPath
import ru.telecor.gm.mobile.droid.entities.GmServerInfo
import ru.telecor.gm.mobile.droid.model.data.server.TruckCrewApi
import ru.telecor.gm.mobile.droid.model.data.storage.GmServerPrefs
import timber.log.Timber
import java.lang.Exception
import java.net.URL
import javax.inject.Inject
import javax.inject.Provider

/**
 * Project Truck Crew
 * Package ru.telecor.gm.mobile.droid.di
 *
 * Retrofit API interface provider.
 *
 * Created by Artem Skopincev (aka sharpyx) 27.07.2020
 * Copyright © 2020 TKOInform. All rights reserved.
 */
class ApiProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    @ServerPath private val serverPath: String,
) : Provider<TruckCrewApi> {

    override fun get(): TruckCrewApi {

        val url = serverPath + "mobile/"

        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TruckCrewApi::class.java)
    }
}