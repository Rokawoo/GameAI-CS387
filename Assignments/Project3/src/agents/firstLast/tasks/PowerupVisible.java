package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;

// a mushroom or flower is out on screen
public class PowerupVisible extends Condition {

    @Override
    protected boolean check(Blackboard bb) {
        return Bb.scout(bb).item != null;
    }
}
