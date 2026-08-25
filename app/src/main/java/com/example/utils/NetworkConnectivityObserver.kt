package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ConnectivityObserver {
    val isOnline: StateFlow<Boolean>
    fun refresh(): Boolean
}

/**
 * Production Central Network Connectivity Observer.
 * Uses Android's ConnectivityManager and NetworkCallback to monitor
 * real-time usable internet status (NET_CAPABILITY_INTERNET && NET_CAPABILITY_VALIDATED).
 */
class NetworkConnectivityObserver(
    private val context: Context
) : ConnectivityObserver {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isObserving = false

    init {
        startObserving()
    }

    /**
     * Synchronously checks whether the device currently has active and validated internet capability.
     */
    fun checkCurrentConnectivity(): Boolean {
        if (overrideForTesting != null) {
            return overrideForTesting!!
        }
        val cm = connectivityManager ?: return false
        return try {
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            // In Android Q and above, NET_CAPABILITY_VALIDATED ensures genuine internet reachability.
            // On older versions or local emulation where validation isn't strict, NET_CAPABILITY_INTERNET is baseline.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hasInternet && (isValidated || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED))
            } else {
                hasInternet
            }
        } catch (e: Exception) {
            Log.w("NetworkObserver", "Error evaluating network capabilities: ${e.message}")
            false
        }
    }

    /**
     * Registers a lifecycle-safe network callback without continuous polling loops.
     */
    fun startObserving() {
        if (isObserving || connectivityManager == null) return

        _isOnline.value = checkCurrentConnectivity()

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val status = checkCurrentConnectivity()
                    Log.d("NetworkObserver", "Network onAvailable -> isOnline=$status")
                    _isOnline.value = status
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        hasInternet && (isValidated || networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED))
                    } else {
                        hasInternet
                    }
                    Log.d("NetworkObserver", "Network onCapabilitiesChanged -> isOnline=$status")
                    _isOnline.value = status
                }

                override fun onLost(network: Network) {
                    val status = checkCurrentConnectivity()
                    Log.d("NetworkObserver", "Network onLost -> isOnline=$status")
                    _isOnline.value = status
                }

                override fun onUnavailable() {
                    Log.d("NetworkObserver", "Network onUnavailable -> isOnline=false")
                    _isOnline.value = false
                }
            }

            networkCallback = callback
            connectivityManager.registerNetworkCallback(request, callback)
            isObserving = true
        } catch (e: Exception) {
            Log.e("NetworkObserver", "Failed to register network callback", e)
            _isOnline.value = checkCurrentConnectivity()
        }
    }

    /**
     * Unregisters the callback cleanly when no longer needed.
     */
    fun stopObserving() {
        if (!isObserving) return
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.w("NetworkObserver", "Failed to unregister callback: ${e.message}")
            }
            networkCallback = null
        }
        isObserving = false
    }

    override fun refresh(): Boolean {
        val current = checkCurrentConnectivity()
        Log.d("NetworkObserver", "Manual refresh requested -> isOnline=$current")
        _isOnline.value = current
        return current
    }

    companion object {
        @Volatile
        private var instance: NetworkConnectivityObserver? = null

        var overrideForTesting: Boolean? = null

        fun getInstance(context: Context): NetworkConnectivityObserver {
            return instance ?: synchronized(this) {
                instance ?: NetworkConnectivityObserver(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
