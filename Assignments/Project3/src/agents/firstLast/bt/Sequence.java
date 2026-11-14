package agents.firstLast.bt;

// runs child nodes in order
// any child node fails -> fail, all child nodes pass -> success
// no while loop, each child node gets at most one cycle per tick
// a running child node is remembered and picked up again next tick
public class Sequence extends Composite {

    @Override
    public Status run(Blackboard bb) {
        for (int i = current; i < children.size(); i++) {
            Status s = children.get(i).run(bb);

            // child node needs more ticks, wait on it
            if (s == Status.RUNNING) {
                current = i;
                return Status.RUNNING;
            }

            // one fail kills the whole sequence
            if (s == Status.FAILURE) {
                reset();
                return Status.FAILURE;
            }
        }

        // everyone passed
        reset();
        return Status.SUCCESS;
    }
}
