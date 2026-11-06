package sa.factory.qiyadakeycarrental.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.qiyadakeycarrental.domain.usecase.*
import sa.factory.qiyadakeycarrental.model.QiyadaKeyItem
import sa.factory.qiyadakeycarrental.model.QiyadaKeyOrder
import sa.factory.qiyadakeycarrental.model.QiyadaKeyTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class QiyadaKeyUiState(
    val catalog: List<QiyadaKeyItem> = emptyList(),
    val filteredCatalog: List<QiyadaKeyItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<QiyadaKeyItem> = emptyList(),
    val telemetry: QiyadaKeyTelemetry? = null,
    val lastOrder: QiyadaKeyOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class QiyadaKeyViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiyadaKeyUiState())
    val uiState: StateFlow<QiyadaKeyUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: QiyadaKeyItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: QiyadaKeyItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
