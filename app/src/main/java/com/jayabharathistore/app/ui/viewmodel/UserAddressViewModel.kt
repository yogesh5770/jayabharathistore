package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.Address
import com.jayabharathistore.app.data.session.UserAddressManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserAddressViewModel @Inject constructor(
    private val addressManager: UserAddressManager
) : ViewModel() {

    val userAddress: StateFlow<Address?> = addressManager.userAddress
    val userAddresses: StateFlow<List<Address>> = addressManager.userAddresses

    fun updateUserAddress(address: Address) {
        addressManager.updateUserAddress(address)
    }

    fun addUserAddress(address: Address) {
        addressManager.addUserAddress(address)
    }

    fun hasAddress(): Boolean {
        return addressManager.hasUserAddress()
    }
}
