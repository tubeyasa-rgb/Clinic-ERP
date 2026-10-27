package com.minecorerp.clinicerp.ui.screens.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minecorerp.clinicerp.data.remote.models.*
import com.minecorerp.clinicerp.data.repository.ActivityRepository
import com.minecorerp.clinicerp.data.repository.AuthRepository
import com.minecorerp.clinicerp.data.repository.CrmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val crmRepository: CrmRepository,
    private val authRepository: AuthRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    var baseUrl by mutableStateOf("")
        private set

    var stages by mutableStateOf<List<CrmStage>>(emptyList())
    var leads by mutableStateOf<List<CrmLead>>(emptyList())
    var availableTags by mutableStateOf<List<CrmTag>>(emptyList())
    var availableDoctors by mutableStateOf<List<ResUser>>(emptyList())
    var availableContacts by mutableStateOf<List<ResUser>>(emptyList())
    var availableActivityTypes by mutableStateOf<List<MailActivityType>>(emptyList())
    
    // 100% Parity Fields
    var availableTeams by mutableStateOf<List<CrmTeam>>(emptyList())
    var utmSources by mutableStateOf<List<UtmSource>>(emptyList())
    var utmMediums by mutableStateOf<List<UtmMedium>>(emptyList())
    var utmCampaigns by mutableStateOf<List<UtmCampaign>>(emptyList())
    var selectedTeamId by mutableStateOf<Int?>(null)
    
    var upcomingActivities by mutableStateOf<List<MailActivity>>(emptyList())
    var calendarEvents by mutableStateOf<List<CalendarEvent>>(emptyList())
    
    var currentUserProfile by mutableStateOf<ResUser?>(null)
    var isMyLeadsOnly by mutableStateOf(true)

    var canCreateContacts by mutableStateOf(false)
    var canWriteContacts by mutableStateOf(false)
    
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    // Specific record details
    var leadActivities by mutableStateOf<List<MailActivity>>(emptyList())
    var isLeadDataLoading by mutableStateOf(false)
    var isUploadingAttachment by mutableStateOf(false)



    private var loadJob: Job? = null
    var isSaving by mutableStateOf(false)
        private set

    private fun runOperation(action: suspend () -> Unit) {
        if (isSaving) return
        isSaving = true
        error = null
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تنفيذ العملية" }
            finally { isSaving = false }
        }
    }

    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            error = null
            try {
                baseUrl = authRepository.sessionManager.serverUrl.firstOrNull() ?: ""
                
                val stagesTask = async { try { crmRepository.getStages() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<CrmStage>() } }
                val leadsTask = async { try { crmRepository.getLeads() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<CrmLead>() } }
                val tagsTask = async { try { crmRepository.getTags() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<CrmTag>() } }
                val usersTask = async { try { crmRepository.getUsers() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<ResUser>() } }
                val partnersTask = async { try { crmRepository.getPartners() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<ResUser>() } }
                val teamsTask = async { try { crmRepository.getTeams() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<CrmTeam>() } }
                val utmSTask = async { try { crmRepository.getUtmSources() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<UtmSource>() } }
                val utmMTask = async { try { crmRepository.getUtmMediums() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<UtmMedium>() } }
                val utmCTask = async { try { crmRepository.getUtmCampaigns() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<UtmCampaign>() } }
                val activitiesTask = async { try { activityRepository.getMyActivities() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<MailActivity>() } }
                val calendarTask = async { try { activityRepository.getMyCalendarEvents() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<CalendarEvent>() } }
                val activityTypesTask = async { try { activityRepository.getActivityTypes() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل البيانات"; emptyList<MailActivityType>() } }
                val profileTask = async { try { authRepository.getCurrentUserDetails().getOrNull() } catch (e: Exception) { null } }
                val permsTask = async { try { crmRepository.checkPartnerPermissions() } catch (e: Exception) { emptyMap() } }

                stages = stagesTask.await()
                leads = leadsTask.await()
                availableTags = tagsTask.await()
                availableDoctors = usersTask.await()
                availableContacts = partnersTask.await()
                availableTeams = teamsTask.await()
                utmSources = utmSTask.await()
                utmMediums = utmMTask.await()
                utmCampaigns = utmCTask.await()
                upcomingActivities = activitiesTask.await()
                calendarEvents = calendarTask.await()
                availableActivityTypes = activityTypesTask.await()
                currentUserProfile = profileTask.await()
                
                val perms = permsTask.await()
                canCreateContacts = perms["create"] == true
                canWriteContacts = perms["write"] == true

            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                error = "خطأ في الاتصال: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadLeadDetails(leadId: Int) {
        viewModelScope.launch {
            isLeadDataLoading = true
            try { leadActivities = activityRepository.getActivitiesForLead(leadId) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "تعذر تحميل أنشطة الحالة" }
            finally { isLeadDataLoading = false }
        }
    }

    fun updateProfile(name: String, email: String, onSuccess: () -> Unit) {
        runOperation {
            check(authRepository.updateCurrentUserDetails(name, email)) { "تعذر حفظ الملف الشخصي" }
            loadData()
            onSuccess()
        }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        runOperation {
            val result = authRepository.changePassword(oldPass, newPass)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "فشل تغيير كلمة المرور")
            }
        }
    }

    fun getFilteredLeads(): List<CrmLead> {
        var result = leads
        
        // 1. Team (Branch) Filtering
        selectedTeamId?.let { teamId ->
            result = result.filter { it.teamId.asOdooId() == teamId }
        }
        
        // 2. Personal Filtering
        if (isMyLeadsOnly) {
            val userUid = currentUserProfile?.id ?: return result
            result = result.filter { it.userId.asOdooId() == userUid }
        }
        
        return result
    }

    fun createLead(
        name: String, contactName: String, phone: String, email: String, 
        notes: String, userId: Int?, tagIds: List<Int>, partnerId: Int?, 
        priority: String, teamId: Int?, probability: Double?,
        campaignId: Int?, mediumId: Int?, sourceId: Int?,
        onSuccess: () -> Unit
    ) {
        runOperation {
            try {
                require(name.isNotBlank()) { "اسم الحالة مطلوب" }
                require(probability == null || probability in 0.0..100.0) { "الاحتمالية لازم تكون من 0 إلى 100" }
                val leadId = crmRepository.createLead(
                    name, contactName, phone, email, notes, userId, tagIds, partnerId, 
                    priority, teamId, probability, campaignId, mediumId, sourceId
                )
                check(leadId != null) { "تعذر إنشاء الحالة" }
                loadData()
                onSuccess()
            } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر حفظ التغييرات" }
        }
    }

    fun updateLead(
        leadId: Int, name: String, contactName: String, phone: String, email: String, 
        notes: String, stageId: Int, userId: Int?, tagIds: List<Int>, 
        partnerId: Int?, priority: String, teamId: Int?, probability: Double?,
        campaignId: Int?, mediumId: Int?, sourceId: Int?,
        onSuccess: () -> Unit
    ) {
        runOperation {
            try {
                require(name.isNotBlank()) { "اسم الحالة مطلوب" }
                require(probability == null || probability in 0.0..100.0) { "الاحتمالية لازم تكون من 0 إلى 100" }
                val success = crmRepository.updateLead(
                    leadId, name, contactName, phone, email, notes, stageId, userId, tagIds, partnerId, 
                    priority, teamId, probability, campaignId, mediumId, sourceId
                )
                check(success) { "تعذر حفظ الحالة" }
                loadData()
                onSuccess()
            } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر حفظ التغييرات" }
        }
    }

    suspend fun getLeadAttachments(leadId: Int): List<OdooAttachment> {
        return try { crmRepository.getAttachments("crm.lead", leadId) } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "تعذر تحميل المرفقات"; emptyList() }
    }

    fun openAttachment(file: OdooAttachment, context: android.content.Context) {
        runOperation {
            val downloaded = crmRepository.downloadAttachment(file.id, file.name)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", downloaded)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                .setDataAndType(uri, file.mimeType ?: "application/octet-stream")
                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try { context.startActivity(intent) }
            catch (e: android.content.ActivityNotFoundException) { error = "تم تنزيل الملف، لكن لا يوجد تطبيق مناسب لفتحه" }
        }
    }

    fun uploadLeadAttachmentFromUri(leadId: Int, uri: android.net.Uri, context: android.content.Context, onComplete: () -> Unit) {
        runOperation {
            isUploadingAttachment = true
            try {
                val (fileName, data) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    var name = "attachment"
                    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) name = cursor.getString(0) ?: name
                    }
                    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            require(output.size() + read <= 15 * 1024 * 1024) { "الحد الأقصى للرفع 15 ميجابايت" }
                            output.write(buffer, 0, read)
                        }
                        output.toByteArray()
                    } ?: error("تعذر قراءة الملف المختار")
                    name to android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                }
                check(crmRepository.uploadAttachment("crm.lead", leadId, fileName, data) != null) { "تعذر رفع المرفق" }
                onComplete()
            } finally { isUploadingAttachment = false }
        }
    }

    fun createPartner(name: String, phone: String, email: String, onSuccess: () -> Unit) {
        runOperation {
            require(name.isNotBlank()) { "اسم جهة الاتصال مطلوب" }
            val result = crmRepository.createPartner(name.trim(), phone.trim(), email.trim())
            check(result != null) { "تعذر حفظ البيانات" }
            loadData()
            onSuccess()
        }
    }

    fun updatePartner(id: Int, name: String, phone: String, email: String, onSuccess: () -> Unit) {
        runOperation {
            require(name.isNotBlank()) { "اسم جهة الاتصال مطلوب" }
            check(crmRepository.updatePartner(id, name.trim(), phone.trim(), email.trim())) { "تعذر تعديل جهة الاتصال" }
            loadData()
            onSuccess()
        }
    }

    fun scheduleLeadActivity(leadId: Int, typeId: Int, summary: String, note: String, deadline: String, assignedUserId: Int, onSuccess: () -> Unit) {
        runOperation {
            // The repository resolves the required Odoo model ID.
            val result = activityRepository.scheduleActivity("crm.lead", leadId, typeId, summary, note, deadline, assignedUserId)
            if (result != null) {
                leadActivities = activityRepository.getActivitiesForLead(leadId)
                loadData()
                onSuccess()
            } else {
                error = "فشل حفظ النشاط. تأكد من الصلاحيات والتاريخ."
            }
        }
    }

    fun createSession(name: String, start: String, stop: String, description: String, patientId: Int?, onSuccess: () -> Unit) {
        runOperation {
            check(activityRepository.createCalendarEvent(name, start, stop, description, patientId) != null) { "تعذر إنشاء الموعد؛ تحقق من صلاحيات الكاليندر" }
            loadData()
            onSuccess()
        }
    }



    fun logout(onSuccess: () -> Unit) {
        runOperation {
            loadJob?.cancel()
            authRepository.logout()
            availableTags = emptyList(); availableDoctors = emptyList(); availableTeams = emptyList()
            utmSources = emptyList(); utmMediums = emptyList(); utmCampaigns = emptyList()
            availableActivityTypes = emptyList(); leadActivities = emptyList()
            selectedTeamId = null; isMyLeadsOnly = true; canCreateContacts = false; canWriteContacts = false
            error = null; baseUrl = ""
            leads = emptyList(); calendarEvents = emptyList(); availableContacts = emptyList()
            upcomingActivities = emptyList(); currentUserProfile = null; stages = emptyList()
            onSuccess()
        }
    }
}
