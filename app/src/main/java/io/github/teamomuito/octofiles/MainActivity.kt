package io.github.teamomuito.octofiles

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import io.github.teamomuito.octofiles.ui.MainViewModel
import io.github.teamomuito.octofiles.ui.PotatoApp
import io.github.teamomuito.octofiles.ui.theme.OctoTheme

class MainActivity : FragmentActivity() {

    private lateinit var vm: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[MainViewModel::class.java]
        if (savedInstanceState == null && intent?.action == ACTION_TIDY) vm.tidyAsked.value = true
        setContent {
            OctoTheme {
                PotatoApp(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_TIDY) vm.tidyAsked.value = true
    }

    companion object {
        const val ACTION_TIDY = "io.github.teamomuito.octofiles.TIDY"
    }
}
