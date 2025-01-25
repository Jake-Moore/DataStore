package com.kamikazejam.datastore.base.store

import com.kamikazejam.datastore.base.Store

interface StoreInstantiator<K : Any, X : Store<X, K>> {
    fun instantiate(): X
}
