package agents.firstLast.mario;

// names of the blackboard slots so nobody typos them
public final class Keys {
    // filled by the agent every tick
    public static final String MODEL = "model";
    public static final String TIMER = "timer";
    public static final String WORLD = "world";
    public static final String PLANNER = "planner";
    public static final String SCOUT = "scout";

    // written by the tree
    public static final String GOAL = "goal";
    public static final String ACTION = "action";
    public static final String FALLBACK = "fallbackAction";
    public static final String BRANCH = "branch";

    // memory that lives across ticks
    public static final String FARM = "farm";

    private Keys() {
    }
}
