package rsalesc.roborio.utils;

/**
 * Created by Roberto Sales on 21/08/17.
 */
public class BattleTime implements Comparable<BattleTime> {
    private Long time;
    private final Integer round;

    public BattleTime(Long time, Integer round) {
        this.time = time;
        this.round = round;
    }

    public int getRound() {
        return round;
    }

    public long getTime() {
        return time;
    }

    @Override
    public int compareTo(BattleTime o) {
        if((long) round == o.round)
            return time.compareTo(o.time);
        return round.compareTo(o.round);
    }
}
