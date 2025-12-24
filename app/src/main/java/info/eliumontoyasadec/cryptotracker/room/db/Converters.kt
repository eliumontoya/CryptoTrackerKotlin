package info.eliumontoyasadec.cryptotracker.room.db

import androidx.room.TypeConverter
import info.eliumontoyasadec.cryptotracker.room.entities.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { TransactionType.valueOf(it) }
    }
}