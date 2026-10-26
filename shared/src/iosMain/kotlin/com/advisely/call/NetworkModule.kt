package com.advisely.call

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

// Module 6 : le moteur HTTP fourni par iOS
val iosNetworkModule = module { single<HttpClientEngine> { Darwin.create() } }
