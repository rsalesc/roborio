package roborio.gunning;

import robocode.ScannedRobotEvent;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.R;

import java.awt.*;

/**
 * Created by Roberto Sales on 27/07/17.
 */
public class DualGuessFactorGun extends VirtualArrayGun {
    private static int LOW_GUN = 0;
    private static int HIGH_GUN = 1;

    private GuessFactorGun[]    guns;

    private static double[]            score = new double[2];
    private static int                 nowActive = -1;

    public DualGuessFactorGun(BackAsFrontRobot robot) {
        super(robot);
        guns = new GuessFactorGun[2];
        guns[LOW_GUN] = new GuessFactorGun(robot, 0.7, true, "gf_low");
        guns[HIGH_GUN] = new GuessFactorGun(robot, 1.95, true, "gf_high");

        setActive(0);
    }

    @Override
    public void doGunning() {
        if(score[nowActive] < score[nowActive ^ 1] - R.EPSILON)
            setActive(nowActive ^ 1);

        GunFireEvent firedEvent = null;
        for(int i = 0; i < 2; i++) {
            guns[i].doGunning();
            if(guns[i].isActive() && guns[i].hasJustFired()) {
                firedEvent = guns[i].getLastGunFireEvent();
            }
        }

        if(firedEvent != null) {
            for(int i = 0; i < 2; i++) {
                if(!guns[i].isActive() && guns[i].hasFirePending()) {
                    guns[i].fireVirtual(guns[i].getFireAngle(), guns[i].getFirePower());
                }
            }
        }

        // check gun hits
        double totalIncrease = 0;
        double[] increase = new double[2];
        for(int i = 0; i < 2; i++) {
            increase[i] = guns[i].wouldHit();
            totalIncrease += increase[i];
        }

        double max = 0;
        if(totalIncrease > 0.1) {
            for(int i = 0; i < 2; i++) {
                score[i] *= 0.92;
                score[i] += increase[i];
                max = Math.max(max, score[i]);
            }
        }

        if(Math.abs(max) > R.EPSILON) {
            for(int i = 0; i < 2; i++) {
                score[i] /= max;
            }
        }
    }

    private void setActive(int index) {
        for(int i = 0; i < 2; i++) {
            guns[i].deactivate();
            if(i == index)
                guns[i].activate();
        }

        if(index != nowActive)
            System.out.println("Switching over to gun " + guns[index].getName() + " (" + index + ")");
        nowActive = index;
    }

    private int getActive() {
        for(int i = 0; i < 2; i++)
            if(guns[i].isActive())
                return i;

        throw new IllegalStateException();
    }

    @Override
    public void onPaint(Graphics2D g) {
        guns[getActive()].onPaint(g);
    }

    @Override
    public void onScan(ScannedRobotEvent e) {
        for(int i = 0; i < 2; i++) {
            guns[i].onScan(e);
        }
    }

    @Override
    public void doFiring() {
        for(int i = 0; i < 2; i++) {
            guns[i].doFiring();
        }
    }

    @Override
    public double wouldHit() {
        return 0;
    }

    @Override
    public String getName() {
        return "DualGuessFactorGun";
    }
}
