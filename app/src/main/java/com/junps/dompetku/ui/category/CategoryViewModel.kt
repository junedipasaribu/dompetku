package com.junps.dompetku.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junps.dompetku.domain.model.Category
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.repository.CategoryRepository
import com.junps.dompetku.domain.usecase.DeleteCategoryUseCase
import com.junps.dompetku.domain.usecase.SaveCategoryRequest
import com.junps.dompetku.domain.usecase.SaveCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val loadError: String? = null,
)

sealed interface CategoryEvent {
    data class ShowMessage(val message: String) : CategoryEvent
    data object Saved : CategoryEvent
    data object Deleted : CategoryEvent
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    repository: CategoryRepository,
    private val saveCategory: SaveCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
) : ViewModel() {
    val uiState: StateFlow<CategoryUiState> = repository.observeAll()
        .map<List<Category>, CategoryUiState> { categories ->
            CategoryUiState(isLoading = false, categories = categories)
        }
        .catch { error ->
            emit(CategoryUiState(isLoading = false, loadError = error.message ?: "Gagal memuat kategori."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryUiState(),
        )

    private val _events = MutableSharedFlow<CategoryEvent>()
    val events: SharedFlow<CategoryEvent> = _events

    fun save(
        id: String?,
        name: String,
        type: TransactionType,
        iconName: String,
        colorCode: String,
    ) {
        viewModelScope.launch {
            runCatching {
                saveCategory(SaveCategoryRequest(id, name, type, iconName, colorCode))
            }.onSuccess {
                _events.emit(CategoryEvent.Saved)
            }.onFailure { error ->
                _events.emit(CategoryEvent.ShowMessage(error.message ?: "Kategori gagal disimpan."))
            }
        }
    }

    fun delete(categoryId: String) {
        viewModelScope.launch {
            runCatching { deleteCategory(categoryId) }
                .onSuccess { _events.emit(CategoryEvent.Deleted) }
                .onFailure { error ->
                    _events.emit(CategoryEvent.ShowMessage(error.message ?: "Kategori gagal dihapus."))
                }
        }
    }
}
