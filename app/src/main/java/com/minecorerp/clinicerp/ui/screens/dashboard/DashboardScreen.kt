package com.minecorerp.clinicerp.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecorerp.clinicerp.data.remote.models.*
import com.minecorerp.clinicerp.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var showActivities by remember { mutableStateOf(false) }
    var showNewLeadDialog by remember { mutableStateOf(false) }
    var selectedLead by remember { mutableStateOf<CrmLead?>(null) }

    if (showActivities) {
        AlertDialog(onDismissRequest = { showActivities = false }, title = { Text("أنشطة المتابعة") }, confirmButton = { TextButton(onClick = { showActivities = false }) { Text("إغلاق") } }, text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(viewModel.upcomingActivities, key = { it.id }) { activity ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(activity.resName ?: "نشاط", fontWeight = FontWeight.Medium)
                        Text(activity.summary ?: activity.note ?: "متابعة", style = MaterialTheme.typography.bodySmall)
                        Text(activity.dateDeadline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        })
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (showNewLeadDialog) {
            NewLeadDialog(
                doctors = viewModel.availableDoctors,
                tags = viewModel.availableTags,
                contacts = viewModel.availableContacts,
                teams = viewModel.availableTeams,
                utmSources = viewModel.utmSources,
                utmMediums = viewModel.utmMediums,
                utmCampaigns = viewModel.utmCampaigns,
                onDismiss = { showNewLeadDialog = false },
                onSave = { name, phone, email, userId, tagIds, partnerId, notes, priority, teamId, prob, campId, medId, sourceId ->
                    viewModel.createLead(name, name, phone, email, notes, userId, tagIds, partnerId, priority, teamId, prob, campId, medId, sourceId) {
                        showNewLeadDialog = false
                    }
                }
            )
        }

        if (selectedLead != null) {
            LeadDetailDialog(
                lead = selectedLead!!,
                stages = viewModel.stages,
                doctors = viewModel.availableDoctors,
                tags = viewModel.availableTags,
                contacts = viewModel.availableContacts,
                teams = viewModel.availableTeams,
                utmSources = viewModel.utmSources,
                utmMediums = viewModel.utmMediums,
                utmCampaigns = viewModel.utmCampaigns,
                viewModel = viewModel,
                onDismiss = { selectedLead = null },
                onUpdate = { name, phone, email, notes, stageId, userId, tagIds, partnerId, priority, teamId, prob, campId, medId, sourceId ->
                    viewModel.updateLead(selectedLead!!.id, name, selectedLead!!.contactName.asOdooString() ?: name, phone, email, notes, stageId, userId, tagIds, partnerId, priority, teamId, prob, campId, medId, sourceId) {
                        selectedLead = null
                    }
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            KpiStrip(leads = viewModel.getFilteredLeads().filter { lead ->
                        listOf(lead.name, lead.contactName.asOdooString(), lead.phone.asOdooString(), lead.emailFrom.asOdooString()).any { it?.contains(searchQuery, ignoreCase = true) == true }
                    }, eventsCount = viewModel.calendarEvents.size)

            if (viewModel.upcomingActivities.isNotEmpty()) {
                TextButton(onClick = { showActivities = true }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("أنشطة المتابعة (${viewModel.upcomingActivities.size})") }
            }
            
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = viewModel.isMyLeadsOnly,
                                onCheckedChange = { viewModel.isMyLeadsOnly = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (viewModel.isMyLeadsOnly) "حالاتي فقط" else "كل الحالات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { showNewLeadDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(9.dp)
                    ) {
                        Text("إضافة حالة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                
                // Branch Selector
                if (viewModel.availableTeams.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        val teamName = viewModel.availableTeams.find { it.id == viewModel.selectedTeamId }?.name ?: "كل فرق المتابعة"
                        Text(teamName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("كل الفرق") }, onClick = { viewModel.selectedTeamId = null; expanded = false })
                        viewModel.availableTeams.forEach { team ->
                            DropdownMenuItem(text = { Text(team.name) }, onClick = { viewModel.selectedTeamId = team.id; expanded = false })
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("ابحث بالاسم أو الهاتف أو البريد") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, shape = RoundedCornerShape(8.dp)
            )
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                KanbanBoard(
                    stages = viewModel.stages,
                    leads = viewModel.getFilteredLeads().filter { lead ->
                        listOf(lead.name, lead.contactName.asOdooString(), lead.phone.asOdooString(), lead.emailFrom.asOdooString()).any { it?.contains(searchQuery, ignoreCase = true) == true }
                    },
                    availableTags = viewModel.availableTags,
                    onLeadClick = { selectedLead = it }
                )
            }
        }

        // Global Loading Overlay
        if (viewModel.isUploadingAttachment) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(0.5f)) {
                Box(contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Teal700)
                            Spacer(Modifier.height(16.dp))
                            Text("جاري رفع المرفق لـ Odoo...", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewLeadDialog(
    doctors: List<ResUser>,
    tags: List<CrmTag>,
    contacts: List<ResUser>,
    teams: List<CrmTeam>,
    utmSources: List<UtmSource>,
    utmMediums: List<UtmMedium>,
    utmCampaigns: List<UtmCampaign>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int?, List<Int>, Int?, String, String, Int?, Double?, Int?, Int?, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedDoctorId by remember { mutableStateOf<Int?>(null) }
    var selectedPartnerId by remember { mutableStateOf<Int?>(null) }
    var selectedTeamId by remember { mutableStateOf<Int?>(null) }
    var selectedSourceId by remember { mutableStateOf<Int?>(null) }
    var selectedMediumId by remember { mutableStateOf<Int?>(null) }
    var selectedCampaignId by remember { mutableStateOf<Int?>(null) }
    var prob by remember { mutableStateOf("0.0") }
    val selectedTagIds = remember { mutableStateListOf<Int>() }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onSave(name, phone, email, selectedDoctorId, selectedTagIds.toList(), selectedPartnerId, notes, priority, selectedTeamId, prob.toDoubleOrNull(), selectedCampaignId, selectedMediumId, selectedSourceId) }, colors = ButtonDefaults.buttonColors(containerColor = Teal900)) {
                Text("حفظ الحالة")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("إضافة حالة جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("المريض والعيادة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Teal700)
                OdooDropdown(label = "اختيار مريض", options = contacts, selectedId = selectedPartnerId) { selectedPartnerId = it; contacts.find { c -> c.id == it }?.let { contact -> name = contact.name; phone = contact.phone.orEmpty(); email = contact.email.orEmpty() } }
                if (selectedPartnerId == null) {
                    com.minecorerp.clinicerp.ui.components.ClinicTextField(value = name, onValueChange = { name = it }, label = "اسم المريض المحتمل *")
                }
                com.minecorerp.clinicerp.ui.components.ClinicTextField(value = phone, onValueChange = { phone = it }, label = "رقم الهاتف *")
                OdooDropdown(label = "فريق المتابعة", options = teams, selectedId = selectedTeamId) { selectedTeamId = it }
                OdooDropdown(label = "المسؤول عن الحالة", options = doctors, selectedId = selectedDoctorId) { selectedDoctorId = it }
                
                Spacer(Modifier.height(12.dp))
                Text("الأولوية والنجاح", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Teal700)
                PrioritySelector(priority) { priority = it }
                com.minecorerp.clinicerp.ui.components.ClinicTextField(value = prob, onValueChange = { prob = it }, label = "احتمالية نجاح الفرصة (%)")

                Spacer(Modifier.height(12.dp))
                Text("مصدر المريض (التسويق)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Teal700)
                OdooDropdown(label = "المصدر (Source)", options = utmSources, selectedId = selectedSourceId) { selectedSourceId = it }
                OdooDropdown(label = "الوسيلة (Medium)", options = utmMediums, selectedId = selectedMediumId) { selectedMediumId = it }
                OdooDropdown(label = "الحملة (Campaign)", options = utmCampaigns, selectedId = selectedCampaignId) { selectedCampaignId = it }

                Spacer(Modifier.height(12.dp))
                Text("تصنيفات وملاحظات", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Teal700)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.forEach { tag ->
                        FilterChip(selected = selectedTagIds.contains(tag.id), onClick = { if (selectedTagIds.contains(tag.id)) selectedTagIds.remove(tag.id) else selectedTagIds.add(tag.id) }, label = { Text(tag.name, fontSize = 10.sp) })
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, placeholder = { Text("ملاحظات التقييم الأولية...") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(9.dp))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeadDetailDialog(
    lead: CrmLead,
    stages: List<CrmStage>,
    doctors: List<ResUser>,
    tags: List<CrmTag>,
    contacts: List<ResUser>,
    teams: List<CrmTeam>,
    utmSources: List<UtmSource>,
    utmMediums: List<UtmMedium>,
    utmCampaigns: List<UtmCampaign>,
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit,
    onUpdate: (String, String, String, String, Int, Int?, List<Int>, Int?, String, Int?, Double?, Int?, Int?, Int?) -> Unit
) {
    var name by remember { mutableStateOf(lead.name) }
    var phone by remember { mutableStateOf(lead.phone.asOdooString() ?: "") }
    var email by remember { mutableStateOf(lead.emailFrom.asOdooString() ?: "") }
    var notes by remember { mutableStateOf(lead.description.asOdooString() ?: "") }
    var priority by remember { mutableStateOf(lead.priority.asOdooString() ?: "0") }
    var prob by remember { mutableStateOf(lead.probability?.toString() ?: "0.0") }
    var stageId by remember { mutableIntStateOf(lead.stageId.asOdooId() ?: 0) }
    var selectedDoctorId by remember { mutableStateOf(lead.userId.asOdooId()) }
    var selectedTeamId by remember { mutableStateOf(lead.teamId.asOdooId()) }
    var selectedSourceId by remember { mutableStateOf(lead.sourceId.asOdooId()) }
    var selectedMediumId by remember { mutableStateOf(lead.mediumId.asOdooId()) }
    var selectedCampaignId by remember { mutableStateOf(lead.campaignId.asOdooId()) }
    var selectedPartnerId by remember { mutableStateOf(lead.partnerId.asOdooId()) }
    val selectedTagIds = remember { mutableStateListOf<Int>().apply { addAll(lead.tagIds ?: emptyList()) } }

    var attachments by remember { mutableStateOf<List<OdooAttachment>>(emptyList()) }
    var isLoadingData by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let {
        viewModel.uploadLeadAttachmentFromUri(lead.id, it, context) { isLoadingData = true }
    }}

    LaunchedEffect(lead.id, isLoadingData) { if (isLoadingData) { attachments = viewModel.getLeadAttachments(lead.id); viewModel.loadLeadDetails(lead.id); isLoadingData = false } }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onUpdate(name, phone, email, notes, stageId, selectedDoctorId, selectedTagIds.toList(), selectedPartnerId, priority, selectedTeamId, prob.toDoubleOrNull(), selectedCampaignId, selectedMediumId, selectedSourceId) }, colors = ButtonDefaults.buttonColors(containerColor = Teal900)) { Text("حفظ التعديلات") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        title = { Text("تفاصيل الحالة", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                var selectedTab by remember { mutableIntStateOf(0) }
                TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Teal900, indicator = { tabPositions -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Teal900) }) {
                    listOf("التفاصيل", "التسويق", "الأنشطة", "المرفقات").forEachIndexed { index, title -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }) }
                }
                Spacer(Modifier.height(16.dp))
                if (isLoadingData) Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Teal700) }
                else when (selectedTab) {
                    0 -> {
                        OdooDropdown(label = "المريض", options = contacts, selectedId = selectedPartnerId ?: lead.contactName.asOdooId()) { selectedPartnerId = it }
                        com.minecorerp.clinicerp.ui.components.ClinicTextField(value = name, onValueChange = { name = it }, label = "الاسم")
                        com.minecorerp.clinicerp.ui.components.ClinicTextField(value = phone, onValueChange = { phone = it }, label = "الهاتف")
                        OdooDropdown(label = "المرحلة", options = stages.map { ResUser(it.id, it.name) }, selectedId = stageId) { stageId = it }
                        OdooDropdown(label = "فريق المتابعة", options = teams, selectedId = selectedTeamId) { selectedTeamId = it }
                        OdooDropdown(label = "الطبيب", options = doctors, selectedId = selectedDoctorId) { selectedDoctorId = it }
                        PrioritySelector(priority) { priority = it }
                        com.minecorerp.clinicerp.ui.components.ClinicTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.height(100.dp), label = "الملاحظات")
                    }
                    1 -> {
                        com.minecorerp.clinicerp.ui.components.ClinicTextField(value = prob, onValueChange = { prob = it }, label = "احتمالية نجاح الفرصة (%)")
                        OdooDropdown(label = "المصدر", options = utmSources, selectedId = selectedSourceId) { selectedSourceId = it }
                        OdooDropdown(label = "الوسيلة", options = utmMediums, selectedId = selectedMediumId) { selectedMediumId = it }
                        OdooDropdown(label = "الحملة", options = utmCampaigns, selectedId = selectedCampaignId) { selectedCampaignId = it }
                    }
                    2 -> {
                        var showScheduleDialog by remember { mutableStateOf(false) }
                        if (showScheduleDialog) ScheduleActivityDialog(leadId = lead.id, activityTypes = viewModel.availableActivityTypes, users = viewModel.availableDoctors, currentUserId = viewModel.currentUserProfile?.id ?: 0, onDismiss = { showScheduleDialog = false }, onSave = { typeId, sum, n, dead, uid -> viewModel.scheduleLeadActivity(lead.id, typeId, sum, n, dead, uid) { showScheduleDialog = false } })
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("الأنشطة", fontWeight = FontWeight.Bold); TextButton(onClick = { showScheduleDialog = true }) { Text("+ جدولة") } }
                        viewModel.leadActivities.forEach { ActivityCard(it) }
                    }
                    3 -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("المرفقات", fontWeight = FontWeight.Bold); TextButton(onClick = { launcher.launch("*/*") }) { Text("رفع جديد") } }
                        attachments.forEach { AttachmentCard(it) { viewModel.openAttachment(it, context) } }
                    }
                }
            }
        }
    )
}

@Composable
fun OdooDropdown(label: String, options: List<OdooNamedModel>, selectedId: Int?, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Teal700, modifier = Modifier.padding(top = 8.dp))
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Border)) {
            Text(options.find { it.id == selectedId }?.name ?: "اختر...", color = Ink, fontSize = 12.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt -> DropdownMenuItem(text = { Text(opt.name) }, onClick = { onSelect(opt.id); expanded = false }) }
        }
    }
}

@Composable
fun PrioritySelector(priority: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val level = (i + 1).toString()
            IconButton(onClick = { onSelect(level) }) { Icon(if (priority >= level) Icons.Default.Star else Icons.Outlined.Star, null, tint = if (priority >= level) Gold else Color.Gray) }
        }
    }
}

@Composable
fun ActivityCard(activity: MailActivity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Mint.copy(0.3f))) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, null, tint = Teal900, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column { Text(activity.activityTypeId.asOdooName() ?: "نشاط", fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(activity.summary ?: "", fontSize = 11.sp); Text(activity.dateDeadline, fontSize = 10.sp, color = Color.Gray) }
        }
    }
}

@Composable
fun AttachmentCard(file: OdooAttachment, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { 
        onOpen()
    }, colors = CardDefaults.cardColors(containerColor = Mint.copy(0.3f))) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AttachFile, null, tint = Teal700, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column { Text(file.name, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${file.fileSize?.let { it/1024 } ?: 0} KB", fontSize = 10.sp, color = Color.Gray) }
        }
    }
}

@Composable
fun ScheduleActivityDialog(leadId: Int, activityTypes: List<MailActivityType>, users: List<ResUser>, currentUserId: Int, onDismiss: () -> Unit, onSave: (Int, String, String, String, Int) -> Unit) {
    var typeId by remember(activityTypes) { mutableIntStateOf(activityTypes.firstOrNull()?.id ?: 0) }
    var sum by remember(activityTypes) { mutableStateOf(activityTypes.firstOrNull()?.name ?: "") }
    var n by remember { mutableStateOf("") }
    var dead by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())) }
    var uid by remember { mutableIntStateOf(currentUserId) }

    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = { onSave(typeId, sum, n, dead, uid) }) { Text("حفظ") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }, title = { Text("جدولة نشاط") }, text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OdooDropdown("نوع النشاط", activityTypes.map { ResUser(it.id, it.name) }, typeId) { typeId = it; sum = activityTypes.find { t -> t.id == it }?.name ?: "" }
            com.minecorerp.clinicerp.ui.components.ClinicTextField(sum, { sum = it }, "الملخص")
            com.minecorerp.clinicerp.ui.components.ClinicTextField(dead, { dead = it }, "التاريخ")
            OdooDropdown("المسؤول", users, uid) { uid = it }
            OutlinedTextField(n, { n = it }, modifier = Modifier.fillMaxWidth().height(80.dp), label = { Text("ملاحظات") })
        }
    })
}

@Composable
fun ActivityStrip(activities: List<MailActivity>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 8.dp)) {
        Text("أنشطة اليوم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            activities.forEach { activity ->
                Card(modifier = Modifier.width(200.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(activity.resName ?: "نشاط", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                        Text(activity.summary ?: activity.note ?: "متابعة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        Text("📅 ${activity.dateDeadline}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun KpiStrip(leads: List<CrmLead>, eventsCount: Int) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        KpiCard("حالة", leads.size.toString())
        KpiCard("موعد محمّل", eventsCount.toString())
        KpiCard("مهمة", leads.count { (it.priority.asOdooString() ?: "0") >= "2" }.toString())
        KpiCard("قيمة الفرص", leads.sumOf { it.expectedRevenue.asOdooDouble() ?: 0.0 }.toInt().toString())
    }
}

@Composable
fun KpiCard(label: String, value: String, isWarning: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun KanbanBoard(stages: List<CrmStage>, leads: List<CrmLead>, availableTags: List<CrmTag>, onLeadClick: (CrmLead) -> Unit) {
    var selectedStage by remember { mutableStateOf<Int?>(null) }
    val visible = leads.filter { selectedStage == null || it.stageId.asOdooId() == selectedStage }
    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(leadingIcon = if (selectedStage == null) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null, selected = selectedStage == null, onClick = { selectedStage = null }, label = { Text("الكل") }) }
            items(stages, key = { it.id }) { stage ->
                FilterChip(leadingIcon = if (selectedStage == stage.id) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null, selected = selectedStage == stage.id, onClick = { selectedStage = stage.id }, label = { Text(stage.name) })
            }
        }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد حالات في هذا العرض", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            items(visible, key = { it.id }) { lead ->
                LeadCard(lead, availableTags) { onLeadClick(lead) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KanbanColumn(stage: CrmStage, leads: List<CrmLead>, availableTags: List<CrmTag>, onLeadClick: (CrmLead) -> Unit) {
    Column(modifier = Modifier.width(280.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).padding(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stage.name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            val rev = leads.sumOf { it.expectedRevenue.asOdooDouble() ?: 0.0 }
            Text("${rev.toInt()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(leads) { lead -> LeadCard(lead = lead, availableTags = availableTags, onClick = { onLeadClick(lead) }) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeadCard(lead: CrmLead, availableTags: List<CrmTag>, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(lead.contactName.asOdooString() ?: lead.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            lead.expectedRevenue.asOdooDouble()?.let { Text(it.toInt().toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        lead.userId.asOdooName()?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        val tags = lead.tagIds.orEmpty().mapNotNull { id -> availableTags.find { it.id == id }?.name }
        if (tags.isNotEmpty()) Text(tags.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if ((lead.priority.asOdooString()?.toIntOrNull() ?: 0) >= 2) Text("أولوية مرتفعة", style = MaterialTheme.typography.labelSmall)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
