package dev.fusionize.storage;

import java.util.HashMap;
import java.util.Map;

public enum StorageType {
    FILE_STORAGE("FILE_STORAGE"),
    VECTOR_STORAGE("VECTOR_STORAGE");

    private final String name;

    StorageType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final Map<String, StorageType> lookup = new HashMap<>();

    static{
        for(StorageType type : StorageType.values())
            lookup.put(type.getName(), type);
    }
    public static StorageType get(String name){
        return lookup.get(name);
    }
}
