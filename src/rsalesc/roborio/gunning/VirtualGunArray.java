package rsalesc.roborio.gunning;

import robocode.ScannedRobotEvent;
import rsalesc.roborio.gunning.utils.GunFireEvent;
import rsalesc.roborio.gunning.virtual.GunChoosingStrategy;
import rsalesc.roborio.gunning.virtual.GunScoring;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.storage.NamedStorage;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roberto Sales on 24/07/17.
 * TODO: get wouldHit of active weapon from onBulletHit
 */
public abstract class VirtualGunArray extends AutomaticGun {
    public int length;
    private List<AutomaticGun> _guns;

    public double[] score;
    public int activeIndex = -1;
    public AutomaticGun[] guns;

    private GunChoosingStrategy strategy;
    private GunScoring scoring;

    public VirtualGunArray(BackAsFrontRobot robot) {
        super(robot, false);
        _guns = new ArrayList<>();
    }

    public VirtualGunArray setStrategy(GunChoosingStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public VirtualGunArray setScoring(GunScoring scoring) {
        this.scoring = scoring;
        return this;
    }

    public VirtualGunArray add(AutomaticGun gun) {
        _guns.add(gun);
        return this;
    }

    public void buildStructure() {
        length = _guns.size();

        for (AutomaticGun gun : _guns) {
            if (!gun.isBuilt()) {
                if(gun.getManager() == null)
                    gun.setManager(getManager()); // use VG energy manager
                gun.setVirtual(true).build();
            }
        }

        guns = _guns.toArray(new AutomaticGun[0]);
        score = new double[length];

        _guns.clear();

        if(scoring.getIdentifier() == null)
            scoring.setIdentifier("virtual_scoring");

        NamedStorage store = NamedStorage.getInstance();
        if(!store.contains(scoring.getIdentifier())) {
            store.add(scoring.getIdentifier(), scoring);
        }

        scoring = (GunScoring) store.get(scoring.getIdentifier());
        score = scoring.evaluate(this);
    }

    public void setActive(int index) {
        for(int i = 0; i < length; i++) {
            if(index == i)
                guns[i].activate();
            else
                guns[i].deactivate();
        }

        if(index != activeIndex) {
            activeIndex = index;
            System.out.println("Switching over to gun " + guns[index].getName() + " (" + R.formattedPercentage(score[index]) + ")");
        }
    }

    public AutomaticGun getActive() {
        if(activeIndex == -1)
            return null;
        return guns[activeIndex];
    }

    @Override
    public void doGunning() {
        setActive(strategy.choose(this));

        GunFireEvent event = null;
        for(int i = 0; i < length; i++) {
            AutomaticGun gun = guns[i];
            gun.doGunning();
            if(gun.isActive() && gun.hasJustFired()) {
                event = gun.getLastGunFireEvent();
                scoring.fire(i, 1.0, event.getPower());
            }
        }

        if(event != null) {
            for(int i = 0; i < length; i++) {
                AutomaticGun gun = guns[i];
                if(!gun.isActive() && gun.hasFirePending()) {
                    gun.fireVirtual(gun.getFireAngle(), gun.getFirePower());
                    scoring.fire(i, 1.0, gun.getFirePower());
                }
            }
        }

        double maxIncrease = 0;
        for(AutomaticGun gun : guns)
            maxIncrease = Math.max(gun.wouldHit(), maxIncrease);

        if(maxIncrease > 0.05) {
            for(int i = 0; i < length; i++) {
                scoring.log(i, guns[i].wouldHit(), guns[i].wouldHitPower());
            }
        }

        if(event != null || maxIncrease > 0.05)
            score = scoring.evaluate(this);
    }

    public double getActiveScore() {
        if(getActive() == null)
            return 0.0;
        return score[activeIndex];
    }

    @Override
    public void onPaint(Graphics2D g) {
        if(getActive() != null)
            getActive().onPaint(g);
    }

    @Override
    public void onScan(ScannedRobotEvent e) {
        for(AutomaticGun gun : guns)
            gun.onScan(e);
    }

    @Override
    public void doFiring() {
        for(AutomaticGun gun : guns)
            gun.doFiring();
    }

    @Override
    public double wouldHit() {
        return 0;
    }

    @Override
    public double wouldHitPower() {
        return 0;
    }

    @Override
    public void printLog() {
        for(int i = 0; i < length; i++) {
            System.out.println(guns[i].getName() + " score: " + R.formattedPercentage(score[i]));
        }
    }
}
