package agents.firstLast.bt;

// runs the child node but never reports a failure
// good for side jobs that shouldnt break the parent
public class Succeeder extends Decorator {

    @Override
    public Status run(Blackboard bb) {
        Status s = child.run(bb);
        return s == Status.RUNNING ? Status.RUNNING : Status.SUCCESS;
    }
}
