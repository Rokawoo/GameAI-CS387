package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;

// fire mario can throw fireballs
public class IsFire extends Condition {

    @Override
    protected boolean check(Blackboard bb) {
        return Bb.world(bb).mode == 2;
    }
}
