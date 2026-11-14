package agents.firstLast.bt;

// base class for every node in the tree
// conditions, actions, composites, decorators all extend this
public abstract class Task {
    private String name = getClass().getSimpleName();
    private Utility utility = null;

    // one cycle of this task, called once per game tick
    public abstract Status run(Blackboard bb);

    // drop any half done work, parent calls this when it moves on
    public void reset() {
    }

    // only matters under a utility selector, zero means skip me
    public double utility(Blackboard bb) {
        return utility == null ? 0 : utility.score(bb);
    }

    public void setUtility(Utility u) {
        utility = u;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }

    // node count, for the 15-20 task check
    public int size() {
        return 1;
    }
}
