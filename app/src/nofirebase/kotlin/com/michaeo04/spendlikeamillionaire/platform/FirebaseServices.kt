package com.michaeo04.spendlikeamillionaire.platform

import android.content.Context
import com.michaeo04.spendlikeamillionaire.data.NetWorthOverrides
import com.michaeo04.spendlikeamillionaire.data.NoOverrides

/** Compiled when app/google-services.json is absent (public repo, CI, contributors). */
object FirebaseServices {
    fun overrides(@Suppress("UNUSED_PARAMETER") context: Context): NetWorthOverrides = NoOverrides
}
