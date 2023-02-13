package com.wmsoftware.trainingtimer

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.wmsoftware.trainingtimer.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityMainBinding
    private var job: Job = Job()
    private var isRunning = false
    private var remainingTime = 0
    private var totalRoundTime = 10
    private var breakTime = 0
    private var rounds = 0
    private var round = 1
    private var totalTime = 0
    private var isPaused = false
    private var currentTime = 0
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRoundTimePlus.setOnClickListener {
            if(totalRoundTime <= 999){
                totalRoundTime += 5
            }
            calculateTotalTime()
        }

        binding.btnRoundTimeLess.setOnClickListener {
            if(totalRoundTime >= 5){
                totalRoundTime -= 5
            }
            calculateTotalTime()
        }

        binding.timerBreakEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateTotalTime()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.timerIntervalsEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateTotalTime()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.timerStartButton.setOnClickListener {
            if (!isRunning) {
                try {
                    totalRoundTime = binding.timerDurationEditText.text.toString().toInt()
                    breakTime = binding.timerBreakEditText.text.toString().toInt()
                    rounds = binding.timerIntervalsEditText.text.toString().toInt()
                    totalTime = (totalRoundTime + breakTime) * rounds
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (totalRoundTime <= 0 || breakTime < 0 || rounds <= 0) {
                    Toast.makeText(
                        this,
                        "Duration, break time and intervals must be positive numbers",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                job = Job()
                remainingTime = totalRoundTime
                binding.progress.progressMax = (totalTime - breakTime).toFloat()
                binding.progress.progress = 0f
                startTimer()
            }
        }

        binding.timerPauseButton.setOnClickListener {
            if(isRunning){
                if (isPaused) {
                    resumeTimer()
                    isPaused = false
                    binding.timerPauseButton.text = "Pause"
                } else {
                    pauseTimer()
                    isPaused = true
                    binding.timerPauseButton.text = "Resume"
                }
            }
        }

        binding.timerStopButton.setOnClickListener {
            if (isRunning) {
                launch {
                    binding.progress.setProgressWithAnimation(0f,500)
                    stopTimer()
                }
            }
        }
        calculateTotalTime()
    }

    private fun calculateTotalTime() {
        try {
            Log.d("TimerDebug","Timer: ${totalRoundTime.toString()}")
            binding.timerDurationEditText.setText(totalRoundTime.toString())
            breakTime = binding.timerBreakEditText.text.toString().toInt()
            rounds = binding.timerIntervalsEditText.text.toString().toInt()
            totalTime = ((totalRoundTime + breakTime) * rounds) - breakTime
            binding.timerCountdownTextView.text = formatTime(totalTime)
        } catch (e: java.lang.Exception) {
            //
        }
    }

    private fun startTimer() {
        isRunning = true
        launch {
            withContext(Dispatchers.IO) {
                while (round <= rounds) {
                    runOnUiThread {
                        binding.progress.backgroundProgressBarColor = Color.DKGRAY
                        binding.progress.progressBarColorStart = ContextCompat.getColor(this@MainActivity, R.color.material_300)
                        binding.progress.progressBarColorEnd = ContextCompat.getColor(this@MainActivity, R.color.material_700)
                        binding.progress.progressBarColorDirection =
                            CircularProgressBar.GradientDirection.TOP_TO_BOTTOM
                        binding.timerInfo.text = getString(R.string.go_go)
                    }
                    for (i in 1..remainingTime) {
                        delay(1000)
                        if (!isPaused) {
                            remainingTime--
                            currentTime++
                            runOnUiThread {
                                binding.progress.setProgressWithAnimation(
                                    (currentTime).toFloat(),
                                    1000
                                )
                                binding.timerCountdownTextView.text = formatTime((totalTime-currentTime)-breakTime)
                            }
                        }
                    }
                    if (round < rounds) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Break time!", Toast.LENGTH_SHORT)
                                .show()
                        }
                        round++
                        runOnUiThread {
                            binding.progress.progressBarColorStart = ContextCompat.getColor(this@MainActivity, R.color.material_red_300)
                            binding.progress.progressBarColorEnd = ContextCompat.getColor(this@MainActivity, R.color.material_red_700)
                            binding.progress.progressBarColorDirection =
                                CircularProgressBar.GradientDirection.TOP_TO_BOTTOM
                            binding.timerInfo.text = getString(R.string.break_info)
                        }
                        for (i in 1..breakTime) {
                            delay(1000)
                            currentTime++
                            runOnUiThread {
                                binding.progress.setProgressWithAnimation(
                                    (currentTime).toFloat(),
                                    1000
                                )
                                binding.timerCountdownTextView.text = formatTime((totalTime-currentTime)-breakTime)
                            }
                        }
                    } else {
                        stopTimer()
                    }
                    remainingTime = totalRoundTime
                }
            }
        }
    }

    private fun pauseTimer() {
        job.cancel()
        runOnUiThread {
            Toast.makeText(this, "Timer paused", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resumeTimer() {
        job = Job()
        startTimer()
        runOnUiThread {
            Toast.makeText(this, "Timer resumed", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun stopTimer() {
        withContext(Dispatchers.IO){
            isRunning = false
            job.cancel()
            remainingTime = totalRoundTime
            round = 1
            currentTime = 0
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Timer stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}