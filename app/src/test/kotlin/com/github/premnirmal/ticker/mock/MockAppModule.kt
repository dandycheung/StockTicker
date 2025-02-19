package com.github.premnirmal.ticker.mock

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.repo.QuoteDao
import com.github.premnirmal.ticker.repo.QuotesDB
import com.github.premnirmal.ticker.repo.StocksStorage
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Module(includes = [MockNetworkModule::class])
class MockAppModule(private val app: StocksApp) {

  @Provides internal fun provideApplicationContext(): Context = app

  @Singleton @Provides internal fun provideCoroutineScope(): CoroutineScope {
    return CoroutineScope(Dispatchers.Unconfined)
  }

  @Provides @Singleton internal fun provideClock(): AppClock = Mocker.provide(AppClock::class)

  @Provides @Singleton internal fun provideDefaultSharedPreferences(
    context: Context): SharedPreferences {
    return context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
  }

  @Provides @Singleton internal fun provideAppWidgetManager(): AppWidgetManager =
    AppWidgetManager.getInstance(app)

  @Provides @Singleton internal fun provideAppPreferences(): AppPreferences = AppPreferences()

  @Provides @Singleton internal fun provideStorage(): StocksStorage =
    StocksStorage()

  @Provides @Singleton internal fun provideAnalytics(): Analytics = Mocker.provide(Analytics::class)

  @Provides @Singleton fun provideQuotesDB(context: Context): QuotesDB {
    return Mocker.provide(QuotesDB::class)
  }

  @Provides @Singleton fun provideQuoteDao(db: QuotesDB): QuoteDao {
    return Mocker.provide(QuoteDao::class)
  }
}