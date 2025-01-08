package com.kamikazejam.datastore.test1.entity.obj;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DataClass {
    private final @NotNull String name;
    private final int age;
    private final @NotNull String email;
    private final @NotNull Map<String, Date> map = new ConcurrentHashMap<>();

    @JsonCreator
    public DataClass(
        @JsonProperty("name") @NotNull String name,
        @JsonProperty("age") int age,
        @JsonProperty("email") @NotNull String email) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.map.put("now", new Date());
        this.map.put("yesterday", new Date(System.currentTimeMillis() - 86400000));
    }
}
