package com.example.dtxvoicerecorder

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.dtxvoicerecorder.utils.SharedPreferenceHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ApplicationMelodious : Application() {

    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferenceHelper
    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(sharedPreferencesHelper.themeFlag[sharedPreferencesHelper.theme])


    }
}