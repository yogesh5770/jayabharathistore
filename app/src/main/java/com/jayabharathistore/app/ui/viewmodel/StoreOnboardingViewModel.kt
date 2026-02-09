package com.jayabharathistore.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.StoreProfile
import com.jayabharathistore.app.data.model.User
import com.jayabharathistore.app.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.jayabharathistore.app.data.session.UserSessionManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreOnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository,
    private val imageRepository: ImageRepository,
    private val sessionManager: UserSessionManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _onboardingStatus = MutableStateFlow<OnboardingStatus>(OnboardingStatus.Idle)
    val onboardingStatus: StateFlow<OnboardingStatus> = _onboardingStatus.asStateFlow()

    private val _currentUser = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    private val _existingStore = MutableStateFlow<StoreProfile?>(null)
    val existingStore: StateFlow<StoreProfile?> = _existingStore.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _currentUser.value = user
                if (user != null) {
                    checkExistingStore(user.uid)
                }
            }
        }
    }

    private suspend fun checkExistingStore(userId: String) {
        val store = storeRepository.getStoreByOwnerId(userId)
        _existingStore.value = store
        if (store != null) {
            _onboardingStatus.value = OnboardingStatus.StoreCreated(store)
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.signIn(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                val dbUser = userRepository.getUser(user.uid)
                
                if (dbUser != null) {
                    if (dbUser.role != "owner" && dbUser.role != "admin") {
                        // Upgrade customer to owner if they log into Creator App
                        val upgradedUser = dbUser.copy(role = "owner")
                        userRepository.saveUser(upgradedUser)
                        sessionManager.loginUser(user, upgradedUser.name, upgradedUser.phoneNumber)
                    } else {
                        sessionManager.loginUser(user, dbUser.name, dbUser.phoneNumber)
                    }
                } else {
                    // This case shouldn't happen with standard sign up, but as fallback:
                    val newUser = User(id = user.uid, name = user.displayName ?: "", email = user.email ?: email, role = "owner")
                    userRepository.saveUser(newUser)
                    sessionManager.loginUser(user, newUser.name, null)
                }
            } else {
                _uiEvent.emit(result.exceptionOrNull()?.message ?: "Login failed")
            }
            _isLoading.value = false
        }
    }

    fun signUp(email: String, pass: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.signUp(email, pass)
            if (result.isSuccess) {
                val firebaseUser = result.getOrNull()!!
                val userProfile = User(
                    id = firebaseUser.uid,
                    name = name,
                    email = email,
                    role = "owner"
                )
                userRepository.saveUser(userProfile)
                sessionManager.loginUser(firebaseUser, name, null)
            } else {
                _uiEvent.emit(result.exceptionOrNull()?.message ?: "Signup failed")
            }
            _isLoading.value = false
        }
    }

    fun registerStoreDetails(
        storeName: String,
        userAppName: String,
        deliveryAppName: String,
        storeAppName: String,
        gstin: String,
        lat: Double,
        lng: Double,
        radius: Double,
        upiVpa: String,
        upiName: String,
        userAppIcon: Uri?,
        deliveryAppIcon: Uri?,
        storeAppIcon: Uri?,
        gstCert: Uri?
    ) {
        val user = _currentUser.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check for duplicate store before starting upload
                val alreadyHasStore = storeRepository.getStoreByOwnerId(user.uid)
                if (alreadyHasStore != null) {
                    _existingStore.value = alreadyHasStore
                    _onboardingStatus.value = OnboardingStatus.StoreCreated(alreadyHasStore)
                    _uiEvent.emit("You already have a store registered!")
                    return@launch
                }
                // 1. Upload Assets
                val userIconUrl = userAppIcon?.let { imageRepository.uploadProductImage(it) } ?: ""
                val deliveryIconUrl = deliveryAppIcon?.let { imageRepository.uploadProductImage(it) } ?: ""
                val storeIconUrl = storeAppIcon?.let { imageRepository.uploadProductImage(it) } ?: ""
                val gstCertUrl = gstCert?.let { imageRepository.uploadProductImage(it) } ?: ""

                // 2. Create Store Profile
                val storeProfile = StoreProfile(
                    ownerId = user.uid,
                    name = storeName,
                    userAppName = userAppName,
                    deliveryAppName = deliveryAppName,
                    storeAppName = storeAppName,
                    userAppIconUrl = userIconUrl,
                    deliveryAppIconUrl = deliveryIconUrl,
                    storeAppIconUrl = storeIconUrl,
                    gstin = gstin,
                    gstCertificateUrl = gstCertUrl,
                    latitude = lat,
                    longitude = lng,
                    deliveryRadiusKm = radius,
                    upiVpa = upiVpa,
                    upiName = upiName,
                    approvalStatus = "PENDING"
                )
                storeRepository.registerStore(storeProfile)

                // 3. Update User with new storeId
                val freshStore = storeRepository.getStoreByOwnerId(user.uid)
                val dbUser = userRepository.getUser(user.uid)
                if (dbUser != null) {
                    userRepository.saveUser(dbUser.copy(storeId = freshStore?.id ?: ""))
                }

                // Immediately update local state to reflect the new store and show the "Pending" screen
                _existingStore.value = freshStore ?: storeProfile.copy(id = "temp_id")
                _onboardingStatus.value = OnboardingStatus.StoreCreated(_existingStore.value!!)
                _uiEvent.emit("Store details submitted for verification!")

            } catch (e: Exception) {
                _uiEvent.emit("Submission error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        authRepository.signOut()
        sessionManager.logoutUser()
        _onboardingStatus.value = OnboardingStatus.Idle
        _existingStore.value = null
    }
}

sealed class OnboardingStatus {
    object Idle : OnboardingStatus()
    data class StoreCreated(val store: StoreProfile) : OnboardingStatus()
}
