package agents.firstLast.bt;

// keeps running the child node across ticks until it fails
// then reports success, one child node cycle per tick
public class UntilFail extends Decorator {

    @Override
    public Status run(Blackboard bb) {
        Status s = child.run(bb);
        if (s == Status.FAILURE) {
            reset();
            return Status.SUCCESS;
        }
        if (s == Status.SUCCESS) {
            child.reset();
        }
        return Status.RUNNING;
    }
}
