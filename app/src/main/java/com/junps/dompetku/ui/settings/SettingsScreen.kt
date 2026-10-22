package com.junps.dompetku.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val allowedAuthenticators = remember {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
    var canUseAppLock by remember {
        mutableStateOf(BiometricManager.from(context).canAuthenticate(allowedAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS)
    }
    DisposableEffect(lifecycleOwner, context, allowedAuthenticators) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canUseAppLock = BiometricManager.from(context).canAuthenticate(allowedAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val coroutineScope = rememberCoroutineScope()
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var pendingBackupPassword by remember { mutableStateOf<CharArray?>(null) }
    var pendingRestorePassword by remember { mutableStateOf<CharArray?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val backupPassword = pendingBackupPassword
        pendingBackupPassword = null
        if (uri == null) {
            backupPassword?.fill('\u0000')
        } else if (backupPassword != null) {
            coroutineScope.launch {
                isBusy = true
                val success = viewModel.backupDatabase(uri, backupPassword)
                isBusy = false
                android.widget.Toast.makeText(
                    context,
                    if (success) "Backup terenkripsi berhasil disimpan." else "Backup gagal dibuat.",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val restorePassword = pendingRestorePassword
        pendingRestorePassword = null
        if (uri == null) {
            restorePassword?.fill('\u0000')
        } else if (restorePassword != null) {
            coroutineScope.launch {
                isBusy = true
                val success = viewModel.restoreDatabase(uri, restorePassword)
                isBusy = false
                android.widget.Toast.makeText(
                    context,
                    if (success) "Data berhasil dipulihkan." else "Restore gagal. Kata sandi atau file mungkin tidak valid.",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        Text("Kunci Aplikasi (App Lock)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (canUseAppLock) "Minta biometrik atau PIN setiap kali aplikasi dibuka kembali."
                            else "Aktifkan biometrik atau kunci layar perangkat sebelum menggunakan kunci aplikasi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (canUseAppLock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        )
                    }
                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled || canUseAppLock) viewModel.setAppLockEnabled(enabled)
                        },
                        enabled = isAppLockEnabled || canUseAppLock,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                Column {
                    Text(
                        text = "Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    )
                    DataAction(
                        title = "Cadangkan Data",
                        description = "Buat backup terenkripsi dengan kata sandi. Backup lama yang belum terenkripsi tidak dapat dipulihkan di sini.",
                        enabled = !isBusy,
                        onClick = {
                            password = ""
                            passwordError = false
                            showBackupPasswordDialog = true
                        },
                    )
                    DataAction(
                        title = "Pulihkan Data",
                        description = "Restore hanya menerima arsip DompetKu terenkripsi yang valid.",
                        enabled = !isBusy,
                        onClick = {
                            password = ""
                            passwordError = false
                            showRestorePasswordDialog = true
                        },
                    )
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
                    }
                }
            }
        }
    }

    if (showBackupPasswordDialog || showRestorePasswordDialog) {
        val isBackup = showBackupPasswordDialog
        AlertDialog(
            onDismissRequest = {
                showBackupPasswordDialog = false
                showRestorePasswordDialog = false
                password = ""
                passwordError = false
            },
            title = { Text(if (isBackup) "Kata sandi backup" else "Kata sandi backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isBackup) "Gunakan minimal 12 karakter. Backup dienkripsi; kata sandi yang hilang tidak dapat dipulihkan."
                        else "Masukkan kata sandi yang digunakan saat membuat backup."
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it.take(128); passwordError = false },
                        label = { Text("Kata sandi") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError,
                    )
                    if (passwordError) Text("Kata sandi backup minimal 12 karakter.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (password.length < 12) {
                            passwordError = true
                        } else {
                            val secret = password.toCharArray()
                            password = ""
                            passwordError = false
                            showBackupPasswordDialog = false
                            showRestorePasswordDialog = false
                            if (isBackup) {
                                pendingBackupPassword = secret
                                val dateFormat = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                                backupLauncher.launch("dompetku_backup_${dateFormat.format(java.util.Date())}.zip")
                            } else {
                                pendingRestorePassword = secret
                                restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            }
                        }
                    },
                ) { Text(if (isBackup) "Pilih lokasi" else "Pilih file") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupPasswordDialog = false
                    showRestorePasswordDialog = false
                    password = ""
                    passwordError = false
                }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun DataAction(
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
