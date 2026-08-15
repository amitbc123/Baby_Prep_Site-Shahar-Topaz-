package com.oryareach.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.oryareach.core.ui.theme.OrYareachTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OrYareachTheme {
                FoundationsScreen(versionName = BuildConfig.VERSION_NAME)
            }
        }
    }
}
