package rsalesc.roborio.utils.colors;

import java.awt.*;

/**
 * Created by Roberto Sales on 24/07/17.
 */
public abstract class ColorUtils {
    public static Color interpolateRGB(Color a, Color b, double alpha) {
        float[] aComp = a.getRGBColorComponents(null);
        float[] bComp = b.getRGBColorComponents(null);
        float[] result = new float[aComp.length];

        for(int i = 0; i < aComp.length; i++) {
            result[i] = (float) (aComp[i] * (1.0 - alpha) + bComp[i] * alpha);
        }

        return new Color(result[0], result[1], result[2]);
    }
}
