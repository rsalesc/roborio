package roborio.enemies;

import robocode.ScannedRobotEvent;

/**
 * Created by Roberto Sales on 21/07/17.
 */
public class EnemyRobot {
    private double bearing;
    private double distance;
    private double energy;
    private double heading;
    private double velocity;

    private String name;

    EnemyRobot() {
        this.clear();
    }

    EnemyRobot(ScannedRobotEvent e) {
        this.update(e);
    }

    public double getBearing() {
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void clear() {
        bearing = 0.0;
        distance = 0.0;
        energy = 0.0;
        velocity = 0.0;
        heading = 0.0;
        velocity = 0.0;

        name = "";
    }

    public boolean populated() {
        return !name.equals("");
    }

    public boolean nil() {
        return !populated();
    }

    public void update(ScannedRobotEvent e) {
        bearing = e.getBearingRadians();
        distance = e.getDistance();
        energy = e.getEnergy();
        heading = e.getHeadingRadians();
        name = e.getName();
        velocity = e.getVelocity();
    }
}
