package roborio.movement.predictor;

import robocode.Rules;
import robocode.util.Utils;
import roborio.utils.BackAsFrontRobot;
import roborio.utils.Physics;
import roborio.utils.Point;
import roborio.utils.R;
import roborio.utils.waves.Wave;

/**
 *  This class has methods to support precise movement prediction.
 *  Note that all methods assume that you are using a BackAsFrontRobot-like robot.
 */
public abstract class MovementPredictor {

    public static PredictedPoint predictOnWaveImpact(PredictedPoint initialPoint, Wave wave, int direction) {
        PredictedPoint cur = initialPoint;
        while(!wave.hasPassed(cur, cur.time)) {
            double angle = Utils.normalAbsoluteAngle(Physics.absoluteBearing(cur, wave.getSource())
                    + R.HALF_PI * direction);
            cur = _fastTick(cur, angle, Rules.MAX_VELOCITY);
        }

        return cur;
    }


    /** This method, different from _tick, predicts assuming that the robot will attempt
     * to move infinitely (hit the maximum speed and never break)
     *
     * This is usually quicker than _tick for wavesurfing because it does not rely
     * on getVelocity() to get the velocity the bot must hit to still be able to break
     * and stop at the destination. There is no real destination here. You can tick, for
     * example, while the enemy's wave does not hit you.
     *
     * @param last point predicted in the last tick (or inital point)
     * @param angle the absolute angle the bot is trying to move, normalized
     * @param maxVelocity the maximum velocity the bot should move
     *                    usually: Physics.MAX_VELOCITY
     * @return  the current predicted point
     */
    private static PredictedPoint _fastTick(PredictedPoint last, double angle, double maxVelocity) {
        double offset = angle - last.heading;
        double turn = BackAsFrontRobot.getQuickestTurn(offset);
        int ahead = offset == turn ? 1 : -1;

        double maxTurning = Physics.maxTurningRate(last.velocity);
        double newHeading = Utils.normalAbsoluteAngle(R.constrain(-maxTurning, turn, maxTurning) + last.heading);

        double newVelocity = getFastVelocity(last.velocity, maxVelocity, ahead);

        double newX = last.x + R.sin(newHeading) * newVelocity;
        double newY = last.y + R.cos(newHeading) * newVelocity;

        return new PredictedPoint(newX, newY, newHeading, newVelocity, last.time + 1);
    }

    private static double getFastVelocity(double velocity, double maxVelocity, int ahead) {
        double aheadVelocity = ahead * velocity;
        double newVelocity;
        if(aheadVelocity < -Rules.DECELERATION)
            newVelocity = Rules.DECELERATION * ahead;
        else if(aheadVelocity >= 0) {
            newVelocity = velocity + Rules.ACCELERATION * ahead;
        } else {
            double decelerationPercent = -aheadVelocity / Rules.DECELERATION;
            newVelocity = Rules.ACCELERATION * ahead * (1.0 - decelerationPercent);
        }

        return R.constrain(-maxVelocity, newVelocity, maxVelocity);
    }

    public static class PredictedPoint extends Point {
        private final double     heading;
        private final double     velocity;
        private final long       time;

        public PredictedPoint(double x, double y, double heading, double velocity, long time) {
            super(x, y);
            this.heading = heading;
            this.velocity = velocity;
            this.time = time;
        }

        public PredictedPoint(Point point, double heading, double velocity, long time) {
            this(point.x, point.y, heading, velocity, time);
        }

        public double getHeading() {
            return heading;
        }

        public double getVelocity() {
            return velocity;
        }

        public long getTime() {
            return time;
        }
    }

}
