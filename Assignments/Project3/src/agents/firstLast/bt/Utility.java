package agents.firstLast.bt;

// how good a branch looks right now
// used by the utility selector (decision theory part)
public interface Utility {
    double score(Blackboard bb);
}
