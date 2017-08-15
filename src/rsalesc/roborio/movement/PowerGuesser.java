package rsalesc.roborio.movement;

/**
 * Created by Roberto Sales on 26/07/17.
 */
public class PowerGuesser {
    private double decay;
    private double acc;

    public PowerGuesser(double decay) {
        acc = 0;
        this.decay = decay;
    }

    public double getGuess() {
        return acc;
    }

    public void push(double x) {
        acc *= (1. - decay);
        acc += x * decay;
    }
}
