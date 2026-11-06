package sa.factory.qiyadakeycarrental.domain.repository

import sa.factory.qiyadakeycarrental.model.QiyadaKeyItem
import sa.factory.qiyadakeycarrental.model.QiyadaKeyOrder
import sa.factory.qiyadakeycarrental.model.QiyadaKeyTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface QiyadaKeyRepository {
    val catalogFlow: Flow<List<QiyadaKeyItem>>
    val activeCart: StateFlow<List<QiyadaKeyItem>>
    val telemetryFlow: Flow<QiyadaKeyTelemetry>
    
    fun addToCart(item: QiyadaKeyItem)
    fun removeFromCart(item: QiyadaKeyItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): QiyadaKeyOrder
    suspend fun refreshCatalog()
}
