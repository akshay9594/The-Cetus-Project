package cetus.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import cetus.analysis.DDGraph;
import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.analysis.DDGraph.Arc;
import cetus.hir.AssignmentExpression;
import cetus.hir.BinaryExpression;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.ForLoop;
import cetus.hir.Loop;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Traversable;

public class DataDependenceUtils {

    public static void printDependenceArcs(Program program) {
        DDGraph ddGraph = program.getDDGraph();
        if (ddGraph == null) {
            System.out.println("Empty dd graph");
            return;
        }
        List<Arc> arcs = ddGraph.getAllArcs();
        int n = arcs.size();
        // HashMap<String,
        // for (int i = 0; i < n; i++) {

        // }

        if (arcs.isEmpty()) {
            System.out.println("No dependencies");
            return;
        }

        HashMap<String, List<Statement[]>> byLoopsDeps = new HashMap<>();

        for (int i = 0; i < n; i++) {
            Arc arc = arcs.get(i);
            Statement sourceStatement = arc.getSourceStatement();
            Statement sinkStatement = arc.getSinkStatement();

            // loop only for deps in the same loop. No cross-loops deps (for now)
            if(getLoopParent(sourceStatement) != getLoopParent(sinkStatement)) {
                continue;
            }

            byte type = arc.getDependenceType();
            String dependencyType = type == 1 ? "Flow" : type == 2 ? "Anti" : type == 3 ? "Output" : "" + type;

            System.err.println("----DEPENDENCE BEGIN----");
            System.out.println();

            System.out.println("(" + getPrintableParentStatement(sourceStatement) + "){");
            System.out.println();

            System.out.println(dependencyType);
            System.out.printf("    %s -> %s\n\n", sourceStatement, sinkStatement);
            System.out.println("}");

            System.err.println("----END DEPENDENCE----\n");
            System.out.println();
            // System.err.println("----DEPENDENCE BEGIN----");
            // System.out.println("Dependency type: " + dependencyType);
            // System.out.printf("# Source for: %s\n## Source: %s\n",
            // getPrintableParentStatement(sourceStatement),
            // sourceStatement);
            // System.out.printf("# sink for: %s\n## Sink: %s\n",
            // getPrintableParentStatement(sinkStatement),
            // sinkStatement);
            // System.err.println("----END DEPENDENCE----");

        }
    }

    public static void printDirectionMatrix(Program program) {

        List<Loop> outermLoops = LoopTools.getOutermostLoops(program);

        for (int i = 0; i < outermLoops.size(); i++) {
            Loop enclosingLoop = outermLoops.get(i);
            LinkedList<Loop> innermLoops = LoopTools.calculateInnerLoopNest(enclosingLoop);
            List<DependenceVector> dependenceVectors = program.getDDGraph().getDirectionMatrix(innermLoops);
            printDirectionMatrix(enclosingLoop, dependenceVectors);
        }

    }

    private static void printDirectionMatrix(Loop enclosingLoop, List<DependenceVector> dependenceVectors) {
        int n = dependenceVectors.size();
        System.out.println("--- ENCLOSING LOOP ---");
        Statement loopBody = enclosingLoop.getBody();
        Expression condition = enclosingLoop.getCondition();
        System.out.println(condition);
        System.out.println("--- ENCLOSING LOOP ---");
        for (int i = 0; i < n; i++) {
            DependenceVector vector = dependenceVectors.get(i);
            System.out.printf("vector: %s\n", vector.VectorToString());
        }
    }

    private static Loop getLoopParent(Statement stm) {
        Traversable stmParent = stm.getParent();
        while (stmParent.getParent() != null && !(stmParent instanceof Loop)) {
            stmParent = stmParent.getParent();
        }

        if (stmParent instanceof Loop) {
            return (Loop) stmParent;
        }
        return null;
    }

    private static String getPrintableParentStatement(Statement stm) {

        Loop stmParent = getLoopParent(stm);
        String stmParentStr = stmParent.toString();
        if (stmParent instanceof ForLoop) {
            ForLoop loopStm = (ForLoop) stmParent;
            if (LoopTools.isCanonical(loopStm)) {
                ExpressionStatement initialStm = (ExpressionStatement) loopStm.getInitialStatement();
                AssignmentExpression loopExpression = (AssignmentExpression) initialStm.getExpression();
                BinaryExpression condExpr = (BinaryExpression) loopStm.getCondition();
                Expression stepExpr = loopStm.getStep();

                return loopExpression + ";" + condExpr + ";" + stepExpr;
            }

        }

        return stmParentStr.toString();
    }

}