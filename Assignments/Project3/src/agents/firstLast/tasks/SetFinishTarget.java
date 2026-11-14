package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.WorldModel;

// aim at the flag, touching it is allowed now
// still grabs coins and kills that are right on the way
public class SetFinishTarget extends Action {

    @Override
    public Status run(Blackboard bb) {
        WorldModel w = Bb.world(bb);
        Goal g = Goal.at("finish", null, w.exitX + 24, w.y);
        g.useY = false;
        g.sticky = true;
        g.allowFinish = true;
        // finishing is a must, a hit that only shrinks big mario is fine
        g.hurtOk = w.mode > 0;
        g.wDist = 1.0;
        g.wVel = 4;
        g.wCoin = 60;
        g.wKill = 40;
        bb.put(Keys.GOAL, g);
        return Status.SUCCESS;
    }
}
