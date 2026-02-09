package com.jayabharathistore.app.data.session

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManager @Inject constructor(
    private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userPhone = MutableStateFlow<String?>(null)
    val userPhone: StateFlow<String?> = _userPhone.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        loadSessionData()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        sharedPreferences.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    fun loginUser(user: FirebaseUser, name: String? = null, phone: String? = null) {
        val finalName = name ?: user.displayName ?: _userName.value
        val finalPhone = phone ?: user.phoneNumber ?: _userPhone.value
        
        _currentUser.value = user
        _userEmail.value = user.email
        _userName.value = finalName
        _userPhone.value = finalPhone
        _isLoggedIn.value = true

        // Save to SharedPreferences
        sharedPreferences.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_id", user.uid)
            putString("user_email", user.email)
            putString("user_name", finalName)
            putString("user_phone", finalPhone)
            apply()
        }
    }

    fun logoutUser() {
        _currentUser.value = null
        _userEmail.value = null
        _userName.value = null
        _userPhone.value = null
        _isLoggedIn.value = false

        // Clear SharedPreferences
        sharedPreferences.edit().apply {
            clear()
            apply()
        }
    }

    fun updateUserName(name: String) {
        _userName.value = name

        sharedPreferences.edit().apply {
            putString("user_name", name)
            apply()
        }
    }

    fun getUserId(): String? = _currentUser.value?.uid
    fun getUserName(): String? = _userName.value?.takeIf { it.isNotBlank() }
    fun getUserPhone(): String? = (_userPhone.value ?: _currentUser.value?.phoneNumber)?.takeIf { it.isNotBlank() }
    fun getUserEmail(): String? = _userEmail.value?.takeIf { it.isNotBlank() }

    private fun loadSessionData() {
        // Restore Firebase User if possible
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        _currentUser.value = firebaseUser

        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false) && firebaseUser != null
        val userEmail = sharedPreferences.getString("user_email", null)
        val userName = sharedPreferences.getString("user_name", null)
        val userPhone = sharedPreferences.getString("user_phone", null)
        val isDarkMode = sharedPreferences.getBoolean("is_dark_mode", false)

        _isLoggedIn.value = isLoggedIn
        _userEmail.value = userEmail ?: firebaseUser?.email
        _userName.value = userName ?: firebaseUser?.displayName
        _userPhone.value = userPhone ?: firebaseUser?.phoneNumber
        _isDarkMode.value = isDarkMode
        
        // If SharedPreferences says logged in but Firebase says no, sync them
        if (sharedPreferences.getBoolean("is_logged_in", false) && firebaseUser == null) {
            logoutUser()
        }
    }
}
