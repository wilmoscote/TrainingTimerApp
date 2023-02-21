package com.wmsoftware.trainingtimer.view

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.wmsoftware.trainingtimer.BuildConfig
import com.wmsoftware.trainingtimer.R
import com.wmsoftware.trainingtimer.databinding.ActivitySettingBinding
import com.wmsoftware.trainingtimer.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private lateinit var userPreferences: UserPreferences
    private var selectedLanguage = 0
    var checkedItem = 0
    val languageCodes = arrayOf("df","en", "es", "pt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserTheme().collect { theme ->
                runOnUiThread {
                    binding.switchTheme.isChecked = theme ?: false
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserVibration().collect { vibrate ->
                runOnUiThread {
                    binding.switchVibrate.isChecked = vibrate ?: true
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserLanguage().collect { language ->
                runOnUiThread {
                    when(language){
                        0 -> {
                            checkedItem = 0
                        }
                        1 -> {
                            checkedItem = 1
                        }
                        2 -> {
                            checkedItem = 2
                        }
                        3 -> {
                            checkedItem = 3
                        }
                        else -> {
                            checkedItem = 0
                        }
                    }
                }
            }
        }

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                userPreferences.saveTheme(isChecked)
            }
        }

        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch(Dispatchers.IO) {
                userPreferences.saveVibration(isChecked)
            }
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                finish()
            }
        })

        binding.btnTerms.setOnClickListener {
            startActivity(Intent(this,TermsActivity::class.java))
        }

        binding.versionInfo.setOnLongClickListener {
            Toast.makeText(this,"S \uD83D\uDC9B",Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }

        binding.switchLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.versionInfo.text = "v${BuildConfig.VERSION_NAME} By Flexifit"

        binding.txtCurrentLanguage.text = resources.configuration.locales.get(0).displayName.toString()
    }

    private fun showLanguageDialog(){
        val singleItems = mutableListOf("Idioma del dispositivo","Inglés","Español","Portugués")
        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            .setTitle("CAMBIAR IDIOMA")
            .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                lifecycleScope.launch(Dispatchers.IO) {
                    userPreferences.saveLanguage(selectedLanguage)
                    runOnUiThread {
                        if(selectedLanguage != 0){
                            setLocale(languageCodes[selectedLanguage])
                        } else {
                            val defaultLocale = Locale.getDefault().language
                            setLocale(defaultLocale)
                        }

                    }
                }
            }
            .setNegativeButton(resources.getString(R.string.cancel)){dialog, which ->
                dialog.dismiss()
            }
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                selectedLanguage = which
                checkedItem = which
            }
            .setCancelable(false)
            .show()
    }

    private fun setLocale(language:String){
        val resources = resources
        val metrics = resources.displayMetrics
        val configuration = resources.configuration
        configuration.locale = Locale(language)
        resources.updateConfiguration(configuration,metrics)
        onConfigurationChanged(configuration)
        recreate()
    }
}