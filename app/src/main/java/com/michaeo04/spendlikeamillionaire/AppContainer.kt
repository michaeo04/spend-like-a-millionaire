package com.michaeo04.spendlikeamillionaire

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.michaeo04.spendlikeamillionaire.data.AssetCatalogRepository
import com.michaeo04.spendlikeamillionaire.data.AssetFxRepository
import com.michaeo04.spendlikeamillionaire.data.AssetPeopleRepository
import com.michaeo04.spendlikeamillionaire.data.CatalogRepository
import com.michaeo04.spendlikeamillionaire.data.DataStoreCartStore
import com.michaeo04.spendlikeamillionaire.data.DataStoreSettingsStore
import com.michaeo04.spendlikeamillionaire.data.FxRepository
import com.michaeo04.spendlikeamillionaire.data.NetWorthOverrides
import com.michaeo04.spendlikeamillionaire.data.OverlayPeopleRepository
import com.michaeo04.spendlikeamillionaire.data.PeopleRepository
import com.michaeo04.spendlikeamillionaire.domain.CartStore
import com.michaeo04.spendlikeamillionaire.domain.SettingsStore
import com.michaeo04.spendlikeamillionaire.platform.AdsGateway
import com.michaeo04.spendlikeamillionaire.platform.FirebaseServices
import com.michaeo04.spendlikeamillionaire.platform.NoOpAdsGateway
import kotlin.coroutines.cancellation.CancellationException

private val Context.appDataStore by preferencesDataStore(name = "app")

/** Manual dependency container; one instance per process, owned by [App]. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    private val netWorthOverrides: NetWorthOverrides = FirebaseServices.overrides(app)

    val catalog: CatalogRepository = AssetCatalogRepository(app)
    val people: PeopleRepository = OverlayPeopleRepository(AssetPeopleRepository(app), netWorthOverrides)
    val fx: FxRepository = AssetFxRepository(app)
    val settingsStore: SettingsStore = DataStoreSettingsStore(app.appDataStore)
    val cartStore: CartStore = DataStoreCartStore(app.appDataStore)
    val ads: AdsGateway = NoOpAdsGateway()

    /** Fetches fresh remote values in the background; never throws. They apply on the next launch. */
    suspend fun refreshRemoteData() {
        try {
            netWorthOverrides.refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline or Firebase unavailable: keep using bundled/cached data.
        }
    }
}
