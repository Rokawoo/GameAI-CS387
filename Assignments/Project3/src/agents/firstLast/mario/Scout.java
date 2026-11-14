package agents.firstLast.mario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// looks over what mario knows and ranks things worth going after
// runs once a tick before the tree, the tree just reads the answers
// each candidate gets value / (1 + travel cost), the slide style utility
public class Scout {

    public static class Cand {
        public String id;
        public String kind;
        public float x, y;
        public int tx = -1, ty = -1;
        public double value, utility;
        public WorldModel.Foe foe;
    }

    // stuff we gave up on, id -> tick it is allowed again
    private final Map<String, Integer> banned = new HashMap<>();

    // this ticks picks, null if nothing
    public Cand coin, enemy, block, smash, item, threat;
    // every farm spot in reach, and the one the tree is looking at now
    public List<Farm.Spot> farms = new ArrayList<>();
    public Farm.Spot farm;

    public void ban(String id, int untilTick) {
        if (id != null) {
            banned.put(id, untilTick);
        }
    }

    public boolean isBanned(String id, int tick) {
        Integer t = banned.get(id);
        return t != null && tick < t;
    }

    public void refresh(WorldModel w) {
        coin = bestCoin(w);
        enemy = bestEnemy(w);
        block = bestBlock(w, false);
        smash = bestBlock(w, true);
        item = bestItem(w);
        threat = null;
        farms = Farm.find(w, this);
        farm = null;
    }

    // coins we know about past some x, still waiting to be grabbed
    public static int coinsAhead(WorldModel w, float fromX) {
        int n = 0;
        for (int tx = Math.max(0, (int) (fromX / 16)); tx < w.tilesW; tx++) {
            for (int ty = 0; ty < w.tilesH; ty++) {
                if (w.tile(tx, ty) == WorldModel.COIN) {
                    n++;
                }
            }
        }
        return n;
    }

    // rough frames to get somewhere
    // sideways ~6px a frame, up is slow, down is cheap
    public static double travel(WorldModel w, double tx, double ty) {
        double dx = Math.abs(tx - w.x);
        double up = Math.max(0, w.y - ty);
        double down = Math.max(0, ty - w.y);
        return dx / 6.0 + up / 4.0 + down / 12.0 + 4;
    }

    // value over time, like the slides
    // 25 frames away = worth half
    private static double util(double value, double frames) {
        return value / (1 + frames / 25.0);
    }

    // ---- coins ----

    // look a bit behind and a bit more ahead
    private Cand bestCoin(WorldModel w) {
        Cand best = null;
        int mtx = (int) (w.x / 16);
        for (int tx = mtx - 10; tx <= mtx + 12; tx++) {
            for (int ty = 0; ty < w.tilesH; ty++) {
                if (w.tile(tx, ty) != WorldModel.COIN) {
                    continue;
                }

                // gave up on it a bit ago
                String id = "coin:" + tx + ":" + ty;
                if (isBanned(id, w.tick)) {
                    continue;
                }

                // way above us, needs a ledge near it to jump from
                float cx = tx * 16 + 8;
                float cy = ty * 16 + 8;
                if (w.y - cy > 72 && !floorNear(w, tx, ty, 5)) {
                    continue;
                }

                // coins in a bunch are worth more together
                int cluster = coinsAround(w, tx, ty);
                Cand c = new Cand();
                c.id = id;
                c.kind = "coin";
                c.tx = tx;
                c.ty = ty;
                c.x = cx;
                c.y = cy;
                c.value = 1 + 0.5 * (cluster - 1);
                c.utility = util(c.value, travel(w, cx, cy));
                if (best == null || c.utility > best.utility) {
                    best = c;
                }
            }
        }
        return best;
    }

    // coins in the 5x5 around a tile, counts itself
    private static int coinsAround(WorldModel w, int tx, int ty) {
        int n = 0;
        for (int ax = tx - 2; ax <= tx + 2; ax++) {
            for (int ay = ty - 2; ay <= ty + 2; ay++) {
                if (w.tile(ax, ay) == WorldModel.COIN) {
                    n++;
                }
            }
        }
        return n;
    }

    // is there a floor a few rows under this tile, close by
    private static boolean floorNear(WorldModel w, int tx, int ty, int rows) {
        for (int ax = tx - 2; ax <= tx + 2; ax++) {
            for (int ay = ty + 1; ay <= ty + rows; ay++) {
                if (w.floorAt(ax, ay)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- enemies ----

    private Cand bestEnemy(WorldModel w) {
        Cand best = null;
        for (WorldModel.Foe f : w.foes) {
            // spikies and plants, no
            if (!WorldModel.stompable(f.type)) {
                continue;
            }
            // already dropping into a pit
            if (f.y > w.tilesH * 16 - 8) {
                continue;
            }
            // not too far back, not too far ahead, not way above
            double dx = f.x - w.x;
            if (dx < -96 || dx > 160 || w.y - f.y > 80) {
                continue;
            }
            // gave up on it a bit ago
            String id = foeId(f);
            if (isBanned(id, w.tick)) {
                continue;
            }

            // aim a bit above it, we want to land on it
            Cand c = new Cand();
            c.id = id;
            c.kind = "enemy";
            c.foe = f;
            c.x = f.x;
            c.y = f.y - 8;
            // winged ones take 2 stomps
            if (WorldModel.winged(f.type)) {
                c.value = 0.7;
            } else {
                c.value = 1.0;
            }
            // a little less than a coin, stomping is riskier
            c.utility = 0.9 * util(c.value, travel(w, c.x, c.y));
            if (best == null || c.utility > best.utility) {
                best = c;
            }
        }
        return best;
    }

    // ---- blocks ----

    // smash=false: ? blocks, and bricks that might hide something while small
    // smash=true : plain bricks big mario can break, low value filler
    private Cand bestBlock(WorldModel w, boolean smash) {
        Cand best = null;
        int mtx = (int) (w.x / 16);
        for (int bx = mtx - 6; bx <= mtx + 10; bx++) {
            for (int by = 0; by < w.tilesH; by++) {
                // not a block we care about
                double value = blockValue(w, bx, by, smash);
                if (value <= 0) {
                    continue;
                }
                // gave up on it a bit ago
                String id = "block:" + bx + ":" + by;
                if (isBanned(id, w.tick)) {
                    continue;
                }
                // need a floor right under it to jump from
                int floor = floorUnder(w, bx, by);
                if (floor < 0) {
                    continue;
                }

                // aim at the floor spot under it
                Cand c = new Cand();
                c.id = id;
                c.kind = "block";
                c.tx = bx;
                c.ty = by;
                c.x = bx * 16 + 8;
                c.y = floor * 16 - 8;
                c.value = value;
                c.utility = util(value, travel(w, c.x, floor * 16 - 1));
                if (best == null || c.utility > best.utility) {
                    best = c;
                }
            }
        }
        return best;
    }

    // how much a block is worth, 0 = skip it
    private static double blockValue(WorldModel w, int bx, int by, boolean smash) {
        int t = w.tile(bx, by);

        // brick smashing pass, only big mario breaks bricks
        if (smash) {
            if (t == WorldModel.BRICK && w.mode > 0) {
                return 0.2;
            }
            return 0;
        }

        // ? blocks always have something
        if (t == WorldModel.QUESTION) {
            return 1.2;
        }
        // brick might hide a mushroom
        // cheap to test while small, once per brick
        if (t == WorldModel.BRICK && w.mode == 0 && !w.probed.contains(pack(bx, by))) {
            return 0.35;
        }
        return 0;
    }

    // floor row under a block that mario can bonk it from, -1 if none
    // big mario is taller so the floor has to be lower
    public static int floorUnder(WorldModel w, int bx, int by) {
        int lo = by + 2;
        int hi = by + 5;
        if (w.mode > 0) {
            lo = by + 3;
            hi = by + 6;
        }
        for (int r = by + 1; r <= hi && r < w.tilesH; r++) {
            if (r >= lo && w.standable(bx, r)) {
                return r;
            }
            // something solid in the way
            if (WorldModel.solid(w.tile(bx, r))) {
                return -1;
            }
        }
        return -1;
    }

    public static long pack(int tx, int ty) {
        return ((long) tx << 16) | (ty & 0xffff);
    }

    // ---- items ----

    private Cand bestItem(WorldModel w) {
        Cand best = null;
        for (int[] it : w.items) {
            Cand c = new Cand();
            c.kind = "item";
            c.tx = it[0];
            c.ty = it[1];
            c.x = it[0] * 16 + 8;
            c.y = it[1] * 16 + 8;
            c.id = "item:" + it[0] + ":" + it[1];
            // power up is big unless we are already fire (then its 1 coin)
            if (w.mode < 2) {
                c.value = 3;
            } else {
                c.value = 1;
            }
            c.utility = util(c.value, travel(w, c.x, c.y));
            if (best == null || c.utility > best.utility) {
                best = c;
            }
        }
        return best;
    }

    // ---- danger ----

    // closest enemy that is right next to us and coming closer
    public static Cand nearestThreat(WorldModel w, double range) {
        Cand best = null;
        double bestD = 1e9;
        for (WorldModel.Foe f : w.foes) {
            // outside the danger box
            double dx = f.x - w.x;
            double dy = f.y - w.y;
            if (Math.abs(dx) > range || dy < -48 || dy > 32) {
                continue;
            }
            // moving away and not on top of us, fine
            double closing = (f.x - f.px) * -Math.signum(dx);
            if (closing <= 0 && Math.abs(dx) > 20) {
                continue;
            }
            // keep the closest
            double d = Math.abs(dx) + Math.abs(dy);
            if (d < bestD) {
                bestD = d;
                best = new Cand();
                best.kind = "threat";
                best.foe = f;
                best.x = f.x;
                best.y = f.y;
                best.id = foeId(f);
            }
        }
        return best;
    }

    // enemies are named by where they spawned
    private static String foeId(WorldModel.Foe f) {
        if (f.from == null) {
            return "foe:?";
        }
        return "foe:" + f.from.key();
    }
}
