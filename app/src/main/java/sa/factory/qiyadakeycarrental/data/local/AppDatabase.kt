package sa.factory.qiyadakeycarrental.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [QiyadaKeyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun car_rentalDao(): QiyadaKeyDao
}
