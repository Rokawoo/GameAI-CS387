package agents.firstLast.mario;

// what the tree wants the planner to go do
// the tree picks the goal, the planner works out the buttons
public class Goal {
    public String kind = "advance";
    // id for progress tracking and the blacklist, null if none
    public String id = null;

    // target point in pixels, nan x means no target
    public float tx = Float.NaN;
    public float ty = Float.NaN;
    public boolean useY = true;
    // how close counts as reached
    public float tolX = 10;
    public float tolY = 12;
    // keep pulling after reach, for spots we want to stand on
    public boolean sticky = false;

    // block to bonk from below, -1 if none
    public int bumpX = -1;
    public int bumpY = -1;

    // touching the flag is only ok when the tree says so
    public boolean allowFinish = false;
    // big mario may take a hit (shrinks, doesnt die), only when we must push thru
    public boolean hurtOk = false;

    // utility weights
    public double wDist = 0.6;
    public double wVel = 3;
    public double reachBonus = 60;
    public double wCoin = 100;
    public double wKill = 100;
    public double wStomp = 25;
    public double wPower = 400;
    public double wBrick = 10;

    public boolean hasTarget() {
        return !Float.isNaN(tx);
    }

    public static Goal at(String kind, String id, float x, float y) {
        Goal g = new Goal();
        g.kind = kind;
        g.id = id;
        g.tx = x;
        g.ty = y;
        return g;
    }

    @Override
    public String toString() {
        return kind + (id == null ? "" : "[" + id + "]")
                + (hasTarget() ? String.format(" -> (%.0f,%.0f)", tx, ty) : "");
    }
}
