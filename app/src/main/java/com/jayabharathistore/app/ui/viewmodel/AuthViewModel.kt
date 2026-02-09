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
import javax.inject.Inject
import com.google.firebase.messaging.FirebaseMessaging

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: UserSessionManager,
    private val userRepository: com.jayabharathistore.app.data.repository.UserRepository
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
                        // Check if user exists in Firestore, if not create them
                        val firestoreUser = userRepository.getUser(user.uid)
                        if (firestoreUser == null) {
                            // User doesn't exist in Firestore, save them
                            val newUser = com.jayabharathistore.app.data.model.User(
                                id = user.uid,
                                name = user.displayName ?: "",
                                email = user.email ?: email,
                                phoneNumber = user.phoneNumber ?: ""
                            )
                            userRepository.saveUser(newUser)
                        }
                        
                        sessionManager.loginUser(user, firestoreUser?.name, firestoreUser?.phoneNumber)
                        // subscribe to user-specific topic for notifications
                        try {
                            FirebaseMessaging.getInstance().subscribeToTopic("user-${user.uid}")
                        } catch (_: Exception) {
                        }
                    }
                } else {
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
                // First try standard Firebase login
                val result = authRepository.signIn(email, password)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null) {
                        // Check if this user has delivery role
                        val firestoreUser = userRepository.getUser(user.uid)
                        
                        if (firestoreUser != null) {
                            if (firestoreUser.role == "delivery") {
                                // Already a delivery partner
                                sessionManager.loginUser(user, firestoreUser.name, firestoreUser.phoneNumber)
                            } else {
                                // Existing customer logging into Delivery App -> Upgrade them
                                // We keep their existing profile info but add delivery capability
                                val pendingUser = firestoreUser.copy(
                                    role = "delivery",
                                    approvalStatus = "PENDING"
                                )
                                userRepository.saveUser(pendingUser)
                                sessionManager.loginUser(user, pendingUser.name, pendingUser.phoneNumber)
                            }
                        } else {
                            // User authenticated but no firestore record? 
                            // This shouldn't happen for a legitimate new signup, but as a fallback:
                            _errorMessage.value = "Registration mandatory. Please use the Sign Up tab to register as a partner."
                        }
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Login failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Delivery Login failed"
            } finally {
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
                val result = authRepository.signUp(email, password)
                if (result.isSuccess) {
                    val firebaseUser = result.getOrNull()
                    if (firebaseUser != null) {
                        // New user: Save details to Firestore with delivery role and pending status
                        val newUser = com.jayabharathistore.app.data.model.User(
                            id = firebaseUser.uid,
                            name = name,
                            email = email,
                            phoneNumber = phone,
                            role = "delivery",
                            approvalStatus = "PENDING"
                        )
                        userRepository.saveUser(newUser)
                        
                        sessionManager.loginUser(firebaseUser, name, phone)
                    }
                } else {
                    // Check if error is "email already in use"
                    val exception = result.exceptionOrNull()
                    val msg = exception?.message ?: ""
                    
                    if (msg.contains("email address is already in use", ignoreCase = true) || 
                        msg.contains("exists", ignoreCase = true)) {
                        
                        // Attempt to sign in and upgrade the user
                        val signInResult = authRepository.signIn(email, password)
                        if (signInResult.isSuccess) {
                            val user = signInResult.getOrNull()
                            if (user != null) {
                                // Fetch existing user
                                val existingUser = userRepository.getUser(user.uid)
                                
                                val updatedUser = if (existingUser != null) {
                                    existingUser.copy(
                                        role = "delivery",
                                        approvalStatus = "PENDING",
                                        phoneNumber = if (phone.isNotBlank()) phone else existingUser.phoneNumber,
                                        name = if (name.isNotBlank()) name else existingUser.name
                                    )
                                } else {
                                    com.jayabharathistore.app.data.model.User(
                                        id = user.uid,
                                        name = name,
                                        email = email,
                                        phoneNumber = phone,
                                        role = "delivery",
                                        approvalStatus = "PENDING"
                                    )
                                }
                                
                                userRepository.saveUser(updatedUser)
                                sessionManager.loginUser(user, updatedUser.name, updatedUser.phoneNumber)
                            }
                        } else {
                            // Password didn't match or other login error
                            _errorMessage.value = "You already have a customer account. Please use the LOG IN tab with your existing password to join as a Delivery Partner."
                            _isLoginMode.value = true // Switch UI to login mode for them
                        }
                    } else {
                        _errorMessage.value = msg.ifBlank { "Sign up failed" }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Sign up failed"
            } finally {
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
