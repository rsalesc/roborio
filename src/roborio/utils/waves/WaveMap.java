package roborio.utils.waves;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Roberto Sales on 26/07/17.
 */
public class WaveMap<T> extends WaveCollection {
    HashMap<Wave, T> map;

    public WaveMap() {
        super();
        map = new HashMap<>();
    }

    @Override
    public void add(Wave wave) {
        super.add(wave);
        map.put(wave, null);
    }

    public void add(Wave wave, T data) {
        super.add(wave);
        map.put(wave, data);
    }

    public void change(Wave wave, T data) {
        if(!map.containsKey(wave))
            throw new IllegalStateException();
        map.put(wave, data);
    }

    @Override
    protected void removeFromIterator(Wave wave, Iterator<Wave> iterator) {
        super.removeFromIterator(wave, iterator);
        map.remove(wave);
    }

    public T getData(Wave wave) {
        if(!map.containsKey(wave))
            throw new IllegalStateException();
        return map.get(wave);
    }
}
