package com.michaeo04.spendlikeamillionaire.platform

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.michaeo04.spendlikeamillionaire.data.NetWorthOverrides
import com.michaeo04.spendlikeamillionaire.data.Override
import com.michaeo04.spendlikeamillionaire.data.parseOverrides
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Compiled only when app/google-services.json exists. Crashlytics/Analytics self-initialize. */
object FirebaseServices {
    fun overrides(@Suppress("UNUSED_PARAMETER") context: Context): NetWorthOverrides = RemoteConfigOverrides()
}

private const val KEY_NET_WORTH_OVERRIDES = "net_worth_overrides"
private const val MIN_FETCH_INTERVAL_SECONDS = 12L * 60 * 60

private class RemoteConfigOverrides : NetWorthOverrides {
    private val config = FirebaseRemoteConfig.getInstance().apply {
        setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = MIN_FETCH_INTERVAL_SECONDS })
        setDefaultsAsync(mapOf(KEY_NET_WORTH_OVERRIDES to "{}"))
    }

    override suspend fun current(): Map<String, Override> = parseOverrides(config.getString(KEY_NET_WORTH_OVERRIDES))

    override suspend fun refresh() {
        suspendCancellableCoroutine { cont ->
            config.fetchAndActivate().addOnCompleteListener { cont.resume(Unit) }
        }
    }
}
