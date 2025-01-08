package com.kamikazejam.datastore.test.entity;

import com.kamikazejam.datastore.framework.BaseDocument;
import com.kamikazejam.datastore.framework.FieldWrapper;
import com.kamikazejam.datastore.test.entity.obj.DataClass;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class User extends BaseDocument<User> {
    public final FieldWrapper<String> name = FieldWrapper.of("name", null, String.class);
    public final FieldWrapper<Integer> age = FieldWrapper.of("age", 0, Integer.class);
    public final FieldWrapper<String> email = FieldWrapper.of("email", null, String.class);
    public final FieldWrapper<DataClass> data = FieldWrapper.of("data", null, DataClass.class);
    public final FieldWrapper<List<String>> list = FieldWrapper.ofColl("list", new ArrayList<>(), List.class);
    public final FieldWrapper<Map<String, Integer>> map = FieldWrapper.ofMap("map", new HashMap<>(), Map.class);

    public User() {}

    @Override
    protected @NotNull Set<FieldWrapper<?>> getCustomFields() {
        return Set.of(name, age, email, data, list, map);
    }
}