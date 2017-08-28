package rsalesc.brazil;

import javafx.util.Pair;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import rsalesc.roborio.utils.BackAsFrontRobot;
import rsalesc.roborio.utils.Physics;
import rsalesc.roborio.utils.geo.G;
import rsalesc.roborio.utils.geo.Point;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Roberto Sales on 25/08/17.
 */
public class RoborioLN extends BackAsFrontRobot {
    private static final int NEEDLE_SIZE = 80;
    private static final int LOG_THRESHOLD = 80;

    private static final double PI = Math.acos(-1);
    private static final int STEPS_TURN = 20;
    private static final int STEPS_VELOCITY = 3;

    private static SuffixAutomaton enemyAutomaton = new SuffixAutomaton();
    private static CharArray enemyLog = new CharArray();
    private static ArrayList<EnemyTick> tickLog = new ArrayList<>();
    private static LinkedList<Pair<Character, Integer>> queuedLog = new LinkedList<>();

    private Double lastHeading = null;

    private Point lastCandidate = null;
    private boolean scanned = false;

    @Override
    public void run() {
        dissociate();

        setColors(new Color(255, 71, 138), Color.WHITE, new Color(168, 234, 228),
            Color.WHITE, new Color(168, 234, 228));

        do {
            if(!scanned)
                spin();

//            move();
//            aim();
//            sweep();

            scanned = false;
            execute();
        } while(true);
    }

    public void spin() {
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        scanned = true;

        if(getGunTurnRemaining() == 0 && getGunHeat() == 0) {
            setFire(selectPower(e));
        }

        double distance = e.getDistance();
        double absBearing = Utils.normalAbsoluteAngle(e.getBearingRadians() + getHeadingRadians());
        setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 1.9999999);

        EnemyTick tick = new EnemyTick(e.getHeadingRadians(), e.getVelocity(), getPoint().project(absBearing, distance),
                                                                getRoundNum());

        lastCandidate = null;
        if(enemyLog.length == 0) {
            headOn(e);
        } else {
            SuffixAutomaton.State state = getBestState();

            if(state == null) {
                headOn(e);
            } else {
                double bulletPower = selectPower(e);
                Point source = getNextPosition();

                boolean collided = false;
                double traveled = 0;
                EnemyTick cur = tick;
                int position = state.position;

                System.out.println(source.distance(cur.point) - traveled);

                while(position < tickLog.size() && source.distance(cur.point) > traveled) {
                    double delta = Utils.normalRelativeAngle(tickLog.get(position).heading - tickLog.get(position - 1).heading);
                    double velocity = tickLog.get(position).velocity;
                    cur = cur.tick(delta, velocity);
                    if(!getBattleField().contains(cur.point)) {
                        collided = true;
                        break;
                    }
                    traveled += Rules.getBulletSpeed(bulletPower);
                    position++;
                }

                if(position == tickLog.size() || collided) {
                    headOn(e);
                } else {
//                    System.out.println(state.position + " " + position + " " +  tickLog.size());
                    double shootAngle = Physics.absoluteBearing(source, cur.point);
                    lastCandidate = cur.point;
                    setGunTo(shootAngle);
                }
            }
        }

        tickLog.add(tick);
        if(lastHeading != null) {
            char c = getPatternChar(lastHeading, e.getHeadingRadians(), e.getVelocity());
            queuedLog.add(new Pair<>(c, tickLog.size() - 1));

            while(queuedLog.size() > LOG_THRESHOLD) {
                Pair<Character, Integer> pair = queuedLog.pollFirst();
                enemyLog.append(pair.getKey());
                enemyAutomaton.append(pair.getKey(), pair.getValue());
            }
        }

        lastHeading = e.getHeadingRadians();
    }

    private SuffixAutomaton.State getBestState() {
        int targetLength;
        SuffixAutomaton.State state = null;
        for(targetLength = Math.min(NEEDLE_SIZE, enemyLog.length); targetLength > 0; targetLength--) {
            SuffixAutomaton.State st = tickArray(enemyLog.length - targetLength, targetLength, false);
            if(st != null) {
                state = st;
                break;
            }

            st = tickArray(enemyLog.length - targetLength, targetLength, true);
            if(st != null) {
                state = st;
                break;
            }
        }

        return state;
    }

    private SuffixAutomaton.State tickArray(int start, int size, boolean rev) {
        SuffixAutomaton.State cur = enemyAutomaton.getRootState();
        for(int i = 0; i < size; i++) {
            cur = cur.tick((char) ((rev ? -1 : 1) * enemyLog.get(start + i)));
            if(cur == null)
                return null;
        }

        return cur;
    }

    private double selectPower(ScannedRobotEvent e) {
        return Math.min(3.0, Math.max(getEnergy() - 0.2, 0));
    }

    public void headOn(ScannedRobotEvent e) {
        setGunTo(Utils.normalAbsoluteAngle(e.getBearingRadians() + getHeadingRadians()));
    }

    public char getPatternChar(double lastHeading, double heading, double velocity) {
        double delta = Utils.normalRelativeAngle(heading - lastHeading);
        delta = Math.min(Rules.MAX_TURN_RATE, Math.max(-Rules.MAX_TURN_RATE, delta));

        char c = (char) ((int) (delta / Rules.MAX_TURN_RATE * STEPS_TURN) * STEPS_VELOCITY
                + (int) (Math.abs(velocity) / Rules.MAX_VELOCITY * STEPS_VELOCITY));

        if(velocity < 0) c = (char) -c;
        return c;
    }

    public EnemyTick tickFromChar(EnemyTick tick, char c) {
        int b = (byte) c;
        int abs = Math.abs(b);

        int turnSegment = abs / STEPS_VELOCITY;
        int velocitySegment = abs % STEPS_VELOCITY;

        double delta = turnSegment * Rules.MAX_TURN_RATE;
        double velocity = velocitySegment * Rules.MAX_VELOCITY;
        if(b < 0)
            velocity = -velocity;

        return tick.tick(delta, velocity);
    }

    @Override
    public void onPaint(Graphics2D gr) {
        G g = new G(gr);
        if(lastCandidate != null) g.drawPoint(lastCandidate, Physics.BOT_WIDTH * 2, Color.WHITE);
    }

    private static class EnemyTick {
        public final double heading;
        public final double velocity;
        public final Point point;
        public final int round;

        private EnemyTick(double heading, double velocity, Point point, int round) {
            this.heading = heading;
            this.velocity = velocity;
            this.point = point;
            this.round = round;
        }

        private EnemyTick tick(double delta, double velocity) {
            double newHeading = Utils.normalAbsoluteAngle(heading + delta);

            return new EnemyTick(newHeading, velocity, point.project(newHeading, velocity), round);
        }
    }
}
