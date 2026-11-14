package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;
import agents.firstLast.mario.WorldModel;

// a fireball thrown now would roll into an enemy
// fireballs bounce along the floor so roughly same height is enough
public class EnemyInFireLine extends Condition {
    private double range;

    @Override
    protected void setup() {
        range = num("range", 112);
    }

    @Override
    protected boolean check(Blackboard bb) {
        WorldModel w = Bb.world(bb);

        // fireballs go the way mario faces
        // guess it from speed, or from the last input if standing still
        int facing = 1;
        if (w.xa > 0.5) {
            facing = 1;
        } else if (w.xa < -0.5) {
            facing = -1;
        } else if (w.lastAction[0]) {
            facing = -1;
        }

        for (WorldModel.Foe f : w.foes) {
            // spikies shrug fireballs off
            if (f.type == 8 || f.type == 9) {
                continue;
            }
            double dx = (f.x - w.x) * facing;
            double dy = f.y - w.y;
            if (dx > 8 && dx < range && dy > -24 && dy < 40) {
                return true;
            }
        }
        return false;
    }
}
