package roborio.gunning;

import robocode.Bullet;
import robocode.util.Utils;
import roborio.gunning.utils.GunFireEvent;
import roborio.gunning.utils.VirtualBullet;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by Roberto Sales on 24/07/17.
 */
public abstract class AutomaticGun extends Gun {
    private HashSet<VirtualBullet> firedBullets;
    private long    lastFire;
    private GunFireEvent lastGunFireEvent;
    private boolean firePending;
    private double  firePower;
    private double  fireAngle;
    private boolean isVirtual;
    private boolean isUsed;

    public AutomaticGun(BackAsFrontRobot robot, boolean isVirtual) {
        super(robot);
        firePending = false;
        firePower = Physics.MAX_POWER;
        this.isVirtual = isVirtual;
        isUsed = false;
        firedBullets = new HashSet<>();
        lastFire = -100;
    }

    public void deactivate() {
        isUsed = false;
    }

    public void activate() {
        isUsed = true;
    }

    public boolean isActive() {
        return !isVirtual || isUsed;
    }

    public long getTime() {
        return getRobot().getTime();
    }

    public void doFiring() {
        checkActive();

        if(shouldFire()) {
            fire(firePower);
        }
    }

    public void checkActive() {
        Iterator<VirtualBullet> iterator = firedBullets.iterator();
        while(iterator.hasNext()) {
            VirtualBullet bullet = iterator.next();
            if(!bullet.isActive())
                iterator.remove();
        }
    }

    public VirtualBullet[] getVirtualBullets() {
        checkActive();
        return firedBullets.toArray(new VirtualBullet[0]);
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
                && firePending && isActive();
    }

    public void fire(double power) {
        if(!shouldFire())
            throw new IllegalStateException();
        Bullet bullet = setFireBullet(power);
        if(bullet == null)
            return;

        lastFire = getTime();
        firePending = false;
        firedBullets.add(new VirtualBullet(getRobot().getPoint(), bullet, getTime()));
        GunFireEvent event = new GunFireEvent(this, getRobot().getPoint(), bullet, getTime());
        lastGunFireEvent = event;
        onFire(event);
    }

    public void fire() {
        fire(firePower);
    }

    public void fireVirtual(double angle, double power) {
        onFire(new GunFireEvent(this, getRobot().getPoint(), angle, power, getTime()));
    }

    public void onFire(GunFireEvent e) {}

    public boolean hasJustFired() {
        return lastFire == getTime();
    }

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

    public long getLastFire() {
        return lastFire;
    }

    public GunFireEvent getLastGunFireEvent() {
        return lastGunFireEvent;
    }

    public void setTurnGunRightRadians(double radians) {
        if(isActive())
            super.setTurnGunRightRadians(radians);
    }

    public void setTurnGunToRadians(double radians) {
        if(isActive())
            super.setTurnGunToRadians(radians);
    }

    public void setFire(double power) {
        if(isActive())
            super.setFire(power);
    }

    public Bullet setFireBullet(double power) {
        if(isActive())
            return super.setFireBullet(power);
        return null;
    }

    public abstract double wouldHit();
}