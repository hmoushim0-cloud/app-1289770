package sa.factory.qiyadakeycarrental.data.repository

import sa.factory.qiyadakeycarrental.domain.repository.QiyadaKeyRepository
import sa.factory.qiyadakeycarrental.hardware.HardwareSensorDriver
import sa.factory.qiyadakeycarrental.model.QiyadaKeyItem
import sa.factory.qiyadakeycarrental.model.QiyadaKeyOrder
import sa.factory.qiyadakeycarrental.model.QiyadaKeyTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QiyadaKeyRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : QiyadaKeyRepository {

    private val _catalog = MutableStateFlow<List<QiyadaKeyItem>>(listOf(
        QiyadaKeyItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع OBD-II CAN-Bus Telematics + BLE Digital Key Fob", 180.0, "معتمد", "الخدمات الرئيسية"),
        QiyadaKeyItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        QiyadaKeyItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<QiyadaKeyItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<QiyadaKeyItem>>(emptyList())
    override val activeCart: StateFlow<List<QiyadaKeyItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<QiyadaKeyTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            QiyadaKeyTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: QiyadaKeyItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: QiyadaKeyItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): QiyadaKeyOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = QiyadaKeyOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
