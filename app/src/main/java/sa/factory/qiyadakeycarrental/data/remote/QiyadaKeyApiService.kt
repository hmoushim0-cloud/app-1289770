package sa.factory.qiyadakeycarrental.data.remote

import sa.factory.qiyadakeycarrental.model.QiyadaKeyItem
import sa.factory.qiyadakeycarrental.model.QiyadaKeyOrder

interface QiyadaKeyApiService {
    suspend fun fetchCatalog(): List<QiyadaKeyItem>
    suspend fun submitOrder(order: QiyadaKeyOrder): Boolean
}
