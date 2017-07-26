package roborio.gunning;

import robocode.util.Utils;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;

/**
 * Created by Roberto Sales on 24/07/17.
 */
public abstract class AutomaticGun extends Gun {
    private boolean firePending;
    private double  firePower;
    private double  fireAngle;
    private boolean isVirtual;

    public AutomaticGun(BackAsFrontRobot robot, boolean isVirtual) {
        super(robot);
        firePending = false;
        firePower = Physics.MAX_POWER;
        this.isVirtual = isVirtual;
    }

    public long getTime() {
        return getRobot().getTime();
    }

    public void doFiring() {
        if(shouldFire() && !isVirtual) {
            fire(firePower);
        }
    }

    public void setFireTo(double angle, double power) {
        firePending = true;
        firePower = power;
        fireAngle = angle;
        setTurnGunToRadians(angle);
    }

    public void setFireToRight(double offset, double power) {
        firePending = true;
        firePower = power;
        fireAngle = Utils.normalAbsoluteAngle(getGunHeadingRadians() + offset);
        setTurnGunRightRadians(offset);
    }

    public boolean hasFirePending() {
        return firePending;
    }

    public boolean shouldFire() {
        return Math.abs(getRobot().getGunTurnRemaining()) < 3 && getGunHeat() == 0
                && firePending;
    }

    public void fire(double power) {
        if(!shouldFire())
            throw new IllegalStateException();
        firePending = false;
        onFire(new GunFireEvent(this, getRobot().getPoint(), fireAngle, firePower, getTime()));
        setFire(power);
    }

    public void fire() {
        fire(firePower);
    }

    public void onFire(GunFireEvent e) {}

    public double getFirePower() {
        if(!firePending)
            throw new IllegalStateException();
        return firePower;
    }

    public double getFireAngle() {
        if(!firePending)
            throw new IllegalStateException();
        return fireAngle;
    }
}