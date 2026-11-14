package agents.firstLast.bt;

// composite with exactly one child node that changes how the child node behaves
public abstract class Decorator extends Task {
    protected Task child;

    public void setChild(Task t) {
        child = t;
    }

    public Task getChild() {
        return child;
    }

    @Override
    public void reset() {
        if (child != null) {
            child.reset();
        }
    }

    @Override
    public int size() {
        return 1 + (child == null ? 0 : child.size());
    }
}
