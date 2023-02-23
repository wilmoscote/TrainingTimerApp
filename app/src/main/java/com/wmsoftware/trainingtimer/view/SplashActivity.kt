package com.wmsoftware.trainingtimer.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.wmsoftware.trainingtimer.databinding.ActivitySettingBinding
import com.wmsoftware.trainingtimer.databinding.ActivitySplashBinding
import com.wmsoftware.trainingtimer.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var userPreferences: UserPreferences
    private val languageCodes = arrayOf("nn", "en", "es", "pt")
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            //
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val screenSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        screenSplash.setKeepOnScreenCondition{true}

        userPreferences = UserPreferences(this)
        askNotificationPermission()

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserTheme().collect { theme ->
                if (theme == null) {
                    runOnUiThread {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                } else if (theme) {
                    runOnUiThread {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                } else {
                    runOnUiThread {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserLanguage().collect { language ->
                runOnUiThread {
                    if(language != null){
                        setLocale(languageCodes[language])
                    }
                }
            }
        }



        startActivity(Intent(this,MainActivity::class.java))
        finish()
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                //
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setLocale(language: String) {
        if(language != "nn"){
            val resources = resources
            val metrics = resources.displayMetrics
            val configuration = resources.configuration
            configuration.locale = Locale(language)
            resources.updateConfiguration(configuration, metrics)
            onConfigurationChanged(configuration)
        } else {
            val locale = Locale.getDefault()
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}