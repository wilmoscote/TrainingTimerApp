package com.wmsoftware.trainingtimer.view

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.common.collect.ImmutableList
import com.wmsoftware.trainingtimer.BuildConfig
import com.wmsoftware.trainingtimer.R
import com.wmsoftware.trainingtimer.databinding.ActivitySettingBinding
import com.wmsoftware.trainingtimer.utils.Security
import com.wmsoftware.trainingtimer.utils.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class SettingActivity : AppCompatActivity(), PurchasesUpdatedListener {
    private lateinit var binding: ActivitySettingBinding
    private lateinit var userPreferences: UserPreferences
    private var selectedLanguage = 0
    private var selectedPrepareTime = 0
    private var selectedCountdown = 0
    private var selectedTypeSound = 0
    var checkedItem = 0
    private val languageCodes = arrayOf("nn", "en", "es", "pt")
    private lateinit var prepareTimeValues: Array<String>
    private lateinit var countdownValues: Array<String>
    private lateinit var typeSoundValues: Array<String>
    lateinit var billingClient: BillingClient
    lateinit var planProductDetails: MutableList<ProductDetails>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)
        lifecycleScope.launch(Dispatchers.IO) {
            initBilling()
            prepareTimeValues = arrayOf(
                getString(R.string.five_secs),
                getString(R.string.ten_secs),
                getString(R.string.fiveteen_secs)
            )
            countdownValues = arrayOf(
                getString(R.string.three_secs),
                getString(R.string.five_secs),
                getString(R.string.ten_secs)
            )
            typeSoundValues = arrayOf(getString(R.string.whistle), getString(R.string.bell))
            userPreferences.getUserTheme().collect { theme ->
                runOnUiThread {
                    binding.switchTheme.isChecked = theme ?: false
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserPremium().collect { premium ->
                if (premium == false) {
                    runOnUiThread {
                        binding.btnAds.isVisible = false
                    }
                } else {
                    initAds()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserVibration().collect { vibrate ->
                runOnUiThread {
                    binding.switchVibrate.isChecked = vibrate ?: true
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserSoundEnable().collect { sound ->
                runOnUiThread {
                    binding.switchSound.isChecked = sound ?: true
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getTypeSound().collect { type ->
                runOnUiThread {
                    try {
                        binding.txtCurrentSound.text = typeSoundValues[type]
                        selectedTypeSound = type
                    } catch (e: Exception) {
                        typeSoundValues =
                            arrayOf(getString(R.string.whistle), getString(R.string.bell))
                        binding.txtCurrentSound.text = typeSoundValues[type]
                        selectedTypeSound = type
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getCountdown().collect { countdown ->
                runOnUiThread {
                    try {
                        binding.txtCurrentCountdown.text = countdownValues[countdown]
                        selectedCountdown = countdown
                    } catch (e: Exception) {
                        countdownValues = arrayOf(
                            getString(R.string.three_secs),
                            getString(R.string.five_secs),
                            getString(R.string.ten_secs)
                        )
                        binding.txtCurrentCountdown.text = countdownValues[countdown]
                        selectedCountdown = countdown
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getPrepareTime().collect { prepareTime ->
                runOnUiThread {
                    try {
                        binding.txtCurrentPreparingTime.text = prepareTimeValues[prepareTime]
                        selectedPrepareTime = prepareTime
                    } catch (e: Exception) {
                        prepareTimeValues = arrayOf(
                            getString(R.string.five_secs),
                            getString(R.string.ten_secs),
                            getString(R.string.fiveteen_secs)
                        )
                        binding.txtCurrentPreparingTime.text = prepareTimeValues[prepareTime]
                        selectedPrepareTime = prepareTime
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserLanguage().collect { language ->
                runOnUiThread {
                    when (language) {
                        0 -> {
                            checkedItem = 0
                        }
                        1 -> {
                            checkedItem = 1
                        }
                        2 -> {
                            checkedItem = 2
                        }
                        3 -> {
                            checkedItem = 3
                        }
                        else -> {
                            checkedItem = 0
                        }
                    }
                }
            }
        }

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            try {
                if (isChecked) {
                    setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    userPreferences.saveTheme(isChecked)
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                    .show()
            }
        }

        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch(Dispatchers.IO) {
                userPreferences.saveVibration(isChecked)
            }
        }

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch(Dispatchers.IO) {
                userPreferences.saveSoundEnable(isChecked)
            }
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        binding.btnTerms.setOnClickListener {
            startActivity(Intent(this, TermsActivity::class.java))
        }

        binding.versionInfo.setOnLongClickListener {
            Toast.makeText(this, "S \uD83D\uDC9B", Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }

        binding.switchLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.layoutPreparingTime.setOnClickListener {
            showPrepareTimeDialog()
        }

        binding.layoutCountdown.setOnClickListener {
            showCountdownDialog()
        }

        binding.layoutTypeSound.setOnClickListener {
            showTypeSoundDialog()
        }

        binding.btnAds.setOnClickListener {
            try {
                launchPurchaseFlow(planProductDetails[0])
            } catch (e: Exception) {
                //
            }
        }

        binding.versionInfo.text = getString(R.string.version_info, BuildConfig.VERSION_NAME)

        binding.txtCurrentLanguage.text =
            resources.configuration.locales.get(0).displayName.toString()
    }

    private fun showTypeSoundDialog() {
        try {

            val singleItems = mutableListOf(
                getString(R.string.whistle).uppercase(),
                getString(R.string.bell).uppercase()
            )
            MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                .setTitle(getString(R.string.change_sound_text).uppercase())
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        userPreferences.saveTypeSound(selectedTypeSound)
                        runOnUiThread {
                            binding.txtCurrentSound.text = typeSoundValues[selectedTypeSound]
                        }
                    }
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                    dialog.dismiss()
                }
                .setSingleChoiceItems(
                    singleItems.toTypedArray(),
                    selectedTypeSound
                ) { dialog, which ->
                    selectedTypeSound = which
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun showCountdownDialog() {
        try {

            val singleItems = mutableListOf(
                getString(R.string.three_secs_long),
                getString(R.string.five_secs_long),
                getString(R.string.ten_secs_long)
            )
            MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                .setTitle(getString(R.string.countdown_title))
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        userPreferences.saveCountdown(selectedCountdown)
                        runOnUiThread {
                            binding.txtCurrentCountdown.text = countdownValues[selectedCountdown]
                        }
                    }
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                    dialog.dismiss()
                }
                .setSingleChoiceItems(
                    singleItems.toTypedArray(),
                    selectedCountdown
                ) { dialog, which ->
                    selectedCountdown = which
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun showPrepareTimeDialog() {
        try {
            val singleItems = mutableListOf(
                getString(R.string.five_secs_long),
                getString(R.string.ten_secs_long),
                getString(R.string.fiveteen_secs_long)
            )
            MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                .setTitle(getString(R.string.preparation_time_title))
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        userPreferences.savePrepareTime(selectedPrepareTime)
                        runOnUiThread {
                            binding.txtCurrentPreparingTime.text =
                                prepareTimeValues[selectedPrepareTime]
                        }
                    }
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                    dialog.dismiss()
                }
                .setSingleChoiceItems(
                    singleItems.toTypedArray(),
                    selectedPrepareTime
                ) { dialog, which ->
                    selectedPrepareTime = which
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun showLanguageDialog() {
        try {
            val singleItems = mutableListOf(
                getString(R.string.device_language),
                getString(R.string.english),
                getString(R.string.spanish),
                getString(R.string.portuguese)
            )
            MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                .setTitle(getString(R.string.change_language_title))
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        userPreferences.saveLanguage(selectedLanguage)
                        runOnUiThread {
                            if (selectedLanguage != 0) {
                                setLocale(languageCodes[selectedLanguage])
                            } else {
                                val defaultLocale = Locale.getDefault().language
                                setLocale(defaultLocale)
                            }

                        }
                    }
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                    dialog.dismiss()
                }
                .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                    selectedLanguage = which
                    checkedItem = which
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun setLocale(language: String) {
        try {
            if (language != "nn") {
                val resources = resources
                val metrics = resources.displayMetrics
                val configuration = resources.configuration
                configuration.locale = Locale(language)
                resources.updateConfiguration(configuration, metrics)
                onConfigurationChanged(configuration)
                recreate()
            } else {
                val locale = Locale.getDefault()
                Locale.setDefault(locale)
                val config = resources.configuration
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun initBilling() {
        try {
            billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build()

            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // The BillingClient is ready. You can query purchases here.
                        //Log.d(TAG, "Conected to Google!")
                        showProducts()
                        CoroutineScope(Dispatchers.IO).launch { checkUserPurcharses() }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.

                    //binding.vpPlanes.isVisible = false
                    //binding.btnBuy.isVisible = false
                    //binding.error.isVisible = true
                    //Log.d(TAG, "Error to connect to Google!")
                }
            })
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    fun showProducts() {
        try {

            val queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        ImmutableList.of(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("remove_ads")
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                        )
                    )
                    .build()

            billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult,
                                                                                productDetailsList ->
                // check billingResult
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    planProductDetails = productDetailsList.reversed().toMutableList()
                    //Log.d(TAG, productDetailsList.toString())
                    //Log.d(TAG, "R: $planProductDetails")
                    /*for (productDetails in productDetailsList) {

                        if (productDetails.productId == "silver_plan_medicall") {
                            val subDetails = productDetails.subscriptionOfferDetails!!
                            //Log.d(TAG, subDetails[0].offerToken)
                            //Log.d(TAG,subDetails[0].pricingPhases.pricingPhaseList[0]
                                .formattedPrice.toString() + " Per Month")
                            //Log.d(TAG, subDetails[0].pricingPhases.toString())
                        }
                    }*/
                }

            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun launchPurchaseFlow(productDetails: ProductDetails) {
        try {
            val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken

            val productDetailsParamsList =
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        //.setOfferToken(offerToken.toString())
                        .build()
                )
            val billingFlowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

            // Launch the billing flow
            val billingResult = billingClient.launchBillingFlow(this, billingFlowParams)
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        try {
            //Log.d(TAG, "Handling purcharse")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    CoroutineScope(Dispatchers.IO).launch { handlePurchase(purchase) }
                }
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        try {
            // Purchase retrieved from BillingClient#queryPurchasesAsync or your PurchasesUpdatedListener.
            //if item is purchased
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                //Log.d(TAG, "Purcharsed")
                if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
                    // Invalid purchase
                    // show error to user
                    withContext(Dispatchers.Main) {
                        Snackbar.make(binding.root, R.string.purcharse_error, Snackbar.LENGTH_SHORT)
                            .show()
                    }
                    //Log.d(TAG, "Error invalid purcharse")
                    //skip current iteration only because other items in purchase list must be checked if present
                    return
                }
                // else purchase is valid
                //if item is purchased and not  Acknowledged
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(
                        acknowledgePurchaseParams
                    ) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            //if purchase is acknowledged
                            //then saved value in preference
                            Snackbar.make(
                                binding.root,
                                R.string.purcharse_success,
                                Snackbar.LENGTH_SHORT
                            ).show()
                            lifecycleScope.launch {
                                userPreferences.saveUserPremium(false)
                            }
                            finishAffinity()
                            startActivity(Intent(this@SettingActivity, MainActivity::class.java))
                        }
                    }
                } else {
                    // Grant entitlement to the user on item purchase
                    userPreferences.getUserPremium().collect { premium ->
                        if (premium == false) {
                            Snackbar.make(
                                binding.root,
                                R.string.purcharse_success,
                                Snackbar.LENGTH_SHORT
                            ).show()
                            userPreferences.saveUserPremium(false)
                            finishAffinity()
                            startActivity(Intent(this@SettingActivity, MainActivity::class.java))
                        }
                    }
                }
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        R.string.purcharse_status_pending,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                //Log.d(TAG, "Purcharse pending")
            } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                //mark purchase false in case of UNSPECIFIED_STATE
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        R.string.purcharse_status_unknown,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                //Log.d(TAG, "Purcharse status unknown")
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            val base64Key =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi4DPXFWdz6mao774nLnzf1kO0sCksT7TgxU7jGiw8fZ6ZUL8noBGzxePK6Ymj8zdd6elAmYy1ty1gxg8YUbfEmz5PCE+lutWb9HJh2xqA56G5SS1dUSiZdRvpHfwaCsu0FKxI0kuANW3R9KRe/PzVQCU8P1DlBBbU4ql9EedbxcEVcLqqkquhrs8UA9jEH/C0KGb9N5m9C9bEDO7KBC5dZh5ruaIkNnLPrAcjnjkmnbqgrih0Te0aSrQ9b5pzMqxRZtCqI2yn70klWSMCQIotr1wxiEZGrTjB0+u/dEhyCT7eJ5SEOr+zgENdF624lgHEX1w/vCLsLEXNPQqbtySywIDAQAB"
            Security.verifyPurchase(base64Key, signedData, signature)
        } catch (e: IOException) {
            false
        }
    }


    private suspend fun checkUserPurcharses() {
        try {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)

            // uses queryPurchasesAsync Kotlin extension function
            val purchasesResult = billingClient.queryPurchasesAsync(params.build())

            for (purcharse in purchasesResult.purchasesList) {
                userPreferences.getUserPremium().collect { premium ->
                    if (purcharse.products[0].toString() == "remove_ads" && (premium == true || premium == null)) {
                        userPreferences.saveUserPremium(false)
                        Snackbar.make(
                            binding.root,
                            R.string.purcharse_restored,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        finishAffinity()
                        startActivity(Intent(this@SettingActivity, MainActivity::class.java))
                    }
                }
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun initAds() {
        try {
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
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.action_error), Snackbar.LENGTH_SHORT)
                .show()
        }
    }
}