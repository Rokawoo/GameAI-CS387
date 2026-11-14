package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.WorldModel;

// nothing to grab, keep moving right
// but park short of the flag, finishing early ends the run
public class SetAdvanceTarget extends Action {
    private double ahead, park;

    @Override
    protected void setup() {
        ahead = num("ahead", 200);
        park = num("park", 40);
    }

    @Override
    public Status run(Blackboard bb) {
        WorldModel w = Bb.world(bb);
        float tx = (float) (w.x + ahead);
        if (w.exitX > 0) {
            tx = (float) Math.min(tx, w.exitX - park);
        }
        Goal g = Goal.at("advance", null, tx, w.y);
        g.useY = false;
        g.sticky = true;
        g.wDist = 0.5;
        bb.put(Keys.GOAL, g);
        return Status.SUCCESS;
    }
}
