package com.kamikazejam.datastore.connections.storage.mongo

import com.kamikazejam.datastore.store.Store

internal class TransactionResult<K : Any, X : Store<X, K>>(val store: X?, val dbStore: X? = null)