package roborio.structures;

import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.PriorityQueue;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by Roberto Sales on 20/07/17.
 */
public class KdTreeTest {
    private static int SIZE_LIMIT = 10000;
    private double[][] points;
    private double[][] queries;
    private int dimension;
    private int pointsCount;
    private int queriesCount;
    private int K;
    private boolean hasDump;

    void dump(double[] point) {
        if(!hasDump) return;
        for(int i = 0; i < point.length; i++)
            System.out.print(point[i] + " ");
        System.out.println("");
    }

    double randomWithRange(double min, double max)
    {
        double range = (max - min);
        return (Math.random() * range) + min;
    }

    double random() {
        return randomWithRange(-100, 100);
    }

    double[] generatePoint(int dimension) {
        double[] res = new double[dimension];
        for(int i = 0; i < dimension; i++)
            res[i] = random();
        return res;
    }

    @BeforeEach
    public void setup() {
        hasDump = false;
        dimension = 4;
        K = 8;
        pointsCount = 30000;
        queriesCount = 1000;

        points = new double[pointsCount][];
        for(int i = 0; i < pointsCount; i++) {
            points[i] = generatePoint(dimension);
        }

        queries = new double[queriesCount][];
        for(int i = 0; i < queriesCount; i++) {
            queries[i] = generatePoint(dimension);
        }
    }

    double[][] withTree() {
        System.out.println("Running with tree");
        EuclideanKdTree<Integer> tree = new EuclideanKdTree<>(dimension, SIZE_LIMIT);
        for(int i = 0; i < pointsCount; i++) {
            tree.add(points[i], null);
        }

        double[][] res = new double[queriesCount][];
        for(int i = 0; i < queriesCount; i++) {
            List<KdTree.Entry<Integer>> entries = tree.kNN(queries[i], this.K);
            int actualK = Math.min(this.K, entries.size());

            res[i] = new double[actualK];

            int j = actualK;
            for(KdTree.Entry<Integer> entry : entries) {
                res[i][--j] = entry.distance;
            }

            dump(res[i]);
        }

        return res;
    }

    double dist(double[] p1, double[] p2) {
        double res = 0;
        for(int i = 0; i < dimension; i++) {
            res += (p1[i]-p2[i])*(p1[i]-p2[i]);
        }
        return res;
    }

    double[][] withoutTree() {
        System.out.println("Running without tree");
        double[][] res = new double[queriesCount][];
        for(int i = 0; i < queriesCount; i++) {
            PriorityQueue<Double> pq = new PriorityQueue<Double>();
            for(int j = Math.max(pointsCount - SIZE_LIMIT, 0); j < pointsCount; j++) {
                pq.add(dist(points[j], queries[i]));
            }

            int actualK = Math.min(this.K, pq.size());
            res[i] = new double[actualK];
            for(int j = 0; j < actualK; j++) {
                res[i][j] = pq.poll();
            }

            dump(res[i]);
        }

        return res;
    }

    @org.junit.jupiter.api.Test
    public void add() {
        double[][] p1 = withTree();
        double[][] p2 = withoutTree();

        int differed = 0;
        double error = 0;
        for(int i = 0; i < queriesCount; i++) {
            if(p1[i].length != p2[i].length) {
                differed++;
            } else {
                for(int j = 0; j < p1[i].length; j++) {
                    error += Math.abs(p1[i][j] - p2[i][j]);
                }
            }
        }

        assertTrue(error < 1e-9);
    }

}