package com.kamikazejam.datastore.test2.entity;

import com.kamikazejam.datastore.DataStoreRegistration;
import com.kamikazejam.datastore.mode.profile.StoreProfileCache;

@SuppressWarnings("unused")
public class RandomProfileColl extends StoreProfileCache<RandomProfile> {
    // ----------------------------------------------------- //
    //                      Singleton                        //
    // ----------------------------------------------------- //
    private static RandomProfileColl instance;
    public static RandomProfileColl get() { return instance; }

    // DataStore requires a constructor with a DataStoreRegistration
    public RandomProfileColl(DataStoreRegistration registration) {
        super(registration, RandomProfile::new, "RandomProfiles", RandomProfile.class);
        instance = this;
    }
}
