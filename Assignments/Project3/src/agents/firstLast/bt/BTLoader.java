package agents.firstLast.bt;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// the bt interpreter
// reads the xml text file and turns every tag into a task object
// xml parsing is the built in java dom parser, the rest is ours
//
// tags it knows:
//   composites  sequence selector parallel utilityselector
//   decorators  inverter succeeder untilfail repeat(times) cooldown(ticks)
//   leaves      <condition>Name</condition> <action>Name</action>
//   any tag can have name="label", leaves can have extra knobs as attributes
//   a child node of a utilityselector gives its score with utility="fnName"
public class BTLoader {
    private final TaskLibrary lib;

    public BTLoader(TaskLibrary lib) {
        this.lib = lib;
    }

    public Task load(InputStream in) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        // no outside entities, just our file
        f.setExpandEntityReferences(false);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setIgnoringComments(true);
        DocumentBuilder b = f.newDocumentBuilder();
        Document doc = b.parse(in);
        Element top = doc.getDocumentElement();

        // optional wrapper tag around the real root
        String tag = top.getTagName().toLowerCase();
        if (tag.equals("behaviortree") || tag.equals("tree") || tag.equals("bt")) {
            List<Element> children = elementChildren(top);
            if (children.size() != 1) {
                throw new IllegalArgumentException("<" + top.getTagName() + "> needs exactly one root task");
            }
            return build(children.get(0));
        }
        return build(top);
    }

    // one xml element -> one task, recursive down the tree
    private Task build(Element e) {
        String tag = e.getTagName().toLowerCase();
        Task t;

        switch (tag) {
            // composites
            case "sequence":
                t = fill(new Sequence(), e);
                break;
            case "selector":
                t = fill(new Selector(), e);
                break;
            case "parallel":
                t = fill(new Parallel(), e);
                break;
            case "utilityselector":
                UtilitySelector us = new UtilitySelector();
                if (e.hasAttribute("stickiness")) {
                    us.setStickiness(Double.parseDouble(e.getAttribute("stickiness")));
                }
                t = fill(us, e);
                break;

            // decorators
            case "inverter":
                t = wrap(new Inverter(), e);
                break;
            case "succeeder":
                t = wrap(new Succeeder(), e);
                break;
            case "untilfail":
                t = wrap(new UntilFail(), e);
                break;
            case "repeat":
                t = wrap(new Repeat(intAttr(e, "times", 1)), e);
                break;
            case "cooldown":
                t = wrap(new Cooldown(intAttr(e, "ticks", 10)), e);
                break;

            // leaves
            case "condition":
            case "action":
                t = leaf(tag, e);
                break;

            default:
                throw new IllegalArgumentException("unknown bt tag <" + e.getTagName() + ">");
        }

        // shared attributes
        if (e.hasAttribute("name")) {
            t.setName(e.getAttribute("name"));
        }
        if (e.hasAttribute("utility")) {
            Utility u = lib.makeUtility(e.getAttribute("utility"));
            if (u == null) {
                throw new IllegalArgumentException("unknown utility '" + e.getAttribute("utility") + "'");
            }
            t.setUtility(u);
        }
        return t;
    }

    private Task fill(Composite c, Element e) {
        List<Element> children = elementChildren(e);
        if (children.isEmpty()) {
            throw new IllegalArgumentException("<" + e.getTagName() + "> has no child nodes");
        }
        for (Element k : children) {
            c.add(build(k));
        }
        return c;
    }

    private Task wrap(Decorator d, Element e) {
        List<Element> children = elementChildren(e);
        if (children.size() != 1) {
            throw new IllegalArgumentException("<" + e.getTagName() + "> needs exactly one child node");
        }
        d.setChild(build(children.get(0)));
        return d;
    }

    private Task leaf(String kind, Element e) {
        // name from the text like the slides, or a name attribute
        String name = e.getTextContent().trim();
        if (name.isEmpty() && e.hasAttribute("task")) {
            name = e.getAttribute("task").trim();
        }
        Leaf l = lib.makeLeaf(kind, name);
        if (l == null) {
            throw new IllegalArgumentException("unknown " + kind + " '" + name + "'");
        }

        // every other attribute is a knob for the leaf
        Map<String, String> params = new HashMap<>();
        NamedNodeMap attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node a = attrs.item(i);
            params.put(a.getNodeName(), a.getNodeValue());
        }
        l.configure(params);
        l.setName(name);
        return l;
    }

    private static List<Element> elementChildren(Element e) {
        List<Element> out = new ArrayList<>();
        NodeList nl = e.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element) {
                out.add((Element) nl.item(i));
            }
        }
        return out;
    }

    private static int intAttr(Element e, String key, int fallback) {
        if (!e.hasAttribute(key)) {
            return fallback;
        }
        return Integer.parseInt(e.getAttribute(key).trim());
    }
}
