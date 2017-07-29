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

    public T getData(Wave wave) {
        if(!map.containsKey(wave))
            throw new IllegalStateException();
        return map.get(wave);
    }

    @Override
    public void remove(Wave wave) {
        super.remove(wave);
        map.remove(wave);
    }

    @Override
    public Iterator<Wave> iterator() {
        return new Iterator<Wave>() {

            private Iterator<Wave> it = waves.iterator();
            private Wave current = null;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Wave next() {
                return current = it.next();
            }

            @Override
            public void remove() {
                if(current == null)
                    throw new IllegalStateException();

                map.remove(current);
                current = null;
                it.remove();
            }
        };
    }
}
