package dev.fusionize.project;

import java.util.HashMap;
import java.util.Map;

public class ProjectPermission {
    public enum Access{
        OWNER("OWNER"),
        ADMIN("ADMIN"),
        DEVELOPER("DEVELOPER"),
        READONLY("READONLY");
        private final String name;
        Access(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        private static final Map<String, Access> lookup = new HashMap<>();

        public boolean check(Access reference){
            return this.ordinal() <= reference.ordinal();
        }

        static{
            for(Access access : Access.values())
                lookup.put(access.toString(), access);
        }
        public static Access get(String name){
            return lookup.get(name);
        }

    }
    private String id;
    private Access access;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Access getAccess() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
    }
}
