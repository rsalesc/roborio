package roborio.gunning;

import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import roborio.enemies.ComplexEnemyRobot;
import roborio.enemies.EnemyLog;
import roborio.enemies.EnemyTracker;
import roborio.gunning.utils.GuessFactorRange;
import roborio.gunning.utils.PowerSelection;
import roborio.myself.MyLog;
import roborio.structures.KdTree;
import roborio.structures.WeightedManhattanKdTree;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;
import roborio.utils.R;
import roborio.utils.geo.G;
import roborio.utils.geo.Point;
import roborio.utils.stats.GuessFactorStats;
import roborio.utils.stats.smoothing.GaussianSmoothing;
import roborio.utils.stats.smoothing.Smoothing;
import roborio.utils.storage.NamedStorage;
import roborio.utils.waves.MyFireWave;
import roborio.utils.waves.MyWave;
import roborio.utils.waves.Wave;
import roborio.utils.waves.WaveMap;

import java.awt.*;
import java.util.Iterator;

/**
 * Created by Roberto Sales on 28/07/17.
 */
public class DCGuessFactorGun extends AutomaticGun {
    private static final double[]   TREE_WEIGHTS = new double[]{2.5, 3, 1.5, 2.0, 0.5, 3, 0.0};

    private static final double[]   FAST_TREE_WEIGHTS = new double[]{2.5, 3, 0, 1.0, 0.0, 3, 0.0};

    private static final double     DIMENSION_COUNT = 7;
    private static final double[]   STATS_WEIGHTS = new double[]{0.75, 0.25};

    private KdTree<GuessFactorRange>    tree, fastTree;
    private WaveMap<Long> waves;
    private double         _wouldHit;
    private GunFireEvent   lastFireEvent;
    private double absFireAngle;

    private GuessFactorStats _lastStats;
    private ComplexEnemyRobot _lastEnemy;
    private double            _lastEscapeAngle;
    private double            _lastGf;
    private long              _bulletsFired;

    @SuppressWarnings("unchecked")
    public DCGuessFactorGun(BackAsFrontRobot robot, boolean isVirtual, String storageHint) {
        super(robot, isVirtual);

        waves = new WaveMap<>();
        _wouldHit = 0;

        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(storageHint)) {
            store.add(storageHint, new KdTree[] {
                    new WeightedManhattanKdTree<GuessFactorRange>(TREE_WEIGHTS, 2000),
                    new WeightedManhattanKdTree<GuessFactorRange>(FAST_TREE_WEIGHTS, 4500)
            });
        }

        KdTree[] trees = (KdTree[]) (store.get(storageHint));

        tree = trees[0];
        fastTree = trees[1];

        _bulletsFired = 0;
    }

    private double[] normalizeQuery(double[] query) {
        return new double[]{
                (query[0] / Physics.bulletVelocity(query[5])) / 80,
                (query[1] + 0.1) / 8.1,
                (query[2] / 16),
                Math.pow(Math.max(query[3] / 1000, 1.0), 1.5),
                (query[4] + 0.1) / 3.1,
                query[5] < 0 ? query[5] / 4 : query[5] / 2,
                0.0 // discarded
        };
    }

    @Override
    public void doGunning() {
        if(lastFireEvent == null) return;

        MyLog log = MyLog.getInstance();
        Wave wave = new MyWave(log, lastFireEvent.getVelocity());

        waves.add(wave, _bulletsFired);
    }

    public void onScan(ScannedRobotEvent e) {
        EnemyLog enemyLog = EnemyTracker.getInstance().getLog(e);
        ComplexEnemyRobot enemy = enemyLog.getLatest();
        ComplexEnemyRobot pastEnemy = enemyLog.atLeastAt(getTime() - 1);

        double bulletPower = PowerSelection.naive(MyLog.getInstance().getLatest(), enemy, 0.0);

        checkHits(enemy);

        double mea = Physics.maxEscapeAngle(Rules.getBulletSpeed(bulletPower));

        double distance = enemy.getDistance();
        double lateralVelocity = enemy.getLateralVelocity();
        double advancingVelocity = enemy.getApproachingVelocity();
        double distanceToWall = enemy.getDistanceToWall();
        double accel = Math.abs(enemy.getVelocity() - pastEnemy.getVelocity())
                * pastEnemy.getDirection();

        double[] query = new double[]{distance, lateralVelocity, advancingVelocity,
                distanceToWall, bulletPower, accel, _bulletsFired};

        double bandwidth = Physics.hitAngle(distance) / (2*mea)
                * GuessFactorStats.BUCKET_COUNT;
        Smoothing smoother = new GaussianSmoothing(bandwidth);

        GuessFactorStats gfRange = getGf(tree, query, 48);
        GuessFactorStats fastRange = getGf(fastTree, query, 12);
        GuessFactorStats stats = GuessFactorStats.merge(new GuessFactorStats[]{
                gfRange, fastRange}, STATS_WEIGHTS);

        stats.setSmoother(smoother);

        int enemyDirection = enemy.getDirection();
        double bestGF = stats.getBestGuessFactor();
        double bearingOffset = enemyDirection * bestGF * mea;

        absFireAngle = Utils.normalAbsoluteAngle(enemy.getAbsoluteBearing()
                + bearingOffset);

        _lastEscapeAngle = mea;
        _lastEnemy = enemy;
        _lastStats = stats;
        _lastGf = bestGF;
        setFireTo(absFireAngle, bulletPower);
    }

    private GuessFactorStats getGf(KdTree<GuessFactorRange> tree, double[] query, int K) {
//        List<KdTree.Entry<GuessFactorRange>> found =
//                tree.kNN(normalizeQuery(query), Math.min(K, (int)Math.ceil(Math.sqrt(tree.size()) + 4)), 0.75);

        GuessFactorStats stats = new GuessFactorStats(Double.POSITIVE_INFINITY);

//        for(KdTree.Entry<GuessFactorRange> entry : found) {
//            double distance = entry.distance;
//            GuessFactorRange range = entry.payload;
//
//            stats.logGuessFactor(range.mean, Math.sqrt(1.0 / distance));
////            stats.logGuessFactor(range.mean, 1.0);
//        }

        return stats;
    }

    private void checkHits(ComplexEnemyRobot enemy) {
        EnemyLog enemyLog = EnemyTracker.getInstance().getLog(enemy);
        _wouldHit = 0;

        Iterator<Wave> iterator = waves.iterator();
        while(iterator.hasNext()) {
            Wave wave = iterator.next();
            if(wave.hasTouchedRobot(enemy.getPoint(), getTime())) {
                ComplexEnemyRobot pastEnemy = enemyLog.atLeastAt(getTime() - 1);
                ComplexEnemyRobot pasterEnemy = enemyLog.atLeastAt(getTime() - 2);

                int enemyDirection = pastEnemy.getDirection();
                double offset = Utils.normalRelativeAngle(
                        Physics.absoluteBearing(wave.getSource(), enemy.getPoint())
                        - pastEnemy.getAbsoluteBearing());

                // TODO: use precise intersection to get a GF range
                double gf = R.constrain(-1, enemyDirection * offset / Physics.maxEscapeAngle(wave.getVelocity()), +1);

                double distance = pastEnemy.getDistance();
                double lateralVelocity = pastEnemy.getLateralVelocity();
                double advancingVelocity = pastEnemy.getApproachingVelocity();
                double wallDistance = pastEnemy.getDistanceToWall();
                double bulletPower = Physics.bulletPower(wave.getVelocity());
                double accel = Math.abs(pastEnemy.getVelocity() - pasterEnemy.getVelocity())
                        * enemyDirection;

                double[] query = new double[]{distance, lateralVelocity, advancingVelocity,
                        wallDistance, bulletPower, accel, waves.getData(wave)};

                if(wave instanceof MyFireWave) {
                    MyFireWave fireWave = (MyFireWave) wave;
                    Point projection = fireWave.project(fireWave.getAngle(), getTime());
                    if(enemy.getHitBox().contains(projection)) {
                        _wouldHit = 1.0;
                    }
                }

//                System.out.println("adding a point to kds");

                if(wave instanceof MyFireWave) {
                    // tree.add(query, new LinearGuessFactorRange(gf, gf));
                }

                // fastTree.add(query, new LinearGuessFactorRange(gf, gf));

                iterator.remove();
            }
        }
    }

    @Override
    public String getName() {
        return "Dynamic Clustering GuessFactorGun";
    }

    @Override
    public double wouldHit() {
        return _wouldHit;
    }

    @Override
    public void onFire(GunFireEvent e) {
        lastFireEvent = e;
        Wave wave = new MyFireWave(MyLog.getInstance(), e.getAngle(), e.getVelocity());
        waves.add(wave, _bulletsFired++);
    }

    @Override
    public void onPaint(Graphics2D gr) {
        G g = new G(gr);

        if(_lastStats == null) return;

        double maxValue = _lastStats.get(_lastStats.getBestBucket());

        g.drawLine(getRobot().getPoint(),
                getRobot().getPoint().project(absFireAngle, 100),
                Color.WHITE);

        for(int i = 0; i < _lastStats.size(); i += 3) {
            double gf = _lastStats.getGuessFactor(i);
            double gfValue = _lastStats.get(i);

            double dangerPercent = Math.sqrt(gfValue / maxValue);
            double angle = _lastEnemy.getAbsoluteBearing() + gf * _lastEscapeAngle * _lastEnemy.getDirection();

            g.drawCircle(getRobot().getPoint().project(angle, 100), 3.0, G.getDangerColor(dangerPercent));
        }
    }
}
