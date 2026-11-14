package agents.firstLast.mario;

import java.util.ArrayList;
import java.util.List;

// respawn farming
// the engine spawns an enemy when its spawn tile scrolls into the camera window
// and the old one is gone, so standing right at the window edge and stepping
// out and back in brings the enemy back again
//
// spawn tile t is in the window when  16t - 144 <= mario x < 16t + 160
public class Farm {
    // how far we would walk back for a farm spot
    public static final float MAX_REACH = 480;

    // one place we could farm from
    public static class Spot {
        public WorldModel.Spawn spawn;
        // mario x where the spawn tile flips in or out of the window
        public float edge;
        // +1 means x >= edge is inside, -1 means x < edge is inside
        public int dirIn;
        public int floorRow;
        // kills per frame we expect while farming here
        public double rate;
        public double score;

        public float inX() {
            return edge + 3 * dirIn;
        }

        public float outX() {
            return edge - 4 * dirIn;
        }

        public boolean inside(float x) {
            return dirIn > 0 ? x >= edge : x < edge;
        }

        // frames for one spawn, die, step out, step in round
        public double cycle() {
            return 1.0 / rate;
        }

        public String id() {
            return "farm:" + spawn.key() + ":" + dirIn;
        }
    }

    // memory for the farm loop, lives on the blackboard
    public static class State {
        public String spotId;
        public boolean wentOut;
        public boolean pending;
        public int emptyTrips;
        public int lastBornSeen = -1;
    }

    // every spot we could reach, best first
    public static List<Spot> find(WorldModel w, Scout s) {
        List<Spot> out = new ArrayList<>();
        if (w.map == null) {
            return out;
        }
        for (WorldModel.Spawn sp : w.spawns.values()) {
            // only farm things we have actually seen die
            if (sp.died < 1) {
                continue;
            }
            // plain red koopas stay on their own ledge and never come to us
            if (sp.type == 4 && sp.fell == 0) {
                continue;
            }
            Spot a = check(w, s, sp, 16 * sp.tx - 144, 1);
            Spot b = check(w, s, sp, 16 * sp.tx + 160, -1);
            if (a != null) {
                out.add(a);
            }
            if (b != null) {
                out.add(b);
            }
        }
        out.sort((p, q) -> Double.compare(q.score, p.score));
        return out;
    }

    // best spot no further than radius from mario, null if none
    public static Spot within(List<Spot> spots, WorldModel w, double radius) {
        for (Spot sp : spots) {
            if (Math.abs(sp.edge - w.x) <= radius) {
                return sp;
            }
        }
        return null;
    }

    private static Spot check(WorldModel w, Scout s, WorldModel.Spawn sp, float edge, int dirIn) {
        if (Math.abs(edge - w.x) > MAX_REACH) {
            return null;
        }
        Spot c = new Spot();
        c.spawn = sp;
        c.edge = edge;
        c.dirIn = dirIn;
        if (s.isBanned(c.id(), w.tick)) {
            return null;
        }

        // need floor under both standing spots, same row
        int bestRow = -1;
        for (int r = 1; r < w.tilesH; r++) {
            if (canStand(w, c.inX(), r) && canStand(w, c.outX(), r)) {
                if (bestRow < 0 || Math.abs(r * 16 - w.y) < Math.abs(bestRow * 16 - w.y)) {
                    bestRow = r;
                }
            }
        }
        if (bestRow < 0) {
            return null;
        }
        c.floorRow = bestRow;

        // kills per frame from what we saw: deaths over their life time
        // plus a few frames for stepping out and back in
        double deathRate = (double) sp.died / Math.max(1, sp.born);
        double life = sp.died > 0 ? (double) sp.lifeSum / sp.died : 90;
        c.rate = deathRate / (life + 6);

        // closer is better, walking back costs time
        double travel = Math.abs(edge - w.x) / 8.0;
        c.score = c.rate * 100 / (1 + travel / 60.0);
        return c;
    }

    // mario stands if either foot is over a floor tile
    private static boolean canStand(WorldModel w, float x, int row) {
        int left = (int) ((x - 4) / 16);
        int right = (int) ((x + 4) / 16);
        return w.floorAt(left, row) || w.floorAt(right, row);
    }
}
