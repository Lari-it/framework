package itu.webdynamique.framework;

import java.util.HashMap;
import java.util.Map;

public class Session {
    private String id;
    private Map<String, Object> attributes;
    private long createdAt;

    public Session(String id) {
        this.id = id;
        this.attributes = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isAuthenticated() {
        return attributes.containsKey("user");
    }

    @Override
    public String toString() {
        return "Session{" +
                "id='" + id + '\'' +
                ", attributes=" + attributes.size() +
                ", createdAt=" + createdAt +
                '}';
    }
}
