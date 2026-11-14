package agents.firstLast.tasks;

import agents.firstLast.bt.Leaf;
import agents.firstLast.bt.TaskLibrary;
import agents.firstLast.bt.Utility;
import agents.firstLast.mario.Scout;

// every leaf name the xml is allowed to use
// the interpreter asks here when it hits <condition> or <action>
public class MarioTasks implements TaskLibrary {

    @Override
    public Leaf makeLeaf(String kind, String name) {
        if (kind.equals("condition")) {
            switch (name) {
                case "MustFinish":
                    return new MustFinish();
                case "InDanger":
                    return new InDanger();
                case "PowerupVisible":
                    return new PowerupVisible();
                case "EnemyTargetAvailable":
                    return new EnemyTargetAvailable();
                case "CoinVisible":
                    return new CoinVisible();
                case "BlockToBump":
                    return new BlockToBump();
                case "FarmAvailable":
                    return new FarmAvailable();
                case "FarmPaysOff":
                    return new FarmPaysOff();
                case "IsFire":
                    return new IsFire();
                case "EnemyInFireLine":
                    return new EnemyInFireLine();
                default:
                    return null;
            }
        }
        if (kind.equals("action")) {
            switch (name) {
                case "SetFinishTarget":
                    return new SetFinishTarget();
                case "SetEvadeGoal":
                    return new SetEvadeGoal();
                case "SetPowerupTarget":
                    return new SetPowerupTarget();
                case "SetEnemyTarget":
                    return new SetEnemyTarget();
                case "SetCoinTarget":
                    return new SetCoinTarget();
                case "SetBlockTarget":
                    return new SetBlockTarget();
                case "SetFarmTarget":
                    return new SetFarmTarget();
                case "SetAdvanceTarget":
                    return new SetAdvanceTarget();
                case "PlanMove":
                    return new PlanMove();
                case "SafeFallback":
                    return new SafeFallback();
                case "ShootFireball":
                    return new ShootFireball();
                default:
                    return null;
            }
        }
        return null;
    }

    // utilities for the utility selector, read straight off the scout
    @Override
    public Utility makeUtility(String name) {
        switch (name) {
            case "coinValue":
                return bb -> pick(Bb.scout(bb).coin);
            case "stompValue":
                return bb -> pick(Bb.scout(bb).enemy);
            case "blockValue":
                return bb -> pick(Bb.scout(bb).block);
            default:
                return null;
        }
    }

    private static double pick(Scout.Cand c) {
        return c == null ? 0 : c.utility;
    }
}
