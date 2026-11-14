package agents.firstLast.bt;

// checks something about the game
// never changes the game, just success or failure
public abstract class Condition extends Leaf {

    protected abstract boolean check(Blackboard bb);

    @Override
    public Status run(Blackboard bb) {
        return check(bb) ? Status.SUCCESS : Status.FAILURE;
    }
}
