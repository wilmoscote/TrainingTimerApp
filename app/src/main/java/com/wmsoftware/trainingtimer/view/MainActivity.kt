package com.wmsoftware.trainingtimer.view

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_KEYBOARD
import com.google.android.material.timepicker.TimeFormat
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.wmsoftware.trainingtimer.R
import com.wmsoftware.trainingtimer.databinding.ActivityMainBinding
import com.wmsoftware.trainingtimer.utils.UserPreferences
import com.wmsoftware.trainingtimer.viewmodel.TrainingTimerViewModel
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TrainingTimerViewModel by lazy {
        TrainingTimerViewModel.getInstance()
    }
    private val languageCodes = arrayOf("df","en", "es", "pt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        val userPreferences = UserPreferences(this)
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserTheme().collect { theme ->
                if (theme == null) {
                    runOnUiThread {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        window.statusBarColor = ContextCompat.getColor(this@MainActivity,R.color.card_night)
                    }
                } else if (theme) {
                    runOnUiThread {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        window.statusBarColor = ContextCompat.getColor(this@MainActivity,R.color.card_night)
                    }
                } else {
                    runOnUiThread {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        window.statusBarColor = ContextCompat.getColor(this@MainActivity,R.color.card_light)
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserLanguage().collect { language ->
                runOnUiThread {
                        setLocale(languageCodes[language ?: 1])
                }
            }
        }

        //Inicializo el viewModel para setear los valores por defecto
        lifecycleScope.launch {
            viewModel.init(applicationContext)
            viewModel.calculateTotalTime()
        }
        val typefaceBold = Typeface.createFromAsset(assets, "Poppins-Bold.ttf")
        val typefaceRegular = Typeface.createFromAsset(assets, "Poppins-Regular.ttf")
        binding.minutePicker.typeface = typefaceRegular
        binding.minutePicker.setSelectedTypeface(typefaceBold)
        binding.secondsPicker.typeface = typefaceRegular
        binding.secondsPicker.setSelectedTypeface(typefaceBold)

        binding.minutePicker2.typeface = typefaceRegular
        binding.minutePicker2.setSelectedTypeface(typefaceBold)
        binding.secondsPicker2.typeface = typefaceRegular
        binding.secondsPicker2.setSelectedTypeface(typefaceBold)

        binding.roundsNumberPicker.typeface = typefaceRegular
        binding.roundsNumberPicker.setSelectedTypeface(typefaceBold)

        /** Inicializo las variables **/
        //Tiempo por ronda, lo recibo y lo muestro al usuario.
        viewModel.totalRoundTime.observe(this) {
            //binding.textRoundTime.setText(viewModel.formatTime(it))
        }

        /** Inicializo los pickers para sumar y restar cantidad **/
        //Inicializo el boton para sumar 5 segundos al tiempo por ronda.
        binding.minutePicker.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setTotalRoundMinuteTime(newVal)
                viewModel.calculateTotalTime()
            }
        }

        binding.secondsPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setTotalRoundSecondsTime(newVal)
                viewModel.calculateTotalTime()
            }
        }

        /** Selectores de Tiempo de descanso **/
        binding.minutePicker2.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setTotalBreakMinuteTime(newVal)
                viewModel.calculateTotalTime()
            }
        }

        binding.secondsPicker2.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setTotalBreakSecondsTime(newVal)
                viewModel.calculateTotalTime()
            }
        }
        /** Selector de Cantidad de Rondas **/
        binding.roundsNumberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setRounds(newVal)
                viewModel.calculateTotalTime()
            }
        }

        viewModel.totalTime.observe(this){
            binding.totalTimeText.text = viewModel.formatTime(it)
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this,SettingActivity::class.java))
        }

        binding.timerStartButton.setOnClickListener {
            if((viewModel.totalRoundTime.value ?: 5) >= 5){
                viewModel.trainingStep.value = 0
                startActivity(Intent(this@MainActivity, TimerActivity::class.java))
            } else {
                Snackbar.make(binding.root,"Debe ingresar un tiempo mínimo de 5 segundos.",Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLocale(language:String){
        val resources = resources
        val metrics = resources.displayMetrics
        val configuration = resources.configuration
        configuration.locale = Locale(language)
        resources.updateConfiguration(configuration,metrics)
        onConfigurationChanged(configuration)
    }
}