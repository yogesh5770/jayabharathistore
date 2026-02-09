package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.ShopSettings
import com.jayabharathistore.app.data.repository.ShopRepository
import com.jayabharathistore.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.jayabharathistore.app.data.util.StoreConfig

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val userRepository: UserRepository,
    private val storeConfig: StoreConfig
) : ViewModel() {

    private val _shopSettings = MutableStateFlow(ShopSettings())
    val shopSettings: StateFlow<ShopSettings> = _shopSettings.asStateFlow()

    private val _onlinePartnersCount = MutableStateFlow(0)
    val onlinePartnersCount = _onlinePartnersCount.asStateFlow()

    private val _availablePartnersCount = MutableStateFlow(0)
    val availablePartnersCount = _availablePartnersCount.asStateFlow()

    val isShopOpen = _shopSettings.map { it.isOpen }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    // Automatic busy status: ONLY true if online partners > 0 AND all of them are busy
    val isDeliveryBusy = combine(_shopSettings, _onlinePartnersCount, _availablePartnersCount) { settings, online, available ->
        val isAutoBusy = online > 0 && available == 0
        android.util.Log.d("ShopViewModel", "Fleet Logic: Online=$online, Available=$available -> IsBusy=$isAutoBusy")
        isAutoBusy // Priority to dynamic fleet status
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Are there ANY online partners?
    val isDeliveryAvailable = _onlinePartnersCount.map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        observeShop()
        observeFleet()
    }

    private fun observeShop() {
        viewModelScope.launch {
            shopRepository.observeShopStatus().collect { settings ->
                _shopSettings.value = settings
            }
        }
    }

    private fun observeFleet() {
        viewModelScope.launch {
            val storeId = storeConfig.getTargetStoreId()
            // HIGH-SPEED: Only observe delivery partners, not all users
            userRepository.observeDeliveryPartners(storeId).collect { partners ->
                // Filtering only for approval status now (role is already filtered by DB query)
                val approvedPartners = partners.filter { 
                    it.approvalStatus.trim().uppercase() == "APPROVED"
                }
                
                val online = approvedPartners.count { it.isOnline }
                val available = approvedPartners.count { it.isOnline && !it.isBusy }
                
                _onlinePartnersCount.value = online
                _availablePartnersCount.value = available
                
                android.util.Log.d("ShopViewModel", "Fast Fleet Update: $online Online, $available Available out of ${approvedPartners.size} approved.")
            }
        }
    }
}


