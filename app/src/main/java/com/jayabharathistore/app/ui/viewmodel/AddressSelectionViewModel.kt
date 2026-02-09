package com.jayabharathistore.app.ui.viewmodel

import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AddressSelectionViewModel @Inject constructor() : ViewModel() {

    private val _selectedLocation = MutableStateFlow<Location?>(null)
    val selectedLocation: StateFlow<Location?> = _selectedLocation.asStateFlow()

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _searchResults = MutableStateFlow<List<android.location.Address>>(emptyList())
    val searchResults: StateFlow<List<android.location.Address>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Store location (Jayabharathi Store)
    private val storeLocation = LatLng(9.888680587432056, 78.08195496590051)
    private val deliveryRadiusKm = 3.0 // Delivery radius in KM

    fun updateLocation(location: Location) {
        _selectedLocation.value = location
        // We rely on the UI (Geocoder) to get the address details
        // and update via updateAddress
    }

    fun updateAddress(newAddress: String) {
        _address.value = newAddress
    }

    fun getCurrentLocation(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    updateLocation(location)
                } else {
                    // If last location is null, try to request updates or use default
                    // For now, defaulting to Store Location as fallback
                     val defaultLocation = Location("").apply {
                        latitude = 9.888680587432056
                        longitude = 78.08195496590051
                    }
                    updateLocation(defaultLocation)
                    _errorMessage.value = "Could not fetch current location. Using default."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to get location: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchPlaces(query: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    // Limit to 5 results
                    val addresses = geocoder.getFromLocationName(query, 5)
                    withContext(Dispatchers.Main) {
                        _searchResults.value = addresses ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                // Ignore geocoder errors
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }


    fun isPincodeValid(pincode: String, addressText: String): Boolean {
        // We rely on distance check (checkDeliveryAvailability) primarily.
        // Pincode check is secondary/optional now.
        return true
    }

    fun checkDeliveryAvailability(latitude: Double, longitude: Double): Boolean {
        val userLocation = Location("").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        return isLocationInDeliveryArea(userLocation)
    }

    fun isLocationInDeliveryArea(location: Location): Boolean {
        val userLatLng = LatLng(location.latitude, location.longitude)
        val results = FloatArray(1)
        
        // Calculate distance between user location and store
        Location.distanceBetween(
            storeLocation.latitude, storeLocation.longitude,
            userLatLng.latitude, userLatLng.longitude,
            results
        )
        
        val distanceInMeters = results[0]
        val distanceInKm = distanceInMeters / 1000
        
        return distanceInKm <= deliveryRadiusKm
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
