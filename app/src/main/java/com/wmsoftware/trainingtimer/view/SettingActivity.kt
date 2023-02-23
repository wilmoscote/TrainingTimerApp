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
    var checkedItem = 0
    private val languageCodes = arrayOf("nn","en", "es", "pt")
    lateinit var billingClient: BillingClient
    lateinit var planProductDetails: MutableList<ProductDetails>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)
        lifecycleScope.launch(Dispatchers.IO) {
            initBilling()
            userPreferences.getUserTheme().collect { theme ->
                runOnUiThread {
                    binding.switchTheme.isChecked = theme ?: false
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userPreferences.getUserPremium().collect { premium ->
                if(premium == false ){
                    runOnUiThread {
                        binding.btnAds.isVisible = false
                    }
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
            userPreferences.getUserLanguage().collect { language ->
                runOnUiThread {
                    when(language){
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
            if (isChecked) {
                setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                userPreferences.saveTheme(isChecked)
            }
        }

        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch(Dispatchers.IO) {
                userPreferences.saveVibration(isChecked)
            }
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                finish()
            }
        })

        binding.btnTerms.setOnClickListener {
            startActivity(Intent(this,TermsActivity::class.java))
        }

        binding.versionInfo.setOnLongClickListener {
            Toast.makeText(this,"S \uD83D\uDC9B",Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }

        binding.switchLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.btnAds.setOnClickListener {
            launchPurchaseFlow(planProductDetails[0])
        }

        binding.versionInfo.text = "v${BuildConfig.VERSION_NAME} By Flexifit"

        binding.txtCurrentLanguage.text = resources.configuration.locales.get(0).displayName.toString()
    }

    private fun showLanguageDialog(){
        val singleItems = mutableListOf("Idioma del dispositivo","Inglés","Español","Portugués")
        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            .setTitle("CAMBIAR IDIOMA")
            .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                lifecycleScope.launch(Dispatchers.IO) {
                    userPreferences.saveLanguage(selectedLanguage)
                    runOnUiThread {
                        if(selectedLanguage != 0){
                            setLocale(languageCodes[selectedLanguage])
                        } else {
                            val defaultLocale = Locale.getDefault().language
                            setLocale(defaultLocale)
                        }

                    }
                }
            }
            .setNegativeButton(resources.getString(R.string.cancel)){dialog, which ->
                dialog.dismiss()
            }
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                selectedLanguage = which
                checkedItem = which
            }
            .setCancelable(false)
            .show()
    }

    private fun setLocale(language:String){
        if(language != "nn") {
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
    }

    private fun initBilling(){

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
                    CoroutineScope(Dispatchers.IO).launch{checkUserPurcharses()}
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
    }

    fun showProducts() {
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
    }

    private fun launchPurchaseFlow(productDetails: ProductDetails) {
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
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        //Log.d(TAG, "Handling purcharse")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                CoroutineScope(Dispatchers.IO).launch { handlePurchase(purchase) }
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        // Purchase retrieved from BillingClient#queryPurchasesAsync or your PurchasesUpdatedListener.
        //if item is purchased
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            //Log.d(TAG, "Purcharsed")
            if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
                // Invalid purchase
                // show error to user
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root,R.string.purcharse_error, Snackbar.LENGTH_SHORT).show()
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
                billingClient.acknowledgePurchase(acknowledgePurchaseParams
                ) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        //if purchase is acknowledged
                        //then saved value in preference
                        Snackbar.make(binding.root,R.string.purcharse_success, Snackbar.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            userPreferences.saveUserPremium(false)
                        }
                        finishAffinity()
                        startActivity(Intent(this@SettingActivity,MainActivity::class.java))
                    }
                }
            } else {
                // Grant entitlement to the user on item purchase
                userPreferences.getUserPremium().collect { premium ->
                    if (premium == false){
                        Snackbar.make(binding.root,R.string.purcharse_success, Snackbar.LENGTH_SHORT).show()
                        userPreferences.saveUserPremium(false)
                        finishAffinity()
                        startActivity(Intent(this@SettingActivity,MainActivity::class.java))
                    }
                }
            }
        }
        else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            withContext(Dispatchers.Main) {
                Snackbar.make(binding.root,R.string.purcharse_status_pending, Snackbar.LENGTH_SHORT).show()
            }
            //Log.d(TAG, "Purcharse pending")
        }
        else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            //mark purchase false in case of UNSPECIFIED_STATE
            withContext(Dispatchers.Main) {
                Snackbar.make(binding.root,R.string.purcharse_status_unknown, Snackbar.LENGTH_SHORT).show()
            }
            //Log.d(TAG, "Purcharse status unknown")
        }
    }

    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            val base64Key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi4DPXFWdz6mao774nLnzf1kO0sCksT7TgxU7jGiw8fZ6ZUL8noBGzxePK6Ymj8zdd6elAmYy1ty1gxg8YUbfEmz5PCE+lutWb9HJh2xqA56G5SS1dUSiZdRvpHfwaCsu0FKxI0kuANW3R9KRe/PzVQCU8P1DlBBbU4ql9EedbxcEVcLqqkquhrs8UA9jEH/C0KGb9N5m9C9bEDO7KBC5dZh5ruaIkNnLPrAcjnjkmnbqgrih0Te0aSrQ9b5pzMqxRZtCqI2yn70klWSMCQIotr1wxiEZGrTjB0+u/dEhyCT7eJ5SEOr+zgENdF624lgHEX1w/vCLsLEXNPQqbtySywIDAQAB"
            Security.verifyPurchase(base64Key, signedData, signature)
        } catch (e: IOException) {
            false
        }
    }


    private suspend fun checkUserPurcharses(){
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)

        // uses queryPurchasesAsync Kotlin extension function
        val purchasesResult = billingClient.queryPurchasesAsync(params.build())

        for (purcharse in purchasesResult.purchasesList){
            Log.d("BuyDebug","History: ${purcharse.toString()} produc: ${purcharse.products[0].toString()}")
            userPreferences.getUserPremium().collect { premium ->
                if (purcharse.products[0].toString() == "remove_ads" && (premium == true || premium == null)){
                    userPreferences.saveUserPremium(false)
                    Snackbar.make(binding.root,R.string.purcharse_restored,Snackbar.LENGTH_SHORT).show()
                    finishAffinity()
                    startActivity(Intent(this@SettingActivity,MainActivity::class.java))
                }
            }

        }
    }
}