package rsalesc.brazil;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * Created by Roberto Sales on 25/08/17.
 */
public class SuffixAutomaton {
    State root;
    State last;

    public SuffixAutomaton() {
        root = new State();
        last = root;
    }

    public State getRootState() {
        return root;
    }

    public void append(char c, int position) {
        append(new char[]{c}, position);
    }

    public void append(char[] c, int position) {
        State newState = new State();
        newState.length = last.length + 1;
        newState.position = position;

        State cur = last;

        while(cur != null && !cur.t.containsKey(c)) {
            cur.t.put(c, newState);
            cur = cur.link;
        }

        if(cur == null) {
            newState.link = root;
        } else {
            State qState = cur.t.get(c);
            if(cur.length + 1 == qState.length) {
                newState.link = qState;
            } else {
                State pState = qState.duplicate();
                pState.length = cur.length + 1;

                while(cur != null && cur.t.get(c) == qState) {
                    cur.t.put(c, pState);
                    cur = cur.link;
                }

                qState.link = pState;
                newState.link = pState;
            }
        }

        last = newState;
    }

    public static class State {
        public int length = 0;
        public int position = 0;
        public State link = null;
        TreeMap<char[], State> t = new TreeMap<>(new Comparator<char[]>() {
            @Override
            public int compare(char[] o1, char[] o2) {
                int sz = Math.min(o1.length, o2.length);
                for(int i = 0; i < sz; i++) {
                    if(o1[i] != o2[i])
                        return (int) Math.signum(o1[i] - o2[i]);
                }

                return (int) Math.signum(o1.length - o2.length);
            }
        });

        public State duplicate() {
            State res = new State();
            res.length = length;
            res.link = link;
            res.t = (TreeMap) t.clone();
            res.position = position;
            return res;
        }

        public State tick(char c) {
            return t.get(new char[]{c});
        }

        public State tick(char[] c) {
            return t.get(c);
        }
    }
}
