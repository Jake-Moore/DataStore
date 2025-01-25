package com.kamikazejam.datastore.base.data

import com.kamikazejam.datastore.base.data.impl.StoreDataLong
import com.kamikazejam.datastore.base.data.impl.StoreDataString
import com.kamikazejam.datastore.base.data.impl.StoreDataUUID
import java.util.*

class Wrapper {
    companion object {
        operator fun invoke(value: String): StoreDataString {
            return StoreDataString(value)
        }
        operator fun invoke(value: UUID): StoreDataUUID {
            return StoreDataUUID(value)
        }
        operator fun invoke(value: Long): StoreDataLong {
            return StoreDataLong(value)
        }
        operator fun invoke(value: Any): Any {
            return when (value) {
                is String -> StoreDataString(value)
                is UUID -> StoreDataUUID(value)
                is Long -> StoreDataLong(value)
                else -> throw IllegalArgumentException("Unsupported type: ${value::class.java}")
            }
        }
    }
}