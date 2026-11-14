package agents.firstLast.bt;

// tries child nodes in order until one does not fail
// starts from the top every tick so higher priority stuff can cut in
// no while loop, each child node gets at most one cycle per tick
public class Selector extends Composite {
    private int running = -1;

    @Override
    public Status run(Blackboard bb) {
        for (int i = 0; i < children.size(); i++) {
            Status s = children.get(i).run(bb);
            if (s == Status.FAILURE) {
                continue;
            }

            // a different child node took over, stop the old one
            if (running != -1 && running != i) {
                children.get(running).reset();
            }
            running = (s == Status.RUNNING) ? i : -1;
            return s;
        }

        // nobody could do it
        running = -1;
        return Status.FAILURE;
    }

    @Override
    public void reset() {
        super.reset();
        running = -1;
    }
}
