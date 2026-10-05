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
import com.michaeo04.spendlikeamillionaire.data.PeopleRepository
import com.michaeo04.spendlikeamillionaire.domain.CartStore
import com.michaeo04.spendlikeamillionaire.domain.SettingsStore
import com.michaeo04.spendlikeamillionaire.platform.AdsGateway
import com.michaeo04.spendlikeamillionaire.platform.NoOpAdsGateway

private val Context.appDataStore by preferencesDataStore(name = "app")

/** Manual dependency container; one instance per process, owned by [App]. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val catalog: CatalogRepository = AssetCatalogRepository(app)
    var people: PeopleRepository = AssetPeopleRepository(app)
    val fx: FxRepository = AssetFxRepository(app)
    val settingsStore: SettingsStore = DataStoreSettingsStore(app.appDataStore)
    val cartStore: CartStore = DataStoreCartStore(app.appDataStore)
    val ads: AdsGateway = NoOpAdsGateway()
}
