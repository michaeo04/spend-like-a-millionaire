package com.michaeo04.spendlikeamillionaire.platform

/**
 * Seam for AdMob (built after the app is feature-complete, spec M9). Screens depend only on this
 * interface, so adding real ads later does not touch them.
 */
interface AdsGateway {
    fun initialize()
}

class NoOpAdsGateway : AdsGateway {
    override fun initialize() = Unit
}
