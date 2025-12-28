package com.vbrosseau.stackgame.data

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.util.Log
import com.android.billingclient.api.*
import com.vbrosseau.stackgame.models.PurchaseState
import com.vbrosseau.stackgame.models.UserLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

class BillingManager(private val context: Context) {
    
    private val _purchaseState = MutableStateFlow(PurchaseState())
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()
    
    private var billingClient: BillingClient? = null
    
    // TODO: Remplacer par votre clé publique de Google Play Console
    private val base64PublicKey = "YOUR_BASE64_PUBLIC_KEY_FROM_GOOGLE_PLAY_CONSOLE"
    
    companion object {
        const val PRODUCT_PREMIUM = "premium_tier"
        const val PRODUCT_ULTRA = "ultra_tier"
        private const val TAG = "BillingManager"
    }
    
    fun initialize(onReady: () -> Unit = {}) {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                }
            }
            .enablePendingPurchases()
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client ready")
                    queryPurchases()
                    onReady()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                // Retry connection
            }
        })
    }
    
    fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }
    
    private fun handlePurchases(purchases: List<Purchase>) {
        var hasPremium = false
        var hasUltra = false
        var purchaseToken: String? = null
        var purchaseTime = 0L
        
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (verifyPurchase(purchase)) {
                    when {
                        purchase.products.contains(PRODUCT_ULTRA) -> {
                            hasUltra = true
                            purchaseToken = purchase.purchaseToken
                            purchaseTime = purchase.purchaseTime
                        }
                        purchase.products.contains(PRODUCT_PREMIUM) -> {
                            hasPremium = true
                            purchaseToken = purchase.purchaseToken
                            purchaseTime = purchase.purchaseTime
                        }
                    }
                    
                    // Acknowledge purchase if not already done
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
        
        _purchaseState.value = PurchaseState(
            hasPremium = hasPremium,
            hasUltra = hasUltra,
            purchaseToken = purchaseToken,
            purchaseTime = purchaseTime
        )
    }
    
    private fun verifyPurchase(purchase: Purchase): Boolean {
        return try {
            val key = generatePublicKey(base64PublicKey)
            verifySignature(key, purchase.originalJson, purchase.signature)
        } catch (e: Exception) {
            Log.e(TAG, "Purchase verification failed", e)
            false
        }
    }
    
    private fun generatePublicKey(encodedPublicKey: String): PublicKey {
        val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
    }
    
    private fun verifySignature(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureBytes = Base64.decode(signature, Base64.DEFAULT)
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initVerify(publicKey)
        sig.update(signedData.toByteArray())
        return sig.verify(signatureBytes)
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            }
        }
    }
    
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()
                
                billingClient?.launchBillingFlow(activity, flowParams)
            }
        }
    }
    
    fun getUserLevel(): UserLevel {
        return _purchaseState.value.getUserLevel()
    }
    
    fun disconnect() {
        billingClient?.endConnection()
    }
}
