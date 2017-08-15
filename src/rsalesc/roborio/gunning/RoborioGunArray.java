package rsalesc.roborio.gunning;

import rsalesc.roborio.gunning.virtual.BestScoreStrategy;
import rsalesc.roborio.gunning.virtual.WeightedHitRateScoring;
import rsalesc.roborio.utils.BackAsFrontRobot;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class RoborioGunArray extends VirtualGunArray {

    public RoborioGunArray(BackAsFrontRobot robot) {
        super(robot);

        add(new DCGuessFactorGun(robot)
            .setName("antisurfer")
            .setTargeting(new AntiSurferTargeting()));
        add(new DCGuessFactorGun(robot)
            .setName("generalpurpose")
            .setTargeting(new GeneralPurposeTargeting()));
//        add(new DCGuessFactorGun(robot)
//            .setName("antishadow")
//            .setTargeting(new AntiShadowTargeting()));

        setScoring(new WeightedHitRateScoring());
        setStrategy(new BestScoreStrategy());
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
