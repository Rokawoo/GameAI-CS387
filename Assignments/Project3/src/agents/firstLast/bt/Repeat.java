package agents.firstLast.bt;

// repeats the child node n times, one child node cycle per tick, no loop
// stops early with failure if the child node fails
public class Repeat extends Decorator {
    private final int times;
    private int done = 0;

    public Repeat(int times) {
        this.times = times;
    }

    @Override
    public Status run(Blackboard bb) {
        Status s = child.run(bb);
        if (s == Status.FAILURE) {
            reset();
            return Status.FAILURE;
        }
        if (s == Status.SUCCESS) {
            done++;
            child.reset();
            if (done >= times) {
                reset();
                return Status.SUCCESS;
            }
        }
        return Status.RUNNING;
    }

    @Override
    public void reset() {
        super.reset();
        done = 0;
    }
}
