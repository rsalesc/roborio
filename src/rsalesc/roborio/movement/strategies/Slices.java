package rsalesc.roborio.movement.strategies;

/**
 * Created by Roberto Sales on 23/08/17.
 */
public class Slices {
    public static double[] EMPTY = {Double.POSITIVE_INFINITY};

    public static double[] BFT_S = new double[]{20, 40, 60};
    public static double[] BFT = new double[]{10, 30, 50, 70};
    public static double[] BFT_P = new double[]{10, 20, 30, 40, 50, 60, 70};

    public static double[] LAT_VEL_S = new double[]{2, 4, 6};
    public static double[] LAT_VEL = new double[]{1, 3, 5, 7};
    public static double[] LAT_VEL_P = new double[]{1, 2, 3, 4, 5, 6, 7};

    public static double[] ADV_VEL_S = new double[]{-4, 0, +4};
    public static double[] ADV_VEL = new double[]{-6, -3, 0, +3, +6};
    public static double[] ADV_VEL_P = new double[]{-7, -5, -2, 0, +2, +5, +7};

    public static double[] ACCEL_S = new double[]{0.0};
    public static double[] ACCEL = new double[]{-0.5, +0.5};
    public static double[] ACCEL_P = new double[]{-0.5, +0.5};

    public static double[] ESCAPE_S = new double[]{200.0};
    public static double[] ESCAPE = new double[]{100, 220, 400};
    public static double[] ESCAPE_P = new double[]{60, 120, 180, 240, 300, 360, 400};

    public static double[] RUN_S = new double[]{0.33, 0.66};
    public static double[] RUN = new double[]{0.2, 0.4, 0.6, 0.8};
    public static double[] RUN_P = new double[]{0.05, 0.2, 0.35, 0.5, 0.65, 0.8, 0.95};

    public static double[] D10_S = new double[]{20, 40, 60};
    public static double[] D10 = new double[]{10, 20, 30, 40, 50, 60, 70};
    public static double[] D10_P = new double[]{10, 20, 30, 40, 50, 60, 70};
}
