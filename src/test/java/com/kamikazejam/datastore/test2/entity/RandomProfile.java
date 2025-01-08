package com.kamikazejam.datastore.test2.entity;

import com.kamikazejam.datastore.base.field.FieldWrapper;
import com.kamikazejam.datastore.mode.profile.StoreProfile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Setter @Getter
public class RandomProfile extends StoreProfile {
    // ----------------------------------------------------- //
    //                        Fields                         //
    // ----------------------------------------------------- //
    public final FieldWrapper<Double> balance = FieldWrapper.of("balance", 0D, Double.class);
    public final FieldWrapper<List<String>> list = FieldWrapper.ofColl("myList", new ArrayList<>(), List.class);

    // For Jackson
    public RandomProfile() {}

    @Override
    public @NotNull Set<FieldWrapper<?>> getCustomFields() {
        return Set.of(balance, list);
    }
}
