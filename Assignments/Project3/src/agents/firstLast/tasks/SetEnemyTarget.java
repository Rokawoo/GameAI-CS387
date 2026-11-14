package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Scout;

// go land on the best enemy, the kill itself is the reward
public class SetEnemyTarget extends Action {

    @Override
    public Status run(Blackboard bb) {
        Scout.Cand c = Bb.scout(bb).enemy;
        if (c == null) {
            return Status.FAILURE;
        }
        Goal g = Goal.at("stomp", c.id, c.x, c.y);
        g.wKill = 140;
        g.wDist = 0.5;
        g.reachBonus = 0;
        g.tolX = 0;
        g.tolY = 0;
        bb.put(Keys.GOAL, g);
        return Status.SUCCESS;
    }
}
