package agents.firstLast.bt;

import java.util.HashMap;
import java.util.Map;

// shared memory for the whole tree
// global, no context, tasks read and write by name
public class Blackboard {
    private final Map<String, Object> store = new HashMap<>();

    public void put(String key, Object value) {
        store.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) store.get(key);
    }

    public boolean has(String key) {
        return store.get(key) != null;
    }

    public void remove(String key) {
        store.remove(key);
    }

    // small typed helpers so tasks stay short
    public int getInt(String key, int fallback) {
        Object v = store.get(key);
        return v instanceof Number ? ((Number) v).intValue() : fallback;
    }

    public double getDouble(String key, double fallback) {
        Object v = store.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : fallback;
    }

    public boolean getBool(String key) {
        Object v = store.get(key);
        return v instanceof Boolean && (Boolean) v;
    }
}
