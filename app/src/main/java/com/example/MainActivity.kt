package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.LockType
import com.example.localization.*
import com.example.ui.components.AppLockScreen
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.AmarDokanTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()
            val isUnlocked by viewModel.isUnlocked.collectAsState()

            val strings = if (settings.language == AppLanguage.BN) BengaliStrings else EnglishStrings

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                LocalCurrency provides settings.currency,
                LocalDatePattern provides settings.dateFormat.pattern
            ) {
                AmarDokanTheme(darkTheme = settings.darkMode) {
                    if (settings.lockType != LockType.NONE && !isUnlocked) {
                        AppLockScreen(
                            shopName = settings.shopName,
                            lockType = settings.lockType,
                            biometricEnabled = settings.biometricEnabled,
                            onVerifySecret = viewModel::unlockWithSecret,
                            onBiometricSuccess = viewModel::unlockViaBiometric
                        )
                    } else {
                        AppNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
