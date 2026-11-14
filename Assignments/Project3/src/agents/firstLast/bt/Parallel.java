package agents.firstLast.bt;

// ticks every child node once per tick
// if one fails it fails, done when all child nodes are done
public class Parallel extends Composite {

    @Override
    public Status run(Blackboard bb) {
        boolean allDone = true;
        for (Task k : children) {
            Status s = k.run(bb);
            if (s == Status.FAILURE) {
                reset();
                return Status.FAILURE;
            }
            if (s == Status.RUNNING) {
                allDone = false;
            }
        }
        if (allDone) {
            reset();
            return Status.SUCCESS;
        }
        return Status.RUNNING;
    }
}
