package roborio.movement;

import roborio.Roborio;

/**
 * Created by Roberto Sales on 22/07/17.
 */
public class RoborioMovement extends GotoSurfingMovement {
    public RoborioMovement(Roborio robot) {
        super(robot);
    }

//    @Override
//    public void onScan(ScannedRobotEvent e) {
//        ComplexEnemyRobot enemy = EnemyTracker.getInstance().getLatestState(e);
//        setTurnRight(e.getBearing());
//
//        if(Math.abs(getTurnRemaining()) < 10) {
//            if(e.getDistance() > 300) {
//                moveWithBackAsFront(enemy.getPoint(), enemy.getDistance() / 2);
//            } else if(e.getDistance() < 100) {
//                runAwayWithBackAsFront(enemy.getPoint(), enemy.getDistance() * 2);
//            }
//        }
//    }
//
//    @Override
//    public void doMovement() {
//
//    }
}
