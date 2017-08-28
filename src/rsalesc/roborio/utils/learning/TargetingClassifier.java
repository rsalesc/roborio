package rsalesc.roborio.utils.learning;

/**
 * Created by Roberto Sales on 24/08/17.
 */

import robocode.util.Utils;
import rsalesc.roborio.gunning.targetings.AntiEverything2Targeting;
import rsalesc.roborio.gunning.Targeting;
import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.geo.AngularRange;
import rsalesc.roborio.utils.geo.Point;
import rsalesc.roborio.utils.geo.Range;
import rsalesc.roborio.utils.waves.BreakType;
import voidious.wavesim.PreciseWaveClassifier;
import voidious.wavesim.PreciseWaveEndRecord;
import voidious.wavesim.TickRecord;

import java.util.Random;

public class TargetingClassifier implements PreciseWaveClassifier {
    private int firingWaves;
    private Targeting targeting;
    private String lastHint;
    private int battles = 0;

    private TargetingLog buildLog(TickRecord tickRecord) {
        TargetingLog log = new TargetingLog();
        log.accel = tickRecord.accel;
        log.lateralVelocity = tickRecord.lateralVelocity();
        log.bulletPower = tickRecord.bulletPower;
        log.direction = tickRecord.orbitDirection ? 1 : -1;
        log.distance = tickRecord.distance;
        log.bulletsFired = firingWaves;
        log.timeAccel = tickRecord.vChangeTime;
        log.timeDecel = tickRecord.vChangeTime;
        log.preciseMea = new Range(tickRecord.negMea, tickRecord.posMea);
        log.displaceLast10 = tickRecord.dl8t / 64 * 80;
        log.timeRevert = tickRecord.dChangeTime;

        Point source = new Point(tickRecord.sourceLocation);
        Point dest = new Point(tickRecord.targetLocation);

        log.absBearing = Physics.absoluteBearing(source, dest);
        log.advancingVelocity = -tickRecord.cosRelHeading * Math.abs(tickRecord.velocity);

        return log;
    }

    @Override
    public void feed(TickRecord tickRecord, PreciseWaveEndRecord waveEndRecord) {
        TargetingLog log = buildLog(tickRecord);

        double hitAngle = Utils.normalAbsoluteAngle((waveEndRecord.minAngle + waveEndRecord.maxAngle) / 2);
        double mn = Utils.normalRelativeAngle(waveEndRecord.minAngle - hitAngle);
        double mx = Utils.normalRelativeAngle(waveEndRecord.maxAngle - hitAngle);

        AngularRange range = new AngularRange(hitAngle, mn, mx);

        log.hitAngle = hitAngle;
        log.preciseIntersection = range;

        targeting.log(log, tickRecord.isFiring ? BreakType.BULLET_BREAK : BreakType.VIRTUAL_BREAK);
    }

    @Override
    public void initialize() {
        long randomId = new Random().nextLong();
        lastHint = "dcgt_" + randomId;

        firingWaves = 0;
        targeting = new AntiEverything2Targeting().setIdentifier(lastHint).build();
    }

    @Override
    public double classify(TickRecord tickRecord) {
        if(tickRecord.isFiring)
            firingWaves++;

        TargetingLog log = buildLog(tickRecord);

        return Utils.normalRelativeAngle(targeting.generateFiringAngle(log)
                - log.absBearing) * log.direction;
    }
}
