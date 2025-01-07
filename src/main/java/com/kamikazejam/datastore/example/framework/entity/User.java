package com.kamikazejam.datastore.example.framework.entity;

import com.kamikazejam.datastore.example.framework.BaseDocument;
import com.kamikazejam.datastore.example.framework.FieldWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class User extends BaseDocument<User> {
    public final FieldWrapper<String> name = new FieldWrapper<>("name", null, String.class);
    public final FieldWrapper<Integer> age = new FieldWrapper<>("age", 0, Integer.class);
    public final FieldWrapper<String> email = new FieldWrapper<>("email", null, String.class);

    public User() {}

    @Override
    protected @NotNull Set<FieldWrapper<?>> getCustomFields() {
        return Set.of(name, age, email);
    }
}