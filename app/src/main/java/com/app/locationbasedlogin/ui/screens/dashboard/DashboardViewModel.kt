package com.app.locationbasedlogin.ui.screens.dashboard

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.locationbasedlogin.data.repository.AuthRepository
import com.app.locationbasedlogin.service.LocationMonitoringService
import com.app.locationbasedlogin.utils.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application.applicationContext)
    private val context: Context = application.applicationContext

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _navigateToLogin = MutableSharedFlow<Unit>()
    val navigateToLogin: SharedFlow<Unit> = _navigateToLogin.asSharedFlow()

    init {
        // Observe login status from AuthRepository
        // This will be crucial for auto-logout initiated by the background service
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                if (!loggedIn) {
                    _navigateToLogin.emit(Unit)
                }
            }
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.logout()
            stopLocationMonitoringService()
            _navigateToLogin.emit(Unit)
            _isLoading.value = false
        }
    }

    private fun stopLocationMonitoringService() {
        val serviceIntent = Intent(context, LocationMonitoringService::class.java).apply {
            putExtra(Constants.PREF_IS_LOGGED_IN, false) // Inform service that user is logged out
        }
        context.stopService(serviceIntent)
    }
}