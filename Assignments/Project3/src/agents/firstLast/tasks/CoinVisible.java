package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;

// a coin we havent given up on is in view (or remembered near by)
public class CoinVisible extends Condition {

    @Override
    protected boolean check(Blackboard bb) {
        return Bb.scout(bb).coin != null;
    }
}
