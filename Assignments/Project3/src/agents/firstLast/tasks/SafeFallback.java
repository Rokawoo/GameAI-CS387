package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Planner;

// last resort, no target at all, just the safest thing the planner finds
// always succeeds so the agent always presses something
public class SafeFallback extends Action {

    @Override
    public Status run(Blackboard bb) {
        Goal g = new Goal();
        g.kind = "survive";
        g.wKill = 120;
        Planner p = Bb.planner(bb);
        boolean[] act = p.plan(Bb.model(bb), g);
        if (!p.safe && bb.has(Keys.FALLBACK)) {
            act = bb.get(Keys.FALLBACK);
        }
        bb.put(Keys.GOAL, g);
        bb.put(Keys.ACTION, act);
        bb.put(Keys.BRANCH, g.toString() + (p.safe ? "" : " (no safe plan)"));
        return Status.SUCCESS;
    }
}
