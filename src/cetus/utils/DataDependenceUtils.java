package cetus.utils;

import java.util.LinkedList;
import java.util.List;

import cetus.analysis.DDGraph;
import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.analysis.DDGraph.Arc;
import cetus.hir.DepthFirstIterator;
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
        for (int i = 0; i < n; i++) {
            Arc arc = arcs.get(i);
            Statement sourceStatement = arc.getSourceStatement();
            Statement sinkStatement = arc.getSinkStatement();
            sourceStatement.getParent();
            byte type = arc.getDependenceType();
            String dependencyType = type == 1 ? "Flow" : type == 2 ? "Anti" : type == 3 ? "Output" : "" + type;

            System.out.printf("%s: %s -> %s\n",
                    dependencyType,
                    sourceStatement,
                    sinkStatement);
        }
    }

    public static void printDirectionMatrix(Program program) {

        List<Loop> outermLoops = LoopTools.getOutermostLoops(program);

        for (int i = 0; i < outermLoops.size(); i++) {
            Loop enclosingLoop = outermLoops.get(i);
            LinkedList<Loop> innermLoops = LoopTools.calculateInnerLoopNest(enclosingLoop);
            List<DependenceVector> dependenceVectors = program.getDDGraph().getDirectionMatrix(innermLoops);
            printDirectionMatrix(dependenceVectors);
        }

    }

    private static void printDirectionMatrix(List<DependenceVector> dependenceVectors) {
        int n = dependenceVectors.size();
        for (int i = 0; i < n; i++) {
            DependenceVector vector = dependenceVectors.get(i);
            System.out.printf("vector: %s\n", vector.VectorToString());
        }
    }

}
