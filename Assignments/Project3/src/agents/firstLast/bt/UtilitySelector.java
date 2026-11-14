package agents.firstLast.bt;

import java.util.Arrays;

// the utility selector from the decision theory slides
// asks every child node for its utility and tries the best one first
// falls back to the next best if it fails, like a normal selector
// last tick winner gets a small bonus so it doesnt flip flop
public class UtilitySelector extends Composite {
    private int lastWinner = -1;
    private double stickiness = 1.25;

    public void setStickiness(double s) {
        stickiness = s;
    }

    @Override
    public Status run(Blackboard bb) {
        int n = children.size();
        double[] u = new double[n];
        Integer[] order = new Integer[n];

        // score every option
        for (int i = 0; i < n; i++) {
            u[i] = children.get(i).utility(bb);
            if (i == lastWinner && u[i] > 0) {
                u[i] *= stickiness;
            }
            order[i] = i;
        }

        // best first
        Arrays.sort(order, (a, b) -> Double.compare(u[b], u[a]));

        for (int k = 0; k < n; k++) {
            int i = order[k];
            // zero or less means not worth doing at all
            if (u[i] <= 0) {
                break;
            }
            Status s = children.get(i).run(bb);
            if (s == Status.FAILURE) {
                continue;
            }
            if (lastWinner != -1 && lastWinner != i) {
                children.get(lastWinner).reset();
            }
            lastWinner = i;
            return s;
        }

        lastWinner = -1;
        return Status.FAILURE;
    }

    @Override
    public void reset() {
        super.reset();
        lastWinner = -1;
    }
}
