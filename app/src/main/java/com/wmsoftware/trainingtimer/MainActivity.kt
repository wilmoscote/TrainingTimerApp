package com.wmsoftware.trainingtimer

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private var totalRoundTime = 0
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

        binding.timerDurationEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateTotalTime()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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
            totalRoundTime = binding.timerDurationEditText.text.toString().toInt()
            breakTime = binding.timerBreakEditText.text.toString().toInt()
            rounds = binding.timerIntervalsEditText.text.toString().toInt()
            totalTime = ((totalRoundTime + breakTime) * rounds) - breakTime
            binding.totalSessionTime.text = totalTime.toString()
        } catch (e: java.lang.Exception) {
            //
        }
    }

    private fun startTimer() {
        isRunning = true
        binding.timerCountdownTextView.text = "$remainingTime sec"
        binding.progress.progressMax = (totalTime - breakTime).toFloat()
        binding.progress.progress = 0f
        launch {
            withContext(Dispatchers.IO) {
                while (round <= rounds) {
                    runOnUiThread {
                        binding.progress.backgroundProgressBarColor = Color.DKGRAY
                        binding.progress.progressBarColorStart = ContextCompat.getColor(this@MainActivity, R.color.material_300)
                        binding.progress.progressBarColorEnd = ContextCompat.getColor(this@MainActivity, R.color.material_700)
                        binding.progress.progressBarColorDirection =
                            CircularProgressBar.GradientDirection.TOP_TO_BOTTOM
                    }
                    for (i in 1..remainingTime) {
                        delay(1000)
                        if (!isPaused) {
                            remainingTime--
                            currentTime++
                            runOnUiThread {
                                binding.progress.setProgressWithAnimation(
                                    (currentTime).toFloat(),
                                    2000
                                )
                                binding.timerCountdownTextView.text = "${remainingTime} sec"
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
                            binding.progress.progressBarColorStart = ContextCompat.getColor(this@MainActivity, R.color.material_purple_300)
                            binding.progress.progressBarColorEnd = ContextCompat.getColor(this@MainActivity, R.color.material_purple_700)
                            binding.progress.progressBarColorDirection =
                                CircularProgressBar.GradientDirection.TOP_TO_BOTTOM
                        }
                        for (i in 1..breakTime) {
                            delay(1000)
                            currentTime++
                            runOnUiThread {
                                binding.progress.setProgressWithAnimation(
                                    (currentTime).toFloat(),
                                    2000
                                )
                                binding.timerCountdownTextView.text = "${breakTime - i} sec"
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
}