package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.jayabharathistore.app.data.repository.AuthRepository
import com.jayabharathistore.app.data.session.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import com.jayabharathistore.app.data.util.StoreConfig
import javax.inject.Inject
import com.google.firebase.messaging.FirebaseMessaging

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: UserSessionManager,
    private val userRepository: com.jayabharathistore.app.data.repository.UserRepository,
    private val storeConfig: StoreConfig
) : ViewModel() {

    // Combine Firebase auth state with session state
    val currentUser = combine(
        authRepository.currentUser,
        sessionManager.currentUser
    ) { firebaseUser, sessionUser ->
        firebaseUser ?: sessionUser
    }

    val isLoggedIn = sessionManager.isLoggedIn
    val userName = sessionManager.userName
    val userEmail = sessionManager.userEmail

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoginMode = MutableStateFlow(true)
    val isLoginMode: StateFlow<Boolean> = _isLoginMode.asStateFlow()

    init {
        // Check if user is already logged in
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            if (sessionManager.isLoggedIn.value) {
                // User is already logged in, no need to do anything
                return@launch
            }
        }
    }

    fun toggleAuthMode() {
        _isLoginMode.value = !_isLoginMode.value
        clearError()
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.signIn(email, password)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null) {
                        val targetStoreId = storeConfig.getTargetStoreId()
                        val firestoreUser = userRepository.getUser(user.uid)
                        
                        // 1. Ensure global user profile exists
                        if (firestoreUser == null) {
                            val newUser = com.jayabharathistore.app.data.model.User(
                                id = user.uid,
                                name = user.displayName ?: "",
                                email = user.email ?: email,
                                phoneNumber = user.phoneNumber ?: ""
                            )
                            userRepository.saveUser(newUser)
                        }

                        // 2. Resolve Role for this Store
                        if (targetStoreId != null) {
                            // In a white-labeled app, users are customers by default if no other membership exists
                            val existingRole = userRepository.getStoreMemberRole(targetStoreId, user.uid)
                            if (existingRole == null) {
                                // First time in this specific store app -> Make them a customer of this store
                                userRepository.saveStoreMember(targetStoreId, user.uid, "user")
                            }
                        }
                        
                        sessionManager.loginUser(user, firestoreUser?.name, firestoreUser?.phoneNumber)
                        try {
                            FirebaseMessaging.getInstance().subscribeToTopic("user-${user.uid}")
                            if (targetStoreId != null) {
                                FirebaseMessaging.getInstance().subscribeToTopic("store-$targetStoreId")
                            }
                        } catch (_: Exception) { }
                    }
                }
 else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Login failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInDelivery(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val targetStoreId = storeConfig.getTargetStoreId()
                if (targetStoreId == null) {
                    _errorMessage.value = "This app is not configured for a specific store."
                    return@launch
                }

                val result = authRepository.signIn(email, password)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null) {
                        val firestoreUser = userRepository.getUser(user.uid)
                        val memberRole = userRepository.getStoreMemberRole(targetStoreId, user.uid)
                        val memberStatus = userRepository.getStoreMemberStatus(targetStoreId, user.uid)
                        
                        if (memberRole == "delivery") {
                            sessionManager.loginUser(user, firestoreUser?.name, firestoreUser?.phoneNumber)
                        } else {
                            // User exists but is not a delivery partner for THIS store -> Invite to join
                            // We don't upgrade automatically on sign-in, the user should use sign-up to register as staff
                             _errorMessage.value = "You are not a delivery partner for this store. Please register using the Sign Up tab."
                        }
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Login failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Delivery Login failed"
            }
 finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, password: String, name: String? = null, phone: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.signUp(email, password)
                if (result.isSuccess) {
                    val firebaseUser = result.getOrNull()
                    if (firebaseUser != null) {
                        // Save user details to Firestore
                        val newUser = com.jayabharathistore.app.data.model.User(
                            id = firebaseUser.uid,
                            name = name ?: "",
                            email = email,
                            phoneNumber = phone ?: ""
                        )
                        userRepository.saveUser(newUser)
                        
                        sessionManager.loginUser(firebaseUser, name, phone)
                        try {
                            FirebaseMessaging.getInstance().subscribeToTopic("user-${firebaseUser.uid}")
                        } catch (_: Exception) {
                        }
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Sign up failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Sign up failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUpDelivery(email: String, password: String, name: String, phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val targetStoreId = storeConfig.getTargetStoreId()
                if (targetStoreId == null) {
                    _errorMessage.value = "Configuration error: Target Store not found."
                    return@launch
                }

                val result = authRepository.signUp(email, password)
                if (result.isSuccess) {
                    val firebaseUser = result.getOrNull()
                    if (firebaseUser != null) {
                        // 1. Global Profile
                        val newUser = com.jayabharathistore.app.data.model.User(
                            id = firebaseUser.uid,
                            name = name,
                            email = email,
                            phoneNumber = phone,
                            storeId = targetStoreId // Track where they first registered
                        )
                        userRepository.saveUser(newUser)
                        
                        // 2. Store Membership (Delivery)
                        userRepository.saveStoreMember(targetStoreId, firebaseUser.uid, "delivery", "PENDING")
                        
                        sessionManager.loginUser(firebaseUser, name, phone)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    val msg = exception?.message ?: ""
                    
                    if (msg.contains("email address is already in use", ignoreCase = true)) {
                        // User exists as customer in some store -> Add delivery role for THIS store
                        val signInResult = authRepository.signIn(email, password)
                        if (signInResult.isSuccess) {
                            val user = signInResult.getOrNull() ?: return@launch
                            val existingUser = userRepository.getUser(user.uid)
                            
                            // Keep global profile but add membership
                            userRepository.saveStoreMember(targetStoreId, user.uid, "delivery", "PENDING")
                            sessionManager.loginUser(user, existingUser?.name ?: name, existingUser?.phoneNumber ?: phone)
                        } else {
                            _errorMessage.value = "Email already exists. Use LOG IN with your password to join this store as a partner."
                        }
                    } else {
                        _errorMessage.value = msg
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Sign up failed"
            }
 finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        authRepository.signOut()
        sessionManager.logoutUser()
    }

    fun updateUserName(name: String) {
        sessionManager.updateUserName(name)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = authRepository.resetPassword(email)
                if (result.isSuccess) {
                    _errorMessage.value = "Password reset email sent to $email"
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send reset email"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
