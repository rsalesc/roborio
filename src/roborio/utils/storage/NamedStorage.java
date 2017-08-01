package roborio.utils.storage;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by Roberto Sales on 28/07/17.
 */
public class NamedStorage {
    private static final NamedStorage SINGLETON = new NamedStorage();
    private static final int SIZE_LIMIT = 12;

    ConcurrentHashMap<String, Object> store;
    Queue<String> keys;

    private NamedStorage() {
        store = new ConcurrentHashMap<>();
        keys = new ConcurrentLinkedDeque<>();
    }

    public static NamedStorage getInstance() {
        return SINGLETON;
    }

    public Object get(String key) {
        return store.get(key);
    }

    public boolean contains(String key) {
        return store.containsKey(key);
    }

    private void ensureSize() {
        while(keys.size() > SIZE_LIMIT) {
            String polled = keys.poll();
            if(store.containsKey(polled))
                store.remove(polled);
        }
    }

    public void add(String key, Object payload) {
        ensureSize();
        store.put(key, payload);
        keys.add(key);
    }

    public void remove(String key) {
        store.remove(key);
    }

    public void clear() {
        store.clear();
    }
}
