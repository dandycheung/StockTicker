package com.github.premnirmal.ticker.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.analytics.ClickEvent
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.CommitsProvider
import com.github.premnirmal.ticker.news.NewsFeedFragment
import com.github.premnirmal.ticker.portfolio.search.SearchFragment
import com.github.premnirmal.ticker.settings.SettingsFragment
import com.github.premnirmal.ticker.settings.SettingsParentFragment
import com.github.premnirmal.ticker.settings.WidgetSettingsFragment
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.ticker.widget.WidgetsFragment
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class MainActivity : BaseActivity<ActivityMainBinding>(), BottomNavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemReselectedListener,
    SettingsFragment.Parent, WidgetSettingsFragment.Parent {

  companion object {
    private const val DIALOG_SHOWN: String = "DIALOG_SHOWN"
    private val FRAGMENT_MAP =
      mapOf<Int, String>(R.id.action_portfolio to HomeFragment::class.java.name,
          R.id.action_widgets to WidgetsFragment::class.java.name,
          R.id.action_search to SearchFragment::class.java.name,
          R.id.action_settings to SettingsParentFragment::class.java.name,
          R.id.action_feed to NewsFeedFragment::class.java.name)
  }

  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var commitsProvider: CommitsProvider

  private var currentChild: ChildFragment? = null
  private var rateDialogShown = false
  override val simpleName: String = "MainActivity"


  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    savedInstanceState?.let { rateDialogShown = it.getBoolean(DIALOG_SHOWN, false) }

    binding.bottomNavigation.setOnNavigationItemSelectedListener(this)
    binding.bottomNavigation.setOnNavigationItemReselectedListener(this)

    currentChild = if (savedInstanceState == null) {
      val fragment = HomeFragment()
      supportFragmentManager.beginTransaction()
          .add(R.id.fragment_container, fragment, fragment.javaClass.name)
          .show(fragment)
          .commit()
      fragment
    } else {
      supportFragmentManager.findFragmentById(R.id.fragment_container) as ChildFragment
    }

    val tutorialShown = appPreferences.tutorialShown()
    if (!tutorialShown) {
      showTutorial()
    }

    if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
      showWhatsNew()
    }
  }

  override fun onResume() {
    super.onResume()
    binding.bottomNavigation.menu.findItem(R.id.action_widgets).isEnabled = widgetDataProvider.hasWidget()
  }

  override fun onBackPressed() {
    val eaten = onNavigationItemSelected(binding.bottomNavigation.menu.findItem(R.id.action_portfolio))
    if (eaten) {
      binding.bottomNavigation.selectedItemId = R.id.action_portfolio
    }
    if (!eaten && !maybeAskToRate()) {
      super.onBackPressed()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean(DIALOG_SHOWN, rateDialogShown)
    super.onSaveInstanceState(outState)
  }

  private fun maybeAskToRate(): Boolean {
    if (!rateDialogShown && appPreferences.shouldPromptRate()) {
      Builder(this)
          .setTitle(R.string.like_our_app)
          .setMessage(R.string.please_rate)
          .setPositiveButton(R.string.yes) { dialog, _ ->
            sendToPlayStore()
            appPreferences.userDidRate()
            dialog.dismiss()
          }.setNegativeButton(R.string.later) { dialog, _ ->
            dialog.dismiss()
          }.create().show()
      rateDialogShown = true
      return true
    }
    return false
  }

  private fun sendToPlayStore() {
    val marketUri: Uri = Uri.parse("market://details?id=$packageName")
    val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
    marketIntent.resolveActivity(packageManager)?.let {
      startActivity(marketIntent)
    }
  }

  // BottomNavigationView.OnNavigationItemSelectedListener

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    val itemId = item.itemId
    var fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_MAP[itemId])
    if (fragment == null) {
      fragment = when (itemId) {
        R.id.action_portfolio -> HomeFragment()
        R.id.action_widgets -> WidgetsFragment()
        R.id.action_search -> SearchFragment()
        R.id.action_settings -> SettingsParentFragment()
        R.id.action_feed -> NewsFeedFragment()
        else -> {
          throw IllegalStateException("Unknown bottom nav itemId: $itemId - ${item.title}")
        }
      }
      supportFragmentManager.beginTransaction()
          .add(R.id.fragment_container, fragment, fragment::class.java.name)
          .hide(fragment)
          .show(currentChild as Fragment)
          .commitNowAllowingStateLoss()
    }
    if (fragment.isHidden) {
      supportFragmentManager.beginTransaction()
          .hide(currentChild as Fragment)
          .show(fragment)
          .commit()
      currentChild = fragment as ChildFragment
      analytics.trackClickEvent(
          ClickEvent("BottomNavClick")
              .addProperty("NavItem", item.title.toString())
      )
      return true
    }
    return false
  }

  // BottomNavigationView.OnNavigationItemReselectedListener

  override fun onNavigationItemReselected(item: MenuItem) {
    val itemId = item.itemId
    val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_MAP[itemId])
    (fragment as? ChildFragment)?.scrollToTop()
  }

  // SettingsFragment.Parent

  override fun showTutorial() {
    showDialog(getString(R.string.how_to_title), getString(R.string.how_to))
    appPreferences.setTutorialShown(true)
  }

  override fun showWhatsNew() {
    val dialog = showDialog(
        getString(R.string.whats_new_in, BuildConfig.VERSION_NAME),
        getString(R.string.loading)
    )
    lifecycleScope.launch {
      commitsProvider.fetchWhatsNew().apply {
        if (wasSuccessful) {
          val whatsNew = data.joinToString("\n\u25CF ", "\u25CF ")
          dialog.setMessage(whatsNew)
          appPreferences.saveVersionCode(BuildConfig.VERSION_CODE)
        } else {
          dialog.setMessage("${getString(R.string.error_fetching_whats_new)}\n\n :( ${error.message.orEmpty()}")
        }
      }
    }
  }

  // WidgetSettingsFragment.Parent

  override fun openSearch(widgetId: Int) {
    binding.bottomNavigation.selectedItemId = R.id.action_search
  }
}