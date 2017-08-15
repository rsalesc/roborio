package rsalesc.roborio.structures;

import org.junit.Test;

import java.util.ArrayList;
import java.util.PriorityQueue;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by Roberto Sales on 20/07/17.
 */
public class FloatingHeapTest {
    private int dataCount;
    private double[] data;
    private boolean[] isPop;
    private int num;
    private int den;

    double randomWithRange(double min, double max)
    {
        double range = (max - min);
        return (Math.random() * range) + min;
    }

    double random() {
        return randomWithRange(-100, 100);
    }

    double[] withHeap(int signal) throws Exception {
        signal *= -1;
        FloatingHeap<Integer> heap = new FloatingHeap<Integer>(signal);
        ArrayList<Double> list = new ArrayList<>();
        for(int i = 0; i < dataCount; i++) {
            if(isPop[i]){
                list.add(heap.size() == 0 ? 0.0 : heap.top().key);
                if(heap.size() > 0) heap.pop();
            } else {
                heap.push(data[i], null);
            }
        }

        double[] res = new double[list.size()];
        int i = 0;
        for(Double x : list)
            res[i++] = x;

        return res;
    }

    double[] withoutHeap(int signal) {
        PriorityQueue<Double> pq = new PriorityQueue<>();
        ArrayList<Double> list = new ArrayList<>();
        for(int i = 0; i < dataCount; i++) {
            if(isPop[i]){
                list.add(pq.size() == 0 ? 0.0 : pq.poll());
            } else {
                pq.add(data[i]*signal);
            }
        }

        double[] res = new double[list.size()];
        int i = 0;
        for(Double x : list)
            res[i++] = x*signal;

        return res;
    }

    @Test
    public void consistency() throws Exception {
        num = 1;
        den = 10;
        dataCount = 1000000;

        data = new double[dataCount];
        isPop = new boolean[dataCount];
        for(int i = 0; i < dataCount; i++) {
            if((Math.random()*den) < num) {
                isPop[i] = true;
            } else {
                data[i] = random();
                isPop[i] = false;
            }
        }

        double[] p1 = withHeap(-1);
        double[] p2 = withoutHeap(-1);

        if(p1.length != p2.length)
            throw new Error("length should be equal");

        double error = 0;
        for(int i = 0; i < p1.length; i++) {
            error += Math.abs(p1[i] - p2[i]);
        }

        assertTrue(error < 1e-9);
    }
}