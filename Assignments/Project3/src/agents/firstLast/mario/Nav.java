package agents.firstLast.mario;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

// tiny floor graph search for stuff the planner cant reach in one jump
// nodes are floor tiles from the map memory, edges are "probably one jump away"
// bfs from mario's floor, returns the first floor to hop to, the planner does the rest
public class Nav {
    private static final int SPAN = 16;

    // stepping stone toward tile (gx, gy), null if it is reachable from where we stand
    // (or if we cant tell), result is {x, y} in pixels for the planner target
    public static float[] waypoint(WorldModel w, int gx, int gy) {
        if (w.map == null) {
            return null;
        }
        int[] start = floorUnder(w);
        if (start == null) {
            return null;
        }
        int reach = w.mode > 0 ? 6 : 5;
        if (canGrab(start[0], start[1], gx, gy, reach)) {
            return null;
        }

        int mtx = start[0];
        Map<Long, Long> parent = new HashMap<>();
        ArrayDeque<int[]> open = new ArrayDeque<>();
        long s = key(start[0], start[1]);
        parent.put(s, s);
        open.add(start);

        while (!open.isEmpty()) {
            int[] cur = open.poll();
            if (canGrab(cur[0], cur[1], gx, gy, reach)) {
                return firstHop(parent, s, key(cur[0], cur[1]));
            }
            // floors one jump away, up to 4 rows up, further sideways if dropping
            for (int c = cur[0] - 6; c <= cur[0] + 6; c++) {
                if (Math.abs(c - mtx) > SPAN) {
                    continue;
                }
                for (int r = cur[1] - 4; r <= cur[1] + 10 && r < w.tilesH; r++) {
                    if (r < 1 || !w.floorAt(c, r)) {
                        continue;
                    }
                    int dx = Math.abs(c - cur[0]);
                    if (r < cur[1] && dx > 5) {
                        continue;
                    }
                    long k = key(c, r);
                    if (parent.containsKey(k)) {
                        continue;
                    }
                    parent.put(k, key(cur[0], cur[1]));
                    open.add(new int[]{c, r});
                }
            }
        }
        return null;
    }

    // can mario standing on floor (c, r) touch tile (gx, gy) with a jump or a step
    private static boolean canGrab(int c, int r, int gx, int gy, int reach) {
        return Math.abs(c - gx) <= 2 && gy >= r - reach;
    }

    // floor tile mario is on, or the first one under him
    private static int[] floorUnder(WorldModel w) {
        int c = (int) (w.x / 16);
        for (int r = Math.max(1, (int) ((w.y + 1) / 16)); r < w.tilesH; r++) {
            if (w.floorAt(c, r)) {
                return new int[]{c, r};
            }
            int c2 = (int) ((w.x + 4) / 16);
            if (c2 != c && w.floorAt(c2, r)) {
                return new int[]{c2, r};
            }
        }
        return null;
    }

    private static float[] firstHop(Map<Long, Long> parent, long start, long goal) {
        long k = goal;
        while (parent.get(k) != start) {
            k = parent.get(k);
        }
        int c = (int) (k >> 16);
        int r = (int) (k & 0xffff);
        return new float[]{c * 16 + 8, r * 16 - 1};
    }

    private static long key(int c, int r) {
        return ((long) c << 16) | (r & 0xffff);
    }
}
