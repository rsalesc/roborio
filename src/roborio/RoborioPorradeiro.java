package roborio;

import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.SkippedTurnEvent;
import robocode.util.Utils;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;
import roborio.utils.R;
import roborio.utils.geo.AxisRectangle;
import roborio.utils.geo.G;
import roborio.utils.geo.Point;

import java.awt.*;
import java.util.*;

/**
 * Created by Roberto Sales on 02/08/17.
 * TODO: lock radar (and possibly gun) on targeted enemy when gun heat <= 0.2
 * TODO: wave-like random movement on 1v1
 */
public class RoborioPorradeiro extends BackAsFrontRobot {
    private static HashMap<String, Scan> scans = new HashMap<>();

    private int skipped;
    private Point gotoPoint;
    private int MAX_LENGTH = 30;
    private ArrayList<Point> _predicted;
    private ArrayList<CandidateAngle> _lastAngles;
    private ArrayList<CandidateAngle> _lastShootAngles;
    private double lastSeenEnergy;

    private CandidateAngle _lastBestAngle;
    private CandidateAngle _lastBestShootAngle;
    private double _lastSelectedPower;

    private long _lastFireTime = -1000;

    private static long bulletsFired = 0;

    private Scan getScan(ScannedRobotEvent e, HashMap<String, Scan> curScans) {
        if(!curScans.containsKey(e.getName()))
            curScans.put(e.getName(), new Scan());
        return curScans.get(e.getName());
    }

    public void run() {
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        lastSeenEnergy = 0;

        // spin
        setTurnRadarRight(Double.POSITIVE_INFINITY);
        execute();

        while(true) {
            if(getGunHeat() == 0 && getGunTurnRemaining() == 0 && _lastSelectedPower > 0) {
                _lastShootAngles = _lastAngles;
                _lastBestShootAngle = _lastBestAngle;
                _lastFireTime = getTime();
                bulletsFired++;
                setFire(_lastSelectedPower);
            }

            aim();
//            move();

            if(isMelee()) {
                int aliveCnt = 0;
                Scan radarTarget = null;
                for(Map.Entry<String, Scan> entry : scans.entrySet()) {
                    Scan scan = entry.getValue();
                    if(scan.dead) continue;

                    aliveCnt++;
                    if(radarTarget == null || radarTarget.getLatest().time
                        > scan.getLatest().time)
                        radarTarget = scan;
                }

                if(radarTarget != null && aliveCnt >= getOthers()) {
                    double absBearing = Physics.absoluteBearing(getPoint(), radarTarget.getLatest().position);
                    double offset = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());

                    if(offset >= 0)
                        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
                    else
                        setTurnRadarRightRadians(Double.NEGATIVE_INFINITY);
                } else {
                    setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
                }
            }

            if(getRadarTurnRemaining() == 0)
                setTurnRadarRight(1);
            execute();
        }
    }

    private double selectPower() {
        if(!isMelee() && lastSeenEnergy == 0)
            return 0;

        if(!isMelee() && getEnergy() > 2*lastSeenEnergy)
            return Math.min(1.7, lastSeenEnergy / 4);
        else if(!isMelee() && getEnergy() > 2*lastSeenEnergy)
            return 1.5;
        else if(getEnergy() > 30 && !isMelee())
            return 2.1;
        else if(getEnergy() > 20)
            return 1.7;
        else if(getEnergy() > 10)
            return 1.3;
        else if(getEnergy() > 0.5)
            return getEnergy() / 10;
        else
            return 0;
    }

    private void move() {
        if(gotoPoint == null || gotoPoint.distance(getPoint()) < 20 || tooClose()) {
            Point dest = getPoint();
            double best = test(dest);

            for (int i = 0; i < 500; i++) {
                double angle = Math.random() * R.PI * 2.0;
                double distance = Math.random() * 120;
                Point cand = getPoint().project(angle, distance);
                if (!getBattleField().shrink(30, 30).contains(cand))
                    continue;

                double value = test(cand);
                if (value < best) {
                    best = value;
                    dest = cand;
                }
            }

            gotoPoint = dest;
        }

        moveWithBackAsFront(gotoPoint);
        doSharpTurning();
    }

    private boolean tooClose() {
        for(Map.Entry<String, Scan> entry : scans.entrySet()) {
            Scan scan = entry.getValue();
            ScanLog log = scan.getLatest();
            if(log.position.distance(getPoint()) < 100)
                return true;
        }

        return false;
    }

    private double test(Point cand) {
        double res = 3.0 / cand.distance(getPoint());
        AxisRectangle field = getBattleField();
        res += 1.0 / cand.distance(field.getCenter());
        for(Point corner : field.getCorners()) {
            res += 2.0 / cand.distance(corner);
        }

        for(Map.Entry<String, Scan> entry : scans.entrySet()) {
            Scan scan = entry.getValue();
            ScanLog log = scan.getLatest();
            if(scan.dead) continue;
            double angleDiff = Utils.normalRelativeAngle(Physics.absoluteBearing(getPoint(), cand)
                    - Physics.absoluteBearing(getPoint(), log.position));
            double steepness = Math.cos(Math.min(angleDiff, R.PI - angleDiff));
            res += R.constrain(2.5, log.energy / getEnergy() * 10 * steepness, 20)
                    / log.position.distance(getPoint());
        }
        return res;
    }

    private double getMoveHeading(ScanLog log) {
        if(Math.signum(log.velocity) < 0)
            return Utils.normalAbsoluteAngle(log.heading + R.PI);
        return log.heading;
    }

    private void aim() {
        final int clusterSize = 32;

        double bulletPower = selectPower();
        _lastSelectedPower = bulletPower;

        bulletPower = Math.max(0.1, bulletPower);

        double bulletSpeed = Rules.getBulletSpeed(bulletPower);

        ArrayList<CandidateAngle> angles = new ArrayList<>();

        for(Map.Entry<String, Scan> entry : scans.entrySet()) {
            ArrayList<CandidateAngle> curAngles = new ArrayList<>();
            Scan scan = entry.getValue();
            if(scan.dead) continue;

            ScanLog latest = scan.getLatest();
            long timeDelta = getTime() - latest.time;

            int many = Math.max(scan.log.size() - 1, 0);
            if(many == 0)  {
                continue;
            }

            int K = Math.min(clusterSize, (int) Math.sqrt(many));

            ScanLog[] logs = new ScanLog[many];
            ScanComparator comparator = new ScanComparator(latest);

            for(int i = 0; i < logs.length; i++)
                logs[i] = scan.log.get(i);

            Arrays.sort(logs, comparator);

            int picked = 0;
            for(int i = 0; i < logs.length && picked < K; i++) {
                int at = logs[i].index;
                long timeSpent = 0;

                boolean did = true;
                double rotation = Utils.normalRelativeAngle(getMoveHeading(logs[i]) - getMoveHeading(latest));
                Point translation = logs[i].position.subtract(latest.position);

                Point transformedMe = getPoint().add(translation).rotate(rotation, logs[i].position);

                while(at < scan.log.size() && scan.log.get(at).position.distance(transformedMe)
                        > (timeSpent - timeDelta) * bulletSpeed) {
                    at++;
                    if(at < scan.log.size()) {
                        long diff = scan.log.get(at).time - scan.log.get(at - 1).time;// shot must be played in time 0?
                        if(diff <= 0) {
                            did = false;
                            break;
                        }
                        timeSpent += diff;
                    }
                }

                if(at >= scan.log.size() || !did) {
                    continue;
                }

                ScanLog last = scan.log.get(at);
                long diff = scan.log.get(at).time - scan.log.get(at-1).time;
                Point impactPoint = last.position;

                if(at > 0 && diff > 0) {
                    long l = 0;
                    long r = diff;
                    while(l < r) {
                        long mid = (l+r) / 2;
                        double percent = 1.0 * mid / diff;
                        Point estimated = last.position.multiply(1.0 - percent)
                                .add(scan.log.get(at - 1).position.multiply(percent));
                        if((scan.log.get(at-1).time + mid - logs[i].time - timeDelta) * bulletSpeed
                            >= transformedMe.distance(estimated)) {
                            r = mid;
                        } else {
                            l = mid+1;
                        }
                    }

                    double percent = 1.0 * l / diff;
                    impactPoint = last.position.multiply(1.0 - percent)
                            .add(scan.log.get(at - 1).position.multiply(percent));
                }

                Point lastPosition = impactPoint.rotate(-rotation, logs[i].position).subtract(translation);
                if(!getBattleField().contains(lastPosition))
                    continue;

                picked++;
                curAngles.add(new CandidateAngle(Physics.absoluteBearing(getPoint(), lastPosition),
                        logs[i].getDistance(latest),
                        lastPosition));
            }

            double distSum = 0;
            for(CandidateAngle candidate : curAngles) {
                distSum += candidate.weight;
            }

            double invAvg = 1.0 * curAngles.size() / distSum;
            for(CandidateAngle candidate : curAngles) {
                candidate.weight *= invAvg;
                angles.add(candidate);
            }
        }

        double best = Double.NEGATIVE_INFINITY;
        CandidateAngle bestAngle = null;

        for(CandidateAngle shootAngle: angles) {
            double weight = 0;

            for(CandidateAngle candidate : angles) {
                double distance = candidate.point.distance(getPoint());
                double angle = candidate.angle;
                double off = Utils.normalRelativeAngle(shootAngle.angle - angle);

                double diff = off / (Physics.hitAngle(distance) * 0.8);
                weight += R.exp(-0.5 * diff * diff) * candidate.weight;
            }

            if(weight > best) {
                best = weight;
                bestAngle = shootAngle;
            }
        }

        if(bestAngle == null) {
            _lastAngles = null;
            Scan scan = null;

            for(Map.Entry<String, Scan> entry : scans.entrySet()) {
                Scan cur = entry.getValue();
                double gunOffset = Utils.normalRelativeAngle(Physics.absoluteBearing(getPoint(), cur.getLatest().position)
                        - getGunHeadingRadians());
                if(scan == null || Math.toDegrees(Math.abs(gunOffset)) < 45
                        ||scan.getLatest().position.distance(getPoint()) >
                            cur.getLatest().position.distance(getPoint())) {
                    scan = cur;
                }
            }

            if(scan != null) {
                double off = Utils.normalRelativeAngle(Physics.absoluteBearing(getPoint(), scan.getLatest().position)
                        - getGunHeadingRadians());
                if (!isMelee()) {
                    setTurnGunRightRadians(off);
                } else {
                    setTurnGunRightRadians(off);
                }
            }
        } else {
            _lastAngles = angles;
            _lastBestAngle = bestAngle;
            double off = Utils.normalRelativeAngle(bestAngle.angle - getGunHeadingRadians());
            setTurnGunRightRadians(off);
        }
    }

    public boolean isMelee() {
        return getOthers() > 1;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        lastSeenEnergy = e.getEnergy();

        Scan scan = getScan(e, scans);
        track(e, scan);
    }

    public void track(ScannedRobotEvent e, Scan scan) {
        scan.dead = false;

        ScanLog last = scan.log.size() > 0 ? scan.getLatest() : null;

        double absBearing = Utils.normalAbsoluteAngle(getHeadingRadians() + e.getBearingRadians());
        Point point = getPoint().project(absBearing, e.getDistance());

        ScanLog log = new ScanLog();
        log.index = scan.log.size();
        log.time = getTime();
        log.energy = e.getEnergy();
        log.position = point;
        log.heading = e.getHeadingRadians();
        log.velocity = e.getVelocity();
        log.closestDistance = 0;
        log.closestLateralVelocity = 0;
        log.gunHeat = getTime() - _lastFireTime <= 5 ? 1 : 0;
        log.bulletsFired = bulletsFired;

        Point closest = getClosest(e.getName(), point);

        log.others = closest != null ? 1 : 0;

        if(closest != null) {
            log.closestDistance = point.distance(closest);
            double closestAbsBearing = Physics.absoluteBearing(point, closest);
            log.closestLateralVelocity = Physics.getLateralVelocityFromStationary(closestAbsBearing,
                                            log.velocity, log.heading);
        }

        log.timeAccel = 0;
        log.timeDecel = 0;
        log.timeRevert = 0;
        log.revertLast20 = 0;
        log.accel = 0;

        if(last != null) {
            if(Math.abs(last.velocity) > Math.abs(log.velocity))
                log.accel = -1;
            else if(Math.abs(last.velocity) < Math.abs(log.velocity))
                log.accel = 1;
        }

        log.timeAccel = log.accel > 0 ? 0 : 1;
        log.timeDecel = log.accel < 0 ? 0 : 1;

        for(int i = 1; i < Math.min(50, scan.log.size() - 1); i++) {
            ScanLog past = scan.log.get(scan.log.size() - i);
            ScanLog paster = scan.log.get(scan.log.size() - i - 1);

            if(log.timeAccel == i && Math.abs(paster.velocity) < Math.abs(past.velocity)) {
                log.timeAccel++;
            }

            if(log.timeDecel == i && Math.abs(paster.velocity) > Math.abs(past.velocity)) {
                log.timeDecel++;
            }
        }

        scan.log.add(log);

        if(!isMelee()) {
            double radarOffset = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
            setTurnRadarRightRadians(2*radarOffset);
        }
    }

    public ScanLog getClosestEnemy(String name, Point position) {
        ScanLog res = null;
        double best = Double.POSITIVE_INFINITY;

        for(Map.Entry<String, Scan> entry : scans.entrySet()) {
            if(entry.getKey() == name || entry.getValue().dead)
                continue;

            Scan scan = entry.getValue();
            ScanLog log = scan.getLatest();

            if(log == null)
                continue;

            double distance = position.distance(log.position);
            if(distance < best) {
                best = distance;
                res = log;
            }
        }

        return res;
    }

    public Point getClosest(String name, Point position) {
        ScanLog closestLog = getClosestEnemy(name, position);
        if(closestLog == null || getPoint().distance(position) < closestLog.position.distance(position))
            return getPoint();
        else
            return closestLog.position;
    }

    public void onRobotDeath(RobotDeathEvent e) {
        if(scans.containsKey(e.getName())) {
            scans.get(e.getName()).dead = true;
        }
    }

    public void onPaint(Graphics2D gr) {
        G g = new G(gr);

        if(_lastShootAngles != null) {
            double max = 1e-7;
            for(CandidateAngle candidate: _lastShootAngles) {
                max = Math.max(max, candidate.weight);
            }

            for (CandidateAngle candidate : _lastShootAngles) {
                if(candidate == _lastBestShootAngle)
                    g.drawPoint(candidate.point, 36, Color.GREEN);
                else
                    g.drawPoint(candidate.point, 36, G.getDangerColor(candidate.weight / max));
            }
        }
    }

    public void onSkippedTurn(SkippedTurnEvent e) {
        System.out.println("Skipped turn " + e.getSkippedTurn());
    }

    private static class Scan {
        ArrayList<ScanLog> log;
        boolean dead;

        public Scan() {
            log = new ArrayList<ScanLog>();
            dead = true;
        }

        public ScanLog getLatest() {
            return log.size() > 0 ? log.get(log.size() - 1) : null;
        }
    }

    private static class ScanLog {
        // utility
        int index;
        Point position;
        long time;
        double heading;
        double energy;
        double velocity;

        // clustering info
        double gunHeat;
        double accel;
        long timeAccel;
        long timeDecel;
        long timeRevert;
        long revertLast20;
        int others;

        long bulletsFired;

        double closestDistance;
        double closestLateralVelocity;

        public ScanLog() {}

        public double getDistance(ScanLog rhs) {
            return Math.abs(timeAccel - rhs.timeAccel) / 50 * 2.5
                    + Math.abs(timeDecel - rhs.timeDecel) / 50 * 4
                    + Math.abs(timeRevert - rhs.timeRevert)
                    + Math.abs(revertLast20 - rhs.revertLast20)
                    + Math.abs(others - rhs.others) * 8
                    + Math.abs(closestDistance - rhs.closestDistance) / 800 * 2
                    + Math.abs(closestLateralVelocity - rhs.closestLateralVelocity) / 8 * 5
                    + (Math.abs(accel - rhs.accel) / 2 + 0.5) * 3
                    + Math.abs(gunHeat - rhs.gunHeat) / 2 * 15
                    + Math.pow(0.35*Math.abs(bulletsFired - rhs.bulletsFired), 1.1)
                    + Math.abs(Math.abs(velocity) - Math.abs(rhs.velocity)) / 8 * 4;
        }
    }

    private class CandidateAngle {
        double angle;
        double weight;
        Point point;

        public CandidateAngle(double angle, double weight, Point point) {
            this.angle = angle;
            this.weight = weight;
            this.point = point;
        }
    }

    private static class ScanComparator implements Comparator<ScanLog> {
        private ScanLog reference;

        public ScanComparator(ScanLog point) {
            this.reference = point;
        }

        @Override
        public int compare(ScanLog o1, ScanLog o2) {
            double d1 = o1.getDistance(reference);
            double d2 = o2.getDistance(reference);
            if(d1 < d2) return -1;
            else if(d1 > d2) return 1;
            else return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }
}
