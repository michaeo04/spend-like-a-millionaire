package com.michaeo04.spendlikeamillionaire

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.michaeo04.spendlikeamillionaire.ui.nav.AppNav
import com.michaeo04.spendlikeamillionaire.ui.theme.SpendTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as App).container
        setContent {
            SpendTheme {
                AppNav(container)
            }
        }
    }
}
