package com.kamikazejam.datastore.test2.entity;

import com.kamikazejam.datastore.DataStoreRegistration;
import com.kamikazejam.datastore.base.index.IndexedField;
import com.kamikazejam.datastore.mode.object.StoreObjectCache;
import com.kamikazejam.datastore.test2.entity.index.BalanceIndex;
import com.kamikazejam.datastore.test2.entity.index.NameIndex;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class RandomObjectColl extends StoreObjectCache<RandomObject> {
    // ----------------------------------------------------- //
    //                      Singleton                        //
    // ----------------------------------------------------- //
    private static RandomObjectColl instance;
    public static RandomObjectColl get() { return instance; }

    // DataStore requires a constructor with a DataStoreRegistration
    private final IndexedField<RandomObject, String> nameField;
    private final IndexedField<RandomObject, Double> balanceField;

    private RandomObjectColl(DataStoreRegistration registration) {
        super(registration, RandomObject::new, "RandomObjects", RandomObject.class);
        instance = this;

        nameField = this.registerIndex(new NameIndex(this, "name"));
        balanceField = this.registerIndex(new BalanceIndex(this, "balance"));
    }

    public static @Nullable RandomObject getByName(@Nullable String name) {
        if (name == null) return null;
        return instance.getByIndex(instance.nameField, name).orElse(null);
    }

    public static @Nullable RandomObject getByBalance(@Nullable Double balance) {
        if (balance == null) return null;
        return instance.getByIndex(instance.balanceField, balance).orElse(null);
    }
}
