package com.angadi.app

import android.app.Application
import android.content.Context
import com.angadi.app.data.local.AngadiDatabase
import com.angadi.app.data.repository.AddressRepository
import com.angadi.app.data.repository.AssistantRepository
import com.angadi.app.data.repository.OrderRepository
import com.angadi.app.data.repository.ProfileRepository
import com.angadi.app.data.repository.ReviewRepository
import com.angadi.app.data.repository.SettingsRepository
import com.angadi.app.data.repository.ShopRepository

/**
 * Hand-written dependency container. The app has a single module and a handful of singletons, so
 * constructor injection through this container (and ViewModel factories) is used instead of Hilt,
 * which adds an annotation-processing step with no benefit at this size.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AngadiDatabase by lazy { AngadiDatabase.create(appContext) }
    val settings by lazy { SettingsRepository(appContext) }
    val shop by lazy { ShopRepository(database) }
    val orders by lazy { OrderRepository(database, shop) }
    val addresses by lazy { AddressRepository(database) }
    val reviews by lazy { ReviewRepository(database) }
    val profile by lazy { ProfileRepository(database) }
    val assistant by lazy { AssistantRepository() }
}

class AngadiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
