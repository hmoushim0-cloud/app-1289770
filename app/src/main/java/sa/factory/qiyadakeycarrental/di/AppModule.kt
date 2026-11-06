package sa.factory.qiyadakeycarrental.di

import sa.factory.qiyadakeycarrental.data.repository.QiyadaKeyRepositoryImpl
import sa.factory.qiyadakeycarrental.domain.repository.QiyadaKeyRepository
import sa.factory.qiyadakeycarrental.domain.usecase.*
import sa.factory.qiyadakeycarrental.hardware.HardwareSensorDriver
import sa.factory.qiyadakeycarrental.ui.viewmodel.QiyadaKeyViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: QiyadaKeyRepository = QiyadaKeyRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): QiyadaKeyViewModel = QiyadaKeyViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
