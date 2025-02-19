package com.github.premnirmal.ticker

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.AppComponent
import com.github.premnirmal.ticker.components.AppModule
import com.github.premnirmal.ticker.components.DaggerAppComponent
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.LoggingTree
import com.github.premnirmal.ticker.network.CommitsProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.notifications.NotificationsHandler
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import com.jakewharton.threetenabp.AndroidThreeTen
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
open class StocksApp : MultiDexApplication() {

  class InjectionHolder {
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var notificationsHandler: NotificationsHandler
    @Inject lateinit var widgetDataProvider: WidgetDataProvider
  }

  private val holder = InjectionHolder()

  override fun onCreate() {
    super.onCreate()
    initLogger()
    initThreeTen()
    Injector.init(createAppComponent())
    ViewPump.init(
        ViewPump.builder()
            .addInterceptor(
                CalligraphyInterceptor(
                    CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Ubuntu-Regular.ttf")
                        .setFontAttrId(io.github.inflationx.calligraphy3.R.attr.fontPath)
                        .build()))
            .build())
    Injector.appComponent.inject(holder)
    AppCompatDelegate.setDefaultNightMode(holder.appPreferences.nightMode)
    initAnalytics()
    initNotificationHandler()
    holder.widgetDataProvider.widgetDataList()
  }

  protected open fun initNotificationHandler() {
    holder.notificationsHandler.initialize()
  }

  protected open fun initThreeTen() {
    AndroidThreeTen.init(this)
  }

  protected open fun createAppComponent(): AppComponent {
    return DaggerAppComponent.builder()
        .appModule(AppModule(this))
        .build()
  }

  protected open fun initLogger() {
    Timber.plant(LoggingTree())
  }

  protected open fun initAnalytics() {
    holder.analytics.initialize(this)
  }
}