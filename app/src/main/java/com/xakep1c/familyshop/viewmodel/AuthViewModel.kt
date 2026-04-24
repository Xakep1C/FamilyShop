package com.xakep1c.familyshop.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.xakep1c.familyshop.BuildConfig
import com.xakep1c.familyshop.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {

    val sessionStatus: StateFlow<SessionStatus> = supabase.auth.sessionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionStatus.Initializing)

    val isLoggedIn: StateFlow<Boolean> = sessionStatus
        .map { it is SessionStatus.Authenticated }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        
        Log.d("Auth", "Attempting login with: $trimmedEmail")
        if (trimmedEmail.isBlank()) {
            _error.value = "Укажите Email"
            return
        }
        if (trimmedPassword.isBlank()) {
            _error.value = "Введите пароль"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    supabase.auth.signInWith(Email) {
                        this.email = trimmedEmail
                        this.password = trimmedPassword
                    }
                }
                Log.d("Auth", "Login successful")
            } catch (e: Exception) {
                Log.e("Auth", "Login error", e)
                _error.value = mapError(e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isBlank()) {
            _error.value = "Логин (Email) не указан"
            return
        }
        if (trimmedPassword.isBlank()) {
            _error.value = "Придумайте пароль"
            return
        }
        if (trimmedPassword.length < 6) {
            _error.value = "Пароль должен быть не менее 6 символов"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                withContext(Dispatchers.IO) {
                    supabase.auth.signUpWith(Email) {
                        this.email = trimmedEmail
                        this.password = trimmedPassword
                    }
                }
                _error.value = "Успешно! Проверьте почту для подтверждения"
            } catch (e: Exception) {
                Log.e("Auth", "Signup error", e)
                _error.value = mapError(e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun mapError(message: String): String {
        return when {
            message.contains("anonymous_provider_disabled", ignoreCase = true) -> 
                "Регистрация по Email временно недоступна или введены неверные данные"
            message.contains("User already registered", ignoreCase = true) -> 
                "Пользователь с такой почтой уже существует"
            message.contains("Invalid login credentials", ignoreCase = true) -> 
                "Неверная почта или пароль"
            message.contains("Unable to resolve host", ignoreCase = true) -> 
                "Нет соединения с интернетом"
            message.contains("Email not confirmed", ignoreCase = true) -> 
                "Почта не подтверждена. Проверьте ваш почтовый ящик"
            message.contains("email_address_invalid", ignoreCase = true) ->
                "Некорректный формат Email"
            else -> "Ошибка: что-то пошло не так. Проверьте данные ($message)"
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d("Auth", "Google sign in started with Client ID: ${BuildConfig.GOOGLE_CLIENT_ID}")
            try {
                val credentialManager = CredentialManager.create(context)
                
                val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()

                val request: GetCredentialRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                Log.d("Auth", "Credential received: ${credential.type}")
                
                if (credential is GoogleIdTokenCredential) {
                    withContext(Dispatchers.IO) {
                        supabase.auth.signInWith(IDToken) {
                            idToken = credential.idToken
                            provider = Google
                        }
                    }
                    Log.d("Auth", "Google sign in success with Supabase")
                } else {
                    Log.e("Auth", "Received unexpected credential type: ${credential.type}")
                    _error.value = "Неподдерживаемый тип входа"
                }
            } catch (e: GetCredentialException) {
                Log.e("Auth", "GetCredentialException: code=${e.type}, message=${e.message}", e)
                _error.value = "Ошибка Google: ${e.message}"
            } catch (e: Exception) {
                Log.e("Auth", "Unexpected Google login error", e)
                _error.value = "Ошибка: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabase.auth.signOut()
                }
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Sign out error", e)
            }
        }
    }
}
