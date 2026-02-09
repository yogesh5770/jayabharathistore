package com.jayabharathistore.app.ui.viewmodel

sealed class CallState {
    object Idle : CallState()
    object Connecting : CallState()
    data class Ready(val numberToDial: String, val isSimulation: Boolean, val isBridgeCall: Boolean = false) : CallState()
    data class Error(val message: String) : CallState()
}
