package rsalesc.roborio.utils;

import rsalesc.roborio.utils.geo.G;
import rsalesc.roborio.utils.geo.Point;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by Roberto Sales on 30/08/17.
 */
public class KeyConfig {
    private static KeyConfig SINGLETON = null;

    TreeMap<Character, Boolean> map = new TreeMap<>();
    TreeSet<Character> meant = new TreeSet<>();
    private boolean defou = true;

    private KeyConfig() {}

    private KeyConfig(boolean defou) {
        this.defou = defou;
    }

    public static KeyConfig getInstance() {
        if(SINGLETON == null)
            SINGLETON = new KeyConfig();
        return SINGLETON;
    }

    public boolean get(KeyEvent e) {
        return get(e.getKeyChar());
    }

    public void toggle(KeyEvent e) {
        map.put(e.getKeyChar(), !map.getOrDefault(e.getKeyChar(), defou));
    }

    public boolean get(char c) {
        if(!meant.contains(c))
            meant.add(c);
        if(!map.containsKey(c))
            map.put(c, defou);
        return map.get(c);
    }

    public void onPaint(Graphics2D gr) {
        G g = new G(gr);

        final int baseY = 560;
        int baseX = 775;
        final int step = 13;

        for(Character ch : meant) {
            g.pushColor(get(ch) ? Color.GREEN : Color.GRAY);
            g.drawString(new Point(baseX, baseY), String.valueOf(ch).toUpperCase());
            g.popColor();

            baseX -= step;
        }
    }
}
