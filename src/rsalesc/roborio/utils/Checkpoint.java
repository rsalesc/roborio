package rsalesc.roborio.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roberto Sales on 12/08/17.
 */
public class Checkpoint {
    private static final Checkpoint SINGLETON = new Checkpoint();

    private HashMap<String, Integer> hs;

    public static Checkpoint getInstance() {
        return SINGLETON;
    }

    private Checkpoint() {
        hs = new HashMap<>();
    }

    public void enter(String cp) {
        if(!hs.containsKey(cp)) {
            hs.put(cp, 0);
        }

        hs.put(cp, hs.get(cp) + 1);
    }

    public void leave(String cp) {
        if(!hs.containsKey(cp)) {
            hs.put(cp, 0);
        }

        hs.put(cp, hs.get(cp) - 1);
    }

    public int get(String cp) {
        if(!hs.containsKey(cp)) {
            hs.put(cp, 0);
        }

        return hs.get(cp);
    }

    public void dump() {
        for(Map.Entry<String, Integer> entry : hs.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }
}
