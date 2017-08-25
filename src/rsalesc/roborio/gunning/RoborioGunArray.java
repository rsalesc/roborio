package rsalesc.roborio.gunning;

import rsalesc.roborio.energy.MirrorPowerManager;
import rsalesc.roborio.gunning.virtual.BestScoreStrategy;
import rsalesc.roborio.gunning.virtual.WeightedHitRateScoring;
import rsalesc.roborio.utils.BackAsFrontRobot;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class RoborioGunArray extends VirtualGunArray {

    public RoborioGunArray(BackAsFrontRobot robot) {
        super(robot);

        add(new GuessFactorGun(robot)
                .setName("antisurfer+")
                .setTargeting(new AntiSurferPlusTargeting()));
//        add(new GuessFactorGun(robot)
//                .setName("mygun")
//                .setTargeting(new MyAdaptiveTargeting()));
//        add(new GuessFactorGun(robot)
//                    .setName("antisurferNN")
//                    .setTargeting(new AntiSurferNNTargeting()));
//        add(new GuessFactorGun(robot)
//            .setName("generalpurpose")
//            .setTargeting(new AntiEverythingTargeting()));
        add(new GuessFactorGun(robot)
                .setName("generalpurpose2")
                .setTargeting(new AntiEverything2Targeting()));
//        add(new GuessFactorGun(robot)
//            .setName("antishadow")
//            .setTargeting(new AntiShadowTargeting()));

        setScoring(new WeightedHitRateScoring());
        setStrategy(new BestScoreStrategy());
    }

    @Override
    public void doGunning() {
        if(getManager() != null && getManager() instanceof MirrorPowerManager)
            ((MirrorPowerManager) getManager()).setScore(getActiveScore());
        super.doGunning();
    }

    public VirtualGunArray build() {
        super.build();
        return this;
    }

    @Override
    public String getName() {
        return "roborio_array";
    }
}
