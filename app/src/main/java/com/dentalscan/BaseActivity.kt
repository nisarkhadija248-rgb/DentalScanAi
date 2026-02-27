package com.dentalscan

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.dentalscan.utils.LanguageManager

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.setLocale(newBase))
    }
}