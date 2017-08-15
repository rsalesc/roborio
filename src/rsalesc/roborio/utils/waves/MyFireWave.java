package rsalesc.roborio.utils.waves;

import rsalesc.roborio.myself.MyLog;

/**
 * Created by Roberto Sales on 25/07/17.
 */
public class MyFireWave extends MyWave {
    private double angle;
    public MyFireWave(MyLog log, double angle, double velocity) {
        super(log, velocity);
        this.angle = angle;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    public double getPower() {
        return (20. - getVelocity()) / 3;
    }
}
