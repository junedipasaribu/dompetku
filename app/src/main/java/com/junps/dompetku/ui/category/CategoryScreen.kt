package com.junps.dompetku.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junps.dompetku.ui.components.CategoryColorOptions
import com.junps.dompetku.ui.components.CategoryIconOptions
import com.junps.dompetku.ui.components.categoryColor
import com.junps.dompetku.ui.components.categoryColorLabel
import com.junps.dompetku.ui.components.categoryIcon
import com.junps.dompetku.ui.components.categoryIconLabel
import com.junps.dompetku.ui.components.LoadingScreen
import com.junps.dompetku.ui.components.MessageScreen
import com.junps.dompetku.domain.model.Category
import com.junps.dompetku.domain.model.TransactionType
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CategoryScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                CategoryEvent.Saved -> {
                    showEditor = false
                    snackbarHostState.showSnackbar("Kategori berhasil disimpan.")
                }
                CategoryEvent.Deleted -> {
                    deletingCategory = null
                    snackbarHostState.showSnackbar("Kategori berhasil dihapus.")
                }
                is CategoryEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    showEditor = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah kategori")
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingScreen("Memuat kategori", Modifier.padding(innerPadding))
            uiState.loadError != null -> CategoryError(uiState.loadError.orEmpty(), Modifier.padding(innerPadding))
            uiState.categories.isEmpty() -> EmptyCategories(Modifier.padding(innerPadding))
            else -> CategoryList(
                categories = uiState.categories,
                onEdit = { category ->
                    editingCategory = category
                    showEditor = true
                },
                onDelete = { deletingCategory = it },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showEditor) {
        CategoryEditorDialog(
            category = editingCategory,
            onDismiss = { showEditor = false },
            onSave = viewModel::save,
        )
    }

    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Hapus kategori?") },
            text = { Text("Kategori “${category.name}” akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(category.id) }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun CategoryList(
    categories: List<Category>,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Kategori", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Atur kategori pemasukan dan pengeluaran Anda.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        categorySection(
            title = "Pemasukan",
            categories = categories.filter { it.type == TransactionType.INCOME },
            onEdit = onEdit,
            onDelete = onDelete,
        )
        categorySection(
            title = "Pengeluaran",
            categories = categories.filter { it.type == TransactionType.EXPENSE },
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.categorySection(
    title: String,
    categories: List<Category>,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    item {
        Text(
            text = title,
            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
    items(categories, key = { it.id }) { category ->
        CategoryRow(category, onEdit, onDelete)
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = categoryColor(category.colorCode).copy(alpha = .14f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon(category.iconName),
                        contentDescription = null,
                        tint = categoryColor(category.colorCode),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(category.name, fontWeight = FontWeight.SemiBold)
                Text(
                    if (category.isDefault) "Kategori bawaan" else "Kategori kustom",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onEdit(category) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${category.name}")
            }
            if (!category.isDefault) {
                IconButton(onClick = { onDelete(category) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus ${category.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditorDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (String?, String, TransactionType, String, String) -> Unit,
) {
    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var type by remember(category?.id) { mutableStateOf(category?.type ?: TransactionType.EXPENSE) }
    var iconName by remember(category?.id) { mutableStateOf(category?.iconName ?: CategoryIconOptions.first()) }
    var colorCode by remember(category?.id) { mutableStateOf(category?.colorCode ?: CategoryColorOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Tambah kategori" else "Edit kategori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama kategori") },
                    singleLine = true,
                    supportingText = { Text("${name.length}/40") },
                )
                if (category == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == TransactionType.EXPENSE,
                            onClick = { type = TransactionType.EXPENSE },
                            label = { Text("Pengeluaran") },
                        )
                        FilterChip(
                            selected = type == TransactionType.INCOME,
                            onClick = { type = TransactionType.INCOME },
                            label = { Text("Pemasukan") },
                        )
                    }
                }
                Text("Ikon", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(CategoryIconOptions) { option ->
                        FilterChip(
                            selected = iconName == option,
                            onClick = { iconName = option },
                            label = {
                                Icon(
                                    categoryIcon(option),
                                    contentDescription = categoryIconLabel(option),
                                )
                            },
                        )
                    }
                }
                Text("Warna", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryColorOptions.forEach { option ->
                        IconButton(
                            onClick = { colorCode = option },
                            modifier = Modifier.semantics {
                                contentDescription = "Pilih warna ${categoryColorLabel(option)}"
                                selected = colorCode == option
                            },
                        ) {
                            Spacer(
                                Modifier
                                    .size(if (colorCode == option) 30.dp else 24.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor(option)),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(category?.id, name, type, iconName, colorCode) },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun CategoryError(message: String, modifier: Modifier = Modifier) {
    MessageScreen(
        title = "Kategori tidak dapat dimuat",
        message = message,
        icon = Icons.Default.Category,
        modifier = modifier,
    )
}

@Composable
private fun EmptyCategories(modifier: Modifier = Modifier) {
    MessageScreen(
        title = "Belum ada kategori",
        message = "Tekan tombol tambah untuk membuat kategori pertama.",
        icon = Icons.Default.Category,
        isError = false,
        modifier = modifier,
    )
}
