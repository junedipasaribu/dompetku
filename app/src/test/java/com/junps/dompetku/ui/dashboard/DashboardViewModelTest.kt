package com.junps.dompetku.ui.dashboard

import com.junps.dompetku.domain.model.FinancialSummary
import com.junps.dompetku.domain.model.MonthRange
import com.junps.dompetku.domain.model.TransactionWithCategory
import com.junps.dompetku.domain.repository.DashboardRepository
import com.junps.dompetku.domain.time.MonthRangeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_combinesSummaryAndRecentTransactions() = runBlocking {
        val summary = FinancialSummary(4_000, 5_000, 1_000)
        val repository = FakeDashboardRepository(
            summaryFlow = flowOf(summary),
            recentFlow = flowOf(emptyList()),
        )
        val viewModel = DashboardViewModel(repository, MonthRangeCalculator())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(summary, state.summary)
        assertEquals(5, repository.requestedRecentLimit)
        assertFalse(state.monthRange == null)
    }

    @Test
    fun uiState_exposesRepositoryFailure() = runBlocking {
        val repository = FakeDashboardRepository(
            summaryFlow = flow { throw IllegalStateException("Database tidak tersedia") },
            recentFlow = flowOf(emptyList()),
        )
        val viewModel = DashboardViewModel(repository, MonthRangeCalculator())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("Database tidak tersedia", state.errorMessage)
    }
}

private class FakeDashboardRepository(
    private val summaryFlow: Flow<FinancialSummary>,
    private val recentFlow: Flow<List<TransactionWithCategory>>,
) : DashboardRepository {
    var requestedRecentLimit: Int? = null

    override fun observeSummary(monthRange: MonthRange): Flow<FinancialSummary> = summaryFlow

    override fun observeRecentTransactions(limit: Int): Flow<List<TransactionWithCategory>> {
        requestedRecentLimit = limit
        return recentFlow
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
