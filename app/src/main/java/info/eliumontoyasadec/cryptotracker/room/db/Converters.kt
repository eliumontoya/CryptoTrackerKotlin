package info.eliumontoyasadec.cryptotracker.room.db

import androidx.room.TypeConverter
import info.eliumontoyasadec.cryptotracker.domain.model.MovementType

class Converters {

    @TypeConverter
    fun fromTransactionType(value: MovementType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): MovementType? {
        return value?.let { MovementType.valueOf(it) }
    }
}