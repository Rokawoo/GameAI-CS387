package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Nav;
import agents.firstLast.mario.Scout;
import agents.firstLast.mario.WorldModel;

// head for the best coin, others on the way count too
// if it is more than one jump up, aim for a stepping stone floor first
public class SetCoinTarget extends Action {

    @Override
    public Status run(Blackboard bb) {
        Scout.Cand c = Bb.scout(bb).coin;
        if (c == null) {
            return Status.FAILURE;
        }
        WorldModel w = Bb.world(bb);
        float[] hop = Nav.waypoint(w, c.tx, c.ty);
        Goal g;
        if (hop != null) {
            g = Goal.at("coinStep", c.id, hop[0], hop[1] - w.marioHeight() / 2f);
            g.tolX = 6;
            g.tolY = 8;
            g.reachBonus = 30;
        } else {
            g = Goal.at("coin", c.id, c.x, c.y);
            g.tolX = 11;
            g.tolY = 13;
            g.reachBonus = 40;
        }
        bb.put(Keys.GOAL, g);
        return Status.SUCCESS;
    }
}
