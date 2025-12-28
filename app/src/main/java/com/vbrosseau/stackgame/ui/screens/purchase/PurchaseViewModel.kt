package com.vbrosseau.stackgame.ui.screens.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbrosseau.stackgame.data.BillingManager
import com.vbrosseau.stackgame.models.PurchaseState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PurchaseUiState(
    val purchaseState: PurchaseState = PurchaseState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PurchaseViewModel(
    private val billingManager: BillingManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            billingManager.purchaseState.collect { purchaseState ->
                _uiState.value = _uiState.value.copy(
                    purchaseState = purchaseState,
                    isLoading = false
                )
            }
        }
    }
    
    fun refreshPurchases() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        billingManager.queryPurchases()
    }
}
