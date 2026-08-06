package com.junps.dompetku.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.Text
import androidx.navigation.compose.rememberNavController
import com.junps.dompetku.ui.theme.DompetKuTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DompetKuAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bottomNavigation_switchesTabs() {
        composeRule.setContent {
            DompetKuTheme {
                DompetKuApp(
                    dashboardContent = { Text("DompetKu") },
                    historyContent = { Text("Belum ada riwayat") },
                )
            }
        }

        composeRule.onNodeWithText("Riwayat").performClick()

        composeRule.onNodeWithText("Belum ada riwayat").assertIsDisplayed()
    }

    @Test
    fun fab_opensForm_hidesBottomBar_andBackReturnsToDashboard() {
        lateinit var navController: androidx.navigation.NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            DompetKuTheme {
                DompetKuApp(
                    navController = navController,
                    dashboardContent = { Text("DompetKu") },
                    historyContent = { Text("Belum ada riwayat") },
                    transactionFormContent = { Text("Tambah transaksi") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Tambah transaksi").performClick()

        composeRule.onNodeWithText("Tambah transaksi").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("transaction_form?transactionId={transactionId}", navController.currentDestination?.route)
            navController.popBackStack()
        }
        composeRule.onNodeWithText("DompetKu").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(AppDestination.Dashboard.route, navController.currentDestination?.route)
        }
    }
}
