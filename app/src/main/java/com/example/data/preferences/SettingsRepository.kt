package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppCurrency
import com.example.localization.AppLanguage
import com.example.localization.DateFormatOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "amar_dokan_settings")

enum class LockType {
    NONE, PIN, PASSWORD
}

data class ShopSettings(
    val shopName: String = "আমার দোকান",
    val ownerName: String = "",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val shopLogoUri: String = "",
    val currency: AppCurrency = AppCurrency.BDT,
    val language: AppLanguage = AppLanguage.BN,
    val dateFormat: DateFormatOption = DateFormatOption.READABLE,
    val darkMode: Boolean = false,
    val openingCashBalance: Double = 0.0,
    val openingBankBalance: Double = 0.0,
    val openingWalletBalance: Double = 0.0,
    val lockType: LockType = LockType.NONE,
    val lockSecretHash: String = "",
    val biometricEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val lowStockNotificationsEnabled: Boolean = true,
    val lastBackupTimestamp: Long = 0L
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val SHOP_PHONE = stringPreferencesKey("shop_phone")
        val SHOP_ADDRESS = stringPreferencesKey("shop_address")
        val SHOP_LOGO_URI = stringPreferencesKey("shop_logo_uri")
        val CURRENCY = stringPreferencesKey("currency")
        val LANGUAGE = stringPreferencesKey("language")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val OPENING_CASH = doublePreferencesKey("opening_cash")
        val OPENING_BANK = doublePreferencesKey("opening_bank")
        val OPENING_WALLET = doublePreferencesKey("opening_wallet")
        val LOCK_TYPE = stringPreferencesKey("lock_type")
        val LOCK_SECRET_HASH = stringPreferencesKey("lock_secret_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LOW_STOCK_NOTIF = booleanPreferencesKey("low_stock_notif")
        val LAST_BACKUP_TS = longPreferencesKey("last_backup_ts")
    }

    val settingsFlow: Flow<ShopSettings> = context.dataStore.data.map { prefs ->
        ShopSettings(
            shopName = prefs[Keys.SHOP_NAME] ?: "আমার দোকান",
            ownerName = prefs[Keys.OWNER_NAME] ?: "",
            shopPhone = prefs[Keys.SHOP_PHONE] ?: "",
            shopAddress = prefs[Keys.SHOP_ADDRESS] ?: "",
            shopLogoUri = prefs[Keys.SHOP_LOGO_URI] ?: "",
            currency = AppCurrency.entries.find { it.code == prefs[Keys.CURRENCY] } ?: AppCurrency.BDT,
            language = AppLanguage.entries.find { it.code == prefs[Keys.LANGUAGE] } ?: AppLanguage.BN,
            dateFormat = DateFormatOption.entries.find { it.name == prefs[Keys.DATE_FORMAT] } ?: DateFormatOption.READABLE,
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            openingCashBalance = prefs[Keys.OPENING_CASH] ?: 0.0,
            openingBankBalance = prefs[Keys.OPENING_BANK] ?: 0.0,
            openingWalletBalance = prefs[Keys.OPENING_WALLET] ?: 0.0,
            lockType = LockType.entries.find { it.name == prefs[Keys.LOCK_TYPE] } ?: LockType.NONE,
            lockSecretHash = prefs[Keys.LOCK_SECRET_HASH] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: true,
            lowStockNotificationsEnabled = prefs[Keys.LOW_STOCK_NOTIF] ?: true,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TS] ?: 0L
        )
    }

    suspend fun updateShopProfile(
        shopName: String,
        ownerName: String,
        phone: String,
        address: String,
        logoUri: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOP_NAME] = shopName.ifBlank { "আমার দোকান" }
            prefs[Keys.OWNER_NAME] = ownerName
            prefs[Keys.SHOP_PHONE] = phone
            prefs[Keys.SHOP_ADDRESS] = address
            prefs[Keys.SHOP_LOGO_URI] = logoUri
        }
    }

    suspend fun updateCurrency(currency: AppCurrency) {
        context.dataStore.edit { it[Keys.CURRENCY] = currency.code }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun updateDateFormat(dateFormat: DateFormatOption) {
        context.dataStore.edit { it[Keys.DATE_FORMAT] = dateFormat.name }
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun updateOpeningBalances(cash: Double, bank: Double, wallet: Double) {
        context.dataStore.edit {
            it[Keys.OPENING_CASH] = cash
            it[Keys.OPENING_BANK] = bank
            it[Keys.OPENING_WALLET] = wallet
        }
    }

    suspend fun updateSecurity(lockType: LockType, rawSecret: String?, biometricEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCK_TYPE] = lockType.name
            if (lockType == LockType.NONE) {
                prefs[Keys.LOCK_SECRET_HASH] = ""
                prefs[Keys.BIOMETRIC_ENABLED] = false
            } else {
                if (!rawSecret.isNullOrBlank()) {
                    prefs[Keys.LOCK_SECRET_HASH] = hashSecret(rawSecret)
                }
                prefs[Keys.BIOMETRIC_ENABLED] = biometricEnabled
            }
        }
    }

    suspend fun updateBackupSettings(autoBackup: Boolean, lowStockNotif: Boolean) {
        context.dataStore.edit {
            it[Keys.AUTO_BACKUP_ENABLED] = autoBackup
            it[Keys.LOW_STOCK_NOTIF] = lowStockNotif
        }
    }

    suspend fun recordBackupTimestamp(ts: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_TS] = ts }
    }

    fun verifySecret(rawSecret: String, storedHash: String): Boolean {
        return storedHash.isNotEmpty() && hashSecret(rawSecret) == storedHash
    }

    private fun hashSecret(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
