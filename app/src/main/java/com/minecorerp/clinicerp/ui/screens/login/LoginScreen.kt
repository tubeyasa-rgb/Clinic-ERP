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
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ClinicLogo(Modifier.size(72.dp))
                Column {
                    Text("Clinic ERP", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("مساحة أهدأ لإدارة عيادتك", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("أهلًا بعودتك", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("مواعيدك، جهات الاتصال، ومتابعة الحالات في مكان واحد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedCard(shape = RoundedCornerShape(24.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("تسجيل الدخول", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    ClinicTextField(viewModel.url, { viewModel.url = it }, "رابط سيرفر العيادة", "https://clinic.example.com", keyboardType = KeyboardType.Uri)
                    ClinicTextField(viewModel.username, { viewModel.username = it }, "اسم المستخدم أو البريد", keyboardType = KeyboardType.Email)
                    OutlinedTextField(value = viewModel.password, onValueChange = { viewModel.password = it }, label = { Text("كلمة المرور") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور") }
                    })
                    TextButton(onClick = { showServerDetails = !showServerDetails }) { Text(if (showServerDetails) "إخفاء إعدادات قاعدة البيانات" else "تحديد قاعدة البيانات (اختياري)") }
                    if (showServerDetails || viewModel.database.isNotBlank()) ClinicTextField(viewModel.database, { viewModel.database = it }, "اسم قاعدة البيانات", "اطلب الاسم من مسؤول النظام")
                    viewModel.error?.let { message ->
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) { Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
                    }
                    ClinicButton(if (viewModel.isLoading) "جارٍ تسجيل الدخول…" else "الدخول إلى العيادة", { viewModel.onLoginClick(onLoginSuccess) }, enabled = !viewModel.isLoading)
                    if (viewModel.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("استخدم بيانات حساب العيادة. التطبيق يتطلب اتصالًا بالإنترنت.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Text("نسخة تجريبية • اختبرها ببيانات غير حقيقية قبل استخدامها في العمل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
