package rsalesc.roborio.utils;

import org.junit.jupiter.api.Test;

/**
 * Created by Roberto Sales on 29/07/17.
 */
class RTest {
    private static final int ITERATIONS = 10000000;

    private static class TrigData {
        double sin, cos, atan2;

        public double compare(TrigData rhs) {
            return Math.abs(sin - rhs.sin) + Math.abs(cos - rhs.cos)
                    + Math.abs(atan2 - rhs.atan2);
        }
    }

    TrigData[] dumb(double[] angles, double[] x, double[] y) {
        TrigData[] res = new TrigData[ITERATIONS];
        for(int i = 0; i < ITERATIONS; i++)
            res[i] = new TrigData();

        long init = System.nanoTime();
        for(int i = 0; i < ITERATIONS; i++) {
            res[i].sin = Math.sin(angles[i]);
        }
        long end = System.nanoTime();
        double took = (end - init) / 1e6;

        System.out.println("Math sin: " + took + " ms");

        init = System.nanoTime();

        for(int i = 0; i < ITERATIONS; i++) {
            res[i].cos = Math.cos(angles[i]);
        }

        end = System.nanoTime();
        took = (end - init) / 1e6;
        System.out.println("Math cos: " + took + " ms");

        init = System.nanoTime();
        for(int i = 0; i < ITERATIONS; i++) {
            res[i].atan2 = Math.atan2(y[i], x[i]);
        }

        end = System.nanoTime();
        took = (end - init) / 1e6;

        System.out.println("Math atan2: " + took + " ms");


        return res;
    }

    TrigData[] smart(double[] angles, double[] x, double[] y) {
        TrigData[] res = new TrigData[ITERATIONS];
        for(int i = 0; i < ITERATIONS; i++)
            res[i] = new TrigData();

        long init = System.nanoTime();
        for(int i = 0; i < ITERATIONS; i++) {
            res[i].sin = R.sin(angles[i]);
        }
        long end = System.nanoTime();
        double took = (end - init) / 1e6;

        System.out.println("R sin: " + took + " ms");

        init = System.nanoTime();

        for(int i = 0; i < ITERATIONS; i++) {
            res[i].cos = R.cos(angles[i]);
        }

        end = System.nanoTime();
        took = (end - init) / 1e6;
        System.out.println("R cos: " + took + " ms");

        init = System.nanoTime();
        for(int i = 0; i < ITERATIONS; i++) {
            res[i].atan2 = R.atan2(y[i], x[i]);
        }

        end = System.nanoTime();
        took = (end - init) / 1e6;

        System.out.println("R atan2: " + took + " ms");


        return res;
    }

    @Test
    void trigonometry() {
        double[] angles = new double[ITERATIONS];
        double[] x = new double[ITERATIONS];
        double[] y = new double[ITERATIONS];

        for(int i = 0; i < ITERATIONS; i++) {
            angles[i] = (Math.random() * 2 - 1) * R.PI + R.PI * (int)(Math.random() * 20);
            x[i] = Math.random();
            y[i] = Math.random();
        }

        TrigData[] dumbass = dumb(angles, x, y);
        TrigData[] smartass = smart(angles, x, y);

        double worstError = 0;
        double avgError = 0;
        for(int i = 0; i < ITERATIONS; i++) {
            worstError = Math.max(worstError, dumbass[i].compare(smartass[i]));
            avgError += Math.abs(dumbass[i].compare(smartass[i]));
        }

        System.out.println("Avg error: " + (avgError / ITERATIONS));
        System.out.println("Worst error: " + worstError);
    }

    private static final int ITERATIONS_EXP = 5000000;

    private double[] dumbExp(double[] values) {
        double[] res = new double[values.length];

        long         init = System.nanoTime();

        for(int i = 0; i < values.length; i++) {
            res[i] = Math.exp(values[i]);
        }

        long end = System.nanoTime();
        double took = (end - init) / 1e6;

        System.out.println("Math exp: " + took + " ms");

        return res;
    }

    private double[] smartExp(double[] values) {
        double[] res = new double[values.length];

        long         init = System.nanoTime();

        for(int i = 0; i < values.length; i++) {
            res[i] = R.exp(values[i]);
        }

        long end = System.nanoTime();
        double took = (end - init) / 1e6;

        System.out.println("R exp: " + took + " ms");

        return res;
    }

    @Test
    void exp() {
        double[] values = new double[ITERATIONS_EXP];
        for(int i = 0; i < ITERATIONS_EXP; i++) {
            values[i] = Math.random() * 100;
        }

        double[] dumbass = dumbExp(values);
        double[] smartass = smartExp(values);

        double worstError = 0;
        double avgError = 0;
        for(int i = 0; i < ITERATIONS_EXP; i++) {
            worstError = Math.max(worstError, Math.abs(dumbass[i] - smartass[i]));
            avgError += Math.abs(Math.abs(dumbass[i] - smartass[i]));
        }

        System.out.println("Avg error: " + (avgError / ITERATIONS_EXP));
        System.out.println("Worst error: " + worstError);

        System.out.println(R.exp(0));
    }
}