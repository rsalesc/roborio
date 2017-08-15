package rsalesc.roborio.gunning.virtual;

import rsalesc.roborio.gunning.VirtualGunArray;

/**
 * Created by Roberto Sales on 14/08/17.
 */
public class BestScoreStrategy extends GunChoosingStrategy {
    @Override
    public int choose(VirtualGunArray array) {
        int best = 0;
        for(int i = 1; i < array.length; i++) {
            if(array.score[i] > array.score[best])
                best = i;
        }

        return best;
    }
}
