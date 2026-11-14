package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.WorldModel;
import engine.helper.MarioActions;

// a fireball comes out on a fresh press of speed
// so flip speed vs last frame, but only if the plan still lives with it
public class ShootFireball extends Action {

    @Override
    public Status run(Blackboard bb) {
        boolean[] act = bb.get(Keys.ACTION);
        if (act == null) {
            return Status.FAILURE;
        }
        WorldModel w = Bb.world(bb);
        int speed = MarioActions.SPEED.getValue();
        boolean[] shot = act.clone();
        shot[speed] = !w.lastAction[speed];
        if (shot[speed] == act[speed]) {
            return Status.SUCCESS;
        }
        if (Bb.planner(bb).variantIsSafe(Bb.model(bb), shot)) {
            bb.put(Keys.ACTION, shot);
            return Status.SUCCESS;
        }
        return Status.FAILURE;
    }
}
