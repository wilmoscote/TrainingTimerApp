package com.wmsoftware.trainingtimer.view

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
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
import com.wmsoftware.trainingtimer.viewmodel.TrainingTimerViewModel
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TrainingTimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Inicializo el viewModel para setear los valores por defecto
        lifecycleScope.launch {
            viewModel.init()
            viewModel.calculateTotalTime()
        }
        //Inicializo el observer para saber cuando el timer se esta ejecutando
        viewModel.isRunning.observe(this){
            binding.timerStartButton.isVisible = !it
            binding.layoutButtons.isVisible = it
            binding.layoutControls.isVisible = !it
            binding.layoutButtonsControls.isVisible = !it
            binding.layoutTrainingInfo.isVisible = it
            binding.timerInfo.text = ""
            if (it) {
                binding.timerPauseButton.foreground = ContextCompat.getDrawable(this, R.drawable.ic_pause)
            } else {
                binding.timerPauseButton.foreground = ContextCompat.getDrawable(this, R.drawable.ic_play)
            }
        }

        /** Inicializo las variables **/
        //Tiempo por ronda, lo recibo y lo muestro al usuario.
        viewModel.totalRoundTime.observe(this) {
            binding.textRoundTime.setText(viewModel.formatTime(it))
        }
        //Tiempo por descanso, lo recibo y lo muestro al usuario.
        viewModel.breakTime.observe(this) {
            binding.textBreakTime.setText(viewModel.formatTime(it))
        }
        //Cantidad de intervalos para las sesión
        viewModel.rounds.observe(this) {
            binding.textInvertal.setText(it.toString())
            binding.textTotalRound.text = it.toString()
        }

        /** Inicializo los botones para sumar y restar cantidad **/
        //Inicializo el boton para sumar 5 segundos al tiempo por ronda.
        binding.btnRoundTimePlus.setOnClickListener {
            lifecycleScope.launch {
                viewModel.plusTotalRoundTime()
                viewModel.calculateTotalTime()
            }
        }
        //Inicializo el boton para restar 5 segundos al tiempo por ronda.
        binding.btnRoundTimeLess.setOnClickListener {
            lifecycleScope.launch {
                viewModel.lessTotalRoundTime()
                viewModel.calculateTotalTime()
            }
        }

        //Inicializo el boton para sumar 5 segundos al tiempo de descanso
        binding.btnBreakTimePlus.setOnClickListener {
            lifecycleScope.launch {
                viewModel.plusBreakTime()
                viewModel.calculateTotalTime()
            }
        }
        //Inicializo el boton para restar 5 segundos al tiempo de descanso
        binding.btnBreakTimeLess.setOnClickListener {
            lifecycleScope.launch {
                viewModel.lessBreakTime()
                viewModel.calculateTotalTime()
            }
        }

        //Inicializo el boton para sumar 1 intervalo a la sesión
        binding.btnIntervalPlus.setOnClickListener {
            lifecycleScope.launch {
                viewModel.plusInterval()
                viewModel.calculateTotalTime()
            }
        }
        //Inicializo el boton para restar 1 intervalo a la sesión
        binding.btnIntervalLess.setOnClickListener {
            lifecycleScope.launch {
                viewModel.lessInterval()
                viewModel.calculateTotalTime()
            }
        }

        //Obtengo el tiempo total de la sesión
        viewModel.totalTime.observe(this) {
            binding.timerCount.text = viewModel.formatTime(it)
        }

        binding.btnSettings.setOnClickListener {
           // startActivity(Intent(this,TrainingView::class.java))
        }
        binding.timerStartButton.setOnClickListener {
            if (viewModel.isRunning.value != true) {
                viewModel.job = Job()
                viewModel.prepareTimer(this@MainActivity)
            }
        }

        viewModel.progressMax.observe(this){
            binding.progress.progressMax = it
        }

        binding.timerPauseButton.setOnClickListener {
            if (viewModel.isRunning.value == true) {
                if (viewModel.isPaused) {
                    viewModel.resumeTimer(this@MainActivity)
                    binding.timerPauseButton.foreground = ContextCompat.getDrawable(this, R.drawable.ic_pause)
                } else {
                    viewModel.pauseTimer()
                    binding.timerPauseButton.foreground = ContextCompat.getDrawable(this, R.drawable.ic_play)
                }
            }
        }

        binding.timerStopButton.setOnClickListener {
            if (viewModel.isRunning.value == true) {
                binding.progress.setProgressWithAnimation(0f, 500)
                lifecycleScope.launch {
                    viewModel.stopTimer(this@MainActivity)
                    viewModel.calculateTotalTime()
                }
            }
        }

        viewModel.timerCountDownText.observe(this) {
            binding.timerCount.text = it
        }

        viewModel.progress.observe(this) {
            binding.progress.setProgressWithAnimation(
                it,
                1000
            )
        }

        viewModel.goRunning.observe(this) {
            when (it) {
                true -> {
                    binding.progress.backgroundProgressBarColor = Color.DKGRAY
                    binding.progress.progressBarColorStart = ContextCompat.getColor(
                        this@MainActivity,
                        R.color.material_300
                    )
                    binding.progress.progressBarColorEnd = ContextCompat.getColor(
                        this@MainActivity,
                        R.color.material_700
                    )
                    binding.progress.progressBarColorDirection =
                        CircularProgressBar.GradientDirection.TOP_TO_BOTTOM
                    binding.timerInfo.text = getString(R.string.go_go)
                }
                false -> {
                    binding.progress.progressBarColorStart = ContextCompat.getColor(
                        this@MainActivity,
                        R.color.material_red_300
                    )
                    binding.progress.progressBarColorEnd = ContextCompat.getColor(
                        this@MainActivity,
                        R.color.material_red_700
                    )
                    binding.progress.progressBarColorDirection =
                        CircularProgressBar.GradientDirection.TOP_TO_BOTTOM
                    binding.timerInfo.text = getString(R.string.break_info)
                }
            }
        }

        viewModel.round.observe(this){
            binding.textCurrentRound.text = it.toString()
        }

        binding.textRoundTime.setOnClickListener {
            manualRoundTime()
        }

        binding.icEditRoundTime.setOnClickListener {
            manualRoundTime()
        }
    }

    private fun manualRoundTime(){
        val picker = MaterialTimePicker.Builder()
            .setInputMode(INPUT_MODE_KEYBOARD)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setTitleText("Tiempo")
            .build()
        picker.show(supportFragmentManager,"tag")
        picker.addOnPositiveButtonClickListener {
            lifecycleScope.launch {
                val time = (picker.hour * 60) + picker.minute
                if (time >= 5){
                    viewModel.manualRoundTime(time)
                    viewModel.calculateTotalTime()
                } else {
                    runOnUiThread {
                        Snackbar.make(binding.root,"Debe ingresar un tiempo mayor a 5 segundos.",Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}