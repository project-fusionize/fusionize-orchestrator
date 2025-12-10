package dev.fusionize.workflow.context;

public class ContextResourceReference {
    private String storage;
    private String referenceKey;
    private String name;
    private String mime;
    private long size;

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getReferenceKey() {
        return referenceKey;
    }

    public void setReferenceKey(String referenceKey) {
        this.referenceKey = referenceKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String storage;
        private String referenceKey;
        private String name;
        private String mime;
        private long size;

        private Builder() {
        }

        public Builder withStorage(String storage) {
            this.storage = storage;
            return this;
        }

        public Builder withReferenceKey(String referenceKey) {
            this.referenceKey = referenceKey;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withMime(String mime) {
            this.mime = mime;
            return this;
        }

        public Builder withSize(long size) {
            this.size = size;
            return this;
        }

        public ContextResourceReference build() {
            ContextResourceReference contextResourceReference = new ContextResourceReference();
            contextResourceReference.setStorage(storage);
            contextResourceReference.setReferenceKey(referenceKey);
            contextResourceReference.setName(name);
            contextResourceReference.setMime(mime);
            contextResourceReference.setSize(size);
            return contextResourceReference;
        }
    }

    @Override
    public String toString() {
        return "FileStorageReference{" +
                "storage='" + storage + '\'' +
                ", referenceKey='" + referenceKey + '\'' +
                ", name='" + name + '\'' +
                ", mime='" + mime + '\'' +
                ", size=" + size +
                '}';
    }
}
