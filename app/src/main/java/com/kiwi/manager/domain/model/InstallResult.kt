package com.kiwi.manager.domain.model

sealed class InstallResult {
    data object Success : InstallResult()
    data class Error(val message: String, val isSignatureConflict: Boolean = false) : InstallResult()
    data object Cancelled : InstallResult()
}
