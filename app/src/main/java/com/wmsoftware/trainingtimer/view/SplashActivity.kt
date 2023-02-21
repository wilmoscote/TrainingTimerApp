package com.wmsoftware.trainingtimer.view

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val screenSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        screenSplash.setKeepOnScreenCondition{true}

        userPreferences = UserPreferences(this)

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

        startActivity(Intent(this,MainActivity::class.java))
        finish()
    }


}