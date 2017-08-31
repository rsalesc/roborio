package rsalesc.roborio.energy;

import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.Rules;
import rsalesc.roborio.enemies.ComplexEnemyRobot;
import rsalesc.roborio.utils.BattleTime;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.R;

/**
 * Created by Roberto Sales on 29/08/17.
 */
public class HeatLog {
    double coolingRate = 0.1;
    double lastHeat = 3.0;
    double heat = 3.0;
    long lastTick = 0;

    ComplexEnemyRobot lastEnemy;
    double lastEnergy;
    BattleTime lastShot;

    double lastPower;

    public HeatLog() {}

    public void tick(long time) {
        if(time == 0) {
            heat = lastHeat = 3.0;
            lastTick = 0;
            lastShot = null;
            return;
        }

        lastHeat = heat;
        heat = Math.max(heat - coolingRate * (time - lastTick), 0);
        lastTick = time;
    }

    public void setCoolingRate(double x) {
        coolingRate = x;
    }

    public boolean hasShot(long time) {
        return lastShot != null && lastShot.getTime() == time;
    }

    public boolean hasShot(BattleTime battleTime) {
        return lastShot != null && battleTime.equals(lastShot);
    }

    public boolean hasCooled() {
        return heat == 0 && lastHeat != heat;
    }

    public double getLastShotPower() {
        if(lastShot == null)
            throw new IllegalStateException();
        return lastPower;
    }

    public long getLastShotTime() {
        if(lastShot == null)
            throw new IllegalStateException();
        return lastShot.getTime();
    }

    public void push(ComplexEnemyRobot enemy) {
        checkShot(enemy);
        lastEnemy = enemy;
        lastEnergy = lastEnemy.getEnergy();
    }

    private void checkShot(ComplexEnemyRobot enemy) {
        double energyDelta = enemy.getEnergy() - lastEnergy;
        if(isCool() && R.nearOrBetween(-Physics.MAX_POWER, energyDelta, -Physics.MIN_POWER)) {
            lastPower = -energyDelta;
            lastHeat = heat;
            heat = 1.0 + lastPower * 0.2 - coolingRate; // shot was fired last tick, gun cooled a bit
            lastShot = new BattleTime(enemy.getBattleTime().getTime() - 1, enemy.getBattleTime().getRound());
        }
    }

    public boolean isCool() {
        return ticksToCool() == 0;
    }

    public long ticksToCool() {
        return (int) Math.ceil(heat / coolingRate);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        lastEnergy += Rules.getBulletHitBonus(e.getBullet().getPower());
    }

    public void onBulletHit(BulletHitEvent e) {
        lastEnergy -= Rules.getBulletDamage(e.getBullet().getPower());
    }
}
