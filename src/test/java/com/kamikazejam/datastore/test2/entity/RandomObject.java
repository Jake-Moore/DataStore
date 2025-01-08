package com.kamikazejam.datastore.test2.entity;

import com.kamikazejam.datastore.base.field.FieldWrapper;
import com.kamikazejam.datastore.mode.object.StoreObject;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Setter @Getter
public class RandomObject extends StoreObject {
    // ----------------------------------------------------- //
    //                        Fields                         //
    // ----------------------------------------------------- //
    public final FieldWrapper<String> name = FieldWrapper.of("name", null, String.class);
    public final FieldWrapper<Double> balance = FieldWrapper.of("balance", 0D, Double.class);
    public final FieldWrapper<List<String>> list = FieldWrapper.ofColl("myList", new ArrayList<>(), List.class);

    // For Jackson
    public RandomObject() {}

    @Override
    public @NotNull Set<FieldWrapper<?>> getCustomFields() {
        return Set.of(name, balance, list);
    }
}
