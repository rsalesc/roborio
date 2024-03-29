package rsalesc.roborio.energy;

import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.enemies.EnemyTracker;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.myself.MyRobot;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.Strategy;
import rsalesc.roborio.utils.storage.NamedStorage;
import rsalesc.roborio.utils.structures.Knn;
import rsalesc.roborio.utils.structures.KnnTree;

import java.util.List;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class MirrorPowerManager extends EnergyManager {
    Knn<Double> knn;
    private double myScore = 0;
    private double hisScore = 0;

    public MirrorPowerManager(String name) {
        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(name)) {
            store.add(name, new KnnTree<Double>()
                    .setLimit(750)
                    .setMode(KnnTree.Mode.MANHATTAN)
                    .setK(12)
                    .setRatio(0.1)
                    .setStrategy(new ConservativeStrategy())
                    .build());
        }

        knn = (Knn) store.get(name);
    }

    public void setScore(double x) {
        myScore = x;
    }

    public double[] getQuery(MyRobot me, ComplexEnemyRobot enemy) {
        return new double[]{
                Math.min(me.getEnergy() / 100.0, 1),
                Math.min(enemy.getEnergy() / 100.0, 1),
                Math.min(enemy.getDistance() / 600, 1.0)
        };
    }

    public double predictEnemyPower(MyRobot me, ComplexEnemyRobot enemy, double myScore, double hisScore) {
        List<Knn.Entry<Double>> list = knn.query(getQuery(me, enemy));

        double distSum = 1e-9;
        for(Knn.Entry<Double> entry : list) {
            distSum += entry.distance;
        }

        double invAvg = list.size() / distSum;

        double best = 0.1;
        double best_h = 0;
        final double bandwidth = 0.25;

        for(double i = 0.1; i <= 3.0; i += 0.1) {
            double acc = 0;
            for(Knn.Entry<Double> entry : list) {
                double diff = entry.distance * invAvg;
                double xdelta = (entry.payload - i) / bandwidth;
                double h = R.exp(-0.5 * diff * diff) * R.exp(-0.5 * xdelta * xdelta) * entry.weight;
                acc += h;
            }

            if(acc > best_h) {
                best = i;
                best_h = acc;
            }
        }

        if(best_h == 0)
            return 1.5;

        return best;
    }

    public void log(MyRobot robot, ComplexEnemyRobot enemy, double myScore, double hisScore, double power) {
        knn.add(getQuery(robot, enemy), power);
    }

    @Override
    public double selectPower(MyRobot robot, ComplexEnemyRobot enemy) {
        if(enemy.getDistance() < 150)
            return Math.min(enemy.getEnergy() * 0.25, Math.min(2.9999, Math.max(robot.getEnergy() - 0.4, 0)));

        boolean ramming = EnemyTracker.getInstance().getLog(enemy).isRamming();
        double hisPower = Double.POSITIVE_INFINITY;
        if(myScore < 0.12) {
            hisPower = predictEnemyPower(robot, enemy, myScore, hisScore);
        } else if(myScore < 0.17) {
            hisPower = Math.min(predictEnemyPower(robot, enemy, myScore, hisScore) + 0.1, 3.0);
        }

        double distance = robot.getPoint().distance(enemy.getPoint());
        double myEnergy = robot.getEnergy();
        double hisEnergy = enemy.getEnergy();
        double basePower = (myScore > 0.31 && robot.getBattleTime().getRound() >= 1 || ramming)
                ? 2.95
                : (hisEnergy*2.5 <= myEnergy && myEnergy >= 15 ? 2.4 : 1.95);

        double expectedPower = Math.max((myEnergy - 20 + R.constrain(-10, myEnergy - hisEnergy, +30) / 2) / 25, 0);

        if(distance > 500) {
            expectedPower -= (distance - 500) / 300;
        }

        if(distance < 300) {
            expectedPower += (300 - distance) / 200;
        }

        double power = R.constrain(0.1, Math.min(basePower, expectedPower), 3.0);

        if(myEnergy < 0.4)
            return 0;

        return R.basicSurferRounding(R.constrain(0.15, Math.min(power, hisEnergy * 0.25),
                Math.max(Math.min(hisPower - 0.1, myEnergy - 0.1), 0.1)));
    }

    private static class ConservativeStrategy extends Strategy {

        @Override
        public double[] getQuery(TargetingLog f) {
            return new double[0];
        }

        @Override
        public double[] getWeights() {
            return new double[]{4.0, 4.0, 1.0};
        }
    }
}
