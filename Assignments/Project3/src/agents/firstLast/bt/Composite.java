package agents.firstLast.bt;

import java.util.ArrayList;
import java.util.List;

// internal node with a list of child nodes
// keeps track of which child node it is on between ticks
public abstract class Composite extends Task {
    protected final List<Task> children = new ArrayList<>();
    protected int current = 0;

    public void add(Task t) {
        children.add(t);
    }

    public List<Task> getChildren() {
        return children;
    }

    @Override
    public void reset() {
        current = 0;
        for (Task k : children) {
            k.reset();
        }
    }

    @Override
    public int size() {
        int n = 1;
        for (Task k : children) {
            n += k.size();
        }
        return n;
    }
}
