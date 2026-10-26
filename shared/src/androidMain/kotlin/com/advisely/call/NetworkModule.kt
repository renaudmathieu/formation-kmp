package com.advisely.call

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

// Module 6 : le moteur HTTP fourni par Android
val androidNetworkModule = module { single<HttpClientEngine> { OkHttp.create() } }
