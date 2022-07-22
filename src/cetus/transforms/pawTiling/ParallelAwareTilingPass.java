package cetus.transforms.pawTiling;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cetus.analysis.LoopTools;
import cetus.exec.CommandLineOptionSet;
import cetus.hir.ArrayAccess;
import cetus.hir.AssignmentExpression;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.Loop;
import cetus.hir.Program;
import cetus.hir.Traversable;
import cetus.transforms.LoopInterchange;
import cetus.transforms.TransformPass;
import cetus.utils.DataDependenceUtils;

public class ParallelAwareTilingPass extends TransformPass {

    public final static String PARAM_NAME = "paw-tiling";

    private CommandLineOptionSet commandLineOptions;

    private int processors;

    private List<Loop> selectedOutermostLoops;

    private PawAnalysisData analysisData = new PawAnalysisData();

    private LoopInterchange loopInterchangePass = null;

    public ParallelAwareTilingPass(Program program) {
        this(program, null);
    }

    public ParallelAwareTilingPass(Program program, CommandLineOptionSet commandLineOptions) {
        super(program);
        this.commandLineOptions = commandLineOptions;
        if (commandLineOptions.getValue("verbosity").equals("1")) {
            System.out.println("Verbosity activated");
            analysisData.verbosity = true;
        }

        loopInterchangePass = new LoopInterchange(program);
    }

    @Override
    public String getPassName() {
        return "[Parallel-aware tiling]";
    }

    @Override
    public void start() {
        System.out.println(program);
        System.out.println("Printing DD Graph");
        DataDependenceUtils.printDependenceArcs(program);

        System.out.println("Printing direction matrix ");
        DataDependenceUtils.printDirectionMatrix(program);

        List<Loop> outermostLoops = LoopTools.getOutermostLoops(program);
        List<Loop> perfectLoops = filterValidLoops(outermostLoops);
        this.selectedOutermostLoops = perfectLoops;

        System.out.println("#### Selected loops: " + selectedOutermostLoops.size() + "\n");
        for (Loop outermostLoop : selectedOutermostLoops) {
            System.out.println(outermostLoop + "\n");
        }
        System.out.println("#### END Selected loops");

        System.out.println(analysisData);

        for (Loop outermostLoop : selectedOutermostLoops) {
            try {
                runPawTiling(outermostLoop);
            } catch (Exception e) {
                System.out.println(" ----");
                System.out.println("Error trying to run paw tiling");
                System.out.println("Loop:");
                System.err.println(outermostLoop);
                System.out.println("Error:");
                e.printStackTrace();
                System.out.println(" ---- ");
            }
        }

    }

    private void runPawTiling(Loop loopNest) {
        runDataReuseAnalysis(loopNest);
        createTiledVersions(loopNest);

    }

    private List<Expression> runDataReuseAnalysis(Loop loopNest) {

        loopInterchangePass.start();

        LinkedList<Loop> loopNestList = LoopTools.calculateInnerLoopNest(loopNest);

        List<Expression> memoryOrder = loopInterchangePass.ReusabilityAnalysis(program, loopNest,
                getLoopAssingmentExpressions(loopNest), getLoopArrayAccesses(loopNest), loopNestList);

        System.out.println(program.toString());

        System.out.println("### REUSABILITY ###");

        for (int i = 0; i < memoryOrder.size(); i++) {
            Expression expr = memoryOrder.get(i);
            System.out.println(expr);
        }

        System.out.println("### REUSABILITY END ###");

        return memoryOrder;
    }

    private void createTiledVersions(Loop loopNest) {

    }

    private List<AssignmentExpression> getLoopAssingmentExpressions(Loop loop) {
        List<AssignmentExpression> assignmentExpressions = new ArrayList<>();
        DepthFirstIterator<Traversable> statementsIterator = new DepthFirstIterator<>(loop);
        while (statementsIterator.hasNext()) {
            Traversable stm = statementsIterator.next();
            if (stm instanceof AssignmentExpression) {
                assignmentExpressions.add((AssignmentExpression) stm);
            }
        }
        return assignmentExpressions;
    }

    public List<ArrayAccess> getLoopArrayAccesses(Loop loop) {
        List<ArrayAccess> arrayAccesses = new ArrayList<>();
        DepthFirstIterator<Traversable> statementsIterator = new DepthFirstIterator<>(loop);
        while (statementsIterator.hasNext()) {
            Traversable stm = statementsIterator.next();
            if (stm instanceof ArrayAccess) {
                arrayAccesses.add((ArrayAccess) stm);
            }
        }
        return arrayAccesses;
    }

    public List<Loop> filterValidLoops(List<Loop> loops) {
        List<Loop> validLoops = new ArrayList<>();
        for (Loop loop : loops) {
            if (isCanonical(loop)
                    && isPerfectNest(loop)
                    && !containsFunctionCalls(loop)
                    && isIncreasingOrder(loop)) {
                validLoops.add(loop);
            }
        }

        return validLoops;
    }

    public boolean isIncreasingOrder(Loop loop) {

        if (LoopTools.getIncrementExpression(loop).toString().equals("-1")) {
            analysisData.nonIncreasingOrderLoops.add(loop);
            return false;
        }

        return true;
    }

    public boolean isCanonical(Loop loop) {
        if (!LoopTools.isCanonical(loop)) {
            analysisData.nonCanonicalLoops.add(loop);
            return false;
        }
        return true;
    }

    public boolean isPerfectNest(Loop loop) {
        if (!LoopTools.isPerfectNest(loop)) {
            analysisData.nonPerfectNestLoops.add(loop);
            return false;
        }
        return true;
    }

    public boolean containsFunctionCalls(Loop loop) {
        if (LoopTools.containsFunctionCall(loop)) {
            analysisData.withFunctionCallLoops.add(loop);
            return true;
        }
        return false;
    }

    // public void stripmining() {
    // chooseStrip();
    // }

    // public int chooseStrip(Loop loop) {

    // }

}
