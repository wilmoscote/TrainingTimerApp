package com.wmsoftware.trainingtimer.view

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.media.MediaPlayer
import android.os.*
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.wmsoftware.trainingtimer.R
import com.wmsoftware.trainingtimer.databinding.ActivityTimerBinding
import com.wmsoftware.trainingtimer.utils.TimerService
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
    private var timerServiceIntent: Intent? = null
    var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userPreferences = UserPreferences(this)
        timerServiceIntent = Intent(this, TimerService::class.java)
        lifecycleScope.launch {
            userPreferences.getUserVibration().collect { vibration ->
                runOnUiThread {
                    viewModel.vibrate = vibration ?: true
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserPremium().collect { premium ->
                if(premium == true || premium == null){
                    initAds()
                }
            }
        }

        try {
            val display: Display? = windowManager?.defaultDisplay
            val size = Point()
            display?.getSize(size)
            val width: Int = size.x  //540
            val height: Int = size.y //960
            if (width > 540 && height > 960) {
                //
            } else {
                binding.trainingAnimation.isVisible = false
            }
        } catch (e: Exception) {
            //Log.e("ScreenDebug", "Error detecting screen")
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
                }
                3 -> {
                    binding.btnBackTimer.isVisible = true
                    binding.timerInfo.text = getString(R.string.well_done)
                    binding.trainingAnimation.scaleX = 1.3f
                    binding.trainingAnimation.scaleY = 1.3f
                    binding.trainingAnimation.setAnimation(R.raw.claps)
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
            doubleBackToExitPressedOnce = true
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    lifecycleScope.launch {
                        viewModel.stopTimer(this@TimerActivity)
                        viewModel.calculateTotalTime()
                        withContext(Dispatchers.Main){
                            viewModel.trainingStep.value = 0
                            viewModelStore.clear()
                            finish()
                        }
                    }
                    return
                }
                doubleBackToExitPressedOnce = true
                Snackbar.make(
                    binding.root,
                    getString(R.string.press_twice_text),
                    Snackbar.LENGTH_LONG
                ).show()
                Handler(Looper.getMainLooper()).postDelayed(kotlinx.coroutines.Runnable {
                    doubleBackToExitPressedOnce = false
                }, 5000)
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

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (viewModel.isRunning.value == true) {
            timerServiceIntent?.apply {
                putExtra("EXTRA_SESSION_DURATION",(viewModel.totalTime.value ?: 10) + (viewModel.prepareTimeTotal-viewModel.elapsedPrepareTime))
                putExtra("EXTRA_SESSION_PROGRESS", (viewModel.notificationElapsedTime))
                action = TimerService.ACTION_START_TIMER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               startForegroundService(timerServiceIntent)
            } else {
                startService(timerServiceIntent)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {*/
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
        //}
        timerServiceIntent?.apply {
            action = TimerService.ACTION_STOP_TIMER
        }
        stopService(timerServiceIntent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // Mostrar de nuevo la barra de estado y los iconos cuando la actividad pierda el foco
        if (!hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    private fun initAds() {
        MobileAds.initialize(this) {}

        val adRequest = AdRequest.Builder().build()
        runOnUiThread {
            binding.adView.loadAd(adRequest)

            binding.adView.adListener = object : AdListener() {
                override fun onAdClicked() {

                    // Code to be executed when the user clicks on an ad.
                }

                override fun onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Code to be executed when an ad request fails.
                }

                override fun onAdImpression() {
                    // Code to be executed when an impression is recorded
                    // for an ad.
                }

                override fun onAdLoaded() {
                    binding.adView.isVisible = true
                    // Code to be executed when an ad finishes loading.
                }

                override fun onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                }
            }
        }
    }
}