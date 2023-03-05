package com.wmsoftware.trainingtimer.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore.Audio.Media
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
    var roundMinuteTime = 0
    var roundsSecondTime = 0
    var breakMinuteTime = 0
    var breakSecondTime = 0
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
    var totalTimeElapsed = 0
    var notificationElapsedTime = 0
    private var restTimeElapsed = 0
    var countdown = 0
    var prepareTime = 0
    var prepareTimeTotal = 0
    var typeSound = 0
    var player: MediaPlayer? = null
    var isSoundEnable = true
    var elapsedPrepareTime = 0
    private var vibrator: Vibrator? = null
    var vibrate = false
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
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
                if(vibrate) vibrateOnce()
                player = if(typeSound == 0) MediaPlayer.create(context, R.raw.sound_end) else MediaPlayer.create(context, R.raw.bell_start)
                if(isSoundEnable) player?.start()
                for (i in 1..remainingTime) {
                    if (!isPaused) {
                        remainingTime--
                        totalTimeElapsed++
                        notificationElapsedTime++
                        progress.postValue(totalTimeElapsed.toFloat())
                        timerCountDownText.postValue(formatTime(remainingTime))
                        //timerCountDownText.postValue(formatTime(((totalTime.value ?: 10) - totalTimeElapsed)))
                    }
                    playCountdownSoundWorkout(context,remainingTime)
                    /*if(isFiveCountdown){
                        if(remainingTime == 3){
                            MediaPlayer.create(context, R.raw.sound_countdown).start()
                        }
                    } else {
                        if(remainingTime == 9){
                            MediaPlayer.create(context, R.raw.sound_countdown_10).start()
                        }
                    }*/

                    delay(1000)
                }
                if ((round.value ?: 1) < (rounds.value ?: 1)) {
                    goRunning.postValue(false)
                    trainingStep.postValue(1)
                    player = if(typeSound == 0) MediaPlayer.create(context, R.raw.sound_start) else MediaPlayer.create(context, R.raw.bell_break)
                    if(isSoundEnable) player?.start()
                    for (i in 1..restTime) {
                        if (!isPaused) {
                            restTime--
                            restTimeElapsed++
                            notificationElapsedTime++
                            progress.postValue((totalTimeElapsed + restTimeElapsed).toFloat())
                            timerCountDownText.postValue(formatTime(restTime))
                            //timerCountDownText.postValue(formatTime(((totalTime.value ?: 10) - (totalTimeElapsed + restTimeElapsed))))
                        }
                        playCountdownSoundWorkout(context,restTime)
                        /*if(isFiveCountdown){
                            if(restTime == 3){
                                MediaPlayer.create(context, R.raw.sound_countdown).start()
                            }
                        } else {
                            if(restTime == 9){
                                MediaPlayer.create(context, R.raw.sound_countdown_10).start()
                            }
                        }*/

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
                    if(vibrate) vibrateEnd()
                    if(isSoundEnable){
                        if(typeSound == 0) MediaPlayer.create(context, R.raw.sound_end).start() else MediaPlayer.create(context, R.raw.bell_end).start()
                    }
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
            Log.d(TAG,"TIMES: ${totalRoundTime.value.toString()} - ${breakTime.value.toString()} - ${rounds.value.toString()}")
            Log.d(TAG,"Total Time: ${totalTime.value.toString()}")
        } catch (e: Exception) {
            //
        }
    }

    fun calculateTotalTimeManually(roundTime: Int, breakTime: Int, rounds: Int) {
        try {
            progress.postValue(0f)
            totalTime.postValue(((roundTime + breakTime) * rounds) - breakTime)
            progressMax.postValue((((roundTime + breakTime) * rounds) - breakTime).toFloat())
            Log.d(TAG,"TIMES: ${roundTime.toString()} - ${breakTime.toString()} - ${rounds.toString()}")
            Log.d(TAG,"Total Time Manual: ${totalTime.value.toString()}")
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
        if(isSoundEnable) player?.stop()
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
            notificationElapsedTime = 0
            isPaused = false
            isRunning.postValue(false)
            if(isSoundEnable) player?.stop()
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

    private suspend fun preparingTime(context: Context) {
        isRunning.postValue(true)

        val prepareTimes = listOf(5, 10, 15, 5)
        prepareTimeTotal = prepareTimes.getOrNull(prepareTime) ?: 5

        trainingStep.postValue(1)

        repeat(prepareTimeTotal) { i ->
            timerCountDownText.postValue(formatTime(prepareTimeTotal - i))
            playCountdownSound(context, i, prepareTimeTotal)
            elapsedPrepareTime++
            delay(1000)
        }

        withContext(Dispatchers.IO) {
            prepareTimeTotal = 0
            elapsedPrepareTime = 0
            startTimer(context)
        }
    }

    private fun playCountdownSound(context: Context, index: Int, prepareTime: Int) {
        if (countdown == 0 && index == prepareTime - 3) {
            player = MediaPlayer.create(context, R.raw.sound_countdown)
            if(isSoundEnable) player?.start()
        } else if (countdown == 1 && index == prepareTime - 5) {
            player = MediaPlayer.create(context, R.raw.sound_countdown_5)
            if(isSoundEnable) player?.start()
        }else if (countdown == 2 && index == prepareTime - 10) {
            player = MediaPlayer.create(context, R.raw.sound_countdown_10)
            if(isSoundEnable) player?.start()
        }
    }

    private fun playCountdownSoundWorkout(context: Context,time: Int) {
        if (countdown == 0 && time == 3) {
            player = MediaPlayer.create(context, R.raw.sound_countdown)
            if(isSoundEnable) player?.start()
        } else if (countdown == 1 && time == 4) {
            player = MediaPlayer.create(context, R.raw.sound_countdown_5)
            if(isSoundEnable) player?.start()
        }else if (countdown == 2 && time == 10) {
            player = MediaPlayer.create(context, R.raw.sound_countdown_10)
            if(isSoundEnable) player?.start()
        }
    }

    private fun vibrateOnce(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator?.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(1000)
        }
    }

    private fun vibrateEnd(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val customVibrationPattern = longArrayOf(0, 500, 100, 500, 100, 500) // milisegundos de vibración y pausa alternadamente
            val vibrationEffect = VibrationEffect.createWaveform(customVibrationPattern, -1) // -1 para que no se repita
            vibrator?.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(1000)
        }
    }
}