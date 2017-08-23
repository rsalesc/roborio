package rsalesc.roborio.utils.nn;

import rsalesc.roborio.gunning.utils.TargetingLog;
import rsalesc.roborio.utils.R;
import rsalesc.roborio.utils.SlicingStrategy;
import rsalesc.roborio.utils.Strategy;
import rsalesc.roborio.utils.stats.smoothing.KernelSmoothing;

import java.util.ArrayList;

/**
 * Created by Roberto Sales on 17/08/17.
 * TODO: batch training
 */
public class GuessFactorNetwork {
    public static final int    BUCKET_SIZE = 51;
    public static final int    BUCKET_MID = (BUCKET_SIZE - 1) / 2;

    private boolean built = false;

    private int totalSlices = 0;
    private double[][]  slices;
    private double      rate;
    private MLP         net;
    private ArrayList<double[]> _slices;

    private KernelSmoothing smoother;
    private Strategy strategy;
    private MLPRegularization regularization;
    private MLPStrategy networkStrategy = new SoftmaxStrategy();

    public GuessFactorNetwork() {
        _slices = new ArrayList<>();
    }

    private void setupSlices() {
        totalSlices = 0;
        for(double[] slice : _slices) {
            totalSlices += slice.length;
        }
        slices = _slices.toArray(new double[0][]);
        _slices.clear();
    }

    public GuessFactorNetwork setSlices(double[][] slices) {
        _slices.clear();
        for(double[] slice : slices) {
            _slices.add(slice);
        }

        return this;
    }

    public GuessFactorNetwork addSlice(double[] slice) {
        _slices.add(slice);
        return this;
    }

    public GuessFactorNetwork setLearningRate(double rate) {
        this.rate = rate;
        return this;
    }

    public GuessFactorNetwork setNetworkStrategy(MLPStrategy strategy) {
        this.networkStrategy = strategy;
        return this;
    }

    private void setupCommon() {
        setupSlices();
        net = new MLP(totalSlices, new int[0], BUCKET_SIZE).setStrategy(networkStrategy);
        if(regularization != null)
            net.setRegularization(regularization);
        built = true;
    }

    public GuessFactorNetwork setStrategy(Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public GuessFactorNetwork setSlicingStrategy(SlicingStrategy strategy) {
        this.strategy = strategy;
        return setSlices(strategy.getSlices());
    }

    public GuessFactorNetwork build() {
        setupCommon();
        net.build();
        return this;
    }

    public GuessFactorNetwork buildRandomly() {
        setupCommon();
        net.buildRandomly();
        return this;
    }

    public boolean isBuilt() {
        return built;
    }

    public GuessFactorNetwork setSmoother(KernelSmoothing smoother) {
        this.smoother = smoother;
        return this;
    }

    private double[] getInput(double[] query) {
        double[] res = new double[totalSlices];

        int ptr = 0;
        for(int i = 0; i < slices.length; i++) {
            double length = slices[i][slices[i].length - 1] - slices[i][0];
            double offset = -slices[i][0];
            double normQuery = R.constrain(0, (query[i] + offset) / length, 1.0);
            for(int j = 0; j < slices[i].length; j++) {
                double ref = (slices[i][j] + offset) / length;
                res[ptr++] = getRadialActivation((normQuery - ref) * (slices[i].length - 1));
            }
        }

        return res;
    }

    private double[] getExpectedOutput(double gf) {
        if(smoother == null)
            return getRawExpectedOutput(gf);

        double[] res = new double[BUCKET_SIZE];
        for(int i = 0; i < BUCKET_SIZE; i++) {
            double correspondingGf = (double) (i - BUCKET_MID) / BUCKET_MID;
            res[i] = Math.max(smoother.evaluateKernel(gf - correspondingGf), 1e-8);
        }

//        return net.getStrategy().getActivation(res);
        return res;
    }

    private double[] getRawExpectedOutput(double gf) {
        double[] res = new double[BUCKET_SIZE];
        double frac = (gf * BUCKET_MID + BUCKET_MID);
        int bucket = (int) frac;
        if(bucket + 1 < BUCKET_SIZE)
            res[bucket + 1] = R.sin((frac - bucket) * R.HALF_PI);
        res[bucket] = R.sin((bucket + 1.0 - frac) * R.HALF_PI);

//        return net.getStrategy().getActivation(res);
        return res;
    }

    public double train(double[] query, double expectedGF) {
        double[] expected = getExpectedOutput(expectedGF);
        return net.train(getInput(query), expected, rate);
    }

    public double[] feed(double[] query) {
        return net.feed(getInput(query));
    }

    public double train(TargetingLog m) {
        double gf = m.getGfFromAngle(m.hitAngle);
        return train(strategy.getQuery(m), gf);
    }

    public double train(TargetingLog m, double expectedGF) {
        return train(strategy.getQuery(m), expectedGF);
    }

    public double[] feed(TargetingLog f) {
        return feed(strategy.getQuery(f));
    }

    public double getBestGuessFactor(double[] query) {
        double[] buffer = feed(query);
        int best = 0;
        double bestHeight = -1;

        for(int i = 0; i < buffer.length; i++) {
            if(buffer[i] > bestHeight) {
                best = i;
                bestHeight = buffer[i];
            }
        }

        return (double) (best - BUCKET_MID) / BUCKET_MID;
    }

    public double getBestGuessFactor(TargetingLog f) {
        return getBestGuessFactor(strategy.getQuery(f));
    }

    private double getRadialActivation(double x) {
        return R.exp(-x*x);
    }

    public MLPRegularization getRegularization() {
        return regularization;
    }

    public GuessFactorNetwork setRegularization(MLPRegularization regularization) {
        this.regularization = regularization;
        return this;
    }
}
