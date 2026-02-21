package com.gymdash.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.remote.AuthEventBus
import com.gymdash.companion.data.remote.BaseUrlInterceptor
import com.gymdash.companion.presentation.navigation.NavGraph
import com.gymdash.companion.presentation.navigation.Routes
import com.gymdash.companion.presentation.theme.GymDashTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferences: SyncPreferences
    @Inject lateinit var baseUrlInterceptor: BaseUrlInterceptor
    @Inject lateinit var authEventBus: AuthEventBus

    private var startDestination by mutableStateOf<String?>(null)
    private var forceLoginNavigation by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val token = preferences.authToken.first()
            val serverUrl = preferences.serverUrl.first()
            baseUrlInterceptor.baseUrl = serverUrl
            startDestination = if (token != null) Routes.HOME else Routes.LOGIN
        }

        lifecycleScope.launch {
            authEventBus.authExpired.collect {
                forceLoginNavigation = true
            }
        }

        setContent {
            GymDashTheme {
                val dest = startDestination
                if (dest != null) {
                    NavGraph(
                        startDestination = dest,
                        forceLoginNavigation = forceLoginNavigation,
                        onForceLoginHandled = { forceLoginNavigation = false }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
