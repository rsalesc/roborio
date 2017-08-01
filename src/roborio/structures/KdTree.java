package roborio.structures;

/**
 * Created by rsalesc on 20/07/17.
 */

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Implementation of K-d Tree inspired by
 * Robério e Seus Teclados' ICPC library
 * based on "Optimizing Search Strategies in k-d Trees"
 * https://graphics.stanford.edu/~tpurcell/pubs/search.pdf
 * and Rednaxela's 2nd gen
 *
 * This version additionally supports
 * approximate kNN, which is usually fine
 * for some applications and is WAY faster,
 * so you can search for much more points.
 *
 * For a given alpha 0 < alpha < 1, it reduces the
 * search ball's radius by (1-alpha)*radius, meaning
 * that points which are in the original ball but are
 * further than alpha*radius unities will be skipped.
 * Though, this ensures that the algorithm will not
 * miss neighbors closer than alpha*[distance to the
 * k-th furthest node]
 *
 * It plays well with Play-It Forward strategies :)
 *
 * TODO: do not split node if its largest bound is already epsilon
 *
 * @param <T> The type of the payload which will be
 *           stored along with the points
 *
 */
abstract public class KdTree<T> {
    private static int      BUCKET_SIZE = 28;
    private static double   EPSILON = 1e-9;

    // initial data
    protected int           dim;
    protected double[][]    points;
    private Object[]        data;
    private int             length;
    private Integer         maxLength;
    private int             bucketSize;

    // shrinked clipping window
    protected double[]           min;
    protected double[]           max;

    // splitting>
    private int             hyperplane;
    private double          cutPosition;

    // neighbors
    private KdTree<T>       left;
    private KdTree<T>       right;
    private KdTree<T>       parent;

    // old points
    private Queue<double[]>         pointQueue;

    public KdTree(int dimensions, int sizeLimit, int bucketLimit) {
        this.dim = dimensions;
        this.bucketSize = bucketLimit;

        this.points = new double[bucketLimit][];
        this.data = new Object[bucketLimit];
        this.length = 0;
        this.maxLength = sizeLimit;

        this.pointQueue = new ArrayDeque<double[]>();
        this.parent = null;

//        this.min = new double[this.dim];
//        Arrays.fill(this.min, Double.POSITIVE_INFINITY);
//        this.max = new double[this.dim];
//        Arrays.fill(this.max, Double.NEGATIVE_INFINITY);
    }

    public KdTree(int dimensions, int sizeLimit) {
        this(dimensions, sizeLimit, BUCKET_SIZE);
    }

    private KdTree(KdTree<T> parent) {
        this.dim = parent.dim;
        this.bucketSize = parent.bucketSize;

        this.points = new double[Math.max(this.bucketSize, parent.length)][];
        this.data = new Object[Math.max(this.bucketSize, parent.length)];
        this.length = 0;
        this.maxLength = parent.maxLength;

        this.parent = parent;
    }

    private class KdNode extends KdTree<T> {
        private KdNode(KdTree<T> parent) {
            super(parent);
        }

        /** Node is the node being partitioned. The hyperrectangles
         * may contain NaN values
         *
         * @param node the node being partitioned
         * @return the best hyperplane to partition this node
         */
        @Override
        public int minkowskiBestHyperplane(KdTree<T> node) {
            throw new NotImplementedException();
        }

        /** Should return the distance between two points in some
         * Minkowski distance (L1, L2, ... Loo)
         *
         * @param a the first point
         * @param b the second point
         * @return minkowski distance Lx between those points for some x >= 1
         */
        @Override
        public double minkowskiDistance(double[] a, double[] b) {
            throw new NotImplementedException();
        }

        /** Distance from the point p to the hyperrectangle
         * with boundaries at min and max
         *
         * Notice that min/max may be null, since we may be
         * checking a node that doesn't have points
         *
         * @param p query point
         * @param min min boundaries of the hyperrectangle
         * @param max max boundaries of the hyperrectangle
         * @return
         */
        @Override
        public double minkowskiToHyperrect(double[] p, double[] min, double[] max) {
            throw new NotImplementedException();
        }
    }

    public int size() {
        return this.length;
    }


    /** Add a point to the tree, along with a payload
     * associated to it. Note that in this operation,
     * points may be removed from the tree if the sizeLimit
     * parameter was specified in the construction and the
     * resulting tree have more than sizeLimit points added to
     * it. In such case, the oldest point will be removed (FIFO)
     *
     * @param point the point to be added
     * @param payload the payload to be associated to
     *                the point
     */
    public void add(double point[], T payload) {
        if(point.length != dim)
            throw new IllegalArgumentException();

        KdTree<T> current = this;

        while(!current.isLeaf() || current.isHeavy()) {
            // in this case we have to split the node
            if(current.isLeaf()) {
                current.hyperplane = this.minkowskiBestHyperplane(current);
                current.cutPosition = (current.max[current.hyperplane] + current.min[current.hyperplane]) / 2;

                if(current.cutPosition == Double.POSITIVE_INFINITY)
                    current.cutPosition = Double.MAX_VALUE;
                else if(current.cutPosition == Double.NEGATIVE_INFINITY)
                    current.cutPosition = Double.MIN_VALUE;
                else if(Double.isNaN(current.cutPosition))
                    current.cutPosition = 0;

                if(current.max[current.hyperplane] == current.min[current.hyperplane]) {
                    current.stretch();
                    break;
                }

                KdTree<T> left = new KdNode(current);
                KdTree<T> right = new KdNode(current);

                for(int i = 0; i < current.length; i++) {
                    KdTree<T> addOn = current.points[i][current.hyperplane] < current.cutPosition ? left : right;
                    addOn.extendNode(current.points[i], current.data[i]);
                }

                current.left = left;
                current.right = right;
                current.points = null;
                current.data = null;
            }

            current.updateClippingWindow(point);
            current.length++;

            if(point[current.hyperplane] < current.cutPosition)
                current = current.left;
            else
                current = current.right;
        }

        // found our node
        current.extendNode(point, payload);

        // check if tree has exceeded sizeLimit?
        this.pointQueue.add(point);
        if(this.maxLength != null && this.maxLength < this.length) {
            this.remove(this.pointQueue.poll());
        }
    }

    private void stretch() {
        double[][] newPoints = new double[this.length * 2][];
        System.arraycopy(this.points, 0, newPoints, 0, this.length);
        this.points = newPoints;

        Object[] newData = new Object[this.length * 2];
        System.arraycopy(this.data, 0, newData, 0, this.length);
        this.data = newData;
    }

    private void extendNode(double[] point, Object payload) {
        this.points[this.length] = point;
        this.data[this.length] = payload;
        this.length++;
        this.updateClippingWindow(point);
    }

    private void updateClippingWindow(double[] point) {
        if(this.length == 0) return;
        if(this.min == null) {
            this.min = new double[this.dim];
            System.arraycopy(point, 0, this.min, 0, this.dim);
            this.max = new double[this.dim];
            System.arraycopy(point, 0, this.max, 0, this.dim);
        } else {
            for(int i = 0; i < this.dim; i++) {
                if(Double.isNaN(point[i])) {
                    this.min[i] = Double.NaN;
                    this.max[i] = Double.NaN;
                } else {
                    this.min[i] = Math.min(this.min[i], point[i]);
                    this.max[i] = Math.max(this.max[i], point[i]);
                }
            }
        }
    }

    /** Return the K neighbors closest to query in the tree by a
     * factor of approximation of alpha (default is 1), which is
     * better explained at the top, in the class documentation.
     *
     * Note that the returned list may contain less than K points
     * if the tree itself have less than K points.
     *
     * @param query the query point, which may not be in the tree
     * @param K how many neighbors should be returned
     * @param alpha the factor of approximation explained in the class
     *              documentation
     * @return a list of the K (possibly less) closest neighbors
     *         in the tree of the query point, as Entry<T>
     */
    public List<Entry<T>> kNN(double[] query, int K, double alpha) {
        if(query.length != dim)
            throw new IllegalArgumentException();

        if(this.size() == 0)
            return new ArrayList<Entry<T>>();

        FloatingHeap.Min<KdTree<T>> queue = new FloatingHeap.Min<>();
        FloatingHeap.Max<T> found = new FloatingHeap.Max<>();

        int actualK = Math.min(K, size());
        queue.push(0, this);

        while(queue.size() > 0 &&
                (found.size() < actualK || queue.top().key < found.top().key)) {
            searchTick(query, actualK, alpha, queue, found);
        }

        ArrayList<Entry<T>> res = new ArrayList<>();

        while(found.size() > 0) {
            res.add(new Entry<T>(found.top().key / alpha, found.top().payload));
            found.pop();
        }

        return res;
    }

    public List<Entry<T>> kNN(double[] query, int K) {
        return kNN(query, K, 1.0);
    }

    private void searchTick(double[] query, int K, double alpha, FloatingHeap.Min<KdTree<T>> queue, FloatingHeap.Max<T> found) {
        KdTree<T> current = queue.top().payload;
        queue.pop();

        while(!current.isLeaf()) {
            // try to find out what is the most promising subtree
            KdTree<T> next;
            KdTree<T> other;

            double leftCost = this.minkowskiToHyperrect(query, current.left.min, current.left.max);
            double rightCost = this.minkowskiToHyperrect(query, current.right.min, current.right.max);
            double distance;

            if(leftCost < rightCost) {
                next = current.left;
                other = current.right;
                distance = rightCost;
            } else {
                next = current.right;
                other = current.left;
                distance = leftCost;
            }

            if(other.length > 0 && (found.size() < K || distance < found.top().key)) {
                queue.push(distance, other); // distance actually
            }

            if(other.length == 0)
                return;
            else
                current = next;
        }

        for(int i = 0; i < current.length; i++) {
            double distance = this.minkowskiDistance(query, current.points[i]) * alpha;
            if(found.size() < K) {
                found.push(distance, (T)current.data[i]);
            } else if(found.top().key > distance) {
                found.pop();
                found.push(distance, (T)current.data[i]);
            }
        }
    }


    /** Remove a point from the Kd Tree. Note, though,
     * that the point is removed according to its object
     * signature (address) instead of its actual content,
     * so if you pass another point with the same data of
     * one of the points in the tree, do not expect it to
     * be removed.
     *
     * @param point the point to be removed
     */
    public void remove(double[] point) {
        KdTree<T> current = this;

        while(!current.isLeaf()) {
            if(point[current.hyperplane] < current.cutPosition)
                current = current.left;
            else
                current = current.right;
        }

        for(int i = 0; i < current.length; i++) {
            // Note that the array address are being compared, not the arrays itself
            if(point == current.points[i]) {
                System.arraycopy(current.points, i+1, current.points, i, current.length-(i+1));
                System.arraycopy(current.data, i+1, current.data, i, current.length-(i+1));
                // GC
                current.points[current.length-1] = null;
                current.data[current.length-1] = null;
                while(current != null) {
                    current.length--;
                    current = current.parent;
                }
                return;
            }
        }

        throw new IllegalStateException("point was not found");
    }

    public static class Entry<T> {
        public final double     distance;
        public final T          payload;

        private Entry(double distance, T payload) {
            this.distance = distance;
            this.payload = payload;
        }
    }

    private boolean isLeaf() {
        return this.data != null;
    }
    private boolean isHeavy() {
        return this.length >= this.data.length;
    }

    public abstract int minkowskiBestHyperplane(KdTree<T> node);
    public abstract double minkowskiDistance(double[] a, double[] b);
    public abstract double minkowskiToHyperrect(double[] p, double[] min, double[] max);
}
