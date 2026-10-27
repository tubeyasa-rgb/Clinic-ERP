package com.minecorerp.clinicerp.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minecorerp.clinicerp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var url by mutableStateOf("")
    var database by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun onLoginClick(onSuccess: () -> Unit) {
        var cleanUrl = url.trim().removeSuffix("/")
        if (cleanUrl.isBlank() || username.isBlank() || password.isBlank()) {
            error = "من فضلك املأ كل الحقول"
            return
        }

        if (!cleanUrl.startsWith("http")) {
            cleanUrl = "https://$cleanUrl"
        }

        val uri = runCatching { java.net.URI(cleanUrl) }.getOrNull()
        if (uri?.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null || uri.query != null || uri.fragment != null) {
            error = "استخدم رابط سيرفر HTTPS صحيح بدون بيانات دخول أو إضافات"
            return
        }
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                // First, try to discover DB
                val dbs = if (database.isNotBlank()) listOf(database.trim()) else authRepository.listDatabases(cleanUrl)
                if (dbs.isEmpty()) {
                    error = "اكتب اسم قاعدة البيانات؛ قد يكون السيرفر يمنع عرض القواعد"
                    return@launch
                }
                if (dbs.size != 1) {
                    error = "اكتب اسم قاعدة البيانات المطلوبة"
                    return@launch
                }
                val db = dbs.single()
                
                // Then authenticate
                val result = authRepository.authenticate(cleanUrl, db, username, password)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    error = result.exceptionOrNull()?.message ?: "بيانات الدخول غير صحيحة"
                }
            } catch (e: Exception) {
                error = "فشل الاتصال: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
