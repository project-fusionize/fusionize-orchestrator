package dev.fusionize.user.activity;

import java.util.HashMap;
import java.util.Map;

public enum ActivityType {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    ACTIVATE("ACTIVATE"),
    DEACTIVATE("DEACTIVATE"),
    PERMISSION_CHANGE("PERMISSION_CHANGE"),
    STATUS_CHANGE("STATUS_CHANGE"),
    ACCESS("ACCESS"),
    OTHER("OTHER");
    private final String name;
    ActivityType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    private static final Map<String, ActivityType> lookup = new HashMap<>();

    static{
        for(ActivityType type : ActivityType.values())
            lookup.put(type.toString(), type);
    }
    public static ActivityType get(String name){
        return lookup.get(name);
    }
}
