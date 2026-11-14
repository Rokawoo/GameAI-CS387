package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;
import agents.firstLast.mario.Farm;
import agents.firstLast.mario.Scout;

// we know a spawn spot that keeps giving kills, with floor on both
// sides of its camera edge, no further than radius pixels away
// it also marks that spot as the one the rest of the branch works on
public class FarmAvailable extends Condition {
    private double radius;

    @Override
    protected void setup() {
        radius = num("radius", Farm.MAX_REACH);
    }

    @Override
    protected boolean check(Blackboard bb) {
        Scout s = Bb.scout(bb);
        s.farm = Farm.within(s.farms, Bb.world(bb), radius);
        return s.farm != null;
    }
}
