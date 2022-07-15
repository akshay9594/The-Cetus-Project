package cetus.transforms.pawTiling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import cetus.analysis.DDGraph;
import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.analysis.DDGraph.Arc;
import cetus.exec.CommandLineOptionSet;
import cetus.hir.ArrayAccess;
import cetus.hir.AssignmentExpression;
import cetus.hir.DFIterator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.FloatLiteral;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Loop;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Symbolic;
import cetus.transforms.TransformPass;
import cetus.utils.DataDependenceUtils;

public class ParallelAwareTilingPass extends TransformPass {

    public final static String PARAM_NAME = "paw-tiling";

    private CommandLineOptionSet commandLineOptions;

    private int processors;

    private List<Loop> selectedOutermostLoops;

    private PawAnalysisData analysisData = new PawAnalysisData();

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

        System.out.println(analysisData);

    }

    public List<Loop> filterValidLoops(List<Loop> loops) {
        List<Loop> perfectLoops = new ArrayList<>();
        for (Loop loop : loops) {
            if (isCanonical(loop) && isPerfectNest(loop) && !containsFunctionCalls(loop)) {
                perfectLoops.add(loop);
            }
        }

        return perfectLoops;
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

    /**
     * Reusability Test to determine the innermost loop in the nest for Max
     * reusability.
     * 1. Loop which accesses the least number of cache lines should be the
     * innermost loop.
     * 2. To find reusability score , for a loop:
     * (a) Form Reference groups with array accesses
     * (b) Array accesses can be of following types:
     * - Accesses with loop carried or non-loop carried dependencies
     * - Accesses with no loop dependencies
     * - Loop invariant accesses
     * * Refer to K.S. McKinley's paper - 'Optimizing for parallelism and locality'
     * on how the ref groups are formed
     * 3. Add the costs in terms of cache lines for each reference group
     * - For an array access w.r.t. candidate innermost loop
     * (a) The array access requires 'trip/Cache Line size' no. of cache lines if
     * the loop index of the
     * candidate innermost loop appears on the rightmost dimension of the
     * access(row-major access)
     * (b) The array access requires 1 cache line if it is loop invariant and
     * (c) The array access requires 'trip' no. of cache lines if the loop index of
     * the candidate innermost loop
     * appears on the leftmost dimension of the access(Column-major access)
     * *trip : Number of iterations of the candidate innermost loop.
     * 4. Multiply the Reference group costs with the number of iterations of loops
     * other than the candidate innnermost loop
     * 5. Here Cache line size is assumed to be 64.
     * 6.Symbolic loop bounds are also supported, though extensive testing needs to
     * be done to ensure accuracy.
     * 
     * @param OriginalProgram - The program
     * @param LoopNest        - The loop nest to be analyzed
     * @param LoopExprs       - The expressions in the loop body
     * @param LoopArrays      - All array accesses in the loop body
     * @param LoopNestList    - List of loops in the loop nest
     * @return - - Order of the loops in the nest for max reusability
     */

    // public List ReusabilityAnalysis(Program OriginalProgram, Loop LoopNest,
    // List<AssignmentExpression> LoopExprs, List<ArrayAccess> LoopArrays,
    // LinkedList<Loop> LoopNestList) {

    // int i, j, k, l;

    // long count;

    // List<Expression> LoopNestOrder = new ArrayList<Expression>();
    // HashMap<Expression, Long> LoopCostMap = new HashMap<Expression, Long>();
    // HashMap<Expression, Expression> Symbolic_LoopCostMap = new
    // HashMap<Expression, Expression>();

    // List<Long> scores = new ArrayList<>();
    // List<Expression> Symbolic_scores = new ArrayList<>();
    // Expression IndexOfInnerLoop = null;

    // DDGraph programDDG = program.getDDGraph();
    // ArrayList<DependenceVector> Loopdpv = new ArrayList<>();

    // Loopdpv = programDDG.getDirectionMatrix(LoopNestList);

    // DepthFirstIterator LoopNestiter = new DepthFirstIterator(LoopNest);

    // while (LoopNestiter.hasNext()) {

    // Object LoopObj = LoopNestiter.next();

    // if (LoopObj instanceof ForLoop) {

    // LoopNestOrder.add(LoopTools.getIndexVariable((ForLoop) LoopObj));

    // }

    // }

    // // System.out.println("loop nest: " + LoopNest +"\n");

    // Boolean SymbolicLoopStride = false;

    // HashMap LoopNestIterationMap = LoopIterationMap(LoopNest);

    // // Getting reusability score

    // DFIterator<ForLoop> forloopiter = new DFIterator<>(LoopNest, ForLoop.class);

    // ArrayList<Long> RefGroupCost = new ArrayList<Long>();
    // ArrayList<Expression> Symbolic_RefGRoupCost = new ArrayList<Expression>();
    // ArrayList<Expression> LHSDim = new ArrayList<Expression>();
    // ArrayList<Expression> RHSDim = new ArrayList<Expression>();
    // long LoopstrideValue = 0;
    // long trip_currentLoop = 0;
    // long TotalLoopCost = 0;

    // IntegerLiteral initalValue = new IntegerLiteral(0);
    // FloatLiteral cls = new FloatLiteral(0.015625);
    // IntegerLiteral Identity = new IntegerLiteral(1);

    // Expression Symbolic_LoopTripCount = null;
    // Expression Symbolic_count = null;
    // Expression Symbolic_TotalLoopCost = null;

    // while (forloopiter.hasNext()) {

    // ForLoop loop = forloopiter.next();

    // // Collecting Loop Information
    // Expression LoopIdx = LoopTools.getIndexVariable(loop);

    // Expression LoopIncExpr = LoopTools.getIncrementExpression(loop);

    // if (LoopIncExpr instanceof IntegerLiteral)
    // LoopstrideValue = ((IntegerLiteral) LoopIncExpr).getValue();

    // else
    // SymbolicLoopStride = true;

    // List ReferenceGroups = RefGroup(loop, LoopArrays, LoopNestOrder);

    // if (!SymbolicIter)
    // trip_currentLoop = (long) LoopNestIterationMap.get(LoopIdx);

    // else
    // Symbolic_LoopTripCount = (Expression) LoopNestIterationMap.get(LoopIdx);

    // RefGroupCost = new ArrayList<>();

    // Symbolic_RefGRoupCost = new ArrayList<>();

    // ArrayList RepresentativeGroup = new ArrayList<>();

    // // Taking one Array Access from each Ref Group to form a representative group

    // for (i = 0; i < ReferenceGroups.size(); i++) {

    // ArrayList Group = (ArrayList) ReferenceGroups.get(i);

    // RepresentativeGroup.add(Group.get(0));

    // }

    // for (j = 0; j < RepresentativeGroup.size(); j++) {

    // count = 0;

    // Symbolic_count = (Expression) initalValue;

    // ArrayAccess Array = (ArrayAccess) RepresentativeGroup.get(j);

    // List<Expression> ArrayDims = Array.getIndices();

    // LHSDim = new ArrayList<>();

    // RHSDim = new ArrayList<>();

    // for (k = 0; k < ArrayDims.size(); k++) {

    // if (k == 0)
    // LHSDim.add(ArrayDims.get(k));
    // else
    // RHSDim.add(ArrayDims.get(k));

    // }

    // /*
    // * Cost = (trip)/cache line size if:
    // * (a) Loop index variable is present in the right hand side dimension of the
    // * array access
    // * (b) The Dimension has a unit stride and
    // * (c) The loop also has a unit stride
    // *
    // */

    // Expression RHSExprWithLoopID = null;

    // for (l = 0; l < RHSDim.size(); l++) {

    // Expression Expr = RHSDim.get(l);

    // if (Expr.toString().contains(LoopIdx.toString())) {

    // RHSExprWithLoopID = Expr;
    // break;

    // }

    // }

    // Expression LHSExpr = LHSDim.get(0);

    // // If Loop Trip count is not Symbolic

    // if (Symbolic_LoopTripCount == null) {

    // if (RHSExprWithLoopID != null && UnitStride(RHSExprWithLoopID, LoopIdx) &&
    // LoopstrideValue == 1)
    // count += (trip_currentLoop / 64);

    // /*
    // *
    // * Cost = 1 if:
    // * Array access is loop invariant
    // *
    // */

    // else if (!LHSExpr.toString().contains(LoopIdx.toString()) &&
    // RHSExprWithLoopID == null) {
    // count += 1;
    // }

    // /*
    // * Cost = (trip) if:
    // * (a) Loop index variable is present in the left hand side dimension of the
    // * array access
    // * (b) The Dimension has a non- unit stride or
    // * (c) The loop candidate loop has a non-unit stride
    // *
    // */

    // else
    // count += trip_currentLoop;

    // RefGroupCost.add(count);
    // }

    // // If Loop Trip Count is Symbolic

    // else {

    // if (RHSExprWithLoopID != null && UnitStride(RHSExprWithLoopID, LoopIdx) &&
    // LoopstrideValue == 1) {
    // Expression result = Symbolic.multiply(Symbolic_LoopTripCount, (Expression)
    // cls);
    // Symbolic_count = Symbolic.add(Symbolic_count, result);
    // }

    // else if (!LHSExpr.toString().contains(LoopIdx.toString()) &&
    // RHSExprWithLoopID == null)
    // Symbolic_count = Symbolic.add(Symbolic_count, (Expression) Identity);

    // else {

    // Symbolic_count = Symbolic.add(Symbolic_count, Symbolic_LoopTripCount);

    // }

    // Symbolic_RefGRoupCost.add(Symbolic_count);

    // }

    // }

    // if (!SymbolicIter) {
    // TotalLoopCost = LoopCost(RefGroupCost, LoopNestIterationMap, LoopIdx);

    // scores.add(TotalLoopCost);

    // LoopCostMap.put(LoopIdx, TotalLoopCost);
    // }

    // else {

    // Symbolic_TotalLoopCost = SymbolicLoopCost(Symbolic_RefGRoupCost,
    // LoopNestIterationMap, LoopIdx);

    // Symbolic_scores.add(Symbolic_TotalLoopCost);
    // Symbolic_LoopCostMap.put(LoopIdx, Symbolic_TotalLoopCost);

    // }

    // }

    // // System.out.println("Map: " + Symbolic_LoopCostMap +"\n");

    // if (!LoopCostMap.isEmpty()) {
    // Collections.sort(scores, Collections.reverseOrder());

    // long MinScore = Collections.min(scores);

    // for (Expression key : LoopCostMap.keySet()) {

    // long Cost = LoopCostMap.get(key);

    // if (Cost == MinScore) {

    // IndexOfInnerLoop = key;

    // }

    // /*
    // * Order of loops in the loop nest according to Cost analysis
    // * Loop accessing the least number of cache lines is innermost while
    // * the one accessing the most number of cache lines is outermost.
    // *
    // */

    // LoopNestOrder.set(scores.indexOf(Cost), key);

    // }
    // }

    // else {

    // // For Symbolic Loop Costs

    // int counter = 0;

    // List<Expression> DominantTerms = new ArrayList<>();

    // Expression variable = null;

    // List<Expression> ListOfExpressionVariables = new ArrayList<>();

    // for (k = 0; k < Symbolic_scores.size(); k++) {

    // Expression se1 = (Expression) Symbolic_scores.get(k);

    // variable = Symbolic.getVariables(se1).get(0);

    // ListOfExpressionVariables.add(variable);

    // List<Expression> Terms = Symbolic.getTerms(se1);

    // List<Integer> VariableCount = new ArrayList<>();

    // for (i = 0; i < Terms.size(); i++) {

    // Expression childterm = Terms.get(i);

    // counter = 0;

    // if (childterm.getChildren().contains(variable)) {

    // List entries = childterm.getChildren();

    // for (j = 0; j < entries.size(); j++) {

    // if (entries.get(j).equals(variable))
    // counter++;

    // }

    // VariableCount.add(counter);
    // }

    // else
    // VariableCount.add(counter);

    // }

    // int MaxCount = Collections.max(VariableCount);

    // int indx = VariableCount.indexOf(MaxCount);

    // DominantTerms.add(Terms.get(indx));

    // }

    // List Coeffs = new ArrayList<>();

    // for (i = 0; i < DominantTerms.size(); i++) {

    // Expression term = DominantTerms.get(i);

    // Identifier VarID = (Identifier) ListOfExpressionVariables.get(i);

    // Coeffs.add(Symbolic.getCoefficient(term, VarID));

    // }

    // Collections.sort(Coeffs, Collections.reverseOrder());

    // List<Expression> Sortedscores = new ArrayList<>();

    // for (i = 0; i < Coeffs.size(); i++) {

    // Expression coefficient = (Expression) Coeffs.get(i);

    // for (j = 0; j < DominantTerms.size(); j++) {

    // if (DominantTerms.get(j).getChildren().contains(coefficient)) {

    // Sortedscores.add(Symbolic_scores.get(j));

    // }

    // }

    // }

    // // System.out.println( "LNO: " + Symbolic_LoopCostMap + " \nScores sorted: "
    // +
    // // Sortedscores +"\n");

    // for (Expression key : Symbolic_LoopCostMap.keySet()) {

    // Expression Symbolic_cost = Symbolic_LoopCostMap.get(key);

    // if (Sortedscores.indexOf(Symbolic_cost) != -1)
    // LoopNestOrder.set(Sortedscores.indexOf(Symbolic_cost), key);

    // }

    // }

    // return LoopNestOrder;

    // }

}
