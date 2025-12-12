package dev.fusionize.user.activity;

import dev.fusionize.common.utility.TextUtil;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Objects;

public class DomainEntity extends AuditableEntity {
    private String name;
    @Indexed(unique = true)
    private String domain;
    private String cover;
    private String key;

    protected void load(DomainEntity entity){
        this.cover = entity.getCover();
        this.domain = entity.getDomain();
        this.name = entity.getName();
        this.key = entity.getKey();
    }

    public static class Builder<T extends Builder<T>> {
        protected String name;
        protected String domain;
        protected String cover;
        protected String key;

        protected final String parentDomain;

        protected Builder(String parentDomain) {
            this.parentDomain = parentDomain;
        }

        protected Builder(String parentDomain, String key) {
            this.parentDomain = parentDomain;
            this.key = key;
        }

        public T withName(String name) {
            this.name = name;
            if (domain == null)
                this.domain = TextUtil.kebabCase(this.name);
            return self();
        }

        public T withDomain(String domain) {
            this.domain = domain;
            return self();
        }

        public T withCover(String cover) {
            this.cover = cover;
            return self();
        }

        public T withKey(String key) {
            this.key = key;
            return self();
        }

        public DomainEntity build() {
            DomainEntity domainEntity = new DomainEntity();
            domainEntity.setName(this.name);
            if(parentDomain!=null && !parentDomain.isEmpty()){
                domainEntity.setDomain(this.parentDomain + "."+this.domain);
            }else{
                domainEntity.setDomain(this.domain);
            }
            domainEntity.setDomain(domainEntity.getDomain().toLowerCase());
            domainEntity.setCover(this.cover);
            domainEntity.setKey(this.key);
            return domainEntity;
        }

        @SuppressWarnings("unchecked")
        protected final T self(){
            return (T) this;
        }

    }

    public static String parent(String domain){
        String[] parts = domain.split("\\.");
        if(parts.length!=2){
            return null;
        }
        return parts[0];
    }

    public static String parent(DomainEntity entity){
        if(entity == null || entity.getDomain()==null){
            return null;
        }
        return parent(entity.getDomain());
    }

    public static String self(String domain){
        String[] parts = domain.split("\\.");
        if(parts.length==1){
            return parts[0];
        }
        return parts[1];
    }
    public static String self(DomainEntity entity){
        if(entity == null || entity.getDomain()==null){
            return null;
        }
        return self(entity.getDomain());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEntity that = (DomainEntity) o;
        return Objects.equals(name, that.name)
                && Objects.equals(domain, that.domain)
                && Objects.equals(cover, that.cover)
                && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, domain, cover, key);
    }

    @Override
    public String toString() {
        return "DomainEntity{" +
                "name='" + name + '\'' +
                ", domain='" + domain + '\'' +
                ", cover='" + cover + '\'' +
                ", key='" + key + '\'' +
                "} " + super.toString();
    }
}
