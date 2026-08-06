package com.junps.dompetku.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.ui.components.LoadingScreen
import com.junps.dompetku.ui.components.MessageScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                TransactionFormEvent.Saved,
                TransactionFormEvent.Deleted,
                -> onNavigateBack()
                is TransactionFormEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit transaksi" else "Tambah transaksi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingScreen("Memuat formulir transaksi", Modifier.padding(innerPadding))
            uiState.loadError != null -> FormError(
                message = uiState.loadError.orEmpty(),
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(innerPadding),
            )
            else -> TransactionForm(
                uiState = uiState,
                onAmountChange = viewModel::onAmountChange,
                onCategoryChange = viewModel::onCategoryChange,
                onDateChange = viewModel::onTransactionDateChange,
                onNoteChange = viewModel::onNoteChange,
                onSave = viewModel::save,
                onDelete = { showDeleteConfirmation = true },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Hapus transaksi?") },
            text = { Text("Transaksi ini akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = viewModel::delete) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Batal") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionForm(
    uiState: TransactionFormUiState,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = uiState.amountInput,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nominal") },
            prefix = { Text("Rp ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded },
        ) {
            OutlinedTextField(
                value = uiState.selectedCategory?.let { category ->
                    "${category.name} · ${if (category.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"}"
                }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                label = { Text("Kategori") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
            ) {
                uiState.categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(category.name)
                                Text(
                                    if (category.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onCategoryChange(category.id)
                            categoryExpanded = false
                        },
                    )
                }
            }
        }

        DateTimeButtons(
            timestamp = uiState.transactionDate,
            onDateClick = { showDatePicker = true },
            onTimeClick = { showTimePicker = true },
        )

        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Catatan (opsional)") },
            minLines = 3,
            maxLines = 5,
            supportingText = { Text("${uiState.note.length}/200") },
        )

        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Simpan transaksi")
            }
        }

        if (uiState.isEditMode) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    "Hapus transaksi",
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.transactionDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            onDateChange(mergeSelectedDate(uiState.transactionDate, selected))
                        }
                        showDatePicker = false
                    },
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val calendar = remember(uiState.transactionDate) {
            Calendar.getInstance().apply { timeInMillis = uiState.transactionDate }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pilih waktu") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateChange(
                            mergeSelectedTime(
                                uiState.transactionDate,
                                timePickerState.hour,
                                timePickerState.minute,
                            ),
                        )
                        showTimePicker = false
                    },
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun DateTimeButtons(timestamp: Long, onDateClick: () -> Unit, onTimeClick: () -> Unit) {
    BoxWithConstraints {
        val compact = maxWidth < 360.dp
        val content: @Composable (Modifier) -> Unit = { buttonModifier ->
            OutlinedButton(onClick = onDateClick, modifier = buttonModifier) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Text(formatDate(timestamp), modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = onTimeClick, modifier = buttonModifier) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Text(formatTime(timestamp), modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content(Modifier.fillMaxWidth()) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { content(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun FormError(
    message: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageScreen(
        title = "Transaksi tidak dapat dimuat",
        message = message,
        modifier = modifier,
        actionLabel = "Kembali",
        onAction = onNavigateBack,
    )
}

private val IndonesianLocale = Locale.forLanguageTag("id-ID")

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy", IndonesianLocale).format(timestamp)

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", IndonesianLocale).format(timestamp)

private fun mergeSelectedDate(original: Long, selectedUtcDate: Long): Long {
    val selected = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selectedUtcDate }
    return Calendar.getInstance().apply {
        timeInMillis = original
        set(Calendar.YEAR, selected.get(Calendar.YEAR))
        set(Calendar.MONTH, selected.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, selected.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun mergeSelectedTime(original: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = original
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
