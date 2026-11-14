package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Scout;

// chase the mushroom or flower thats out
public class SetPowerupTarget extends Action {

    @Override
    public Status run(Blackboard bb) {
        Scout.Cand c = Bb.scout(bb).item;
        if (c == null) {
            return Status.FAILURE;
        }
        // items move so one id for all of them
        Goal g = Goal.at("powerUp", "item", c.x, c.y);
        g.wPower = 600;
        g.reachBonus = 80;
        g.tolX = 12;
        g.tolY = 14;
        bb.put(Keys.GOAL, g);
        return Status.SUCCESS;
    }
}
