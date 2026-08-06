package com.junps.dompetku.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.junps.dompetku.ui.theme.DompetKuTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingScreen_exposesAccessibleLabel() {
        composeRule.setContent {
            DompetKuTheme { LoadingScreen("Memuat laporan pengeluaran") }
        }

        composeRule.onNodeWithContentDescription("Memuat laporan pengeluaran").assertIsDisplayed()
    }

    @Test
    fun messageScreen_displaysMessageAndInvokesAction() {
        var clicked = false
        composeRule.setContent {
            DompetKuTheme {
                MessageScreen(
                    title = "Data tidak dapat dimuat",
                    message = "Silakan coba kembali.",
                    actionLabel = "Kembali",
                    onAction = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Silakan coba kembali.").assertIsDisplayed()
        composeRule.onNodeWithText("Kembali").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }
}
