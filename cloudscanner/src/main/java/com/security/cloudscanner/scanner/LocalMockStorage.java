package com.security.cloudscanner.scanner;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalMockStorage {
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    public void put(String key, byte[] data) {
        storage.put(key, data);
    }

    public byte[] get(String key) {
        return storage.get(key);
    }

    public boolean contains(String key) {
        return storage.containsKey(key);
    }

    public Set<String> keys() {
        return storage.keySet();
    }
}
