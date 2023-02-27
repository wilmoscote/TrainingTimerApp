package com.wmsoftware.trainingtimer.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_KEYBOARD
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.crashlytics.internal.model.CrashlyticsReport.Session.User
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.wmsoftware.trainingtimer.BuildConfig
import com.wmsoftware.trainingtimer.R
import com.wmsoftware.trainingtimer.databinding.ActivityMainBinding
import com.wmsoftware.trainingtimer.model.Profile
import com.wmsoftware.trainingtimer.utils.UserPreferences
import com.wmsoftware.trainingtimer.viewmodel.TrainingTimerViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = "TimerDebug"
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TrainingTimerViewModel by lazy {
        TrainingTimerViewModel.getInstance()
    }
    private val languageCodes = arrayOf("nn", "en", "es", "pt")
    private val userPreferences = UserPreferences(this)
    private var profileSelected = 0
    private lateinit var userProfiles: MutableList<Profile>
    private lateinit var lastSesionDelete: Profile
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)


        lifecycleScope.launch(Dispatchers.IO) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                // Get new FCM registration token
                val token = task.result
                //Log.d(TAG, token.toString())
            })
            val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 1
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

            remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this@MainActivity) { task ->
                    if (task.isSuccessful) {
                        val appVersion = Firebase.remoteConfig.getDouble("appversion")
                        //Log.d(TAG, appVersion.toString())
                        if (appVersion.toInt() > BuildConfig.VERSION_CODE) {
                            forceUpdate()
                        }
                    }
                }
            userPreferences.getUserTheme().collect { theme ->
                if (theme == null) {
                    runOnUiThread {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        window.statusBarColor =
                            ContextCompat.getColor(this@MainActivity, R.color.card_night)
                    }
                } else if (theme) {
                    runOnUiThread {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        window.statusBarColor =
                            ContextCompat.getColor(this@MainActivity, R.color.card_night)
                    }
                } else {
                    runOnUiThread {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        window.statusBarColor =
                            ContextCompat.getColor(this@MainActivity, R.color.card_light)
                    }
                }
            }
        }

        //Obtener Perfiles
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getProfiles().collect { profiles ->
                userProfiles = profiles.toMutableList()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserLanguage().collect { language ->
                runOnUiThread {
                    if (language != null) {
                        setLocale(languageCodes[language])
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserPremium().collect { premium ->
                if (premium == true || premium == null) {
                    initAds()
                }
            }
        }

        //Inicializo el viewModel para setear los valores por defecto
        lifecycleScope.launch {
            viewModel.init(applicationContext)
            viewModel.calculateTotalTime()
        }
        val typefaceBold = Typeface.createFromAsset(assets, "Poppins-Bold.ttf")
        val typefaceRegular = Typeface.createFromAsset(assets, "Poppins-Regular.ttf")
        binding.minutePicker.typeface = typefaceRegular
        binding.minutePicker.setSelectedTypeface(typefaceBold)
        binding.secondsPicker.typeface = typefaceRegular
        binding.secondsPicker.setSelectedTypeface(typefaceBold)

        binding.minutePicker2.typeface = typefaceRegular
        binding.minutePicker2.setSelectedTypeface(typefaceBold)
        binding.secondsPicker2.typeface = typefaceRegular
        binding.secondsPicker2.setSelectedTypeface(typefaceBold)

        binding.roundsNumberPicker.typeface = typefaceRegular
        binding.roundsNumberPicker.setSelectedTypeface(typefaceBold)

        val popupMenu = PopupMenu(this, binding.btnProfiles)
        popupMenu.menuInflater.inflate(R.menu.user_menu, popupMenu.menu)

        /** Inicializo las variables **/
        //Tiempo por ronda, lo recibo y lo muestro al usuario.
        viewModel.totalRoundTime.observe(this) {
            //binding.textRoundTime.setText(viewModel.formatTime(it))
        }

        /** Inicializo los pickers para sumar y restar cantidad **/
        //Inicializo el boton para sumar 5 segundos al tiempo por ronda.
        binding.minutePicker.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setTotalRoundMinuteTime(newVal)
                viewModel.calculateTotalTime()
            }
        }

        binding.secondsPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setTotalRoundSecondsTime(newVal)
                viewModel.calculateTotalTime()
            }
        }

        /** Selectores de Tiempo de descanso **/
        binding.minutePicker2.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setTotalBreakMinuteTime(newVal)
                viewModel.calculateTotalTime()
            }
        }

        binding.secondsPicker2.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setTotalBreakSecondsTime(newVal)
                viewModel.calculateTotalTime()
            }
        }
        /** Selector de Cantidad de Rondas **/
        binding.roundsNumberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            lifecycleScope.launch {
                viewModel.setRounds(newVal)
                viewModel.calculateTotalTime()
            }
        }

        viewModel.totalTime.observe(this) {
            binding.totalTimeText.text = viewModel.formatTime(it)
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        binding.btnProfiles.setOnClickListener {
            //Guardar Perfil
            popupMenu.show()
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.mySessionsOption -> {
                    // Acción para la opción "Mis sesiones"
                    if (userProfiles.isEmpty()) {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.no_sessions),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        val singleItems = mutableListOf<String>()
                        try {
                            for (profile in userProfiles) {
                                singleItems.add(profile.name ?: "")
                            }
                        } catch (e:Exception){
                            //
                        }

                        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                            .setTitle(getString(R.string.my_sessions_title))
                            .setPositiveButton(getString(R.string.load_session_option)) { dialog, which ->
                                lifecycleScope.launch {
                                    viewModel.totalRoundTime.value =
                                        userProfiles[profileSelected].roundTime
                                    viewModel.breakTime.value =
                                        userProfiles[profileSelected].breakTime
                                    viewModel.rounds.value = userProfiles[profileSelected].rounds
                                    viewModel.calculateTotalTime()
                                    runOnUiThread {
                                        binding.roundsNumberPicker.value = userProfiles[profileSelected].rounds
                                        binding.secondsPicker.value = userProfiles[profileSelected].roundSeconds
                                        binding.minutePicker.value = userProfiles[profileSelected].roundMinutes
                                        binding.secondsPicker2.value = userProfiles[profileSelected].breakSeconds
                                        binding.minutePicker2.value = userProfiles[profileSelected].breakMinutes
                                        viewModel.roundsSecondTime = userProfiles[profileSelected].roundSeconds
                                        viewModel.roundMinuteTime = userProfiles[profileSelected].roundMinutes
                                        viewModel.breakSecondTime = userProfiles[profileSelected].breakSeconds
                                        viewModel.breakMinuteTime = userProfiles[profileSelected].breakMinutes
                                        binding.layoutCurrentSession.isVisible = true
                                        binding.txtCurrentSession.text = getString(R.string.current_session,userProfiles[profileSelected].name)
                                    }
                                }
                            }
                            .setSingleChoiceItems(
                                singleItems.toTypedArray(),
                                profileSelected
                            ) { dialog, which ->
                                profileSelected = which
                            }
                            .setNegativeButton(getString(R.string.cancel_option)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setNeutralButton(getString(R.string.delete_option)) { dialog, which ->
                                MaterialAlertDialogBuilder(
                                    this,
                                    R.style.MaterialAlertDialog_rounded
                                )
                                    .setTitle(getString(R.string.delete_session))
                                    .setMessage(getString(R.string.delete_session_message,userProfiles[profileSelected].name))
                                    .setPositiveButton(getString(R.string.delete_option)) { _, _ ->
                                        CoroutineScope(Dispatchers.IO).launch {

                                            val profileToDelete =
                                                userProfiles.find { it.id == userProfiles[profileSelected].id }
                                            if (profileToDelete != null) {
                                                lastSesionDelete = profileToDelete
                                                userProfiles.remove(profileToDelete)
                                            }
                                            userPreferences.saveProfiles(userProfiles.toList())
                                            runOnUiThread {
                                                Snackbar.make(
                                                    binding.root,
                                                    getString(R.string.session_deleted),
                                                    Snackbar.LENGTH_LONG
                                                )
                                                    .setAction(
                                                        getString(R.string.undo_option)
                                                    ) {
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            userProfiles.add(lastSesionDelete)
                                                            userPreferences.saveProfiles(
                                                                userProfiles
                                                            )
                                                            runOnUiThread {
                                                                Snackbar.make(
                                                                    binding.root,
                                                                    getString(R.string.session_restored),
                                                                    Snackbar.LENGTH_LONG
                                                                ).show()
                                                            }
                                                        }
                                                    }
                                                    .show()
                                            }
                                        }
                                    }
                                    .setNegativeButton(getString(R.string.cancel_option)) { _, _ ->
                                        dialog.dismiss()
                                    }
                                    .show()
                            }
                            .setCancelable(false)
                            .show()
                    }

                    true
                }
                R.id.saveSessionOption -> {
                    if (userProfiles.size < 8) {
                        // Guardar
                        val builder = MaterialAlertDialogBuilder(
                            this,
                            R.style.MaterialAlertDialog_rounded
                        )
                        val viewDialog = layoutInflater.inflate(R.layout.dialog_save_profile, null)
                        val txtSessionName = viewDialog.findViewById<EditText>(R.id.textSessionName)
                        builder.setTitle(getString(R.string.save_session))
                        builder.setView(viewDialog)
                        builder.setPositiveButton(getString(R.string.save_option)) { dialog, which ->
                            // Acción para el botón Guardar
                            userProfiles.add(
                                Profile(
                                    UUID.randomUUID().toString(),
                                    txtSessionName.text.toString(),
                                    (viewModel.totalRoundTime.value ?: 10),
                                    (viewModel.breakTime.value ?: 10),
                                    (viewModel.rounds.value ?: 1),
                                    viewModel.roundsSecondTime,
                                    viewModel.roundMinuteTime,
                                    viewModel.breakSecondTime,
                                    viewModel.breakMinuteTime
                                )
                            )
                            lifecycleScope.launch {
                                userPreferences.saveProfiles(userProfiles.toList())
                                runOnUiThread {
                                    binding.txtCurrentSession.text = getString(R.string.current_session,txtSessionName.text.toString())
                                    Snackbar.make(
                                        binding.root,
                                        getString(R.string.session_saved),
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        builder.setNegativeButton(getString(R.string.cancel_option), null)
                        val dialog = builder.create()

                        dialog.show()
                        txtSessionName.requestFocus()
                    } else {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.limit_session),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    true
                }
                else -> false
            }
        }

        binding.timerStartButton.setOnClickListener {
            if ((viewModel.totalRoundTime.value ?: 5) >= 5) {
                viewModel.trainingStep.value = 0
                startActivity(Intent(this@MainActivity, TimerActivity::class.java))
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.min_roundtime_limit),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getCountdown().collect { countdown ->
                runOnUiThread {
                    viewModel.countdown = countdown
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getPrepareTime().collect { prepareTime ->
                runOnUiThread {
                    viewModel.prepareTime = prepareTime
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserSoundEnable().collect { sound ->
                runOnUiThread {
                    viewModel.isSoundEnable = sound ?: true
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getTypeSound().collect { type ->
                runOnUiThread {
                    viewModel.typeSound = type
                }
            }
        }
    }

    private fun setLocale(language: String) {
        if (language != "nn") {
            val resources = resources
            val metrics = resources.displayMetrics
            val configuration = resources.configuration
            configuration.locale = Locale(language)
            resources.updateConfiguration(configuration, metrics)
            onConfigurationChanged(configuration)
        } else {
            val locale = Locale.getDefault()
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }

    private fun forceUpdate() {
        val alertadd = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
        val factory = LayoutInflater.from(this)
        val view: View = factory.inflate(R.layout.update_dialog, null)
        alertadd.setView(view)
        alertadd.setCancelable(true)
        alertadd.setPositiveButton(
            getString(R.string.but_update)
        ) { dlg, _ ->
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$packageName")
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            }
            finish()
        }
        alertadd.show()
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