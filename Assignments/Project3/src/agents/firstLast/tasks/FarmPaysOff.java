package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;
import agents.firstLast.mario.Farm;
import agents.firstLast.mario.Scout;
import agents.firstLast.mario.WorldModel;

// time budget check before one more farm round
// frames left must cover: walk to the spot + one round + coins still ahead
// + the part of the level we never saw + the run to the flag
public class FarmPaysOff extends Condition {
    private double coinCost, tileCost, minRate;

    @Override
    protected void setup() {
        // frames we expect to spend per coin still ahead
        coinCost = num("coinCost", 10);
        // frames per tile of level we have not seen yet
        tileCost = num("tileCost", 1.0);
        // slower than this (kills per frame) is not worth standing around for
        minRate = num("minRate", 0.008);
    }

    @Override
    protected boolean check(Blackboard bb) {
        WorldModel w = Bb.world(bb);
        Farm.Spot spot = Bb.scout(bb).farm;
        if (spot == null || w.exitX < 0 || spot.rate < minRate) {
            return false;
        }
        double walk = Math.abs(spot.edge - w.x) / 8.0;
        double ahead = Scout.coinsAhead(w, spot.edge) * coinCost;
        double unseen = Math.max(0, w.exitX / 16 - w.maxSeenCol) * tileCost;
        double need = walk + spot.cycle() + ahead + unseen + Bb.framesToFinish(bb, spot.edge);
        return w.framesLeft > need;
    }
}
