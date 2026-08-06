package com.junps.dompetku.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionFormDestinationTest {
    @Test
    fun createRoute_withoutId_createsAddRoute() {
        assertEquals("transaction_form", TransactionFormDestination.createRoute())
    }

    @Test
    fun createRoute_withId_createsEditRoute() {
        assertEquals(
            "transaction_form?transactionId=transaction-123",
            TransactionFormDestination.createRoute("transaction-123"),
        )
    }
}
