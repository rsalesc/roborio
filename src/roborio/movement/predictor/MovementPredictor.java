package roborio.movement.predictor;

import robocode.Rules;
import robocode.util.Utils;
import roborio.movement.WallSmoothing;
import roborio.utils.*;
import roborio.utils.waves.Wave;

import java.util.ArrayList;
import java.util.List;

/**
 *  This class has methods to support precise movement prediction.
 *  Note that all methods assume that you are using a BackAsFrontRobot-like robot.
 */
public abstract class MovementPredictor {

    public static List<PredictedPoint> predictOnWaveImpact(AxisRectangle field, PredictedPoint initialPoint, Wave wave,
                                                           int direction, double perpendiculator) {
        AxisRectangle shrinkedField = field.shrink(18, 18);

        List<PredictedPoint> res = new ArrayList<PredictedPoint>();

        PredictedPoint cur = initialPoint;
        while(!wave.hasTouchedRobot(cur, cur.time)) {
            double angle = Utils.normalAbsoluteAngle(WallSmoothing.naive(shrinkedField, cur,
                    Physics.absoluteBearing(wave.getSource(), cur)
                    + perpendiculator * direction, direction));
            cur = _fastTick(cur, angle, Rules.MAX_VELOCITY);
            res.add(cur);
        }

        return res;
    }

    public static List<PredictedPoint> predictOnWaveImpact(PredictedPoint initialPoint, Wave wave,
                                                           Point dest) {
        List<PredictedPoint> res = new ArrayList<PredictedPoint>();

        PredictedPoint cur = initialPoint;
        while(!wave.hasTouchedRobot(cur, cur.time)) {
            double distance = cur.distance(dest);
            double angle = R.isNear(distance, 0) ? 0 : Physics.absoluteBearing(cur, dest);
            cur = _tick(cur, angle, Rules.MAX_VELOCITY, cur.distance(dest));
            res.add(cur);
        }

        return res;
    }

    public static Range getBetterMaximumEscapeAngle(AxisRectangle field, PredictedPoint initialPoint, Wave wave) {
        List<PredictedPoint> cwList = predictOnWaveImpact(field, initialPoint, wave, 1, R.HALF_PI);
        List<PredictedPoint> ccwList = predictOnWaveImpact(field, initialPoint, wave, -1, R.HALF_PI);

        Point cw = cwList.get(cwList.size() - 1);
        Point ccw = ccwList.get(ccwList.size() - 1);

        double absBearing = Physics.absoluteBearing(wave.getSource(), initialPoint);
        Range res = new Range();
        res.push(Utils.normalRelativeAngle(Physics.absoluteBearing(wave.getSource(), cw) - absBearing));
        res.push(Utils.normalRelativeAngle(Physics.absoluteBearing(wave.getSource(), ccw) - absBearing));

        return res;
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
        double offset = Utils.normalRelativeAngle(angle - last.heading);
        double turn = BackAsFrontRobot.getQuickestTurn(offset);
        int ahead = offset == turn ? 1 : -1;

        double maxTurning = Physics.maxTurningRate(last.velocity);
        double newHeading = Utils.normalAbsoluteAngle(R.constrain(-maxTurning, turn, maxTurning) + last.heading);

        double newVelocity = getFastVelocity(last.velocity, maxVelocity, ahead);

        double newX = last.x + R.sin(newHeading) * newVelocity;
        double newY = last.y + R.cos(newHeading) * newVelocity;

        return new PredictedPoint(newX, newY, newHeading, newVelocity, last.time + 1);
    }

    private static PredictedPoint _tick(PredictedPoint last, double angle, double maxVelocity, double remaining) {
        double offset = Utils.normalRelativeAngle(angle - last.heading);
        double turn = BackAsFrontRobot.getQuickestTurn(offset);
        int ahead = offset == turn ? +1 : -1;

        double maxTurning = Physics.maxTurningRate(last.velocity);
        double newHeading = Utils.normalAbsoluteAngle(R.constrain(-maxTurning, turn, maxTurning) + last.heading);

        double newVelocity = getNewVelocity(last.velocity, maxVelocity, ahead, remaining);

        double newX = last.x + R.sin(newHeading) * newVelocity;
        double newY = last.y + R.cos(newHeading) * newVelocity;

        return new PredictedPoint(newX, newY,  newHeading, newVelocity, last.time + 1);
    }

    private static double getNewVelocity(double velocity, double maxVelocity, int ahead, double remaining) {
        if(ahead < 0) {
            return -getNewVelocity(-velocity, maxVelocity, -ahead, remaining);
        }

        return getVelocity(velocity, maxVelocity, remaining);
    }

    /* Piece of code from Robocode engine, too hard to try to come up with this from scratch */
    private static double getVelocity(double currentVelocity,
                                      double maxVelocity, double distanceRemaining) {
        if (distanceRemaining < 0) {
            return -getVelocity(-currentVelocity, maxVelocity,
                    -distanceRemaining);
        }

        double newVelocity = currentVelocity;

        final double maxSpeed = Math.abs(maxVelocity);
        final double currentSpeed = Math.abs(currentVelocity);

        // Check if we are decelerating, i.e. if the velocity is negative.
        // Note that if the speed is too high due to a new max. velocity, we
        // must also decelerate.
        if (currentVelocity < 0 || currentSpeed > maxSpeed) {
            // If the velocity is negative, we are decelerating
            newVelocity = currentSpeed - Rules.DECELERATION;

            // Check if we are going from deceleration into acceleration
            if (newVelocity < 0) {
                // If we have decelerated to velocity = 0, then the remaining
                // time must be used for acceleration
                double decelTime = currentSpeed / Rules.DECELERATION;
                double accelTime = (1 - decelTime);

                // New velocity (v) = d / t, where time = 1 (i.e. 1 turn).
                // Hence, v = d / 1 => v = d
                // However, the new velocity must be limited by the max.
                // velocity
                newVelocity = Math.min(maxSpeed, Math.min(Rules.DECELERATION
                        * decelTime * decelTime + Rules.ACCELERATION
                        * accelTime * accelTime, distanceRemaining));

                // Note: We change the sign here due to the sign check later
                // when returning the result
                currentVelocity *= -1;
            }
        } else {
            // Else, we are not decelerating, but might need to start doing so
            // due to the remaining distance

            // Deceleration time (t) is calculated by: v = a * t => t = v / a
            final double decelTime = currentSpeed / Rules.DECELERATION;

            // Deceleration time (d) is calculated by: d = 1/2 a * t^2 + v0 * t
            // + t
            // Adding the extra 't' (in the end) is special for Robocode, and v0
            // is the starting velocity = 0
            final double decelDist = 0.5 * Rules.DECELERATION * decelTime
                    * decelTime + decelTime;

            // Check if we should start decelerating
            if (distanceRemaining <= decelDist) {
                // If the distance < max. deceleration distance, we must
                // decelerate so we hit a distance = 0

                // Calculate time left for deceleration to distance = 0
                double time = distanceRemaining / (decelTime + 1); // 1 is added
                // here due
                // to the extra 't'
                // for Robocode

                // New velocity (v) = a * t, i.e. deceleration * time, but not
                // greater than the current speed

                if (time <= 1) {
                    // When there is only one turn left (t <= 1), we set the
                    // speed to match the remaining distance
                    newVelocity = Math.max(currentSpeed - Rules.DECELERATION,
                            distanceRemaining);
                } else {
                    // New velocity (v) = a * t, i.e. deceleration * time
                    newVelocity = time * Rules.DECELERATION;

                    if (currentSpeed < newVelocity) {
                        // If the speed is less that the new velocity we just
                        // calculated, then use the old speed instead
                        newVelocity = currentSpeed;
                    } else if (currentSpeed - newVelocity > Rules.DECELERATION) {
                        // The deceleration must not exceed the max.
                        // deceleration.
                        // Hence, we limit the velocity to the speed minus the
                        // max. deceleration.
                        newVelocity = currentSpeed - Rules.DECELERATION;
                    }
                }
            } else {
                // Else, we need to accelerate, but only to max. velocity
                newVelocity = Math
                        .min(currentSpeed + Rules.ACCELERATION, maxSpeed);
            }
        }

        // Return the new velocity with the correct sign. We have been working
        // with the speed, which is always positive
        return (currentVelocity < 0) ? -newVelocity : newVelocity;
    }

    /** This implementation is not getVelocity compliant. That means Robocode
     * does not necessarily goes from decel to accel like this, but this performs
     * precisely enough for _fastTick.
     *
     * @param velocity
     * @param maxVelocity
     * @param ahead
     * @return
     */
    private static double getFastVelocity(double velocity, double maxVelocity, int ahead) {
        double aheadVelocity = ahead * velocity;
        double newVelocity;
        if(aheadVelocity < -Rules.DECELERATION)
            newVelocity = velocity + Rules.DECELERATION * ahead;
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
