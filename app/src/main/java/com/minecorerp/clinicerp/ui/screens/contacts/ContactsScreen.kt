package com.minecorerp.clinicerp.ui.screens.contacts

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecorerp.clinicerp.data.remote.models.ResUser
import com.minecorerp.clinicerp.ui.screens.dashboard.DashboardViewModel
import com.minecorerp.clinicerp.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    viewModel: DashboardViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredContacts by remember {
        derivedStateOf {
            viewModel.availableContacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.phone.orEmpty().contains(searchQuery) || it.email.orEmpty().contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val selectedContacts = remember { mutableStateListOf<Int>() }
    var isSelectionMode by remember { mutableStateOf(false) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<ResUser?>(null) }
    var showMergeDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        PartnerDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, email ->
                viewModel.createPartner(name, phone, email) {
                    showAddDialog = false
                }
            }
        )
    }

    if (contactToEdit != null) {
        PartnerDialog(
            partner = contactToEdit,
            onDismiss = { contactToEdit = null },
            onSave = { name, phone, email ->
                viewModel.updatePartner(contactToEdit!!.id, name, phone, email) {
                    contactToEdit = null
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("ابحث بالاسم أو الهاتف أو البريد") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                if (isSelectionMode) {
                    IconButton(onClick = { showMergeDialog = true }, enabled = selectedContacts.size == 2) {
                        Icon(Icons.Default.Merge, contentDescription = "دمج", tint = if (selectedContacts.size == 2) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                    IconButton(onClick = { isSelectionMode = false; selectedContacts.clear() }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (viewModel.isLoading && viewModel.availableContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد جهات اتصال مطابقة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ContactCard(
                            contact = contact,
                            isSelected = selectedContacts.contains(contact.id),
                            onClick = {
                                if (isSelectionMode) {
                                    if (selectedContacts.contains(contact.id)) selectedContacts.remove(contact.id)
                                    else if (selectedContacts.size < 2) selectedContacts.add(contact.id)
                                } else if (viewModel.canWriteContacts) {
                                    contactToEdit = contact
                                }
                            },
                            onLongClick = {
                                if (viewModel.canWriteContacts) contactToEdit = contact
                            }
                        )
                    }
                }
            }
        }

        if (viewModel.canCreateContacts && !isSelectionMode) {
            FloatingActionButton(
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                onClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مريض")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactCard(
    contact: ResUser,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(if (isSelected) Icons.Default.Check else Icons.Default.PersonOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            val detail = contact.phone?.takeIf { it.isNotBlank() } ?: contact.email.orEmpty()
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun PartnerDialog(
    partner: ResUser? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(partner?.name ?: "") }
    var email by remember { mutableStateOf(partner?.email ?: "") }
    var phone by remember { mutableStateOf(partner?.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, email) }, 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = MaterialTheme.colorScheme.primary) }
        },
        title = { Text(if (partner == null) "إضافة مريض جديد" else "تعديل بيانات المريض", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                com.minecorerp.clinicerp.ui.components.ClinicTextField(value = name, onValueChange = { name = it }, label = "الاسم بالكامل *")
                Spacer(Modifier.height(8.dp))
                com.minecorerp.clinicerp.ui.components.ClinicTextField(value = phone, onValueChange = { phone = it }, label = "رقم الهاتف")
                Spacer(Modifier.height(8.dp))
                com.minecorerp.clinicerp.ui.components.ClinicTextField(value = email, onValueChange = { email = it }, label = "البريد الإلكتروني")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
