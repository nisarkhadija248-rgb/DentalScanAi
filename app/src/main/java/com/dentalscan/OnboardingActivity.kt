package com.dentalscan

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.min

/**
 * First-launch introduction: 4 swipeable slides with dot indicators.
 * Shown once, before the disclaimer (see SplashActivity). Both Skip and Get Started mark it as
 * complete ("onboarding_complete") and continue to the disclaimer, or straight to the home screen
 * if the disclaimer was already accepted.
 */
class OnboardingActivity : BaseActivity() {

    private class Page(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        @StringRes val text: Int
    )

    private val pages = listOf(
        Page(R.drawable.ic_medical_services, R.string.onb_1_title, R.string.onb_1_text),
        Page(R.drawable.ic_phone_android, R.string.onb_2_title, R.string.onb_2_text),
        Page(R.drawable.ic_search, R.string.onb_3_title, R.string.onb_3_text),
        Page(R.drawable.ic_trending_up, R.string.onb_4_title, R.string.onb_4_text)
    )

    private lateinit var pager: ViewPager2
    private lateinit var skipButton: Button
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var dotsRow: LinearLayout
    private val dots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pager = findViewById(R.id.onboardingPager)
        skipButton = findViewById(R.id.onboardingSkip)
        prevButton = findViewById(R.id.onboardingPrev)
        nextButton = findViewById(R.id.onboardingNext)
        dotsRow = findViewById(R.id.onboardingDots)

        skipButton.text = getString(R.string.onb_skip)
        prevButton.text = getString(R.string.onb_prev)

        pager.adapter = PageAdapter(pages)
        pager.offscreenPageLimit = pages.size

        // Slides fade and the illustration drifts a little while swiping
        // (skipped when the phone's animations are turned off)
        if (animationsEnabled()) {
            pager.setPageTransformer { page, position ->
                val dist = min(abs(position), 1f)
                page.alpha = 1f - dist * 0.6f
                page.findViewById<View>(R.id.onboardingArt)?.let { art ->
                    art.translationX = -position * page.width * 0.2f
                    art.scaleX = 1f - dist * 0.15f
                    art.scaleY = 1f - dist * 0.15f
                }
            }
        }

        buildDots()
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = showPage(position)
        })
        showPage(0)

        skipButton.setOnClickListener { finishOnboarding() }
        prevButton.setOnClickListener {
            if (pager.currentItem > 0) pager.currentItem = pager.currentItem - 1
        }
        nextButton.setOnClickListener {
            if (pager.currentItem >= pages.lastIndex) finishOnboarding()
            else pager.currentItem = pager.currentItem + 1
        }

        // Back goes to the previous slide; on the first slide it leaves the app as usual
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pager.currentItem > 0) pager.currentItem = pager.currentItem - 1 else finish()
            }
        })
    }

    private fun animationsEnabled(): Boolean = try {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    } catch (e: Exception) {
        true
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildDots() {
        dotsRow.removeAllViews()
        dots.clear()
        repeat(pages.size) {
            val dot = View(this)
            dot.layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
            dotsRow.addView(dot)
            dots += dot
        }
    }

    /** Dots, Previous / Skip visibility and the Next / Get Started label for the current slide. */
    private fun showPage(position: Int) {
        val last = position == pages.lastIndex

        dots.forEachIndexed { i, dot ->
            val active = i == position
            dot.setBackgroundResource(
                if (active) R.drawable.bg_onboarding_dot_active else R.drawable.bg_onboarding_dot_inactive
            )
            dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).apply {
                width = if (active) dp(24) else dp(8)
            }
        }

        prevButton.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        skipButton.visibility = if (last) View.INVISIBLE else View.VISIBLE
        nextButton.text = getString(if (last) R.string.onb_get_started else R.string.onb_next)
    }

    private fun finishOnboarding() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_complete", true).apply()

        val next = if (prefs.getBoolean("disclaimer_accepted", false)) {
            MainActivity::class.java
        } else {
            DisclaimerActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }

    private class PageAdapter(private val pages: List<Page>) : RecyclerView.Adapter<PageAdapter.Holder>() {

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.onboardingIcon)
            val title: TextView = view.findViewById(R.id.onboardingTitle)
            val subtitle: TextView = view.findViewById(R.id.onboardingSubtitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding_page, parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val page = pages[position]
            holder.icon.setImageResource(page.icon)
            holder.title.setText(page.title)
            holder.subtitle.setText(page.text)
        }

        override fun getItemCount() = pages.size
    }
}
