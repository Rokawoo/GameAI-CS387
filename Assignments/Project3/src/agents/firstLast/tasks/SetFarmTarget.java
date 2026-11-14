package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Farm;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Scout;
import agents.firstLast.mario.WorldModel;

// one step of the farm loop
//   enemy out   -> wait on the inside edge, stomp it if it walks up
//   enemy gone  -> step out so its spawn tile leaves the camera window
//   we are out  -> step back in, engine spawns it again
// this is the fsm-ish part that is clunky in a bt, the loop state lives on the blackboard
public class SetFarmTarget extends Action {
    private int maxEmpty, maxWait;

    @Override
    protected void setup() {
        maxEmpty = (int) num("trips", 4);
        maxWait = (int) num("wait", 150);
    }

    @Override
    public Status run(Blackboard bb) {
        WorldModel w = Bb.world(bb);
        Scout s = Bb.scout(bb);
        Farm.Spot spot = s.farm;
        if (spot == null) {
            return Status.FAILURE;
        }

        // new spot, fresh loop memory
        Farm.State st = bb.get(Keys.FARM);
        if (st == null || !spot.id().equals(st.spotId)) {
            st = new Farm.State();
            st.spotId = spot.id();
            bb.put(Keys.FARM, st);
        }

        boolean inside = spot.inside(w.x);
        float standY = spot.floorRow * 16 - w.marioHeight() / 2f;
        Goal g;

        if (spot.spawn.alive) {
            // enemy is around, hold the inside spot
            st.wentOut = false;
            st.emptyTrips = 0;
            if (st.lastBornSeen != spot.spawn.lastBorn) {
                st.lastBornSeen = spot.spawn.lastBorn;
            }
            if (w.tick - spot.spawn.lastBorn > maxWait) {
                // it just walks around and never dies, not worth it
                s.ban(spot.id(), w.tick + 300);
                return Status.FAILURE;
            }
            g = Goal.at("farmWait", null, spot.inX() + 6 * spot.dirIn, standY);
            g.wKill = 180;
        } else if (!inside) {
            // out of the window, walk back in to spawn it
            st.wentOut = true;
            g = Goal.at("farmIn", null, spot.inX() + 2 * spot.dirIn, standY);
        } else if (st.wentOut) {
            // just came back in, the spawn fires during this tick
            st.wentOut = false;
            st.pending = true;
            g = Goal.at("farmIn", null, spot.inX() + 2 * spot.dirIn, standY);
        } else {
            // nothing came out last time we stepped in
            if (st.pending) {
                st.pending = false;
                st.emptyTrips++;
                if (st.emptyTrips >= maxEmpty) {
                    s.ban(spot.id(), w.tick + 300);
                    return Status.FAILURE;
                }
            }
            g = Goal.at("farmOut", null, spot.outX(), standY);
        }

        g.sticky = true;
        g.tolX = 1;
        g.wDist = 1.0;
        bb.put(Keys.GOAL, g);
        return Status.SUCCESS;
    }
}
