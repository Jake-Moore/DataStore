package com.kamikazejam.datastore.example.framework;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonUtil {
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure visibility
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        
        // Configure features
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Register custom module for FieldWrapper
        SimpleModule module = new SimpleModule();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<FieldWrapper<?>> wrapperClass = (Class) FieldWrapper.class;
        module.addSerializer(wrapperClass, new FieldWrapper.FieldWrapperSerializer());
        module.addDeserializer(wrapperClass, new FieldWrapper.FieldWrapperDeserializer<>());
        mapper.registerModule(module);
        
        return mapper;
    }
    
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
} 