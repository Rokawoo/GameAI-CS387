package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;

// some stompable enemy is close enough to go after
public class EnemyTargetAvailable extends Condition {

    @Override
    protected boolean check(Blackboard bb) {
        return Bb.scout(bb).enemy != null;
    }
}
