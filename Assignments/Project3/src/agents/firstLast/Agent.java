package agents.firstLast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import agents.firstLast.bt.BTLoader;
import agents.firstLast.bt.Blackboard;
import agents.firstLast.bt.Task;
import agents.firstLast.mario.Keys;
import agents.firstLast.mario.Planner;
import agents.firstLast.mario.Scout;
import agents.firstLast.mario.WorldModel;
import agents.firstLast.tasks.MarioTasks;
import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

// behavior tree mario agent
// the tree lives in mario_bt.xml next to this file and is read when the game starts
// every frame: update what we know -> tick the tree once -> press what it picked
// -Dbt.debug=true prints the branch each frame, -Dbt.tree=file.xml loads another tree
public class Agent implements MarioAgent {
    private static final String TREE_FILE = "mario_bt.xml";

    // set -Dbt.debug=true to print the branch every frame
    private static final boolean DEBUG = Boolean.getBoolean("bt.debug");

    private Task root;
    private Blackboard bb;
    private WorldModel world;
    private Scout scout;
    private Planner planner;

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        world = new WorldModel();
        scout = new Scout();
        planner = new Planner();
        planner.setWorld(world);

        bb = new Blackboard();
        bb.put(Keys.WORLD, world);
        bb.put(Keys.SCOUT, scout);
        bb.put(Keys.PLANNER, planner);

        // build the tree once from the text file
        try (InputStream in = openTree()) {
            root = new BTLoader(new MarioTasks()).load(in);
        } catch (Exception e) {
            throw new RuntimeException("could not load " + TREE_FILE + ": " + e.getMessage(), e);
        }
        System.out.println("[firstLast] behavior tree loaded, " + root.size() + " tasks");
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        // perception first
        world.update(model);
        scout.refresh(world);

        // fresh slots for this tick
        bb.put(Keys.MODEL, model);
        bb.put(Keys.TIMER, timer);
        bb.remove(Keys.ACTION);
        bb.remove(Keys.FALLBACK);

        // one tick of the tree
        root.run(bb);

        boolean[] act = bb.get(Keys.ACTION);
        if (act == null) {
            act = bb.get(Keys.FALLBACK);
        }
        if (act == null) {
            act = new boolean[MarioActions.numberOfActions()];
        }
        world.lastAction = act.clone();

        if (DEBUG) {
            System.out.printf("t=%3d x=%6.1f y=%6.1f left=%3d  %s | %s%n", world.tick, world.x, world.y,
                    world.framesLeft, (Object) bb.get(Keys.BRANCH), planner.debug);
        }
        return act;
    }

    @Override
    public String getAgentName() {
        return "firstLastBTAgent";
    }

    // find the xml whether we run from an ide (copied to the class folder)
    // or straight from the project folder
    private InputStream openTree() throws Exception {
        // -Dbt.tree=some/other.xml swaps in a different tree, handy for testing
        String other = System.getProperty("bt.tree");
        if (other != null) {
            return new FileInputStream(other);
        }
        InputStream in = Agent.class.getResourceAsStream(TREE_FILE);
        if (in != null) {
            return in;
        }
        // folder from the package so renaming the agent folder just works
        String pkg = Agent.class.getPackage().getName().replace('.', '/') + "/";
        String[] spots = {"src/" + pkg, pkg, "../src/" + pkg, ""};
        for (String s : spots) {
            File f = new File(s + TREE_FILE);
            if (f.isFile()) {
                return new FileInputStream(f);
            }
        }
        // walk up from wherever the classes are and look for the source folder
        URL where = Agent.class.getProtectionDomain().getCodeSource().getLocation();
        File dir = new File(where.toURI());
        for (int i = 0; i < 6 && dir != null; i++, dir = dir.getParentFile()) {
            File f = new File(dir, "src/" + pkg + TREE_FILE);
            if (f.isFile()) {
                return new FileInputStream(f);
            }
        }
        throw new java.io.FileNotFoundException(TREE_FILE + " not found next to the classes or under src/" + pkg);
    }
}
