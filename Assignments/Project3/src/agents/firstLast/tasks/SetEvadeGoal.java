package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Scout;
import agents.firstLast.mario.WorldModel;

// something is on top of us
// stompable -> try to land on it, otherwise back off
// the planner throws out any plan that dies so this is safe either way
public class SetEvadeGoal extends Action {

    @Override
    public Status run(Blackboard bb) {
        WorldModel w = Bb.world(bb);
        Scout.Cand t = Bb.scout(bb).threat;
        Goal g;
        if (t != null && WorldModel.stompable(t.foe.type)) {
            g = Goal.at("stompThreat", t.id, t.x, t.y - 8);
            g.wKill = 160;
            g.wDist = 0.3;
            g.reachBonus = 0;
            g.tolX = 0;
            g.tolY = 0;
        } else if (t != null) {
            float away = w.x - 48 * Math.signum(t.x - w.x);
            g = Goal.at("backOff", null, away, w.y);
            g.useY = false;
            g.sticky = true;
            g.wDist = 0.4;
        } else {
            g = new Goal();
            g.kind = "evade";
        }
        bb.put(Keys.GOAL, g);
        return Status.SUCCESS;
    }
}
