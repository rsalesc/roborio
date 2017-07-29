package roborio.utils.storage;

import java.util.HashMap;

/**
 * Created by Roberto Sales on 28/07/17.
 */
public class NamedStorage {
    private static final NamedStorage SINGLETON = new NamedStorage();

    HashMap<String, Object> store;
    private NamedStorage() {
        store = new HashMap<>();
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

    public void add(String key, Object payload) {
        store.put(key, payload);
    }
}
