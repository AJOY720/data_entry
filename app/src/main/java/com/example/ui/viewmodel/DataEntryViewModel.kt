package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.DataExportHelper
import com.example.data.local.AppDatabase
import com.example.data.model.DataEntryRecord
import com.example.data.repository.DataEntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FormState(
    val editingRecordId: String? = null,
    val name: String = "",
    val certificate: String = "",
    val totalAmount: String = "",
    val amountPaid: String = "",
    val remaining: Double = 0.0,
    val paymentType: String = "Full Paid",
    val status: String = "Active",
    val mobile: String = "",
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val description: String = "",
    val nameError: String? = null,
    val certificateError: String? = null
)

data class StatsState(
    val totalCount: Int = 0,
    val totalAmount: Double = 0.0,
    val totalAmountPaid: Double = 0.0,
    val totalRemaining: Double = 0.0
)

class DataEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DataEntryRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DataEntryRepository(database.dataEntryDao())
    }

    val allRecords: StateFlow<List<DataEntryRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPaymentFilter = MutableStateFlow<String?>(null)
    val selectedPaymentFilter: StateFlow<String?> = _selectedPaymentFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<String?>(null)
    val selectedStatusFilter: StateFlow<String?> = _selectedStatusFilter.asStateFlow()

    // CRITICAL REQUIREMENT: Save Successful Pop-up state
    private val _showSaveSuccessPopup = MutableStateFlow(false)
    val showSaveSuccessPopup: StateFlow<Boolean> = _showSaveSuccessPopup.asStateFlow()

    private val _savedRecordForPopup = MutableStateFlow<DataEntryRecord?>(null)
    val savedRecordForPopup: StateFlow<DataEntryRecord?> = _savedRecordForPopup.asStateFlow()

    private val _isUpdatedPopup = MutableStateFlow(false)
    val isUpdatedPopup: StateFlow<Boolean> = _isUpdatedPopup.asStateFlow()

    // Duplicate certificate warning dialog
    private val _showDuplicateWarning = MutableStateFlow(false)
    val showDuplicateWarning: StateFlow<Boolean> = _showDuplicateWarning.asStateFlow()

    private val _duplicateCertValue = MutableStateFlow("")
    val duplicateCertValue: StateFlow<String> = _duplicateCertValue.asStateFlow()

    // Delete confirmation dialog
    private val _recordToDelete = MutableStateFlow<DataEntryRecord?>(null)
    val recordToDelete: StateFlow<DataEntryRecord?> = _recordToDelete.asStateFlow()

    // Feedback message (Snackbar)
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Filtered records
    val filteredRecords: StateFlow<List<DataEntryRecord>> = combine(
        allRecords,
        _searchQuery,
        _selectedPaymentFilter,
        _selectedStatusFilter
    ) { records, query, paymentFilter, statusFilter ->
        val q = query.trim().lowercase(Locale.ROOT)
        records.filter { record ->
            val matchesQuery = if (q.isEmpty()) true else {
                record.name.lowercase().contains(q) ||
                        record.certificate.lowercase().contains(q) ||
                        record.mobile.lowercase().contains(q) ||
                        record.payment.lowercase().contains(q) ||
                        record.status.lowercase().contains(q) ||
                        record.description.lowercase().contains(q) ||
                        record.date.lowercase().contains(q)
            }
            val matchesPayment = paymentFilter == null || record.payment.equals(paymentFilter, ignoreCase = true)
            val matchesStatus = statusFilter == null || record.status.equals(statusFilter, ignoreCase = true)
            matchesQuery && matchesPayment && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated Stats
    val stats: StateFlow<StatsState> = allRecords.combine(_searchQuery) { records, _ ->
        val count = records.size
        val totalAmt = records.sumOf { it.totalAmount }
        val totalPaid = records.sumOf { it.amountPaid }
        val totalRem = records.sumOf { it.remaining }
        StatsState(
            totalCount = count,
            totalAmount = totalAmt,
            totalAmountPaid = totalPaid,
            totalRemaining = totalRem
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsState())

    fun updateName(name: String) {
        _formState.update { it.copy(name = name, nameError = null) }
    }

    fun updateCertificate(cert: String) {
        _formState.update { it.copy(certificate = cert, certificateError = null) }
    }

    fun updateTotalAmount(total: String) {
        _formState.update { state ->
            val totalVal = total.toDoubleOrNull() ?: 0.0
            val paidVal = state.amountPaid.toDoubleOrNull() ?: 0.0
            val remaining = (totalVal - paidVal).coerceAtLeast(0.0)
            
            // Auto suggest payment type if user hasn't explicitly locked it
            val suggestedPayment = when {
                totalVal > 0 && paidVal >= totalVal -> "Full Paid"
                totalVal > 0 && paidVal > 0 -> "Partial Paid"
                paidVal == 0.0 -> "Unpaid"
                else -> state.paymentType
            }
            state.copy(
                totalAmount = total,
                remaining = remaining,
                paymentType = suggestedPayment
            )
        }
    }

    fun updateAmountPaid(paid: String) {
        _formState.update { state ->
            val totalVal = state.totalAmount.toDoubleOrNull() ?: 0.0
            val paidVal = paid.toDoubleOrNull() ?: 0.0
            val remaining = (totalVal - paidVal).coerceAtLeast(0.0)

            val suggestedPayment = when {
                totalVal > 0 && paidVal >= totalVal -> "Full Paid"
                totalVal > 0 && paidVal > 0 -> "Partial Paid"
                paidVal == 0.0 -> "Unpaid"
                else -> state.paymentType
            }
            state.copy(
                amountPaid = paid,
                remaining = remaining,
                paymentType = suggestedPayment
            )
        }
    }

    fun updatePaymentType(paymentType: String) {
        _formState.update { it.copy(paymentType = paymentType) }
    }

    fun updateStatus(status: String) {
        _formState.update { it.copy(status = status) }
    }

    fun updateMobile(mobile: String) {
        _formState.update { it.copy(mobile = mobile) }
    }

    fun updateDate(date: String) {
        _formState.update { it.copy(date = date) }
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun setPaymentFilter(payment: String?) {
        _selectedPaymentFilter.value = payment
    }

    fun setStatusFilter(status: String?) {
        _selectedStatusFilter.value = status
    }

    fun clearForm() {
        _formState.value = FormState(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
    }

    fun editRecord(record: DataEntryRecord) {
        _formState.value = FormState(
            editingRecordId = record.id,
            name = record.name,
            certificate = record.certificate,
            totalAmount = if (record.totalAmount > 0) String.format(Locale.US, "%.2f", record.totalAmount) else "",
            amountPaid = if (record.amountPaid > 0) String.format(Locale.US, "%.2f", record.amountPaid) else "",
            remaining = record.remaining,
            paymentType = record.payment,
            status = record.status,
            mobile = record.mobile,
            date = record.date.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
            description = record.description,
            nameError = null,
            certificateError = null
        )
    }

    fun saveRecord() {
        val current = _formState.value
        val trimmedName = current.name.trim()

        if (trimmedName.isEmpty()) {
            _formState.update { it.copy(nameError = "Please enter a name (নাম আবশ্যক)") }
            return
        }

        val trimmedCert = current.certificate.trim()
        val isEditing = current.editingRecordId != null

        viewModelScope.launch {
            // Duplicate certificate check:
            if (trimmedCert.isNotEmpty()) {
                val existing = repository.getRecordByCertificate(trimmedCert)
                if (existing != null && existing.id != current.editingRecordId) {
                    _duplicateCertValue.value = trimmedCert
                    _showDuplicateWarning.value = true
                    _formState.update { it.copy(certificateError = "এই Certificate No আগে থেকেই এন্ট্রি করা আছে") }
                    return@launch
                }
            }

            val total = current.totalAmount.toDoubleOrNull() ?: 0.0
            val paid = current.amountPaid.toDoubleOrNull() ?: 0.0
            val rem = (total - paid).coerceAtLeast(0.0)

            val record = if (isEditing) {
                DataEntryRecord(
                    id = current.editingRecordId!!,
                    name = trimmedName,
                    certificate = trimmedCert,
                    totalAmount = total,
                    amountPaid = paid,
                    remaining = rem,
                    payment = current.paymentType,
                    status = current.status,
                    mobile = current.mobile.trim(),
                    date = current.date,
                    description = current.description.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                DataEntryRecord(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName,
                    certificate = trimmedCert,
                    totalAmount = total,
                    amountPaid = paid,
                    remaining = rem,
                    payment = current.paymentType,
                    status = current.status,
                    mobile = current.mobile.trim(),
                    date = current.date,
                    description = current.description.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }

            if (isEditing) {
                repository.update(record)
            } else {
                repository.insert(record)
            }

            // Haptic vibration feedback
            triggerHapticFeedback()

            // CRITICAL: Trigger the Save Successful Popup
            _savedRecordForPopup.value = record
            _isUpdatedPopup.value = isEditing
            _showSaveSuccessPopup.value = true

            // Reset form for next entry
            clearForm()
        }
    }

    fun dismissSaveSuccessPopup() {
        _showSaveSuccessPopup.value = false
        _savedRecordForPopup.value = null
    }

    fun dismissDuplicateWarning() {
        _showDuplicateWarning.value = false
    }

    fun confirmDelete(record: DataEntryRecord) {
        _recordToDelete.value = record
    }

    fun dismissDeleteDialog() {
        _recordToDelete.value = null
    }

    fun executeDelete() {
        val record = _recordToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteById(record.id)
            if (_formState.value.editingRecordId == record.id) {
                clearForm()
            }
            _recordToDelete.value = null
            _toastMessage.value = "Record for ${record.name} deleted"
        }
    }

    fun exportCsv(context: Context) {
        val records = allRecords.value
        if (records.isEmpty()) {
            _toastMessage.value = "No records to export"
            return
        }
        val csv = DataExportHelper.generateCsv(records)
        DataExportHelper.shareExportFile(context, "data-entry-records.csv", csv, "text/csv")
    }

    fun backupData(context: Context) {
        val records = allRecords.value
        if (records.isEmpty()) {
            _toastMessage.value = "No records to backup"
            return
        }
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val json = DataExportHelper.generateBackupJson(records)
        DataExportHelper.shareExportFile(
            context,
            "data-entry-backup-$dateStr.json",
            json,
            "application/json"
        )
    }

    fun restoreBackupFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val records = DataExportHelper.parseBackupJson(jsonString)
                if (records.isEmpty()) {
                    _toastMessage.value = "No valid records found in backup"
                    return@launch
                }
                repository.restoreRecords(records)
                clearForm()
                _toastMessage.value = "Successfully restored ${records.size} records!"
            } catch (e: Exception) {
                _toastMessage.value = "Failed to restore backup: ${e.message}"
            }
        }
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    private fun triggerHapticFeedback() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration is not supported
        }
    }
}
