package com.minecorerp.clinicerp.ui.screens.login

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.minecorerp.clinicerp.ui.components.ClinicButton
import com.minecorerp.clinicerp.ui.components.ClinicTextField

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulse Line Drawing
                PulseLine(modifier = Modifier.fillMaxWidth().height(34.dp).padding(bottom = 18.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Clinic ")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                            append("Erp")
                        }
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "CRM مخصص لإدارة عملاء العيادة، متصل مباشرة بقاعدة بيانات أودو",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 26.dp)
                )

                ClinicTextField(
                    value = viewModel.url,
                    onValueChange = { viewModel.url = it },
                    label = "رابط السيرفر (URL)",
                    placeholder = "https://yourclinic.odoo.com"
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                ClinicTextField(
                    value = viewModel.database,
                    onValueChange = { viewModel.database = it },
                    label = "قاعدة البيانات (اختياري لو السيرفر فيه قاعدة واحدة)"
                )
                Spacer(Modifier.height(14.dp))
                ClinicTextField(
                    value = viewModel.username,
                    onValueChange = { viewModel.username = it },
                    label = "اسم المستخدم",
                    placeholder = "admin أو البريد الإلكتروني"
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                ClinicTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = "كلمة المرور",
                    placeholder = "••••••••",
                    visualTransformation = PasswordVisualTransformation()
                )

                if (viewModel.error != null) {
                    Text(
                        text = viewModel.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                ClinicButton(
                    text = if (viewModel.isLoading) "جاري الاتصال..." else "تسجيل الدخول",
                    onClick = { viewModel.onLoginClick(onLoginSuccess) },
                    enabled = !viewModel.isLoading
                )
            }
        }
    }
}

@Composable
fun PulseLine(modifier: Modifier = Modifier) {
    val pulseColor = MaterialTheme.colorScheme.secondary
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        val path = Path().apply {
            moveTo(0f, centerY)
            lineTo(width * 0.35f, centerY)
            lineTo(width * 0.40f, centerY - 15.dp.toPx())
            lineTo(width * 0.45f, centerY + 15.dp.toPx())
            lineTo(width * 0.50f, centerY)
            lineTo(width, centerY)
        }
        
        drawPath(
            path = path,
            color = pulseColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
