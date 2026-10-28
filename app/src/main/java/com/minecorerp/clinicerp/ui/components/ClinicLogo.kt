package com.minecorerp.clinicerp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.minecorerp.clinicerp.R

@Composable
fun ClinicLogo(modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color(0xFFF8F9F3)) {
        Image(painterResource(R.drawable.clinic_logo), contentDescription = "Clinic ERP — طبيب وتروس مترابطة", modifier = Modifier.padding(4.dp))
    }
}
