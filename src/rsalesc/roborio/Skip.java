package rsalesc.roborio;

import robocode.AdvancedRobot;

/**
 * Created by Roberto Sales on 12/08/17.
 */
public class Skip extends AdvancedRobot {

    @Override
    public void run() {
        int cnt = 0;
        while(cnt < 1e9) {
            cnt++;
        }
        execute();
    }
}
