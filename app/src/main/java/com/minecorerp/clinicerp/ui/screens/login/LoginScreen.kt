package com.minecorerp.clinicerp.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minecorerp.clinicerp.ui.components.*

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    var showServerDetails by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ClinicLogo(Modifier.size(44.dp))
                Column {
                    Text("Clinic ERP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Text("تسجيل الدخول", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("ادخل بحساب العيادة للمتابعة.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ClinicTextField(viewModel.url, { viewModel.url = it }, "رابط سيرفر العيادة", "https://clinic.example.com", keyboardType = KeyboardType.Uri)
                    ClinicTextField(viewModel.username, { viewModel.username = it }, "اسم المستخدم أو البريد", keyboardType = KeyboardType.Email)
                    OutlinedTextField(value = viewModel.password, onValueChange = { viewModel.password = it }, label = { Text("كلمة المرور") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور") }
                    })
                    TextButton(onClick = { showServerDetails = !showServerDetails }) { Text(if (showServerDetails) "إخفاء إعدادات قاعدة البيانات" else "تحديد قاعدة البيانات (اختياري)") }
                    if (showServerDetails || viewModel.database.isNotBlank()) ClinicTextField(viewModel.database, { viewModel.database = it }, "اسم قاعدة البيانات", "اطلب الاسم من مسؤول النظام")
                    viewModel.error?.let { message ->
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) { Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
                    }
                    ClinicButton(if (viewModel.isLoading) "جارٍ تسجيل الدخول…" else "دخول", { viewModel.onLoginClick(onLoginSuccess) }, enabled = !viewModel.isLoading)
                    if (viewModel.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("يتطلب اتصالًا بالإنترنت.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("نسخة تجريبية — استخدم بيانات اختبار فقط.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
