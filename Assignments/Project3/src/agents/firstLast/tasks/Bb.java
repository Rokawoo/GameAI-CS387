package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Planner;
import agents.firstLast.mario.Scout;
import agents.firstLast.mario.WorldModel;
import engine.core.MarioForwardModel;

// short getters so the leaves stay readable
final class Bb {
    private Bb() {
    }

    static WorldModel world(Blackboard bb) {
        return bb.get(Keys.WORLD);
    }

    static Scout scout(Blackboard bb) {
        return bb.get(Keys.SCOUT);
    }

    static Planner planner(Blackboard bb) {
        return bb.get(Keys.PLANNER);
    }

    static MarioForwardModel model(Blackboard bb) {
        return bb.get(Keys.MODEL);
    }

    // frames needed to run to the flag from some x, with a safety margin
    static double framesToFinish(Blackboard bb, double fromX) {
        WorldModel w = world(bb);
        if (w.exitX < 0) {
            return 0;
        }
        double speed = bb.getDouble("finishSpeed", 5.5);
        double margin = bb.getDouble("finishMargin", 45);
        return Math.max(0, w.exitX - fromX) / speed + margin;
    }
}
