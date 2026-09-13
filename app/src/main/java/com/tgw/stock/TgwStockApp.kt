package com.tgw.stock

import android.app.Application
import com.tgw.stock.di.AppContainer
import com.tgw.stock.ui.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TgwStockApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        container = AppContainer(this)
        applicationScope.launch {
            container.sampleDataSeeder.seedIfEmpty()
        }
    }

    /** Called after a database restore so the next screen reads use a freshly built
     * container against the newly-copied file instead of any cached state. */
    fun reinitializeContainer() {
        container = AppContainer(this)
    }
}
