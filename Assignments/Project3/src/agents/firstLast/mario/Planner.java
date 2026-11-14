package agents.firstLast.mario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;

// look ahead planner, the "how" part under PlanMove
// decision theory slides: utility function + list of actions + simulate them
// beam search over button combos, each held for 2 frames
// every plan gets a score, a plan that dies gets thrown out
public class Planner {

    // ---- button combos ----
    // order is left right down speed jump
    public static final boolean[][] ACTS = {
            {false, false, false, false, false}, // 0 nothing
            {false, true, false, false, false},  // 1 right
            {false, true, false, true, false},   // 2 right run
            {false, true, false, false, true},   // 3 right jump
            {false, true, false, true, true},    // 4 right run jump
            {true, false, false, false, false},  // 5 left
            {true, false, false, true, false},   // 6 left run
            {true, false, false, false, true},   // 7 left jump
            {true, false, false, true, true},    // 8 left run jump
            {false, false, false, false, true},  // 9 jump
    };

    // dumb follow ups for the end of plan check
    // nothing, left, right, jump, left jump, right jump
    private static final int[] ESCAPES = {0, 5, 1, 9, 7, 3};

    // ---- score constants ----
    private static final double WIN_REWARD = 20000;
    private static final double HURT = 3000;
    private static final double TIMEOUT = 50000;
    private static final double DOOM_GUESS = 300;

    // ---- search knobs ----
    public int beam = 16;
    public int depth = 12;
    public int hold = 2;
    public int safeFrames = 24;
    public int checkTop = 8;
    public double gamma = 0.99;
    // last ticks plan gets a little benefit of the doubt
    public double warmBonus = 4;

    // ---- state ----
    // tile memory, used for the cheap doom guess
    private WorldModel world;
    // last picked plan, one action per frame
    private int[] lastPlan = null;
    private int rootCoins, rootKills;
    // set per search from the goal
    private boolean hurtOk = false;

    // ---- report for the tree ----
    public boolean safe;
    public double startDist;
    public boolean reached;
    public int gainCoins, gainKills;

    // ---- debug only ----
    private static final boolean DEBUG = Boolean.getBoolean("bt.debug");
    private static final String[] NAMES = {"-", "R", "Rs", "Rj", "Rsj", "L", "Ls", "Lj", "Lsj", "J"};
    public String debug = "";

    // one point in the search
    private static class Node {
        MarioForwardModel m;
        Node parent;
        int act;
        int firstAct;
        int frames;
        // discounted reward so far
        double reward;
        // counters last time we looked
        int coins, kills, mode, bricks;
        boolean reached, won, timedOut;
        // target distance, best and last
        double minDist = 1e9;
        double lastDist;
        double score;
        // only set for the replayed plan
        int[] warmActs;
    }

    public void setWorld(WorldModel w) {
        world = w;
    }

    // ================= main entry =================

    public boolean[] plan(MarioForwardModel root, Goal g) {
        hurtOk = g.hurtOk;

        // root node, counters to compare against
        Node r = new Node();
        r.m = root;
        r.coins = root.getNumCollectedCoins();
        r.kills = root.getKillsTotal();
        r.mode = root.getMarioMode();
        r.bricks = root.getNumDestroyedBricks();
        r.firstAct = -1;
        rootCoins = r.coins;
        rootKills = r.kills;

        float[] p0 = root.getMarioFloatPos();
        startDist = dist(p0[0], p0[1], r.mode, g);
        r.minDist = startDist;
        r.lastDist = startDist;

        // grow the beam one step at a time
        List<Node> frontier = new ArrayList<>();
        frontier.add(r);
        List<Node> done = new ArrayList<>();

        for (int d = 0; d < depth; d++) {
            List<Node> children = new ArrayList<>(frontier.size() * ACTS.length);
            for (Node n : frontier) {
                for (int a = 0; a < ACTS.length; a++) {
                    Node c = step(n, a, g);
                    // died
                    if (c == null) {
                        continue;
                    }
                    // game over (flag or clock), stop growing it
                    if (c.won || c.timedOut) {
                        done.add(c);
                    } else {
                        children.add(c);
                    }
                }
            }
            // everything died, keep the last beam
            if (children.isEmpty()) {
                break;
            }
            frontier = prune(children, g);
        }

        // candidates = last beam + early finishers
        List<Node> cands = new ArrayList<>(frontier);
        cands.remove(r);
        cands.addAll(done);
        for (Node c : cands) {
            c.score = evaluate(c, g);
        }
        cands.sort((a, b) -> Double.compare(b.score, a.score));

        // replay last ticks plan too, stops flip flopping
        Node warm = warmStart(root, g, r);

        // best plan we can survive
        Node best = pickSafe(cands);
        safe = best != null;

        // warm plan wins ties if it is also safe
        boolean warmGood = warm != null && (best == null || warm.score + warmBonus >= best.score);
        if (warmGood && (warm.won || escapes(warm.m))) {
            best = warm;
            safe = true;
        }

        // nothing safe, take the top plan and hope
        if (best == null) {
            if (cands.isEmpty()) {
                best = warm;
            } else {
                best = cands.get(0);
            }
        }

        // nothing at all, stand still
        if (best == null) {
            lastPlan = null;
            report(null);
            return ACTS[0].clone();
        }

        lastPlan = framesOf(best);
        report(best);
        if (DEBUG) {
            debug = debugLine(cands, warm, best);
        }
        return ACTS[lastPlan[0]].clone();
    }

    // ================= simulation =================

    // hold one button combo for a few frames
    private Node step(Node n, int a, Goal g) {
        // copy the parent
        Node c = new Node();
        c.m = n.m.clone();
        c.parent = n;
        c.act = a;
        if (n.firstAct < 0) {
            c.firstAct = a;
        } else {
            c.firstAct = n.firstAct;
        }
        c.frames = n.frames;
        c.reward = n.reward;
        c.coins = n.coins;
        c.kills = n.kills;
        c.mode = n.mode;
        c.bricks = n.bricks;
        c.reached = n.reached;
        c.minDist = n.minDist;
        c.lastDist = n.lastDist;

        // run the frames
        for (int f = 0; f < hold; f++) {
            c.m.advance(ACTS[a]);
            c.frames++;
            if (!alive(c, g)) {
                return null;
            }
            stompCheck(c, g);
            if (c.won || c.timedOut) {
                break;
            }
        }

        tally(c, g);
        return c;
    }

    // stomp bounce is exactly 8 * -1.9 up
    // nothing else moves mario that fast up
    // small credit, even if it only knocked wings off
    private void stompCheck(Node c, Goal g) {
        float ya = c.m.getMarioFloatVelocity()[1];
        if (ya < -14.5f) {
            c.reward += g.wStomp * Math.pow(gamma, c.frames);
        }
    }

    // game end checks
    // false = throw this node away
    private boolean alive(Node c, Goal g) {
        GameStatus st = c.m.getGameStatus();

        // died
        if (st == GameStatus.LOSE) {
            return false;
        }

        // flag, only ok when the tree said so
        if (st == GameStatus.WIN) {
            if (!g.allowFinish) {
                return false;
            }
            c.won = true;
            c.reward += WIN_REWARD * Math.pow(gamma, c.frames);
        }

        // clock ran out, really bad but keep it as a last resort
        if (st == GameStatus.TIME_OUT) {
            c.timedOut = true;
            c.reward -= TIMEOUT;
        }
        return true;
    }

    // add up what changed since the parent
    private void tally(Node c, Goal g) {
        double disc = Math.pow(gamma, c.frames);

        int coins = c.m.getNumCollectedCoins();
        int kills = c.m.getKillsTotal();
        int mode = c.m.getMarioMode();
        int bricks = c.m.getNumDestroyedBricks();

        // coins and kills
        if (coins > c.coins) {
            c.reward += (coins - c.coins) * g.wCoin * disc;
        }
        if (kills > c.kills) {
            c.reward += (kills - c.kills) * g.wKill * disc;
        }

        // power up good, getting hit bad
        if (mode > c.mode) {
            c.reward += (mode - c.mode) * g.wPower * disc;
        } else if (mode < c.mode) {
            if (g.hurtOk) {
                c.reward -= HURT / 15;
            } else {
                c.reward -= HURT;
            }
        }

        // smashed bricks, tiny
        if (bricks > c.bricks) {
            c.reward += (bricks - c.bricks) * g.wBrick * disc;
        }

        c.coins = coins;
        c.kills = kills;
        c.mode = mode;
        c.bricks = bricks;

        // no target, done
        if (!g.hasTarget()) {
            return;
        }

        float[] p = c.m.getMarioFloatPos();
        double d = dist(p[0], p[1], mode, g);

        // pay progress when it happens
        // sooner beats later, else "wait a frame then go" wins forever
        if (!c.reached || g.sticky) {
            c.reward += g.wDist * (c.lastDist - d) * disc;
        }
        c.lastDist = d;
        if (d < c.minDist) {
            c.minDist = d;
        }

        // first time touching the target
        if (!c.reached && reachedNow(p, mode, g)) {
            c.reached = true;
            c.reward += g.reachBonus * disc;
        }
    }

    // ================= utility =================

    private double evaluate(Node c, Goal g) {
        double s = c.reward;

        // finished one way or the other, reward says it all
        if (c.won || c.timedOut) {
            return s;
        }

        float[] p = c.m.getMarioFloatPos();
        float[] v = c.m.getMarioFloatVelocity();

        // target still matters
        // progress is already in reward, just a nudge for
        // passing close and for still heading the right way
        if (g.hasTarget() && (!c.reached || g.sticky)) {
            s -= 0.2 * g.wDist * c.minDist;
            double dir = Math.signum(g.tx - p[0]);
            if (Math.abs(g.tx - p[0]) > 24) {
                s += g.wVel * v[0] * dir;
            }
        }

        // on the ground is nicer than in the air
        // in the air over nothing = dead after the horizon
        if (c.m.isMarioOnGround()) {
            s += 2;
        } else if (doomed(p[0], p[1], v[0], v[1])) {
            s -= DOOM_GUESS;
        }
        return s;
    }

    // cheap doom guess from the tile memory, no simulation
    // fall with no jump boost, steer full left and full right
    // any floor top crossed before falling off the level = not doomed
    // unseen tiles count as floor, so far plans dont get punished
    private boolean doomed(float x, float y, float xa, float ya) {
        if (world == null || world.map == null) {
            return false;
        }

        // left and right edge of where mario could be, and his fall
        float lo = x, hi = x, vlo = xa, vhi = xa;
        float yy = y, vy = ya;
        float floorOut = world.tilesH * 16 + 32;

        for (int k = 0; k < 48; k++) {
            // full left
            vlo -= 1.2f;
            lo += vlo;
            vlo *= 0.89f;
            // full right
            vhi += 1.2f;
            hi += vhi;
            vhi *= 0.89f;

            float ny = yy + vy;

            // falling, check tile tops crossed this frame
            if (vy > 0) {
                int r0 = (int) Math.floor(yy / 16) + 1;
                int r1 = (int) Math.floor(ny / 16);
                int c0 = (int) Math.floor((lo - 4) / 16);
                int c1 = (int) Math.floor((hi + 4) / 16);
                for (int r = Math.max(0, r0); r <= r1 && r < world.tilesH; r++) {
                    for (int col = c0; col <= c1; col++) {
                        if (col < 0 || col >= world.tilesW) {
                            continue;
                        }
                        int t = world.map[col][r];
                        if (t == WorldModel.UNKNOWN || world.floorAt(col, r)) {
                            return false;
                        }
                    }
                }
            }

            // same gravity as mario
            yy = ny;
            vy = vy * 0.85f + 3;
            if (yy > floorOut) {
                return true;
            }
        }
        return false;
    }

    // distance to target, x plus y from mario's middle
    public static double dist(float x, float y, int mode, Goal g) {
        if (!g.hasTarget()) {
            return 0;
        }
        double h = 12;
        if (mode > 0) {
            h = 24;
        }
        double dx = Math.abs(g.tx - x);
        double dy = 0;
        if (g.useY) {
            dy = Math.abs(g.ty - (y - h / 2));
        }
        return dx + dy;
    }

    private boolean reachedNow(float[] p, int mode, Goal g) {
        double h = 12;
        if (mode > 0) {
            h = 24;
        }

        // block target: head touching the bottom of it
        if (g.bumpX >= 0) {
            double head = p[1] - h;
            double bottom = g.bumpY * 16 + 16;
            boolean under = Math.abs(p[0] - (g.bumpX * 16 + 8)) <= 10;
            boolean touching = head <= bottom + 1;
            boolean below = p[1] > bottom;
            return under && touching && below;
        }

        // point target: inside the tolerance box
        double dx = Math.abs(g.tx - p[0]);
        double dy = 0;
        if (g.useY) {
            dy = Math.abs(g.ty - (p[1] - h / 2));
        }
        return dx <= g.tolX && dy <= g.tolY;
    }

    // ================= beam upkeep =================

    // keep the best few
    // but always keep the best child node of each first move
    // so the final pick has real choices
    private List<Node> prune(List<Node> children, Goal g) {
        for (Node c : children) {
            c.score = evaluate(c, g);
        }
        children.sort((a, b) -> Double.compare(b.score, a.score));

        List<Node> out = new ArrayList<>(beam + ACTS.length);
        Set<Long> seen = new HashSet<>();
        boolean[] haveFirst = new boolean[ACTS.length];

        // pass 1, best per first action
        for (Node c : children) {
            if (!haveFirst[c.firstAct] && seen.add(key(c))) {
                haveFirst[c.firstAct] = true;
                out.add(c);
            }
        }

        // pass 2, fill up with the best overall
        for (Node c : children) {
            if (out.size() >= beam + 2) {
                break;
            }
            if (seen.add(key(c))) {
                out.add(c);
            }
        }
        return out;
    }

    // rough state id, keeps copies out of the beam
    // position in 3px buckets, rounded speed, ground and jump flags
    private static long key(Node c) {
        float[] p = c.m.getMarioFloatPos();
        float[] v = c.m.getMarioFloatVelocity();
        long kx = (long) (p[0] / 3);
        long ky = (long) ((p[1] + 64) / 3);
        long kv = (long) Math.round(v[0]) + 32;
        long kg = c.m.isMarioOnGround() ? 1 : 0;
        long kj = c.m.getMarioCanJumpHigher() ? 1 : 0;
        return (kx << 32) ^ (ky << 16) ^ (kv << 4) ^ (kg << 1) ^ kj ^ ((long) c.mode << 2);
    }

    // ================= safety =================

    // best scoring plan whose ending we can live through
    private Node pickSafe(List<Node> cands) {
        // top few in score order
        int checked = 0;
        for (Node c : cands) {
            if (c.timedOut) {
                continue;
            }
            if (c.won) {
                return c;
            }
            if (checked >= checkTop) {
                break;
            }
            checked++;
            if (escapes(c.m)) {
                return c;
            }
        }

        // still nothing, best of each first move
        boolean[] tried = new boolean[ACTS.length];
        for (Node c : cands) {
            if (c.timedOut || tried[c.firstAct]) {
                continue;
            }
            tried[c.firstAct] = true;
            if (escapes(c.m)) {
                return c;
            }
        }
        return null;
    }

    // can mario live a bit past the end of this plan
    // tries: brake, brake + jump, then the held escapes
    // one that lives is enough
    private boolean escapes(MarioForwardModel m) {
        int mode0 = m.getMarioMode();
        for (int pol = 0; pol < ESCAPES.length + 2; pol++) {
            MarioForwardModel c = m.clone();
            boolean ok = true;
            for (int f = 0; f < safeFrames; f++) {
                c.advance(ACTS[escapeAct(pol, c)]);
                GameStatus st = c.getGameStatus();

                // dead, or hit when hits arent ok
                boolean hit = !hurtOk && c.getMarioMode() < mode0;
                if (st == GameStatus.LOSE || hit) {
                    ok = false;
                    break;
                }
                // flag or clock, counts as made it
                if (st != GameStatus.RUNNING) {
                    break;
                }
            }
            if (ok) {
                return true;
            }
        }
        return false;
    }

    // policy 0 brake, policy 1 brake + jump, rest are held combos
    // braking = push the other way with run held
    private static int escapeAct(int pol, MarioForwardModel c) {
        if (pol >= 2) {
            return ESCAPES[pol - 2];
        }
        float xa = c.getMarioFloatVelocity()[0];
        boolean jump = pol == 1;

        // moving right, push left
        if (xa > 0.5f) {
            if (jump) {
                return 8;
            }
            return 6;
        }
        // moving left, push right
        if (xa < -0.5f) {
            if (jump) {
                return 4;
            }
            return 2;
        }
        // stopped
        if (jump) {
            return 9;
        }
        return 0;
    }

    // ================= warm start =================

    // last ticks plan shifted by one frame, padded with its last action
    private Node warmStart(MarioForwardModel root, Goal g, Node r) {
        if (lastPlan == null || lastPlan.length < 2) {
            return null;
        }

        // shift and pad
        int len = depth * hold;
        int[] acts = new int[len];
        for (int i = 0; i < len; i++) {
            int src = Math.min(i + 1, lastPlan.length - 1);
            acts[i] = lastPlan[src];
        }

        // start from the root counters
        Node c = new Node();
        c.m = root.clone();
        c.coins = r.coins;
        c.kills = r.kills;
        c.mode = r.mode;
        c.bricks = r.bricks;
        c.minDist = r.minDist;
        c.lastDist = r.lastDist;
        c.firstAct = acts[0];

        // replay it, tally on the same 2 frame beat as the beam
        for (int i = 0; i < len; i++) {
            c.m.advance(ACTS[acts[i]]);
            c.frames++;
            if (!alive(c, g)) {
                return null;
            }
            boolean beat = (i + 1) % hold == 0;
            if (beat || c.won || c.timedOut) {
                tally(c, g);
            }
            if (c.won || c.timedOut) {
                break;
            }
        }

        c.warmActs = acts;
        c.score = evaluate(c, g);
        return c;
    }

    // ================= plan bookkeeping =================

    // node -> one action per frame
    private int[] framesOf(Node n) {
        if (n.warmActs != null) {
            return n.warmActs;
        }

        // walk back up to the root
        List<Integer> macro = new ArrayList<>();
        for (Node k = n; k != null && k.parent != null; k = k.parent) {
            macro.add(0, k.act);
        }

        // each macro step is held for a few frames
        int[] out = new int[Math.max(1, macro.size() * hold)];
        for (int i = 0; i < macro.size(); i++) {
            for (int f = 0; f < hold; f++) {
                out[i * hold + f] = macro.get(i);
            }
        }
        return out;
    }

    // what PlanMove reads after a search
    private void report(Node best) {
        if (best == null) {
            reached = false;
            gainCoins = 0;
            gainKills = 0;
            return;
        }
        reached = best.reached;
        gainCoins = best.coins - rootCoins;
        gainKills = best.kills - rootKills;
    }

    // pressing these buttons now, then following the plan, still lives?
    public boolean variantIsSafe(MarioForwardModel root, boolean[] first) {
        MarioForwardModel c = root.clone();
        int mode0 = c.getMarioMode();

        // the changed first frame
        c.advance(first);
        if (c.getGameStatus() == GameStatus.LOSE || c.getMarioMode() < mode0) {
            return false;
        }

        // the rest of the plan
        if (lastPlan != null) {
            for (int i = 1; i < lastPlan.length; i++) {
                c.advance(ACTS[lastPlan[i]]);
                if (c.getGameStatus() == GameStatus.LOSE || c.getMarioMode() < mode0) {
                    return false;
                }
            }
        }
        return escapes(c);
    }

    // ================= debug =================

    private String debugLine(List<Node> cands, Node warm, Node best) {
        int safeCount = 0;
        for (int i = 0; i < Math.min(5, cands.size()); i++) {
            if (escapes(cands.get(i).m)) {
                safeCount++;
            }
        }
        String warmScore = "-";
        if (warm != null) {
            warmScore = String.format("%.1f", warm.score);
        }
        String pick = "beam";
        if (best == warm) {
            pick = "warm";
        }
        return String.format("cands=%d top5safe=%d warm=%s pick=%s score=%.1f +c%d +k%d reach=%b plan=%s",
                cands.size(), safeCount, warmScore, pick, best.score, gainCoins, gainKills, reached,
                planString(lastPlan));
    }

    private static String planString(int[] plan) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < plan.length; i += 2) {
            sb.append(NAMES[plan[i]]).append(' ');
        }
        return sb.toString().trim();
    }
}
