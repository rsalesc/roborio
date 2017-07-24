package roborio.utils.waves;

/**
 * Created by Roberto Sales on 23/07/17.
 */
public abstract class WaveCondition {
    public abstract boolean test(Wave wave);

    public static class Tautology extends WaveCondition {
        @Override
        public boolean test(Wave wave) {
            return true;
        }
    }
}
