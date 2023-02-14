package com.wmsoftware.trainingtimer.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wmsoftware.trainingtimer.R
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class TrainingTimerViewModel : ViewModel(), CoroutineScope {
    var job: Job = Job()
    var isRunning = false
    var isPaused = false
    private var remainingTime = 0
    var totalRoundTime = MutableLiveData<Int>()
    var breakTime = MutableLiveData<Int>()
    var rounds = MutableLiveData<Int>()
    private var round = 1
    var totalTime = MutableLiveData<Int>()
    private var currentTime = 0
    var timerCountDownText = MutableLiveData<String>()
    var goRunning = MutableLiveData<Boolean>()
    var progress = MutableLiveData<Float>()
    var progressMax = MutableLiveData<Float>()
    val TAG = "TimerDebug"
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    suspend fun init(){
        withContext(Dispatchers.IO){
            totalRoundTime.postValue(10)
            breakTime.postValue(10)
            rounds.postValue(1)
            progressMax.postValue(10f)
            progress.postValue(0f)
            calculateTotalTime()
            Log.d(TAG,"ViewModel Initialized")
        }
    }

    fun startTimer(context:Context) {
        isRunning = true
        remainingTime = (totalRoundTime.value ?: 10)
        Log.d(TAG,"Total Time: ${totalTime.value}")
        Log.d(TAG,"Progress Max: ${progressMax.value}")
        launch {
                while (round <= (rounds.value ?: 1)) {
                    goRunning.postValue(true)
                    MediaPlayer.create(context, R.raw.sound_start).start()
                    for (i in 1..remainingTime) {
                        delay(1000)
                        if (!isPaused) {
                            remainingTime--
                            currentTime++
                            progress.postValue(currentTime.toFloat())
                            timerCountDownText.postValue(formatTime(((totalTime.value ?: 10)-currentTime)))
                        }
                    }
                    if (round < (rounds.value ?: 1)) {
                        round++
                        goRunning.postValue(false)
                        MediaPlayer.create(context, R.raw.sound_break).start()
                        for (i in 1..(breakTime.value ?: 10)) {
                            delay(1000)
                            currentTime++
                            progress.postValue(currentTime.toFloat())
                            timerCountDownText.postValue(formatTime(((totalTime.value ?: 10)-currentTime)))
                        }
                    } else {
                        stopTimer(context)
                    }
                    remainingTime = totalRoundTime.value ?: 10
                }
        }
    }

    fun calculateTotalTime() {
        try {
            progress.postValue(0f)
            totalTime.postValue((((totalRoundTime.value ?: 10) + (breakTime.value ?: 10)) * (rounds.value ?: 1)) - (breakTime.value ?: 10))
            progressMax.postValue(((((totalRoundTime.value ?: 10) + (breakTime.value ?: 10)) * (rounds.value ?: 1)) - (breakTime.value ?: 10)).toFloat())
        } catch (e: java.lang.Exception) {
            //
        }
    }

    suspend fun plusTotalRoundTime(){
        withContext(Dispatchers.IO){
            if((((totalRoundTime.value ?: 10) <= 999))){
                totalRoundTime.postValue((totalRoundTime.value ?: 10) + 5)
            }
        }
    }

    suspend fun lessTotalRoundTime(){
        withContext(Dispatchers.IO) {
            if ((((totalRoundTime.value ?: 10) > 10))) {
                totalRoundTime.postValue((totalRoundTime.value ?: 10) - 5)
            }
        }
    }

    suspend fun plusBreakTime(){
        withContext(Dispatchers.IO) {
            if ((((breakTime.value ?: 10) <= 999))) {
                breakTime.postValue((breakTime.value ?: 10) + 5)
            }
        }
    }

    suspend fun lessBreakTime(){
        withContext(Dispatchers.IO) {
            if ((((breakTime.value ?: 10) > 10))) {
                breakTime.postValue((breakTime.value ?: 10) - 5)
            }
        }
    }

    suspend fun plusInterval(){
        withContext(Dispatchers.IO) {
            if ((rounds.value ?: 1) < 999) {
                rounds.postValue((rounds.value ?: 1) + 1)
            }
        }
    }

    suspend fun lessInterval(){
        withContext(Dispatchers.IO) {
            if (((rounds.value ?: 1) > 1)) {
                rounds.postValue((rounds.value ?: 1) - 1)
            }
        }
    }

    fun pauseTimer() {
        isPaused = true
        job.cancel()
    }

    fun resumeTimer(context:Context) {
        isPaused = false
        job = Job()
        startTimer(context)
    }

    suspend fun stopTimer(context:Context) {
        withContext(Dispatchers.IO){
            isRunning = false
            job.cancel()
            remainingTime = totalRoundTime.value ?: 10
            round = 1
            currentTime = 0
            MediaPlayer.create(context, R.raw.sound_end).start()
        }
    }

    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}