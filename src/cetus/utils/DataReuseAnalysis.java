package cetus.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import cetus.analysis.LoopTools;
import cetus.hir.ArrayAccess;
import cetus.hir.AssignmentExpression;
import cetus.hir.BinaryExpression;
import cetus.hir.DFIterator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.FloatLiteral;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Loop;
import cetus.hir.Statement;
import cetus.hir.Symbolic;
import cetus.hir.Traversable;

public class DataReuseAnalysis {

    private Loop loopNest;
    private HashMap<Expression, ?> loopCosts;
    private List<Expression> loopNestMemoryOrder;

    public DataReuseAnalysis(Loop loopNest) {

        this.loopNest = loopNest;

        this.loopNestMemoryOrder = ReuseAnalysis();
    }

    public Loop getLoopNest() {
        return loopNest;
    }

    public HashMap<Expression, ?> getLoopCosts() {
        return loopCosts;
    }

    public List<Expression> getLoopNestMemoryOrder() {
        return loopNestMemoryOrder;
    }

    /**
     * Following method forms reference groups of Array Accesses for each loop in
     * the Nest.
     * 1. Criteria for 2 references to be in the same group depend on whether they
     * have dependencies (Loop carrried and Non Loop Carried).
     * 2. Criteria for forming groups have been derived from the paper - "Optimizing
     * for Parallelism and Data Locality" - K.S Mckinley
     * 
     * @param candidateLoop         - The loop with the array accesses
     * @param loopBodyArrays        - Arrays in the loop body
     * @param originalLoopNestOrder - Original order of the loops
     * @return - list of reference groups
     */

    public List<List<ArrayAccess>> refGroup(Loop candidateLoop, List<ArrayAccess> loopBodyArrays,
            List<Expression> originalLoopNestOrder) {

        int i, j, k;

        List<List<ArrayAccess>> refGroups = new ArrayList<>();
        ArrayList<Expression> parentArrays = new ArrayList<Expression>();

        for (i = 0; i < loopBodyArrays.size(); i++) {

            parentArrays.add(loopBodyArrays.get(i).getArrayName());

        }

        LinkedHashSet<Expression> hashSet = new LinkedHashSet<>(parentArrays);

        parentArrays = new ArrayList<>(hashSet);

        List<Expression> loopIndexExpressions = new ArrayList<Expression>();
        Expression candidateLoopid = LoopTools.getIndexVariable(candidateLoop);

        for (i = 0; i < loopBodyArrays.size(); i++) {

            ArrayAccess a = loopBodyArrays.get(i);

            List<Expression> indicesExprs = a.getIndices();

            for (j = 0; j < indicesExprs.size(); j++) {

                Expression e = indicesExprs.get(j);

                for (k = 0; k < originalLoopNestOrder.size(); k++) {

                    Expression id = originalLoopNestOrder.get(k);

                    if (e.getChildren().contains(id) &&
                            !id.equals(candidateLoopid)) {
                        loopIndexExpressions.add(e);
                    }

                    else if (e.getChildren().isEmpty() &&
                            !e.equals(candidateLoopid) &&
                            !loopIndexExpressions.contains(e)) {
                        loopIndexExpressions.add(e);
                    }

                }
            }

        }

        ArrayList<ArrayAccess> references = new ArrayList<ArrayAccess>();

        ArrayList<ArrayAccess> arraysAssignedToGroups = new ArrayList<ArrayAccess>();

        for (i = 0; i < parentArrays.size(); i++) {

            Expression expr = parentArrays.get(i);

            for (j = 0; j < loopIndexExpressions.size(); j++) {

                Expression idxExpr = loopIndexExpressions.get(j);

                references = new ArrayList<>();
                for (k = 0; k < loopBodyArrays.size(); k++) {

                    ArrayAccess loopArray = loopBodyArrays.get(k);
                    if (loopArray.getIndices().contains(idxExpr)
                            && loopArray.getArrayName().equals(expr)
                            && !arraysAssignedToGroups.contains(loopArray)) {
                        references.add(loopArray);
                        arraysAssignedToGroups.add(loopArray);

                    }

                }

                if (!references.isEmpty()) {
                    refGroups.add(references);
                }

            }

        }

        LinkedHashSet<List<ArrayAccess>> tempHashSet = new LinkedHashSet<>(refGroups);

        refGroups = new ArrayList<>(tempHashSet);
        return refGroups;
    }

    public static String printRefGroup(List<List<ArrayAccess>> refGroup) {

        String format = "";
        format += "#### Ref group ####\n";

        for (int i = 0; i < refGroup.size(); i++) {

            List<ArrayAccess> references = refGroup.get(i);
            String referencesStr = "";
            for (int j = 0; j < references.size(); j++) {
                ArrayAccess reference = references.get(j);
                referencesStr += reference;
                if (j != references.size() - 1) {
                    referencesStr += ", ";
                }

            }

            format += "{ " + referencesStr + " }\n";

        }

        format += "\n#### END RefGroup ####\n";

        return format;
    }

    /**
     * Get a list of the representative elements from each
     * ref group passed as paramater
     * 
     * @param refGroups - A list of represnetative array access obtained by calling
     *                  the refGroup util
     * @return List of all representative array accesses
     */

    public static List<ArrayAccess> getRepresentativeRefs(List<List<ArrayAccess>> refGroups) {
        List<ArrayAccess> representatives = new ArrayList<>();

        for (List<ArrayAccess> group : refGroups) {
            if (group.size() > 0) {
                representatives.add(group.get(0));
            }
        }

        return representatives;
    }

    /**
     * Determines if the access is a stride 1 access
     * 
     * @param Expr - Input Expression
     * @param var  - LoopIndex
     * @return - boolean
     */
    // Determines if an access is a stride 1 access
    public static boolean UnitStride(Expression Expr, Expression var) {

        boolean isStrideOneAccess = false;

        Expression lhs = null;

        Expression rhs = null;

        if (Expr.equals(var)) {

            return true;

        }

        if (Expr instanceof BinaryExpression) {

            BinaryExpression binaryExpr = (BinaryExpression) Expr;

            lhs = binaryExpr.getLHS();

            rhs = binaryExpr.getRHS();

            if (binaryExpr.getOperator().toString().equals("*"))
                isStrideOneAccess = false;

            else if (lhs.getChildren().contains(var) && lhs.toString().contains("*"))
                isStrideOneAccess = false;

            else if (rhs.getChildren().contains(var) && rhs.toString().contains("*"))
                isStrideOneAccess = false;

            else
                isStrideOneAccess = true;

        }

        return isStrideOneAccess;

    }

    public static String printLoopCosts(HashMap<Expression, ?> loopCostMap) {

        String format = "";
        format += "#### Loop costs ####\n";

        Iterator<Expression> loopCostIter = loopCostMap.keySet().iterator();
        while (loopCostIter.hasNext()) {
            Expression keyExpr = loopCostIter.next();
            Object loopCost = loopCostMap.get(keyExpr);
            format += "#### Loop: " + keyExpr + ", cost:" + loopCost + "\n";
        }

        format += "#### end loop costs ####\n";

        return format;

    }

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
     * @return - - Order of the loops in the nest for max reusability
     */

    public List<Expression> ReuseAnalysis() {

        List<Loop> nestedLoops = new ArrayList<>();
        new DFIterator<Loop>(loopNest, Loop.class).forEachRemaining(nestedLoops::add);

        Loop innermostLoop = nestedLoops.get(0);
        Statement innermostLoopStm = innermostLoop.getBody();

        List<AssignmentExpression> loopExprs = new ArrayList<AssignmentExpression>();
        List<ArrayAccess> loopArrays = new ArrayList<>();

        new DepthFirstIterator<Traversable>(innermostLoopStm).forEachRemaining(child -> {
            if (child instanceof AssignmentExpression) {
                loopExprs.add((AssignmentExpression) child);
            } else if (child instanceof ArrayAccess) {
                loopArrays.add((ArrayAccess) child);
            }
        });

        // This was loops.get(0)
        HashMap<Expression, ?> loopNestIterMap = LoopIterationMap(loopNest);
        boolean hasSymbolicIter = false;
        if (HasSymbolicBounds(loopNestIterMap)) {
            hasSymbolicIter = true;
        } else if (HasNonSymbolicBounds(loopNestIterMap)) {
            hasSymbolicIter = false;
        }

        int i, j, k, l;

        long count;

        List<Expression> LoopNestOrder = new ArrayList<Expression>();
        HashMap<Expression, Long> LoopCostMap = new HashMap<Expression, Long>();
        HashMap<Expression, Expression> Symbolic_LoopCostMap = new HashMap<Expression, Expression>();

        List<Long> scores = new ArrayList<>();
        List<Expression> Symbolic_scores = new ArrayList<>();

        DepthFirstIterator<Traversable> LoopNestiter = new DepthFirstIterator<Traversable>(loopNest);

        while (LoopNestiter.hasNext()) {

            Object LoopObj = LoopNestiter.next();

            if (LoopObj instanceof ForLoop) {

                LoopNestOrder.add(LoopTools.getIndexVariable((ForLoop) LoopObj));

            }

        }

        HashMap<Expression, ?> LoopNestIterationMap = LoopIterationMap(loopNest);

        // Getting reusability score
        DFIterator<ForLoop> forloopiter = new DFIterator<ForLoop>(loopNest, ForLoop.class);

        ArrayList<Long> RefGroupCost = new ArrayList<Long>();
        ArrayList<Expression> Symbolic_RefGRoupCost = new ArrayList<Expression>();
        ArrayList<Expression> LHSDim = new ArrayList<Expression>();
        ArrayList<Expression> RHSDim = new ArrayList<Expression>();
        long LoopstrideValue = 0;
        long trip_currentLoop = 0;
        long TotalLoopCost = 0;

        IntegerLiteral initalValue = new IntegerLiteral(0);
        FloatLiteral cls = new FloatLiteral(0.015625);
        IntegerLiteral Identity = new IntegerLiteral(1);

        Expression Symbolic_LoopTripCount = null;
        Expression Symbolic_count = null;
        Expression Symbolic_TotalLoopCost = null;

        while (forloopiter.hasNext()) {

            ForLoop loop = forloopiter.next();

            // Collecting Loop Information
            Expression LoopIdx = LoopTools.getIndexVariable(loop);

            Expression LoopIncExpr = LoopTools.getIncrementExpression(loop);

            if (LoopIncExpr instanceof IntegerLiteral)
                LoopstrideValue = ((IntegerLiteral) LoopIncExpr).getValue();

            List<List<ArrayAccess>> ReferenceGroups = refGroup(loop, loopArrays, LoopNestOrder);

            DataReuseAnalysis.printRefGroup(ReferenceGroups);

            if (!hasSymbolicIter)
                trip_currentLoop = (long) LoopNestIterationMap.get(LoopIdx);

            else
                Symbolic_LoopTripCount = (Expression) LoopNestIterationMap.get(LoopIdx);

            RefGroupCost = new ArrayList<>();

            Symbolic_RefGRoupCost = new ArrayList<>();

            ArrayList<ArrayAccess> RepresentativeGroup = new ArrayList<>();

            // Taking one Array Access from each Ref Group to form a representative group

            for (i = 0; i < ReferenceGroups.size(); i++) {

                ArrayList<ArrayAccess> Group = (ArrayList<ArrayAccess>) ReferenceGroups.get(i);

                RepresentativeGroup.add(Group.get(0));

            }

            for (j = 0; j < RepresentativeGroup.size(); j++) {

                count = 0;

                Symbolic_count = (Expression) initalValue;

                ArrayAccess Array = (ArrayAccess) RepresentativeGroup.get(j);

                List<Expression> ArrayDims = Array.getIndices();

                LHSDim = new ArrayList<>();

                RHSDim = new ArrayList<>();

                for (k = 0; k < ArrayDims.size(); k++) {

                    if (k == 0)
                        LHSDim.add(ArrayDims.get(k));
                    else
                        RHSDim.add(ArrayDims.get(k));

                }

                /*
                 * Cost = (trip)/cache line size if:
                 * (a) Loop index variable is present in the right hand side dimension of the
                 * array access
                 * (b) The Dimension has a unit stride and
                 * (c) The loop also has a unit stride
                 * 
                 */

                Expression RHSExprWithLoopID = null;

                for (l = 0; l < RHSDim.size(); l++) {

                    Expression Expr = RHSDim.get(l);

                    if (Expr.toString().contains(LoopIdx.toString())) {

                        RHSExprWithLoopID = Expr;
                        break;

                    }

                }

                Expression LHSExpr = LHSDim.get(0);

                // If Loop Trip count is not Symbolic

                if (Symbolic_LoopTripCount == null) {

                    if (RHSExprWithLoopID != null && UnitStride(RHSExprWithLoopID, LoopIdx) && LoopstrideValue == 1)
                        count += (trip_currentLoop / 64);

                    /*
                     * 
                     * Cost = 1 if:
                     * Array access is loop invariant
                     * 
                     */

                    else if (!LHSExpr.toString().contains(LoopIdx.toString()) && RHSExprWithLoopID == null) {
                        count += 1;
                    }

                    /*
                     * Cost = (trip) if:
                     * (a) Loop index variable is present in the left hand side dimension of the
                     * array access
                     * (b) The Dimension has a non- unit stride or
                     * (c) The loop candidate loop has a non-unit stride
                     * 
                     */

                    else
                        count += trip_currentLoop;

                    RefGroupCost.add(count);
                }

                // If Loop Trip Count is Symbolic

                else {

                    if (RHSExprWithLoopID != null && UnitStride(RHSExprWithLoopID, LoopIdx) && LoopstrideValue == 1) {
                        Expression result = Symbolic.multiply(Symbolic_LoopTripCount, (Expression) cls);
                        Symbolic_count = Symbolic.add(Symbolic_count, result);
                    }

                    else if (!LHSExpr.toString().contains(LoopIdx.toString()) && RHSExprWithLoopID == null)
                        Symbolic_count = Symbolic.add(Symbolic_count, (Expression) Identity);

                    else {

                        Symbolic_count = Symbolic.add(Symbolic_count, Symbolic_LoopTripCount);

                    }

                    Symbolic_RefGRoupCost.add(Symbolic_count);

                }

            }

            if (!hasSymbolicIter) {
                TotalLoopCost = LoopCost(RefGroupCost, LoopNestIterationMap, LoopIdx);

                scores.add(TotalLoopCost);

                LoopCostMap.put(LoopIdx, TotalLoopCost);
            }

            else {

                Symbolic_TotalLoopCost = SymbolicLoopCost(Symbolic_RefGRoupCost, LoopNestIterationMap, LoopIdx);

                Symbolic_scores.add(Symbolic_TotalLoopCost);
                Symbolic_LoopCostMap.put(LoopIdx, Symbolic_TotalLoopCost);

            }

        }

        if (!hasSymbolicIter) {
            loopCosts = LoopCostMap;
        } else {
            loopCosts = Symbolic_LoopCostMap;
        }

        if (!LoopCostMap.isEmpty()) {

            Collections.sort(scores, Collections.reverseOrder());

            for (Expression key : LoopCostMap.keySet()) {

                long Cost = LoopCostMap.get(key);

                /*
                 * Order of loops in the loop nest according to Cost analysis
                 * Loop accessing the least number of cache lines is innermost while
                 * the one accessing the most number of cache lines is outermost.
                 * 
                 */

                LoopNestOrder.set(scores.indexOf(Cost), key);

            }

        }

        else {

            // For Symbolic Loop Costs
            int counter = 0;

            List<Expression> DominantTerms = new ArrayList<>();

            Expression variable = null;

            List<Expression> ListOfExpressionVariables = new ArrayList<>();

            for (k = 0; k < Symbolic_scores.size(); k++) {

                Expression se1 = (Expression) Symbolic_scores.get(k);

                if (Symbolic.getVariables(se1).size() == 0) {
                    continue;
                }

                variable = Symbolic.getVariables(se1).get(0);

                ListOfExpressionVariables.add(variable);

                List<Expression> Terms = Symbolic.getTerms(se1);

                List<Integer> VariableCount = new ArrayList<>();

                for (i = 0; i < Terms.size(); i++) {

                    Expression childterm = Terms.get(i);

                    counter = 0;

                    if (childterm.getChildren().contains(variable)) {

                        List<Traversable> entries = childterm.getChildren();

                        for (j = 0; j < entries.size(); j++) {

                            if (entries.get(j).equals(variable))
                                counter++;

                        }

                        VariableCount.add(counter);
                    }

                    else
                        VariableCount.add(counter);

                }

                int MaxCount = Collections.max(VariableCount);

                int indx = VariableCount.indexOf(MaxCount);

                DominantTerms.add(Terms.get(indx));

            }

            List<Expression> Coeffs = new ArrayList<>();

            for (i = 0; i < DominantTerms.size(); i++) {

                Expression term = DominantTerms.get(i);

                Identifier VarID = (Identifier) ListOfExpressionVariables.get(i);

                Coeffs.add(Symbolic.getCoefficient(term, VarID));

            }

            Collections.sort(Coeffs, Collections.reverseOrder());

            List<Expression> Sortedscores = new ArrayList<>();

            for (i = 0; i < Coeffs.size(); i++) {

                Expression coefficient = (Expression) Coeffs.get(i);

                for (j = 0; j < DominantTerms.size(); j++) {

                    if (DominantTerms.get(j).getChildren().contains(coefficient)) {

                        Sortedscores.add(Symbolic_scores.get(j));

                    }

                }

            }

            // System.out.println( "LNO: " + Symbolic_LoopCostMap + " \nScores sorted: " +
            // Sortedscores +"\n");

            for (Expression key : Symbolic_LoopCostMap.keySet()) {

                Expression Symbolic_cost = Symbolic_LoopCostMap.get(key);

                if (Sortedscores.indexOf(Symbolic_cost) != -1)
                    LoopNestOrder.set(Sortedscores.indexOf(Symbolic_cost), key);

            }

        }

        return LoopNestOrder;

    }

    private boolean HasSymbolicBounds(HashMap<Expression, ?> LoopIterMap) {

        Set<Expression> LoopIndices = LoopIterMap.keySet();

        for (Expression e : LoopIndices) {

            Object o = LoopIterMap.get(e);

            if (o instanceof IntegerLiteral ||
                    o instanceof Long || o instanceof ArrayAccess) {

                return false;
            }

            if (o instanceof Expression) {
                Expression ubexp = (Expression) o;

                if (Symbolic.getVariables(ubexp) == null)
                    return false;

            }
        }

        return true;

    }

    private boolean HasNonSymbolicBounds(HashMap<Expression, ?> LoopIterMap) {

        Set<Expression> LoopIndices = LoopIterMap.keySet();

        for (Expression e : LoopIndices) {

            if (!(LoopIterMap.get(e) instanceof IntegerLiteral ||
                    LoopIterMap.get(e) instanceof Long)) {

                return false;
            }
        }

        return true;

    }

    private HashMap<Expression, ?> LoopIterationMap(Loop LoopNest)

    {

        HashMap<Expression, Long> LoopIterationCount = new HashMap<Expression, Long>();
        HashMap<Expression, Expression> SymbolicIterationCount = new HashMap<Expression, Expression>();

        DFIterator<ForLoop> forloopiter = new DFIterator<>(LoopNest, ForLoop.class);

        long LoopUpperBound = 0;
        long LoopLowerBound = 0;
        long Loopstride = 0;

        Boolean UpperboundisSymbolic = false;
        Boolean LowerboundisSymbolic = false;
        Boolean StrideboundisSymbolic = false;

        Expression Symbolic_Iter = null;

        long numLoopiter = 0;

        while (forloopiter.hasNext()) {

            ForLoop loop = forloopiter.next();

            // Using custom method to find loop upper bound as Range analysis
            // gives probles when substituting the value range of a symbolic
            // loop upperbound.

            Expression upperbound = LoopTools.getUpperBoundExpression(loop, false);

            Expression lowerbound = LoopTools.getLowerBoundExpression(loop);

            Expression incExpr = LoopTools.getIncrementExpression(loop);

            if (upperbound instanceof IntegerLiteral)
                LoopUpperBound = ((IntegerLiteral) upperbound).getValue();
            else
                UpperboundisSymbolic = true;

            if (lowerbound instanceof IntegerLiteral)
                LoopLowerBound = ((IntegerLiteral) lowerbound).getValue();
            else
                LowerboundisSymbolic = true;

            if (incExpr instanceof IntegerLiteral)
                Loopstride = ((IntegerLiteral) incExpr).getValue();
            else
                StrideboundisSymbolic = true;

            if (!UpperboundisSymbolic && !LowerboundisSymbolic && !StrideboundisSymbolic) {
                numLoopiter = ((LoopUpperBound - LoopLowerBound + Loopstride) / Loopstride);

                LoopIterationCount.put(LoopTools.getIndexVariable(loop), numLoopiter);
            }

            else {

                Expression Bound_Difference = Symbolic.subtract(upperbound, lowerbound);

                Expression Numerator = Symbolic.add(Bound_Difference, incExpr);

                Symbolic_Iter = Symbolic.divide(Numerator, incExpr);

                SymbolicIterationCount.put(LoopTools.getIndexVariable(loop), Symbolic_Iter);

            }

        }

        if (!LoopIterationCount.isEmpty())
            return LoopIterationCount;

        else
            return SymbolicIterationCount;

    }

    /**
     * Determines the cost in terms of cache lines for the loop.
     * Add the costs of all the reference groups and multiply with the iteration
     * count
     * of the loops other than the candidate innermost loop.
     * 
     * @param referenceCosts    - Cost of Ref groups
     * @param loopNestIterCount - Number of iterations of each loop in the nest
     * @param currentLoop       - Loop index of current loop
     * @return - Loop cost in terms of cache lines accessed
     */

    protected long LoopCost(ArrayList<Long> referenceCosts, HashMap<Expression, ?> loopNestIterCount,
            Expression currentLoop) {

        long RestOfLoops_Iterations = 1, SumOfRefCosts = 0;

        int i;

        for (Object key : loopNestIterCount.keySet()) {

            if (!currentLoop.equals(key))
                RestOfLoops_Iterations *= (long) loopNestIterCount.get(key);

        }

        for (i = 0; i < referenceCosts.size(); i++) {

            SumOfRefCosts += (long) referenceCosts.get(i);

        }

        return (SumOfRefCosts * RestOfLoops_Iterations);

    }

    /**
     * Symbolic loop cost. Same as the routine to calculate loop cost with
     * long loop bounds.
     */

    protected Expression SymbolicLoopCost(ArrayList<Expression> ReferenceCosts,
            HashMap<Expression, ?> LoopNestIterCount, Expression CurrentLoop) {

        int i;
        IntegerLiteral InitialVal = new IntegerLiteral(1);
        IntegerLiteral IntialValForSum = new IntegerLiteral(0);
        Expression Symbolic_RestOfLoopIterations = (Expression) InitialVal;
        Expression Symbolic_SumOfRefCosts = (Expression) IntialValForSum;

        for (Object key : LoopNestIterCount.keySet()) {

            if (!CurrentLoop.equals(key))
                Symbolic_RestOfLoopIterations = Symbolic.multiply(Symbolic_RestOfLoopIterations,
                        (Expression) LoopNestIterCount.get(key));

        }

        for (i = 0; i < ReferenceCosts.size(); i++) {

            Symbolic_SumOfRefCosts = Symbolic.add(Symbolic_SumOfRefCosts, (Expression) ReferenceCosts.get(i));

        }

        List<Expression> Loop_ref_cost = new ArrayList<Expression>();
        Loop_ref_cost.add(Symbolic_SumOfRefCosts);

        Expression result = Symbolic_SumOfRefCosts;

        return result;

    }

}
