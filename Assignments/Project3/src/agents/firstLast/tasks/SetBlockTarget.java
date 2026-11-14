package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Scout;

// get under a block and bonk it with mario's head
// pick="smash" uses the brick smashing pick instead of the ? block one
public class SetBlockTarget extends Action {
    private boolean smash;

    @Override
    protected void setup() {
        smash = "smash".equals(params.get("pick"));
    }

    @Override
    public Status run(Blackboard bb) {
        Scout.Cand c = smash ? Bb.scout(bb).smash : Bb.scout(bb).block;
        if (c == null) {
            return Status.FAILURE;
        }
        Goal g = Goal.at(smash ? "smash" : "block", c.id, c.x, c.y);
        g.bumpX = c.tx;
        g.bumpY = c.ty;
        g.reachBonus = 120 * c.value;
        bb.put(Keys.GOAL, g);
        return Status.SUCCESS;
    }
}
