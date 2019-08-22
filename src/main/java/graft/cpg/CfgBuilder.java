package graft.cpg;

import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.tagkit.*;
import soot.toolkits.graph.UnitGraph;

import graft.cpg.visitors.StmtVisitor;
import graft.db.GraphUtil;
import graft.traversal.CpgTraversalSource;

import static graft.Const.*;

/**
 * Generate the control flow graph.
 *
 * @author Wim Keirsgieter
 */
public class CfgBuilder {

    private static Logger log = LoggerFactory.getLogger(CfgBuilder.class);

    // ********************************************************************************************
    // public methods
    // ********************************************************************************************

    /**
     * Build a CFG from the given unit graph, storing the generated nodes in a map with their
     * corresponding units as keys.
     *
     * @param unitGraph the unit graph
     * @param generatedNodes the map to store the generated nodes in
     */
    public static void buildCfg(UnitGraph unitGraph, Map<Unit, Object> generatedNodes) {
        Body body = unitGraph.getBody();
        log.debug("Building CFG for method '{}'", body.getMethod().getName());

        // generate method entry node
        SootMethod method = body.getMethod();
        Vertex entryNode = genCfgNode(null, ENTRY, method.getSignature(), method.getName());
        CpgUtil.addNodeProperty(entryNode, METHOD_SIG, method.getSignature());
        CpgUtil.addNodeProperty(entryNode, METHOD_NAME, method.getName());
        CpgUtil.addNodeProperty(entryNode, METHOD_SCOPE, method.getDeclaringClass().getName());

        // get the class AST node
        Vertex classNode = GraphUtil.graph().traversal(CpgTraversalSource.class)
                .V().hasLabel(AST_NODE)
                .has(NODE_TYPE, CLASS)
                .has(FULL_NAME, body.getMethod().getDeclaringClass().getName())
                .next();

        // draw method or constructor edge from class to method node
        if (body.getMethod().isConstructor()) {
            AstBuilder.genAstEdge(classNode, entryNode, CONSTRUCTOR, CONSTRUCTOR);
        } else {
            AstBuilder.genAstEdge(classNode, entryNode, METHOD, METHOD);
        }

        // generate control flow graph and add nodes recursively
        for (Unit head : unitGraph.getHeads()) {
            Vertex headVertex = genUnitNode(head, unitGraph, method.getSignature(), generatedNodes);
            genCfgEdge(entryNode, headVertex, EMPTY, EMPTY);
        }
    }

    /**
     * Generate a CFG node with the given node type and text label properties.
     *
     * @param stmt the statement to generate a node for (contains source file info)
     * @param nodeType the node type of the CFG node
     * @param textLabel the text label of the CFG node
     * @return the generated CFG node
     */
    public static Vertex genCfgNode(Stmt stmt, String nodeType, String methodSig, String textLabel) {
        CpgTraversalSource g = GraphUtil.graph().traversal(CpgTraversalSource.class);
        Vertex node = g.addV(CFG_NODE)
                .property(NODE_TYPE, nodeType)
                .property(TEXT_LABEL, textLabel)
                .property(FILE_PATH, getSourcePath(stmt))
                .property(FILE_NAME, getSourceFile(stmt))
                .property(METHOD_SIG, methodSig)
                .property(LINE_NO, getLineNr(stmt))
                .next();
        // GraphUtil.graph().tx().commit();
        return node;
    }

    /**
     * Generate a CFG edge between two nodes with the given edge type and text label properties.
     *
     * @param from the out-vertex of the CFG edge
     * @param to the in-vertex of the CFG edge
     * @param edgeType the type of the CFG edge
     * @param textLabel the text label of the CFG edge
     * @return the generated CFG edge
     */
    public static Edge genCfgEdge(Vertex from, Vertex to, String edgeType, String textLabel) {
        CpgTraversalSource g = GraphUtil.graph().traversal(CpgTraversalSource.class);
        assert from != null;
        assert to != null;
        Edge edge = g.addE(CFG_EDGE)
                .from(from).to(to)
                .property(EDGE_TYPE, edgeType)
                .property(TEXT_LABEL, textLabel)
                .next();
        // GraphUtil.graph().tx().commit();
        return edge;
    }

    // ********************************************************************************************
    // private methods
    // ********************************************************************************************

    private static Vertex genUnitNode(Unit unit, UnitGraph unitGraph, String methodSig, Map<Unit, Object> generated) {
        if (unit instanceof GotoStmt) {
            return genUnitNode(((GotoStmt) unit).getTarget(), unitGraph, methodSig, generated);
        }
        CpgTraversalSource g = GraphUtil.graph().traversal(CpgTraversalSource.class);
        log.debug("Generating Unit '{}'", unit.toString());

        Vertex unitVertex;
        if (generated.containsKey(unit)) {
            unitVertex = g.V(generated.get(unit)).next();
        } else {
            unitVertex = genUnitNode(unit, methodSig);
            generated.put(unit, unitVertex);
        }
        if (unitVertex == null) {
            log.warn("Could not generate CFG node for unit '{}'", unit.toString());
            return null;
        }

        for (Unit succ : unitGraph.getSuccsOf(unit)) {
            Vertex succVertex;
            if (generated.containsKey(succ)) {
                succVertex = g.V(generated.get(succ)).next();
            } else {
                succVertex = genUnitNode(succ, unitGraph, methodSig, generated);
            }
            if (succVertex == null) {
                log.warn("Could not generate CFG node for unit '{}'", succ.toString());
                continue;
            }
            if (unit instanceof IfStmt) {
                if (succ.equals(((IfStmt) unit).getTarget())) {
                    genCfgEdge(unitVertex, succVertex, TRUE, TRUE);
                } else {
                    genCfgEdge(unitVertex, succVertex, FALSE, FALSE);
                }
            } else {
                genCfgEdge(unitVertex, succVertex, EMPTY, EMPTY);
            }
        }

        return unitVertex;
    }

    private static Vertex genUnitNode(Unit unit, String methodSig) {
        Stmt stmt = (Stmt) unit;
        StmtVisitor visitor = new StmtVisitor(methodSig);
        stmt.apply(visitor);
        return (Vertex) visitor.getResult();
    }

    // Get the source file path from the statement tags
    private static String getSourcePath(Stmt stmt) {
        if (stmt == null) {
            return UNKNOWN;
        }
        return UNKNOWN; // TODO
    }

    // Get the source file name from the statement tags
    private static String getSourceFile(Stmt stmt) {
        if (stmt == null) {
            return UNKNOWN;
        }
        if (stmt.getTag("SourceFileTag") != null) {
            return ((SourceFileTag) stmt.getTag("SourceFileTag")).getSourceFile();
        } else {
            return UNKNOWN;
        }
    }

    // Get the source line number from the statement tags
    private static int getLineNr(Stmt stmt) {
        if (stmt == null) {
            return -1;
        }
        if (stmt.getTag("SourceLnPosTag") != null) {
            return ((SourceLnPosTag) stmt.getTag("SourceLnPosTag")).startLn();
        } else if (stmt.getTag("JimpleLineNumberTag") != null) {
            return ((JimpleLineNumberTag) stmt.getTag("JimpleLineNumberTag")).getLineNumber();
        } else if (stmt.getTag("LineNumberTag") != null) {
            return ((LineNumberTag) stmt.getTag("LineNumberTag")).getLineNumber();
        } else if (stmt.getTag("SourceLineNumberTag") != null) {
            return ((SourceLineNumberTag) stmt.getTag("SourceLineNumberTag")).getLineNumber();
        } else {
            return -1;
        }
    }

}
