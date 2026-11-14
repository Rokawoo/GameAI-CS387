package agents.firstLast.tasks;

import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Condition;

// a block worth bonking with a floor under it
// pick="smash" -> plain bricks big mario can break
// otherwise    -> ? blocks, or bricks that might hide a mushroom while small
public class BlockToBump extends Condition {
    private boolean smash;

    @Override
    protected void setup() {
        smash = "smash".equals(params.get("pick"));
    }

    @Override
    protected boolean check(Blackboard bb) {
        return (smash ? Bb.scout(bb).smash : Bb.scout(bb).block) != null;
    }
}
