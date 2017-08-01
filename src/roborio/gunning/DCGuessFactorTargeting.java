package roborio.gunning;

import robocode.Rules;
import robocode.util.Utils;
import roborio.enemies.ComplexEnemyRobot;
import roborio.gunning.utils.GuessFactorRange;
import roborio.gunning.utils.LinearGuessFactorRange;
import roborio.gunning.utils.TargetingLog;
import roborio.structures.KdTree;
import roborio.structures.WeightedManhattanKdTree;
import roborio.utils.Physics;
import roborio.utils.R;
import roborio.utils.geo.Range;
import roborio.utils.stats.GuessFactorStats;
import roborio.utils.stats.smoothing.GaussianSmoothing;
import roborio.utils.stats.smoothing.Smoothing;
import roborio.utils.storage.NamedStorage;

import java.util.List;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public class DCGuessFactorTargeting extends Targeting {
    public static double[]   TREE_WEIGHTS =
//            new double[]{1.10960507, 1.25116754, 0.74747866, 3.79628778, 3.46687841, 0.27820012, 0.64978600, 0.03063299, 0.63320994, 1.03030407};
    new double[]{1.95475399, 1.25116754, 0.74747866, 3.79628778, 3.46687841, 0.27820012, 0.64978600, 0.03063299, 0.63320994, 0.62950402};
//    new double[]{1.38871431, 1.25116754, 1.10382164, 3.94343543, 3.46687841, 0.30843991, 0.61432433, 0.03063299, 0.63320994, 1.03030407};
//            new double[]{0.07156278, 2.59060502, 3.16783404, 3.06793451, 3.28476405, 0.56447738, 2.44355202, 0.01142163, 0.00928990, 0.01061349};
    public static double[]   FAST_TREE_WEIGHTS =
//            new double[]{2.54556417, 3.98395705, 2.55397415, 1.02103734, 3.00612545, 3.66734076, 2.58995819, 3.62542105, 0.98450673, 1.98383319};
    new double[]{2.54556417, 3.98395705, 2.55397415, 1.02103734, 3.88640523, 3.66734076, 2.58995819, 3.62542105, 1.16104436, 3.42316365};
//    new double[]{1.00032246, 3.07121921, 0.65754068, 1.02103734, 3.32374692, 1.65309620, 2.58995819, 1.79098129, 3.58339310, 1.64771390};
//            new double[]{1.98705888, 3.51323605, 1.79426455, 1.44260859, 2.23592448, 3.49960089, 3.09796858, 1.92346144, 2.40983701, 0.13431434};
    public static double[] NORMALIZING_PARAMS =
//            new double[]{0.14353381, 2.17483163, 0.90552729, 0.58769006, 1.18945944};
    new double[]{0.12915903, 2.17483163, 0.90552729, 0.58769006, 1.18945944};
//    new double[]{0.52188855, 2.17483163, 0.88776368, 0.58769006, 1.18945944};
//            new double[]{0.48251554, 3.96371245, 0.96825349, 1.02417362, 1.18831336};

    private static final double     DIMENSION_COUNT = 10;
    public static double[]   STATS_WEIGHTS =
            new double[]{0.9, 0.1};
    public static int[]         STATS_K = new int[]{100, 12};

    public KdTree<GuessFactorRange>    tree, fastTree;

    public Range            _lastEscapeAngle;
    public Double            _lastGf;
    public Double            _lastFiringGf;
    public ComplexEnemyRobot _lastEnemy;
    public GuessFactorStats _lastStats;
    public TargetingLog _lastMissLog;

    public DCGuessFactorTargeting(String storageHint) {
        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint)) {
            store.add(storageHint, new KdTree[] {
                    new WeightedManhattanKdTree<GuessFactorRange>(TREE_WEIGHTS, 10000),
                    new WeightedManhattanKdTree<GuessFactorRange>(FAST_TREE_WEIGHTS, 2500)
            });
        }

        KdTree[] trees = (KdTree[]) (store.get(storageHint));
        tree = trees[0];
        fastTree = trees[1];
    }

    private double[] getQuery(TargetingLog firingLog) {
        return normalizeQuery(new double[]{
                firingLog.distance / Physics.bulletVelocity(firingLog.bulletPower),
                firingLog.lateralVelocity,
                firingLog.advancingVelocity,
                firingLog.positiveEscape,
                firingLog.negativeEscape,
                firingLog.bulletPower,
                firingLog.accel,
                firingLog.bulletsFired,
                firingLog.timeAccel,
                firingLog.timeDecel
        });
    }

    public double generateFiringAngle(TargetingLog firingLog) {
        double[] query = getQuery(firingLog);

        Range preciseMea = firingLog.getPreciseMea();

//        double mea = Physics.maxEscapeAngle(Rules.getBulletSpeed(firingLog.bulletPower));
        double bandwidth = Physics.hitAngle(firingLog.distance) / (2*preciseMea.maxAbsolute())
                * GuessFactorStats.BUCKET_COUNT;
        Smoothing smoother = new GaussianSmoothing(bandwidth);

        GuessFactorStats gfRange = getGf(tree, query, STATS_K[0]);
        GuessFactorStats fastRange = getGf(fastTree, query, STATS_K[1]);
        GuessFactorStats stats = GuessFactorStats.merge(new GuessFactorStats[]{
                gfRange, fastRange}, STATS_WEIGHTS);

        stats.setSmoother(smoother);

        int enemyDirection = firingLog.direction;
        double bestGF = stats.getBestGuessFactor();
        double bearingOffset = enemyDirection * bestGF * (bestGF > 0 ? preciseMea.max : -preciseMea.min);

        double res = Utils.normalAbsoluteAngle(firingLog.absBearing
                + bearingOffset);

        _lastEscapeAngle = preciseMea;
        _lastStats = stats;
        _lastGf = bestGF;

        return res;
    }

    @Override
    public void log(TargetingLog missLog, boolean isVirtual) {
        double offset = Utils.normalRelativeAngle(missLog.hitAngle - missLog.absBearing);

        Range preciseMea = missLog.getPreciseMea();
        double bulletSpeed = Rules.getBulletSpeed(missLog.bulletPower);
        // TODO: use precise intersection to get a GF range
        double gf = R.constrain(-1,
                missLog.direction * offset /
                        (missLog.direction * offset > 0 ? preciseMea.max : -preciseMea.min), +1);

        double[] query = getQuery(missLog);

        if(!isVirtual) {
            _lastMissLog = missLog;
            _lastFiringGf = gf;
            tree.add(query, new LinearGuessFactorRange(gf, gf));
        }

        fastTree.add(query, new LinearGuessFactorRange(gf, gf));
    }


    public GuessFactorStats getGf(KdTree<GuessFactorRange> tree, double[] query, int K) {
        List<KdTree.Entry<GuessFactorRange>> found =
                tree.kNN(normalizeQuery(query), Math.min(K, (int)Math.ceil(Math.sqrt(tree.size()) + 4)), 0.75);

        GuessFactorStats stats = new GuessFactorStats(Double.POSITIVE_INFINITY);
        double distanceSum = 1e-9;
        for(KdTree.Entry<GuessFactorRange> entry : found) {
            distanceSum += entry.distance;
        }

        for(KdTree.Entry<GuessFactorRange> entry : found) {
            GuessFactorRange range = entry.payload;

            double diff = entry.distance * found.size() / distanceSum;
            double weight = Math.exp(-0.5 * diff * diff);
//            double weight = 1.0;
            stats.logGuessFactor(range.mean, weight);
        }

        return stats;
    }

    public double[] normalizeQuery(double[] query) {
        double[] p = NORMALIZING_PARAMS;
        return new double[]{
                query[0] / 80,
                (query[1] + 0.1) / 8.1,
                (query[2] / 16) + 0.5,
                query[3],
                query[4],
                (query[5] + 0.1) / 3.1,
                (query[6] < 0 ? query[6] * 0.5 : query[6]) / 2 + 1,
                Math.min(Math.pow(query[7]*p[1], p[2]), 1e12),
                Math.min(Math.pow(query[8]*p[3], p[4]), 1e12),
                Math.min(Math.pow(query[9]*p[3], p[4]), 1e12)
        };
    }
}
