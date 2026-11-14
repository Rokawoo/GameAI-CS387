package agents.firstLast.bt;

// runs the child node and flips success and failure
public class Inverter extends Decorator {

    @Override
    public Status run(Blackboard bb) {
        Status s = child.run(bb);
        if (s == Status.SUCCESS) {
            return Status.FAILURE;
        }
        if (s == Status.FAILURE) {
            return Status.SUCCESS;
        }
        return s;
    }
}
