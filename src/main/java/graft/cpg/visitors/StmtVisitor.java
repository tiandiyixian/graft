package graft.cpg.visitors;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.*;

import graft.cpg.AstBuilder;
import graft.cpg.CfgBuilder;

import static graft.Const.*;

/**
 * Visitor applied to Jimple statements to create CFG nodes for them.
 *
 * @author Wim Keirsgieter
 */
public class StmtVisitor extends AbstractStmtSwitch {

    private static Logger log = LoggerFactory.getLogger(StmtVisitor.class);

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        log.trace("Visiting AssignStmt");
        caseDefinitionStmt(stmt);
    }

    @Override
    public void caseBreakpointStmt(BreakpointStmt stmt) {
        log.trace("Visiting BreakpointStmt");
        // TODO
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
        log.trace("Visiting EnterMonitorStmt");
        // TODO
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
        log.trace("Visiting ExitMonitorStmt");
        // TODO
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void caseGotoStmt(GotoStmt stmt) {
        log.trace("Visiting GotoStmt - ignoring");
    }

    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        log.trace("Visiting IdentityStmt");
        caseDefinitionStmt(stmt);
    }

    @Override
    public void caseIfStmt(IfStmt stmt) {
        log.trace("Visiting IfStmt");
        Vertex ifVertex = CfgBuilder.genCfgNode(stmt, CONDITIONAL_STMT, stmt.toString());

        Vertex condVertex = AstBuilder.genValueNode(stmt.getCondition());
        AstBuilder.genAstEdge(ifVertex, condVertex, EXPR, EXPR);

        setResult(ifVertex);
    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        log.trace("Visiting InvokeStmt");
        Vertex invokeVertex = CfgBuilder.genCfgNode(stmt, INVOKE_STMT, stmt.toString());

        Vertex exprVertex = AstBuilder.genValueNode(stmt.getInvokeExpr());
        AstBuilder.genAstEdge(invokeVertex, exprVertex, EXPR, EXPR);

        setResult(invokeVertex);
    }

    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        log.trace("Visiting LookupSwitchStmt");
        // TODO
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void caseNopStmt(NopStmt stmt) {
        log.trace("Visiting NopStmt - ignoring");
    }

    @Override
    public void caseRetStmt(RetStmt stmt) {
        // TODO: ret stmt vs return stmt?
        log.trace("Visiting RetStmt");
        Vertex retVertex = CfgBuilder.genCfgNode(stmt, RETURN_STMT, stmt.toString());
        setResult(retVertex);
    }

    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        log.trace("Visiting ReturnStmt");
        Vertex retVertex = CfgBuilder.genCfgNode(stmt, RETURN_STMT, stmt.toString());

        Vertex opVertex = AstBuilder.genValueNode(stmt.getOp());
        AstBuilder.genAstEdge(retVertex, opVertex, RETURNS, RETURNS);

        setResult(retVertex);
    }

    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
        log.trace("Visiting ReturnVoidStmt");
        Vertex retVertex = CfgBuilder.genCfgNode(stmt, RETURN_STMT, stmt.toString());
        setResult(retVertex);
    }

    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        log.trace("Visiting TableSwitchStmt");
        // TODO
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
        log.trace("Visiting ThrowStmt");
        // TODO
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void defaultCase(Object obj) {
        log.warn("Unrecognised Stmt class '{}'", obj.getClass());
        throw new RuntimeException("Unrecognised statement class (see logs for details)");
    }

    // Generate CFG node for definition statements, with AST nodes for the target and value
    private void caseDefinitionStmt(DefinitionStmt stmt) {
        Vertex assignVertex = CfgBuilder.genCfgNode(stmt, ASSIGN_STMT, stmt.toString());

        Vertex leftOpVertex = AstBuilder.genValueNode(stmt.getLeftOp());
        Vertex rightOpVertex = AstBuilder.genValueNode(stmt.getRightOp());
        AstBuilder.genAstEdge(assignVertex, leftOpVertex, TARGET, TARGET);
        AstBuilder.genAstEdge(assignVertex, rightOpVertex, VALUE, VALUE);

        setResult(assignVertex);
    }

}
