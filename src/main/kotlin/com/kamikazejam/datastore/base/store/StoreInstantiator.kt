package com.kamikazejam.datastore.base.store

import com.kamikazejam.datastore.base.Store

interface StoreInstantiator<K, X : Store<X, K>> {
    fun instantiate(): X
}
