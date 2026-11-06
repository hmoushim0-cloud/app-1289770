package sa.factory.qiyadakeycarrental.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.qiyadakeycarrental.model.QiyadaKeyItem

@Entity(tableName = "car_rental_items_cache")
data class QiyadaKeyEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): QiyadaKeyItem = QiyadaKeyItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
