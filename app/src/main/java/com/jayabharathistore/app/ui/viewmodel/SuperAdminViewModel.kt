package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.StoreProfile
import com.jayabharathistore.app.data.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuperAdminViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val githubRepository: com.jayabharathistore.app.data.repository.GitHubRepository
) : ViewModel() {

    private val _stores = MutableStateFlow<List<StoreProfile>>(emptyList())
    val stores: StateFlow<List<StoreProfile>> = _stores.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeStores()
    }

    private fun observeStores() {
        viewModelScope.launch {
            _isLoading.value = true
            storeRepository.observeAllStores().collect {
                _stores.value = it.sortedByDescending { store -> store.createdAt }
                _isLoading.value = false
            }
        }
    }

    fun markForGeneration(storeId: String) {
        viewModelScope.launch {
            // 1. Set status to BUILDING
            storeRepository.updateStoreStatus(storeId, "BUILDING")
            
            // 2. Trigger GitHub Build
            val store = storeRepository.getStoreById(storeId)
            if (store != null) {
                val success = githubRepository.triggerBuild(store)
                if (!success) {
                    storeRepository.updateStoreStatus(storeId, "TRIGGER_FAILED")
                }
            } else {
                 storeRepository.updateStoreStatus(storeId, "TRIGGER_FAILED")
            }
        }
    }

    fun approveStore(storeId: String, userUrl: String = "", deliveryUrl: String = "", storeUrl: String = "") {
        viewModelScope.launch {
            if (userUrl.isNotBlank()) {
                storeRepository.approveStoreWithLinks(storeId, userUrl, deliveryUrl, storeUrl)
            } else {
                storeRepository.updateStoreStatus(storeId, "APPROVED")
            }
        }
    }

    fun rejectStore(storeId: String) {
        viewModelScope.launch {
            storeRepository.updateStoreStatus(storeId, "REJECTED")
        }
    }
}
