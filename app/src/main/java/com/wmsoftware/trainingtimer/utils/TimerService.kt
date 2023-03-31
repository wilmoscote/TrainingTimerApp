package com.wmsoftware.trainingtimer.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData

import com.wmsoftware.trainingtimer.R
import com.wmsoftware.trainingtimer.view.MainActivity
import com.wmsoftware.trainingtimer.view.MainActivity.Companion.TAG
import com.wmsoftware.trainingtimer.view.TimerActivity
import com.wmsoftware.trainingtimer.viewmodel.TrainingTimerViewModel
import kotlinx.coroutines.*
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class TimerService : Service() {

    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO)
    private var timerJob: Job? = null
    private val timerLiveData = MutableLiveData<Long>()
    private var sessionDurationTotal = 0

    companion object {
        const val ACTION_START_TIMER = "ACTION_START_TIMER"
        const val ACTION_STOP_TIMER = "ACTION_STOP_TIMER"
        const val EXTRA_SESSION_DURATION = "EXTRA_SESSION_DURATION"
        const val EXTRA_SESSION_PROGRESS = "EXTRA_SESSION_PROGRESS"
        const val NOTIFICATION_CHANNEL_ID = "TimerChannelId"
        const val NOTIFICATION_ID = 1000
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForegroundNotification(sessionDuration: Long, sessionProgress: Long) {
        val pendingIntent: PendingIntent =
            Intent(this, TimerActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.session_in_progress))
            .setContentText(getString(R.string.workout_progress))
            .setSmallIcon(R.drawable.ic_countdown)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setProgress(sessionDuration.toInt(), sessionProgress.toInt(), false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(time: Long, progress: Double) {

        val pendingIntent: PendingIntent =
            Intent(this, TimerActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.session_in_progress))
            .setContentText("${(Math.round(progress * 10.0) / 10.0).toFloat()} %")
            .setSmallIcon(R.drawable.ic_countdown)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setProgress(sessionDurationTotal, time.toInt(), false)
            .build()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun notificationEnd() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.session_ended))
            .setContentText(getString(R.string.workout_ended))
            .setSmallIcon(R.drawable.ic_countdown)
            .setAutoCancel(true)
            .build()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Training Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        when (intent?.action) {
            ACTION_START_TIMER -> {
                if (!isRunning) {
                    val sessionDuration = intent.getIntExtra(EXTRA_SESSION_DURATION, 0).toLong()
                    val sessionProgress = intent.getIntExtra(EXTRA_SESSION_PROGRESS, 0).toLong()
                    sessionDurationTotal = intent.getIntExtra(EXTRA_SESSION_DURATION, 0)
                    //Log.d(TAG, "Starting service and notify")
                    isRunning = true
                    startTimer(
                        sessionDuration,
                        sessionProgress,
                        intent.getIntExtra(EXTRA_SESSION_DURATION, 0),
                        intent.getIntExtra(EXTRA_SESSION_PROGRESS, 0)
                    )
                    startForegroundNotification(sessionDuration, sessionProgress)
                }
            }
            ACTION_STOP_TIMER -> {
                if (isRunning) {
                    timerJob?.cancel()
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
        val pendingIntent: PendingIntent =
            Intent(this, TimerActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.session_in_progress))
            .setContentText(getString(R.string.workout_progress))
            .setSmallIcon(R.drawable.ic_countdown)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .setAutoCancel(false)
            .setProgress(intent?.getIntExtra(EXTRA_SESSION_DURATION, 0) ?: 0, intent?.getIntExtra(EXTRA_SESSION_PROGRESS, 0) ?: 0, false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun startTimer(
        sessionDuration: Long,
        sessionProgress: Long,
        sessionDurationInt: Int,
        sessionProgressInt: Int
    ) {
        timerJob = scope.launch {
            var time = sessionProgress
            var progress = sessionProgressInt
            while (time < sessionDuration) {
                delay(1000)
                time++
                progress++
                updateNotification(time, progress.toDouble() / sessionDurationTotal.toDouble() * 100)
            }
            notificationEnd()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    override fun onCreate() {
        super.onCreate()

    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopTimer()
    }
}

