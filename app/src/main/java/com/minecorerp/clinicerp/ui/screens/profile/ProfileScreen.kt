package com.minecorerp.clinicerp.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecorerp.clinicerp.ui.screens.dashboard.DashboardViewModel
import com.minecorerp.clinicerp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(viewModel.currentUserProfile?.name ?: "") }
    var email by remember { mutableStateOf(viewModel.currentUserProfile?.email ?: "") }
    val isSaving = viewModel.isSaving

    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(android.os.Build.VERSION.SDK_INT < 33 ||
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationsEnabled = it }

    LaunchedEffect(viewModel.currentUserProfile) {
        viewModel.currentUserProfile?.let {
            name = it.name
            email = it.email ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الملف الشخصي", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
                }
            }
            
            viewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp)) }
            Spacer(Modifier.height(32.dp))
            com.minecorerp.clinicerp.ui.components.ClinicTextField(
                value = name,
                onValueChange = { name = it },
                label = "الاسم بالكامل",
                placeholder = "ادخل اسمك"
            )
            
            Spacer(Modifier.height(16.dp))
            
            com.minecorerp.clinicerp.ui.components.ClinicTextField(
                value = email,
                onValueChange = { email = it },
                label = "البريد الإلكتروني",
                placeholder = "name@example.com"
            )
            
            Spacer(Modifier.height(48.dp))
            
            Button(
                onClick = {
                    viewModel.updateProfile(name, email) {
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("حفظ التغييرات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(Modifier.height(32.dp))

            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تنبيهات المتابعة", fontWeight = FontWeight.Bold)
                    Text("فحص دوري للأنشطة المستحقة والمتأخرة عند توفر الإنترنت", style = MaterialTheme.typography.bodySmall)
                    if (notificationsEnabled) Text("الإشعارات مسموحة", color = MaterialTheme.colorScheme.primary)
                    else OutlinedButton(onClick = { if (android.os.Build.VERSION.SDK_INT >= 33) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) { Text("السماح بالإشعارات") }
                }
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("عن مساحة العمل", fontWeight = FontWeight.Bold)
                    Text("Clinic ERP • إصدار تجريبي 1.1", style = MaterialTheme.typography.bodyMedium)
                    Text("السيرفر: " + (runCatching { java.net.URI(viewModel.baseUrl).host }.getOrNull() ?: "غير متاح"), style = MaterialTheme.typography.bodySmall)
                    Text("البيانات والصلاحيات يحددها سيرفر العيادة. لا توجد مزامنة دون اتصال أو ملفات علاجية متكاملة في هذه النسخة.", style = MaterialTheme.typography.bodySmall)
                }
            }
            var showPasswordFields by remember { mutableStateOf(false) }
            TextButton(onClick = { showPasswordFields = !showPasswordFields }) {
                Text(if (showPasswordFields) "إلغاء تغيير كلمة المرور" else "تغيير كلمة المرور", color = MaterialTheme.colorScheme.primary)
            }

            if (showPasswordFields) {
                var oldPass by remember { mutableStateOf("") }
                var newPass by remember { mutableStateOf("") }
                var confirmPass by remember { mutableStateOf("") }
                var passError by remember { mutableStateOf<String?>(null) }
                var passSuccess by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        com.minecorerp.clinicerp.ui.components.ClinicTextField(
                            value = oldPass,
                            onValueChange = { oldPass = it },
                            label = "كلمة المرور الحالية",
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                        Spacer(Modifier.height(12.dp))
                        com.minecorerp.clinicerp.ui.components.ClinicTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = "كلمة المرور الجديدة",
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                        Spacer(Modifier.height(12.dp))
                        com.minecorerp.clinicerp.ui.components.ClinicTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            label = "تأكيد كلمة المرور الجديدة",
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )

                        if (passError != null) {
                            Text(passError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                        if (passSuccess) {
                            Text("تم تغيير كلمة المرور بنجاح", color = Teal700, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (oldPass.isBlank() || newPass.isBlank()) {
                                    passError = "من فضلك ادخل كلمة المرور"
                                    return@Button
                                }
                                if (newPass != confirmPass) {
                                    passError = "كلمة المرور الجديدة غير متطابقة"
                                    return@Button
                                }
                                            viewModel.changePassword(oldPass, newPass, {
                                                passSuccess = true
                                    passError = null
                                    oldPass = ""; newPass = ""; confirmPass = ""
                                }, {
                                                passError = it
                                })
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal900),
                            enabled = !isSaving
                        ) {
                            Text("تحديث كلمة المرور", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
