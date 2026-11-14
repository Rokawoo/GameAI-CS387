package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;
import agents.firstLast.mario.WorldModel;

// true when the clock says we have to head for the flag now
// frames left vs distance to flag / rush speed + margin
public class MustFinish extends Condition {
    private double speed, margin;

    @Override
    protected void setup() {
        speed = num("speed", 5.5);
        margin = num("margin", 45);
    }

    @Override
    protected boolean check(Blackboard bb) {
        // share the knobs with the farm check
        bb.put("finishSpeed", speed);
        bb.put("finishMargin", margin);
        WorldModel w = Bb.world(bb);
        if (w.exitX < 0) {
            return false;
        }
        return w.framesLeft <= Bb.framesToFinish(bb, w.x);
    }
}
