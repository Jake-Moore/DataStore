package com.kamikazejam.datastore.base.data

import com.kamikazejam.datastore.base.data.impl.StringData

class Wrapper {
    companion object {
        operator fun invoke(value: String): StringData {
            return StringData(value)
        }
    }
}