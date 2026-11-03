package com.youki.pawnforge

import android.content.Intent
import android.graphics.Insets
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemBarsCompatibilityTest {

    @Test
    fun statusBarRemainsVisibleAndContentRespectsItsSafeArea() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent) as MainActivity

        try {
            instrumentation.waitForIdleSync()

            val hiddenFlags = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            assertEquals(0, activity.window.decorView.systemUiVisibility and hiddenFlags)
            assertEquals(
                0,
                activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val root = content.getChildAt(0)
            val statusBarResource = activity.resources.getIdentifier(
                "status_bar_height",
                "dimen",
                "android"
            )
            val navigationBarResource = activity.resources.getIdentifier(
                "navigation_bar_height",
                "dimen",
                "android"
            )
            val statusBarHeight = if (statusBarResource != 0) {
                activity.resources.getDimensionPixelSize(statusBarResource)
            } else {
                24
            }
            val navigationBarHeight = if (navigationBarResource != 0) {
                activity.resources.getDimensionPixelSize(navigationBarResource)
            } else {
                24
            }

            if (Build.VERSION.SDK_INT >= 35) {
                // Exercise the inset listener even on headless emulator images with no visible status-bar inset.
                val simulatedSystemBars = WindowInsets.Builder()
                    .setInsets(
                        WindowInsets.Type.statusBars(),
                        Insets.of(0, statusBarHeight, 0, 0)
                    )
                    .setInsets(
                        WindowInsets.Type.navigationBars(),
                        Insets.of(0, 0, 0, navigationBarHeight)
                    )
                    .build()

                root.dispatchApplyWindowInsets(simulatedSystemBars)
                assertTrue(
                    "Edge-to-edge layout must reserve the supplied status-bar inset.",
                    root.paddingTop >= statusBarHeight
                )
                assertTrue(
                    "Edge-to-edge layout must reserve the supplied navigation-bar inset.",
                    root.paddingBottom >= navigationBarHeight
                )
            } else {
                val location = IntArray(2)
                root.getLocationOnScreen(location)
                assertTrue(
                    "Content must start below the visible status bar.",
                    location[1] >= statusBarHeight && location[1] > 0
                )
            }
        } finally {
            activity.finish()
        }
    }
}
