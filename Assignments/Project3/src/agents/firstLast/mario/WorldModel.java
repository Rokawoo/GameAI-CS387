package agents.firstLast.mario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import engine.core.MarioForwardModel;

// the perception part, what mario knows each tick
// only uses what the forward model hands out (screen grids, enemy list)
// plus a memory of tiles seen so far and where enemies spawn
public class WorldModel {
    // tile codes from the detail 0 scene grid
    public static final int UNKNOWN = -1;
    public static final int EMPTY = 0;
    public static final int COIN = MarioForwardModel.OBS_COIN;
    public static final int BRICK = MarioForwardModel.OBS_BRICK;
    public static final int QUESTION = MarioForwardModel.OBS_QUESTION_BLOCK;
    public static final int USED = MarioForwardModel.OBS_USED_BLOCK;

    // sprite codes from the screen enemy grid
    private static final int GRID_STOMPABLE = 2;
    private static final int GRID_ITEM = 12;

    // mario right now
    public float x, y, xa, ya;
    public int mode;
    public boolean onGround, mayJump;
    public int tick = 0;
    public int framesLeft;
    public float exitX = -1;
    public int tilesW, tilesH;
    // right most column we have ever had on screen
    public int maxSeenCol = 0;
    public boolean[] lastAction = new boolean[5];

    // tiles seen so far, UNKNOWN if never on screen
    public int[][] map;

    // enemies alive this tick, tracked between ticks
    public final List<Foe> foes = new ArrayList<>();
    // power ups and shells on screen, tile coords
    public final List<int[]> items = new ArrayList<>();
    public final List<int[]> shells = new ArrayList<>();

    // spots enemies came out of, for farming
    public final Map<String, Spawn> spawns = new HashMap<>();
    // bricks already bonked once, no point doing it again
    public final Set<Long> probed = new HashSet<>();

    // one tracked enemy
    public static class Foe {
        public int type;
        public float x, y, px, py;
        public Spawn from;
        public int born;
    }

    // one enemy spawn spot and what happened to the enemies from it
    public static class Spawn {
        public final int tx, ty, type;
        public int born, died, fell;
        // frames from spawn to death, added up, for the farm kill rate
        public int lifeSum;
        public int lastBorn = -1000, lastGone = -1000;
        public boolean alive;

        Spawn(int tx, int ty, int type) {
            this.tx = tx;
            this.ty = ty;
            this.type = type;
        }

        public String key() {
            return tx + "_" + ty + "_" + type;
        }
    }

    public void update(MarioForwardModel m) {
        tick++;

        // mario state
        float prevYa = ya;
        float[] p = m.getMarioFloatPos();
        float[] v = m.getMarioFloatVelocity();
        x = p[0];
        y = p[1];
        xa = v[0];
        ya = v[1];
        mode = m.getMarioMode();
        onGround = m.isMarioOnGround();
        mayJump = m.mayMarioJump();
        framesLeft = m.getRemainingTime() / 30;

        // level size and flag spot, only once
        if (map == null) {
            float[] dims = m.getLevelFloatDimensions();
            tilesW = (int) (dims[0] / 16);
            tilesH = (int) (dims[1] / 16);
            map = new int[tilesW][tilesH];
            for (int[] col : map) {
                java.util.Arrays.fill(col, UNKNOWN);
            }
        }
        if (exitX < 0 && x > 0) {
            float pct = m.getCompletionPercentage();
            if (pct > 0) {
                exitX = x / pct;
            }
        }

        // the rest of perception, in order
        rememberTiles(m);
        trackFoes(m);
        scanSprites(m);
        checkBonk(prevYa);
    }

    // head bonk = was going up, now coming down, still in the air
    // remember the block so we dont keep testing it
    private void checkBonk(float prevYa) {
        if (prevYa >= 0 || ya <= 0 || onGround) {
            return;
        }
        // row right above mario's head, check left foot, middle, right foot
        int row = (int) ((y - marioHeight() - 1) / 16);
        for (int dx = -4; dx <= 4; dx += 4) {
            int col = (int) ((x + dx) / 16);
            int t = tile(col, row);
            if (t == BRICK || t == QUESTION || t == USED) {
                probed.add(Scout.pack(col, row));
            }
        }
    }

    // copy the 16x16 screen grid around mario into the map memory
    private void rememberTiles(MarioForwardModel m) {
        int[][] scene = m.getMarioSceneObservation(0);
        int mtx = (int) (x / 16);
        int mty = (int) (y / 16);
        for (int ox = 0; ox < scene.length; ox++) {
            int wx = mtx - scene.length / 2 + ox;
            if (wx < 0 || wx >= tilesW) {
                continue;
            }
            maxSeenCol = Math.max(maxSeenCol, wx);
            for (int oy = 0; oy < scene[ox].length; oy++) {
                int wy = mty - scene[ox].length / 2 + oy;
                // rows off the level get clamped by the engine, skip them
                if (wy < 0 || wy >= tilesH) {
                    continue;
                }
                map[wx][wy] = scene[ox][oy];
            }
        }
    }

    // match this ticks enemies to last ticks ones
    // the engine gives no ids, so match by type and position
    // leftovers = new spawns, missing ones = dead or scrolled off
    private void trackFoes(MarioForwardModel m) {
        // list comes as type, x, y, type, x, y ...
        float[] raw = m.getEnemiesFloatPos();
        List<Foe> old = new ArrayList<>(foes);
        foes.clear();

        for (int i = 0; i + 2 < raw.length; i += 3) {
            int type = (int) raw[i];
            float ex = raw[i + 1];
            float ey = raw[i + 2];

            // closest old one of the same type
            // near where it should be if it kept its speed
            Foe best = null;
            float bestD = 28;
            for (Foe f : old) {
                if (f.type != type) {
                    continue;
                }
                float predX = f.x + (f.x - f.px);
                float predY = f.y + (f.y - f.py);
                float d = Math.abs(ex - predX) + Math.abs(ey - predY);
                if (d < bestD) {
                    bestD = d;
                    best = f;
                }
            }

            // same enemy as before, just moved
            if (best != null) {
                old.remove(best);
                best.px = best.x;
                best.py = best.y;
                best.x = ex;
                best.y = ey;
                foes.add(best);
                continue;
            }

            // brand new enemy, it just spawned
            Foe f = new Foe();
            f.type = type;
            f.x = ex;
            f.px = ex;
            f.y = ey;
            f.py = ey;
            f.born = tick;
            // spawn spot = the tile it showed up in
            f.from = spawnFor((int) (ex / 16), (int) (ey / 16), type);
            f.from.born++;
            f.from.lastBorn = tick;
            f.from.alive = true;
            foes.add(f);
        }

        // the ones we lost
        for (Foe f : old) {
            if (f.from == null) {
                continue;
            }
            f.from.alive = false;
            f.from.lastGone = tick;

            // near the bottom and going down = fell in a pit
            boolean fell = f.y > tilesH * 16 - 24 && f.y >= f.py;
            // right next to mario = stomped
            boolean squished = Math.abs(f.x - x) < 40 && Math.abs(f.y - y) < 48;
            // anything else just scrolled off, not a death

            if (fell) {
                f.from.fell++;
            }
            if (fell || squished) {
                f.from.died++;
                f.from.lifeSum += tick - f.born;
            }
        }
    }

    // same spot within a tile counts as the same spawn
    private Spawn spawnFor(int tx, int ty, int type) {
        for (Spawn s : spawns.values()) {
            if (s.type == type && Math.abs(s.tx - tx) <= 1 && Math.abs(s.ty - ty) <= 1) {
                return s;
            }
        }
        Spawn s = new Spawn(tx, ty, type);
        spawns.put(s.key(), s);
        return s;
    }

    // items and shells only show up in the screen sprite grid
    private void scanSprites(MarioForwardModel m) {
        items.clear();
        shells.clear();
        int[][] g = m.getMarioEnemiesObservation(0);
        int mtx = (int) (x / 16);
        int mty = (int) (y / 16);
        for (int ox = 0; ox < g.length; ox++) {
            for (int oy = 0; oy < g[ox].length; oy++) {
                int wx = mtx - g.length / 2 + ox;
                int wy = mty - g[ox].length / 2 + oy;
                if (g[ox][oy] == GRID_ITEM) {
                    items.add(new int[]{wx, wy});
                } else if (g[ox][oy] == GRID_STOMPABLE && !foeInTile(wx, wy)) {
                    shells.add(new int[]{wx, wy});
                }
            }
        }
    }

    private boolean foeInTile(int tx, int ty) {
        for (Foe f : foes) {
            if ((int) (f.x / 16) == tx && (int) (f.y / 16) == ty) {
                return true;
            }
        }
        return false;
    }

    // ---- tile helpers ----

    public int tile(int tx, int ty) {
        if (map == null || tx < 0 || tx >= tilesW || ty < 0 || ty >= tilesH) {
            return EMPTY;
        }
        return map[tx][ty];
    }

    // blocks from every side
    // codes are engine tile + 16 in the detail 0 grid
    public static boolean solid(int c) {
        // ground, pyramid, bullet bill cannon, brick
        if (c >= 17 && c <= 22) {
            return true;
        }
        // ? block and used block
        if (c == QUESTION || c == USED) {
            return true;
        }
        // pipes
        if (c >= 34 && c <= 37) {
            return true;
        }
        // single pipes
        return c == 68 || c == 69;
    }

    // jump thru platform, only blocks when landing on top
    public static boolean platform(int c) {
        return c >= 59 && c <= 62;
    }

    public boolean standable(int tx, int ty) {
        int c = tile(tx, ty);
        return solid(c) || platform(c);
    }

    // somewhere you can stand on top of
    // needs room above it too
    public boolean floorAt(int tx, int ty) {
        if (!standable(tx, ty)) {
            return false;
        }
        return !solid(tile(tx, ty - 1));
    }

    public int marioHeight() {
        if (mode > 0) {
            return 24;
        }
        return 12;
    }

    // goombas, koopas (winged too), bullet bills
    // spikies and piranha plants hurt to land on
    public static boolean stompable(int type) {
        switch (type) {
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 10:
                return true;
            default:
                return false;
        }
    }

    // odd types are the winged ones
    public static boolean winged(int type) {
        return type == 3 || type == 5 || type == 7 || type == 9;
    }
}
