package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;
import agents.firstLast.mario.Scout;

// an enemy is right next to mario and closing in
public class InDanger extends Condition {
    private double range;

    @Override
    protected void setup() {
        range = num("range", 40);
    }

    @Override
    protected boolean check(Blackboard bb) {
        Scout s = Bb.scout(bb);
        s.threat = Scout.nearestThreat(Bb.world(bb), range);
        return s.threat != null;
    }
}
