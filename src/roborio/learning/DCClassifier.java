package roborio.learning;

import robocode.util.Utils;
import roborio.gunning.DCGuessFactorTargeting;
import roborio.gunning.utils.TargetingLog;
import roborio.utils.Physics;
import roborio.utils.geo.Point;
import voidious.wavesim.TickRecord;
import voidious.wavesim.TraditionalWaveClassifier;
import voidious.wavesim.TraditionalWaveEndRecord;

import java.util.Random;

/**
 * Created by Roberto Sales on 30/07/17.
 */
public class DCClassifier implements TraditionalWaveClassifier {
    private int firingWaves;
    private DCGuessFactorTargeting targeting;
    private String lastHint;
    private int battles = 0;

    @Override
    public void feed(TickRecord tickRecord, TraditionalWaveEndRecord waveEndRecord) {
        TargetingLog log = new TargetingLog();
        log.accel = tickRecord.accel;
        log.lateralVelocity = tickRecord.lateralVelocity();
        log.bulletPower = tickRecord.bulletPower;
        log.direction = tickRecord.orbitDirection ? 1 : -1;
        log.distance = tickRecord.distance;
        log.bulletsFired = firingWaves;
        log.positiveEscape = tickRecord.wallDistance;
        log.negativeEscape = tickRecord.revWallDistance;
        log.timeAccel = tickRecord.vChangeTime;
        log.timeDecel = tickRecord.vChangeTime;

        Point source = new Point(tickRecord.sourceLocation);
        Point dest = new Point(tickRecord.targetLocation);

        log.absBearing = Physics.absoluteBearing(source, dest);
        log.advancingVelocity = -tickRecord.cosRelHeading * Math.abs(tickRecord.velocity);

        log.hitAngle = waveEndRecord.hitAngle * log.direction + log.absBearing;
        log.hitDistance = waveEndRecord.hitDistance;

        targeting.log(log, !tickRecord.isFiring);
    }

    @Override
    public void initialize() {
        long randomId = new Random().nextLong();
        lastHint = "dcgt_" + randomId;

        firingWaves = 0;
        targeting = new DCGuessFactorTargeting(lastHint);
    }

    @Override
    public double classify(TickRecord tickRecord) {
        if(tickRecord.isFiring)
            firingWaves++;

        TargetingLog log = new TargetingLog();
        log.accel = tickRecord.accel;
        log.lateralVelocity = tickRecord.lateralVelocity();
        log.bulletPower = tickRecord.bulletPower;
        log.direction = tickRecord.orbitDirection ? 1 : -1;
        log.distance = tickRecord.distance;
        log.bulletsFired = firingWaves;
        log.positiveEscape = tickRecord.wallDistance;
        log.negativeEscape = tickRecord.revWallDistance;
        log.timeAccel = tickRecord.vChangeTime;
        log.timeDecel = tickRecord.vChangeTime;

        Point source = new Point(tickRecord.sourceLocation);
        Point dest = new Point(tickRecord.targetLocation);

        log.absBearing = Physics.absoluteBearing(source, dest);
        log.advancingVelocity = -tickRecord.cosRelHeading * Math.abs(tickRecord.velocity);

        return Utils.normalRelativeAngle(targeting.generateFiringAngle(log)
                - log.absBearing) * log.direction;
    }
}
