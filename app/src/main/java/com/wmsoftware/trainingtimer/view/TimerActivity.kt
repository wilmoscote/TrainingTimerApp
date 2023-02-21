package com.wmsoftware.trainingtimer.view

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.lifecycleScope
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.wmsoftware.trainingtimer.R
import com.wmsoftware.trainingtimer.databinding.ActivityTimerBinding
import com.wmsoftware.trainingtimer.utils.UserPreferences
import com.wmsoftware.trainingtimer.viewmodel.TrainingTimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimerBinding
    private val viewModel: TrainingTimerViewModel by lazy {
        TrainingTimerViewModel.getInstance()
    }
    val TAG = "TimerDebug"
    private var vibrate: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val userPreferences = UserPreferences(this)
        lifecycleScope.launch {
            userPreferences.getUserVibration().collect(){ vibration ->
                runOnUiThread {
                    vibrate = vibration ?: true
                }
            }
        }

        binding.timerCount.text = viewModel.formatTime(viewModel.totalTime.value ?: 8)
        binding.textTotalRound.text = (viewModel.rounds.value ?: 1).toString()
        binding.progress.progressMax = (viewModel.progressMax.value ?: 10f)
        binding.textCurrentRound.text = (viewModel.round.value ?: 1).toString()


        viewModel.isRunning.observe(this){
            binding.layoutButtons.isVisible = it
            if (it) {
                binding.timerPauseButton.foreground = ContextCompat.getDrawable(this, R.drawable.ic_pause)
            } else {
                binding.timerPauseButton.foreground = ContextCompat.getDrawable(this, R.drawable.ic_play)
            }
        }

        viewModel.trainingStep.observe(this){
            when(it){
                0 -> {
                    binding.btnBackTimer.isVisible = false
                }
                1 -> {
                    binding.timerInfo.text = getString(R.string.u_ready)
                    binding.trainingAnimation.setAnimation(R.raw.breathe)
                }
                2 -> {
                    binding.trainingAnimation.setAnimation(R.raw.workout)
                    if(vibrate){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val vibrationEffect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
                            vibrator.vibrate(vibrationEffect)
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(1000)
                        }
                    }
                }
                3 -> {
                    binding.btnBackTimer.isVisible = true
                    binding.timerInfo.text = getString(R.string.well_done)
                    binding.trainingAnimation.scaleX = 1.3f
                    binding.trainingAnimation.scaleY = 1.3f
                    binding.trainingAnimation.setAnimation(R.raw.claps)
                    if(vibrate){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val customVibrationPattern = longArrayOf(0, 500, 100, 500, 100, 500) // milisegundos de vibración y pausa alternadamente
                            val vibrationEffect = VibrationEffect.createWaveform(customVibrationPattern, -1) // -1 para que no se repita
                            vibrator.vibrate(vibrationEffect)
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(1000)
                        }
                    }

                    MediaPlayer.create(this, R.raw.sound_end).start()
                }
            }
            binding.trainingAnimation.playAnimation()
        }

        binding.timerPauseButton.setOnClickListener {
            if (viewModel.isRunning.value == true) {
                if (viewModel.isPaused) {
                    viewModel.resumeTimer(this@TimerActivity)
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
                    viewModel.stopTimer(this@TimerActivity)
                    viewModel.calculateTotalTime()
                    withContext(Dispatchers.Main){
                        viewModel.trainingStep.value = 0
                        finish()
                    }
                }
            }
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
                        this@TimerActivity,
                        R.color.material_300
                    )
                    binding.progress.progressBarColorEnd = ContextCompat.getColor(
                        this@TimerActivity,
                        R.color.material_700
                    )
                    binding.progress.progressBarColorDirection =
                        CircularProgressBar.GradientDirection.TOP_TO_BOTTOM
                    binding.timerInfo.text = getString(R.string.go_go)
                }
                false -> {
                    binding.progress.progressBarColorStart = ContextCompat.getColor(
                        this@TimerActivity,
                        R.color.material_red_300
                    )
                    binding.progress.progressBarColorEnd = ContextCompat.getColor(
                        this@TimerActivity,
                        R.color.material_red_700
                    )
                    binding.progress.progressBarColorDirection =
                        CircularProgressBar.GradientDirection.TOP_TO_BOTTOM
                    binding.timerInfo.text = getString(R.string.break_info)
                }
            }
        }

        viewModel.timerCountDownText.observe(this) {
            binding.timerCount.text = it
        }

        viewModel.round.observe(this){
            binding.textCurrentRound.text = it.toString()
        }

        if (viewModel.isRunning.value != true) {
            viewModel.job = Job()
            viewModel.prepareTimer(this@TimerActivity)
        }

        binding.btnBackTimer.setOnClickListener {
            viewModel.trainingStep.value = 0
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    viewModel.stopTimer(this@TimerActivity)
                    viewModel.calculateTotalTime()
                    withContext(Dispatchers.Main){
                        viewModel.trainingStep.value = 0
                        viewModelStore.clear()
                        finish()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            viewModel.stopTimer(this@TimerActivity)
            viewModel.calculateTotalTime()
            withContext(Dispatchers.Main){
                viewModel.trainingStep.value = 0
                viewModelStore.clear()
            }
        }
    }
}