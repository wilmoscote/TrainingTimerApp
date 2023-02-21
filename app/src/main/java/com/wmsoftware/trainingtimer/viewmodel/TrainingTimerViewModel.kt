package com.wmsoftware.trainingtimer.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
    private var roundMinuteTime = 0
    private var roundsSecondTime = 0
    private var breakMinuteTime = 0
    private var breakSecondTime = 0
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
    var trainingStep = MutableLiveData<Int>()
    val TAG = "TimerDebug"
    private var totalTimeElapsed = 0
    private var restTimeElapsed = 0
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    companion object {
        private var instance: TrainingTimerViewModel? = null

        fun getInstance(): TrainingTimerViewModel {
            if (instance == null) {
                instance = TrainingTimerViewModel()
            }
            return instance!!
        }
    }

    suspend fun init(context:Context){
        withContext(Dispatchers.IO){
            isRunning.postValue(false)
            totalRoundTime.postValue(10)
            breakTime.postValue(10)
            totalTime.postValue(10)
            round.postValue(1)
            rounds.postValue(1)
            progressMax.postValue(10f)
            progress.postValue(0f)
        }
    }

    private fun startTimer(context:Context) {
        isRunning.postValue(true)

         launch {
            while ((round.value ?: 1) <= (rounds.value ?: 1)) {
                if (remainingTime > 0) goRunning.postValue(true)
                trainingStep.postValue(2)
                MediaPlayer.create(context, R.raw.sound_end).start()
                for (i in 1..remainingTime) {
                    if (!isPaused) {
                        remainingTime--
                        totalTimeElapsed++
                        progress.postValue(totalTimeElapsed.toFloat())
                        timerCountDownText.postValue(formatTime(remainingTime))
                        //timerCountDownText.postValue(formatTime(((totalTime.value ?: 10) - totalTimeElapsed)))
                    }
                    if(remainingTime == 3){
                        MediaPlayer.create(context, R.raw.sound_countdown).start()
                    }
                    delay(1000)
                }
                if ((round.value ?: 1) < (rounds.value ?: 1)) {
                    goRunning.postValue(false)
                    trainingStep.postValue(1)
                    MediaPlayer.create(context, R.raw.sound_start).start()
                    for (i in 1..restTime) {
                        if (!isPaused) {
                            restTime--
                            restTimeElapsed++
                            progress.postValue((totalTimeElapsed + restTimeElapsed).toFloat())
                            timerCountDownText.postValue(formatTime(restTime))
                            //timerCountDownText.postValue(formatTime(((totalTime.value ?: 10) - (totalTimeElapsed + restTimeElapsed))))
                        }
                        if(restTime == 3){
                            MediaPlayer.create(context, R.raw.sound_countdown).start()
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
                    trainingStep.postValue(3)
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

    suspend fun setTotalRoundMinuteTime(value: Int){
        withContext(Dispatchers.IO){
            roundMinuteTime = value
            setTotalRoundTime()
        }
    }

    suspend fun setTotalRoundSecondsTime(value: Int){
        withContext(Dispatchers.IO){
            roundsSecondTime = value
            setTotalRoundTime()
        }
    }

    private suspend fun setTotalRoundTime(){
        withContext(Dispatchers.IO) {
            totalRoundTime.postValue((roundMinuteTime*60) + roundsSecondTime)
        }
    }

    suspend fun setTotalBreakMinuteTime(value: Int){
        withContext(Dispatchers.IO) {
            breakMinuteTime = value
            setTotalBreakTime()
        }
    }

    suspend fun setTotalBreakSecondsTime(value: Int){
        withContext(Dispatchers.IO) {
            breakSecondTime = value
            setTotalBreakTime()
        }
    }

    private suspend fun setTotalBreakTime(){
        withContext(Dispatchers.IO) {
            breakTime.postValue((breakMinuteTime*60) + breakSecondTime)
        }
    }


    fun prepareTimer(context: Context){
        launch {
            remainingTime = ((totalRoundTime.value ?: 10) - currentTime)
            restTime = (breakTime.value ?: 10)
            totalTimeElapsed = 0
            progress.postValue(0f)
            preparingTime(context)
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
        withContext(Dispatchers.IO) {
            job.cancel()
            remainingTime = totalRoundTime.value ?: 10
            restTime = breakTime.value ?: 10
            round.postValue(1)
            currentTime = 0
            totalTimeElapsed = 0

            isPaused = false
            isRunning.postValue(false)
        }
    }

    suspend fun setRounds(value: Int){
        withContext(Dispatchers.IO){
            rounds.postValue(value)
        }
    }

    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private suspend fun preparingTime(context: Context){
        trainingStep.postValue(1)
        withContext(Dispatchers.IO){
            for (i in 1..5) {
                timerCountDownText.postValue(formatTime(5-i))
                if(i == 2){
                    MediaPlayer.create(context, R.raw.sound_countdown).start()
                }
                delay(1000)
            }
            startTimer(context)
        }
    }
}