package sa.factory.qiyadakeycarrental.domain.usecase

import sa.factory.qiyadakeycarrental.domain.repository.QiyadaKeyRepository
import sa.factory.qiyadakeycarrental.model.QiyadaKeyItem
import sa.factory.qiyadakeycarrental.model.QiyadaKeyOrder
import sa.factory.qiyadakeycarrental.model.QiyadaKeyTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: QiyadaKeyRepository) {
    operator fun invoke(): Flow<List<QiyadaKeyItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: QiyadaKeyRepository) {
    val cart = repository.activeCart
    fun add(item: QiyadaKeyItem) = repository.addToCart(item)
    fun remove(item: QiyadaKeyItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: QiyadaKeyRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): QiyadaKeyOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: QiyadaKeyRepository) {
    operator fun invoke(): Flow<QiyadaKeyTelemetry> = repository.telemetryFlow
}
