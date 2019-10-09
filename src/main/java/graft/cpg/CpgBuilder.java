package graft.cpg;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import graft.Graft;
import graft.Options;
import graft.traversal.CpgTraversalSource;
import graft.utils.FileUtil;

import static graft.Const.*;

/**
 * Handles the actual construction of the CPG.
 *
 * @author Wim Keirsgieter
 */
public class CpgBuilder {

    private static Logger log = LoggerFactory.getLogger(CpgBuilder.class);

    public static void buildCpg(SootClass cls, File classFile) {
        // TODO: how does this handle interfaces, extensions, enums etc?
        Vertex classNode = AstBuilder.genAstNode(CLASS, cls.getShortName());
        CpgUtil.addNodeProperty(classNode, SHORT_NAME, cls.getShortName());
        CpgUtil.addNodeProperty(classNode, FULL_NAME, cls.getName());
        CpgUtil.addNodeProperty(classNode, FILE_NAME, classFile.getName());
        CpgUtil.addNodeProperty(classNode, FILE_PATH, classFile.getPath());
        CpgUtil.addNodeProperty(classNode, FILE_HASH, FileUtil.hashFile(classFile));

        log.debug("New class AST root");
        log.debug(CpgUtil.debugVertex(classNode));

        for (SootMethod method : cls.getMethods()) {
            try {
                Body body = method.retrieveActiveBody();
                buildCpg(body);
            } catch (RuntimeException e) {
                log.warn("No body for method '{}'", method.getSignature(), e);
            }
        }
    }

    /**
     * Build a CPG for the given method body.
     *
     * @param body the method body
     */
    public static void buildCpg(Body body) {
        UnitGraph unitGraph = new BriefUnitGraph(body);
        Map<Unit, Object> unitNodes = new HashMap<>();
        CfgBuilder.buildCfg(unitGraph, unitNodes);
        PdgBuilder.buildPdg(unitGraph, unitNodes);

        // TODO: make sure to do this everywhere its needed
        if (Options.v().getString(OPT_DB_IMPLEMENTATION).equals(NEO4J)) {
            Graft.cpg().commit();
        }
    }

    public static void amendCpg(SootClass cls, File classFile) {
        CpgTraversalSource g = Graft.cpg().traversal();
        g.V().hasLabel(AST_NODE)
                .has(NODE_TYPE, CLASS)
                .has(FULL_NAME, cls.getName())
                .drop()
                .iterate();

        for (SootMethod method : cls.getMethods()) {
            CpgUtil.dropCfg(method);
        }

        buildCpg(cls, classFile);
    }

}
