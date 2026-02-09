package com.jayabharathistore.app.data.session

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jayabharathistore.app.data.model.Address
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAddressManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("user_address_session", Context.MODE_PRIVATE)
    
    // Legacy prefs to migrate from
    private val legacyPrefs: SharedPreferences = 
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _userAddress = MutableStateFlow<Address?>(null)
    val userAddress: StateFlow<Address?> = _userAddress.asStateFlow()

    private val _userAddresses = MutableStateFlow<List<Address>>(emptyList())
    val userAddresses: StateFlow<List<Address>> = _userAddresses.asStateFlow()

    init {
        loadAddressData()
    }

    private fun loadAddressData() {
        scope.launch {
            // Try to load from new prefs first
            var currentAddrJson = prefs.getString("current_address", null)
            var savedAddrJson = prefs.getString("saved_addresses", null)

            // If empty, check legacy
            if (currentAddrJson == null && savedAddrJson == null) {
                migrateFromLegacy()
                // Reload after migration
                currentAddrJson = prefs.getString("current_address", null)
                savedAddrJson = prefs.getString("saved_addresses", null)
            }

            val currentAddr = if (currentAddrJson != null) {
                try {
                    gson.fromJson(currentAddrJson, Address::class.java)
                } catch (e: Exception) { null }
            } else null

            val savedList = try {
                if (!savedAddrJson.isNullOrBlank()) {
                    val type = object : TypeToken<List<Address>>() {}.type
                    gson.fromJson<List<Address>>(savedAddrJson, type) ?: emptyList()
                } else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            _userAddress.value = currentAddr
            _userAddresses.value = savedList
            
            // Sync current address to list if needed
            if (currentAddr != null && savedList.isEmpty()) {
                 _userAddresses.value = listOf(currentAddr)
                 saveAddresses(listOf(currentAddr))
            }
        }
    }

    private fun migrateFromLegacy() {
        // Read legacy fields
        val street = legacyPrefs.getString("address_street", null)
        if (street != null) {
            val legacyAddress = Address(
                street = street,
                city = legacyPrefs.getString("address_city", "") ?: "",
                state = legacyPrefs.getString("address_state", "") ?: "",
                pincode = legacyPrefs.getString("address_pincode", "") ?: "",
                landmark = legacyPrefs.getString("address_landmark", "") ?: "",
                latitude = legacyPrefs.getFloat("address_latitude", 0f).toDouble(),
                longitude = legacyPrefs.getFloat("address_longitude", 0f).toDouble()
            )
            
            // Read legacy list
            val savedJson = legacyPrefs.getString("user_addresses_json", null)
            val savedList = try {
                if (!savedJson.isNullOrBlank()) {
                    val type = object : TypeToken<List<Address>>() {}.type
                    gson.fromJson<List<Address>>(savedJson, type) ?: emptyList()
                } else emptyList()
            } catch (_: Exception) { emptyList() }
            
            // Save to new prefs
            prefs.edit().apply {
                putString("current_address", gson.toJson(legacyAddress))
                if (savedList.isNotEmpty()) {
                    putString("saved_addresses", gson.toJson(savedList))
                }
                apply()
            }
        }
    }

    fun updateUserAddress(address: Address) {
        _userAddress.value = address
        scope.launch {
            prefs.edit().apply {
                putString("current_address", gson.toJson(address))
                apply()
            }
        }
    }

    fun addUserAddress(address: Address) {
        val currentList = _userAddresses.value
        // Dedupe logic
        val key = { a: Address -> "${a.latitude}|${a.longitude}|${a.street}" }
        val newKey = key(address)
        
        val newList = (currentList.filter { key(it) != newKey } + address).takeLast(5)
        
        _userAddresses.value = newList
        saveAddresses(newList)
    }
    
    private fun saveAddresses(list: List<Address>) {
        scope.launch {
            prefs.edit().apply {
                putString("saved_addresses", gson.toJson(list))
                apply()
            }
        }
    }

    fun clearAddresses() {
        _userAddress.value = null
        _userAddresses.value = emptyList()
        scope.launch {
             prefs.edit().clear().apply()
        }
    }
    
    fun hasUserAddress(): Boolean {
        return _userAddress.value?.street?.isNotBlank() == true
    }
    
    fun getAddress(): Address? = _userAddress.value
}
