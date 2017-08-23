package rsalesc.roborio.utils.stats.smoothing;

/**
 * Created by Roberto Sales on 29/07/17.
 */
public abstract class KernelSmoothing extends Smoothing {
    protected double bandwidth;

    protected double[]  profile;
    private int profileLength;

    public KernelSmoothing(double bandwidth) {
        this.bandwidth = bandwidth;
        profile = new double[1];
        profile[0] = evaluateKernel(0);
        profileLength = 1;
    }

    public abstract double evaluateKernel(double diff);

    @Override
    public double[] smooth(double[] input) {
        ensureProfile(input.length);
        double[] output = new double[input.length];

        for(int i = 0; i < input.length; i++) {
            for(int j = 0; j < input.length; j++) {
                output[i] += input[j] * retrieveKernel(i, j);
            }
        }

        return output;
    }

    public double retrieveKernel(int x, int x0) {
        int diff = x - x0;
        return profile[diff + profileLength - 1];
    }

    private void ensureProfile(int length) {
        if(length > profileLength) {
            double[] newProfile = new double[2*length - 1];
            System.arraycopy(profile, 0, newProfile,
                    length - profileLength, 2*profileLength - 1);

            for(int i = 0; i < length - profileLength; i++)
                newProfile[i] = evaluateKernel(i - length);
            for(int i = length - 1 + profileLength; i < 2*length-1; i++)
                newProfile[i] = evaluateKernel(i - length);

            profile = newProfile;
            profileLength = length;
        }
    }
}
