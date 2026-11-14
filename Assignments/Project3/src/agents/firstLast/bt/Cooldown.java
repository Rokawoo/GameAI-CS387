package agents.firstLast.bt;

// after the child node fails, the child node is benched for a few ticks
// stops the tree from retrying something hopeless every frame
public class Cooldown extends Decorator {
    private final int ticks;
    private int benched = 0;

    public Cooldown(int ticks) {
        this.ticks = ticks;
    }

    @Override
    public Status run(Blackboard bb) {
        if (benched > 0) {
            benched--;
            return Status.FAILURE;
        }
        Status s = child.run(bb);
        if (s == Status.FAILURE) {
            benched = ticks;
        }
        return s;
    }

    @Override
    public void reset() {
        // keep the bench timer, only the child node forgets
        super.reset();
    }
}
