package agents.firstLast.bt;

// the game side tells the loader how to build leaves by name
public interface TaskLibrary {

    // kind is "condition" or "action", null if the name is unknown
    Leaf makeLeaf(String kind, String name);

    // utility function by name, null if unknown
    Utility makeUtility(String name);
}
