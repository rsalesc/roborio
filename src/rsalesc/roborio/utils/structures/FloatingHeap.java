package rsalesc.roborio.utils.structures;

/**
 * Created by rsalesc on 20/07/17.
 */
public class FloatingHeap<T> {
    private static int      INITIAL_CAP = 3;
    private static double   FACTOR = 1.7;

    protected int           capacity;
    protected int           size;
    protected double[]      key;
    protected Object[]      data;
    protected int           signal;

    public FloatingHeap(int signal) {
        this.signal = signal;
        this.size = 0;
        this.capacity = INITIAL_CAP;
        this.data = new Object[this.capacity];
        this.key = new double[this.capacity];
    }

    void rellocate(int n) {
        double[] nkey = new double[n];
        Object[] ndata = new Object[n];
        System.arraycopy(key, 0, nkey, 0, size);
        System.arraycopy(data, 0, ndata, 0, size);
        this.key = nkey;
        this.data = ndata;
        this.capacity = n;
    }

    void resize(int n) {
        if(n <= size)
            return;
        rellocate(n);
    }

    void ensure(int n) {
        if(n > capacity)
            resize(Math.max(n, (int)Math.ceil(FACTOR*n)));
    }

    void ensure() {
        ensure(this.size + 1);
    }

    public int size() {
        return this.size;
    }

    public void push(double k, T payload) {
        ensure();
        this.key[this.size] = k;
        this.data[this.size] = payload;
        heapifyUp(this.size++);
    }

    public void push(double k) {
        push(k, null);
    }

    protected double weighted(int i) {
        return this.key[i] * this.signal;
    }

    private void heapifyUp(int u) {
        while(u > 0 && weighted((u-1)/2) < weighted(u)) {
            double tmpKey = this.key[u];
            this.key[u] = this.key[(u-1)/2];
            this.key[(u-1)/2] = tmpKey;

            Object tmpData = this.data[u];
            this.data[u] = this.data[(u-1)/2];
            this.data[(u-1)/2] = tmpData;

            u = (u-1)/2;
        }
    }

    @SuppressWarnings("unchecked")
    public Entry<T> top() {
        return new Entry<T>(this.key[0], (T)this.data[0]);
    }

    public void pop() {
        if(this.size == 0)
            throw new RuntimeException();

        this.size--;
        this.key[0] = this.key[this.size];
        this.data[0] = this.data[this.size];
        // GC
        this.data[this.size] = null;

        if(this.size > 0)
            heapifyDown(0);
    }

    private void heapifyDown(int u) {
        if(2*u+1 >= this.size) return;
        int larger =  2*u+1;
        double best = weighted(larger);

        if(2*u+2 < this.size && best < weighted(2*u+2)) {
            larger = 2 * u + 2;
            best = weighted(2*u+2);
        }

        if(best > weighted(u)) {
            double tmpKey = this.key[u];
            this.key[u] = this.key[larger];
            this.key[larger] = tmpKey;

            Object tmpData = this.data[u];
            this.data[u] = this.data[larger];
            this.data[larger] = tmpData;
        }

        heapifyDown(larger);
    }

    void assertHeap() throws Exception {
        for(int i = 0; i < this.size; i++) {
            if(2*i+1 < this.size && this.key[i]*signal < this.key[2*i+1]*signal) {
                throw new Error("not a heap");
            }

            if(2*i+2 < this.size && this.key[i]*signal < this.key[2*i+2]*signal) {
                throw new Error("not a heap");
            }
        }
    }

    void dump() {
        for(int i = 0; i < this.size; i++)
            System.out.print(this.key[i] + " ");
        System.out.println("");
    }

    public static class Entry<T> {
        public final double     key;
        public final T          payload;

        private Entry(double key, T payload) {
            this.key = key;
            this.payload = payload;
        }
    }

    public static class Max<T> extends FloatingHeap<T> {
        public Max() {
            super(1);
        }
    }

    public static class Min<T> extends FloatingHeap<T> {
        public Min() {
            super(-1);
        }
    }
}
