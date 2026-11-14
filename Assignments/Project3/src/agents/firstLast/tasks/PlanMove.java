package agents.firstLast.tasks;

import agents.firstLast.bt.Action;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Status;
import agents.firstLast.mario.Goal;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Planner;
import agents.firstLast.mario.Scout;
import agents.firstLast.mario.WorldModel;

// works out this frames buttons for whatever goal the branch set
// uses the forward model planner, so jumps over pits are simulated not guessed
// fails if there is no safe plan, or if we stopped getting closer to the target
// (then the target goes on the blacklist and the tree picks something else)
public class PlanMove extends Action {
    private int stuckLimit, banTicks, patience;
    private double minGain;
    private boolean force;
    private String lastId;
    private double bestDist;
    private int lastGain, firstTick;
    private float lastTx, lastTy;

    @Override
    protected void setup() {
        // ticks with no progress before we give up on a target
        stuckLimit = (int) num("stuck", 45);
        // ticks on one target in total, even with progress
        patience = (int) num("patience", 1000);
        banTicks = (int) num("ban", 150);
        // pixels closer that count as real progress
        minGain = num("gain", 16);
        // force="true": act on the best plan even if nothing looks safe
        force = "true".equals(params.get("force"));
    }

    @Override
    public Status run(Blackboard bb) {
        Goal g = bb.get(Keys.GOAL);
        if (g == null) {
            return Status.FAILURE;
        }
        WorldModel w = Bb.world(bb);
        Planner p = Bb.planner(bb);
        boolean[] act = p.plan(Bb.model(bb), g);

        // keep something to press in case every branch fails
        if (!bb.has(Keys.FALLBACK)) {
            bb.put(Keys.FALLBACK, act);
        }
        if (!p.safe && !force) {
            return Status.FAILURE;
        }

        // are we still getting anywhere with this target
        if (g.id != null) {
            if (!g.id.equals(lastId)) {
                lastId = g.id;
                bestDist = p.startDist;
                lastGain = w.tick;
                firstTick = w.tick;
            } else if (Math.abs(g.tx - lastTx) + Math.abs(g.ty - lastTy) > 32) {
                // same thing but the aim point jumped (stepping stone done), fresh baseline
                bestDist = p.startDist;
                lastGain = w.tick;
            }
            lastTx = g.tx;
            lastTy = g.ty;
            boolean progress = p.reached || p.gainCoins > 0 || p.gainKills > 0;
            if (p.startDist < bestDist - minGain) {
                bestDist = p.startDist;
                progress = true;
            }
            if (progress) {
                lastGain = w.tick;
            }
            if (w.tick - lastGain > stuckLimit || w.tick - firstTick > patience) {
                giveUp(bb, g, w.tick + banTicks);
                lastId = null;
                return Status.FAILURE;
            }
        }

        bb.put(Keys.ACTION, act);
        bb.put(Keys.BRANCH, g.toString() + (p.safe ? "" : " (forced)"));
        return Status.SUCCESS;
    }

    // ban the target, for coins the whole little cluster since they share a ledge
    private static void giveUp(Blackboard bb, Goal g, int until) {
        Scout s = Bb.scout(bb);
        s.ban(g.id, until);
        if (g.id.startsWith("coin:")) {
            String[] parts = g.id.split(":");
            int tx = Integer.parseInt(parts[1]);
            int ty = Integer.parseInt(parts[2]);
            WorldModel w = Bb.world(bb);
            for (int ax = tx - 2; ax <= tx + 2; ax++) {
                for (int ay = ty - 1; ay <= ty + 1; ay++) {
                    if (w.tile(ax, ay) == WorldModel.COIN) {
                        s.ban("coin:" + ax + ":" + ay, until);
                    }
                }
            }
        }
    }
}
