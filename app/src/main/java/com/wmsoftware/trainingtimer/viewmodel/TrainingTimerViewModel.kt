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
    var isRunning = MutableLiveData<Boolean>()
    var isPaused = false
    private var remainingTime = 0
    private var restTime = 0
    var totalRoundTime = MutableLiveData<Int>()
    var breakTime = MutableLiveData<Int>()
    var rounds = MutableLiveData<Int>()
    var round = MutableLiveData<Int>()
    var totalTime = MutableLiveData<Int>()
    private var currentTime = 0
    var timerCountDownText = MutableLiveData<String>()
    var goRunning = MutableLiveData<Boolean>()
    var progress = MutableLiveData<Float>()
    var progressMax = MutableLiveData<Float>()
    val TAG = "TimerDebug"
    private var totalTimeElapsed = 0
    private var restTimeElapsed = 0
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    suspend fun init(){
        withContext(Dispatchers.IO){
            isRunning.postValue(false)
            totalRoundTime.postValue(5)
            breakTime.postValue(5)
            totalTime.postValue(5)
            round.postValue(1)
            rounds.postValue(1)
            progressMax.postValue(5f)
            progress.postValue(0f)
        }
    }

    private fun startTimer(context:Context) {
        isRunning.postValue(true)
         launch {
            while ((round.value ?: 1) <= (rounds.value ?: 1)) {
                if (remainingTime > 0) goRunning.postValue(true)
                MediaPlayer.create(context, R.raw.sound_end).start()
                for (i in 1..remainingTime) {
                    if (!isPaused) {
                        remainingTime--
                        totalTimeElapsed++
                        progress.postValue(totalTimeElapsed.toFloat())
                        timerCountDownText.postValue(formatTime(((totalTime.value ?: 10) - totalTimeElapsed)))
                    }
                    delay(1000)
                }
                if ((round.value ?: 1) < (rounds.value ?: 1)) {
                    goRunning.postValue(false)
                    MediaPlayer.create(context, R.raw.sound_start).start()
                    for (i in 1..restTime) {
                        if (!isPaused) {
                            restTime--
                            restTimeElapsed++
                            progress.postValue((totalTimeElapsed + restTimeElapsed).toFloat())
                            timerCountDownText.postValue(formatTime(((totalTime.value ?: 10) - (totalTimeElapsed + restTimeElapsed))))
                        }
                        delay(1000)
                    }
                    round.postValue((round.value ?: 1) + 1)
                    currentTime = 0
                    remainingTime = ((totalRoundTime.value ?: 10) - currentTime)
                    restTime = (breakTime.value ?: 10)
                    totalTimeElapsed += restTimeElapsed
                    restTimeElapsed = 0
                } else {
                    stopTimer(context)
                }
            }
        }
    }



    fun calculateTotalTime() {
        try {
            progress.postValue(0f)
            totalTime.postValue((((totalRoundTime.value ?: 10) + (breakTime.value ?: 10)) * (rounds.value ?: 1)) - (breakTime.value ?: 10))
            progressMax.postValue(((((totalRoundTime.value ?: 10) + (breakTime.value ?: 10)) * (rounds.value ?: 1)) - (breakTime.value ?: 10)).toFloat())
        } catch (e: Exception) {
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

    fun prepareTimer(context: Context){
        remainingTime = ((totalRoundTime.value ?: 10) - currentTime)
        restTime = (breakTime.value ?: 10)
        totalTimeElapsed = 0
        startTimer(context)
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
            job.cancel()
            remainingTime = totalRoundTime.value ?: 10
            restTime = breakTime.value ?: 10
            round.postValue(1)
            currentTime = 0
            totalTimeElapsed = 0
            MediaPlayer.create(context, R.raw.sound_end).start()
            isPaused = false
            isRunning.postValue(false)
        }
    }

    suspend fun manualRoundTime(time:Int){
        withContext(Dispatchers.IO){
            totalRoundTime.postValue(time)
        }
    }

    suspend fun manualBreakTime(time:Int){
        withContext(Dispatchers.IO){
            breakTime.postValue(time)
        }
    }

    suspend fun manualRounds(time:Int){
        withContext(Dispatchers.IO){
            rounds.postValue(time)
        }
    }

    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}