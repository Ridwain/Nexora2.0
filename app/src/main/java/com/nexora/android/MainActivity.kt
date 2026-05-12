package com.nexora.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.nexora.android.core.deeplink.DeepLinkRepository
import com.nexora.android.core.ui.AppNavGraph
import com.nexora.android.ui.theme.NexoraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var deepLinkRepository: DeepLinkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureInviteDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            NexoraTheme {
                AppNavGraph()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureInviteDeepLink(intent)
    }

    private fun captureInviteDeepLink(intent: Intent?) {
        lifecycleScope.launch {
            deepLinkRepository.captureInviteUri(intent?.dataString)
        }
    }
}
