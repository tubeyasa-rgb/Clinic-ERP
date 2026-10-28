package com.minecorerp.clinicerp.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minecorerp.clinicerp.data.remote.models.*
import com.minecorerp.clinicerp.ui.components.ClinicTextField
import com.minecorerp.clinicerp.ui.screens.dashboard.DashboardViewModel
import com.minecorerp.clinicerp.ui.screens.dashboard.OdooDropdown
import java.time.*
import java.time.format.DateTimeFormatter

private val rpcDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: DashboardViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val day = LocalDate.parse(selectedDate)
    val events = viewModel.calendarEvents.sortedBy { it.start }.filter { event ->
        showAll || runCatching { localEventStart(event).toLocalDate() == day }.getOrDefault(false)
    }
    if (showDatePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { selectedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                showDatePicker = false; showAll = false
            }) { Text("اختيار") }
        }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") } }) { DatePicker(picker) }
    }
    if (showDialog) NewSessionDialog(viewModel.availableContacts, selectedDate, viewModel.isSaving,
        onDismiss = { showDialog = false },
        onSave = { name, start, stop, description, patientId ->
            viewModel.createSession(name, start, stop, description, patientId) { showDialog = false }
        })
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("التوقيت المحلي لجهازك", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { selectedDate = day.minusDays(1).toString(); showAll = false }) { Text("السابق") }
                        OutlinedButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, null); Spacer(Modifier.width(6.dp)); Text(selectedDate) }
                        TextButton(onClick = { selectedDate = day.plusDays(1).toString(); showAll = false }) { Text("التالي") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !showAll && day == LocalDate.now(), onClick = { selectedDate = LocalDate.now().toString(); showAll = false }, label = { Text("اليوم") })
                        FilterChip(selected = showAll, onClick = { showAll = !showAll }, label = { Text("كل المواعيد") })
                        AssistChip(onClick = { viewModel.loadData() }, label = { Text("${events.size} موعد • تحديث") })
                    }
                }
            }
            if (viewModel.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (events.isEmpty() && !viewModel.isLoading) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.DateRange, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (viewModel.error != null) "تعذر تحميل بعض البيانات؛ راجع رسالة الخطأ" else "لا توجد مواعيد في العرض الحالي")
                    OutlinedButton(onClick = { showDialog = true }) { Text("حجز موعد جديد") }
                }
            }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(events, key = { it.id }) { event -> CalendarEventCard(event) }
            }
        }
        ExtendedFloatingActionButton(elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp), onClick = { showDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp), icon = { Icon(Icons.Default.Add, null) }, text = { Text("موعد جديد") })
    }
}

private fun localEventStart(event: CalendarEvent): LocalDateTime =
    if (event.allday) LocalDate.parse(event.start.take(10)).atStartOfDay()
    else LocalDateTime.parse(event.start, rpcDateTime).atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()

@Composable
fun CalendarEventCard(event: CalendarEvent) {
    val local = runCatching { localEventStart(event) }.getOrNull()
    Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
        Text(if (event.allday) "طوال اليوم" else local?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—", Modifier.width(72.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium)
            Text(local?.toLocalDate()?.toString() ?: event.start, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            event.description?.takeIf { it.isNotBlank() }?.let { Text(android.text.Html.fromHtml(it, android.text.Html.FROM_HTML_MODE_LEGACY).toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun NewSessionDialog(contacts: List<ResUser>, initialDate: String, saving: Boolean, onDismiss: () -> Unit, onSave: (String, String, String, String, Int?) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var patientId by rememberSaveable { mutableStateOf<Int?>(null) }
    var date by rememberSaveable { mutableStateOf(initialDate) }
    var time by rememberSaveable { mutableStateOf("09:00") }
    var minutes by rememberSaveable { mutableStateOf("60") }
    var description by rememberSaveable { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("حجز موعد", fontWeight = FontWeight.Bold) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("التاريخ والوقت بتوقيت جهازك", style = MaterialTheme.typography.bodySmall)
            OdooDropdown("جهة الاتصال / المريض", contacts, patientId) { id -> patientId = id; if (name.isBlank()) name = contacts.find { it.id == id }?.name ?: "" }
            ClinicTextField(name, { name = it }, "عنوان الموعد *")
            ClinicTextField(date, { date = it }, "التاريخ: yyyy-MM-dd")
            ClinicTextField(time, { time = it }, "الوقت: HH:mm")
            ClinicTextField(minutes, { minutes = it }, "المدة بالدقائق")
            ClinicTextField(description, { description = it }, "ملاحظات")
            validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        Button(enabled = !saving, onClick = {
            runCatching {
                require(name.isNotBlank()) { "اكتب عنوان الموعد" }
                val (utcStart, utcStop) = AppointmentTime.utcRange(date, time, minutes)
                validationError = null
                onSave(name.trim(), utcStart, utcStop, description.trim(), patientId)
            }.onFailure { validationError = it.message?.takeIf { _ -> it is IllegalArgumentException } ?: "راجع التاريخ والوقت: مثال 2026-10-27 و09:00" }
        }) { Text(if (saving) "جارٍ الحفظ…" else "حفظ الموعد") }
    }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("إلغاء") } })
}
