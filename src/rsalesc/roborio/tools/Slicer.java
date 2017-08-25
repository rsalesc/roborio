package rsalesc.roborio.tools;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by Roberto Sales on 23/08/17.
 */
public class Slicer {
    private static final String[] simpleNames =
            new String[]{"BFT", "LAT_VEL", "ADV_VEL", "ESCAPE", "ESCAPE", "ACCEL"};

    private static final String[] adaptiveNames =
            new String[]{"BFT", "LAT_VEL", "ADV_VEL", "ESCAPE", "ESCAPE", "ACCEL", "RUN", "D10"};

    private static final double[] simpleChances =
            new double[]{0.5, 0.8, 0.3, 0.15, 0.1, 0.5};

    private static final double[] adaptiveChances =
            new double[]{0.5, 0.8, 0.35, 0.8, 0.4, 0.7, 0.5, 0.5};

    private static final double[] flattenerChances =
            new double[]{0.3, 0.5, 0.3, 0.6, 0.3, 0.65, 0.75, 0.75};


    private static final int sliceCount = 60;
    private static final String[] names = adaptiveNames;
    private static final double[] chances = adaptiveChances;

    public static void main(String[] args) {
        String[][] slices = new String[sliceCount][names.length];

        for(int i = 0; i < sliceCount; i++) {
            for(int j = 0; j < names.length; j++) {
                double rnd = Math.random();
                if(rnd > chances[j])
                    slices[i][j] = getName(j, 0);
                else
                    slices[i][j] = getName(j, (int) (rnd / chances[j] * 2.999999999) + 1);
            }
        }

        Arrays.sort(slices, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                for(int i = 0; i < o1.length; i++) {
                    if(o1[i] != o2[i])
                        return -o1[i].compareToIgnoreCase(o2[i]);
                }

                return 0;
            }
        });
        printSlices(slices);
    }

    private static String getName(int i, int type) {
        if(type == 0)
            return "EMPTY";
        else if(type == 1)
            return names[i] + "_S";
        else if(type == 2)
            return names[i];
        else return names[i] + "_P";
    }

    static void printSlices(String[][] slices) {
        System.out.println("return new double[][][]{");
        for(int i = 0; i < slices.length; i++) {
            System.out.print("\t{");
            for(int j = 0; j < slices[i].length; j++) {
                if(j > 0)
                    System.out.print(", ");
                System.out.print(slices[i][j]);
            }
            System.out.println("}" + (i + 1 == slices.length ? "" : ","));
        }

        System.out.println("};");
    }
}
