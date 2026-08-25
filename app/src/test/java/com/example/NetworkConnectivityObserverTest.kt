package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.NetworkConnectivityObserver
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkConnectivityObserverTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        NetworkConnectivityObserver.overrideForTesting = null
    }

    @After
    fun tearDown() {
        NetworkConnectivityObserver.overrideForTesting = null
    }

    @Test
    fun testNetworkConnectivityObserver_defaultState() {
        val observer = NetworkConnectivityObserver(context)
        // Verify observer instance is created and state flow is accessible
        val initialState = observer.isOnline.value
        // In Robolectric without active network simulation, refresh returns boolean
        val refreshed = observer.refresh()
        org.junit.Assert.assertEquals(initialState, refreshed)
    }

    @Test
    fun testNetworkConnectivityObserver_overrideForTesting_offlineAndOnline() {
        val observer = NetworkConnectivityObserver(context)

        // 1. Force offline
        NetworkConnectivityObserver.overrideForTesting = false
        val isOffline = observer.refresh()
        assertFalse("Expected offline state when overridden to false", isOffline)
        assertFalse(observer.isOnline.value)

        // 2. Force online
        NetworkConnectivityObserver.overrideForTesting = true
        val isOnline = observer.refresh()
        assertTrue("Expected online state when overridden to true", isOnline)
        assertTrue(observer.isOnline.value)
    }

    @Test
    fun testNetworkConnectivityObserver_singletonInstance() {
        val instance1 = NetworkConnectivityObserver.getInstance(context)
        val instance2 = NetworkConnectivityObserver.getInstance(context)
        org.junit.Assert.assertSame("getInstance must return the same singleton instance", instance1, instance2)
    }
}
