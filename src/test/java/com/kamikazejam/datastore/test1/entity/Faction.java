package com.kamikazejam.datastore.test1.entity;

import com.kamikazejam.datastore.base.field.FieldWrapper;
import com.kamikazejam.datastore.mode.object.StoreObject;
import com.kamikazejam.datastore.test1.entity.obj.DataClass;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public class Faction extends StoreObject {
    public final FieldWrapper<String> name = FieldWrapper.of("name", null, String.class);
    public final FieldWrapper<Integer> age = FieldWrapper.of("age", 0, Integer.class);
    public final FieldWrapper<String> email = FieldWrapper.of("email", null, String.class);
    public final FieldWrapper<DataClass> data = FieldWrapper.of("data", null, DataClass.class);
    public final FieldWrapper<List<String>> list = FieldWrapper.ofColl("list", new ArrayList<>(), List.class);
    public final FieldWrapper<Map<String, Integer>> map = FieldWrapper.ofMap("map", new HashMap<>(), Map.class);

    public Faction() {}

    @Override
    public @NotNull Set<FieldWrapper<?>> getCustomFields() {
        return Set.of(name, age, email, data, list, map);
    }
}