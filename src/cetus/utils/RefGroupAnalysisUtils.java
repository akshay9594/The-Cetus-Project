package cetus.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cetus.analysis.LoopTools;
import cetus.hir.ArrayAccess;
import cetus.hir.DFIterator;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.IntegerLiteral;
import cetus.hir.Loop;
import cetus.hir.Symbolic;

public final class RefGroupAnalysisUtils {

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

    public static List<List<ArrayAccess>> refGroup(Loop candidateLoop, List<ArrayAccess> loopBodyArrays,
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

    public static boolean HasSymbolicBounds(Map<Expression, ?> LoopIterMap) {

        Set<Expression> LoopIndices = LoopIterMap.keySet();

        for (Expression e : LoopIndices) {

            Object o = LoopIterMap.get(e);

            //TODO: Need to be analyzed through examples
            if(o instanceof ArrayAccess || o instanceof Expression) {
                return true;
            }
            if (o instanceof IntegerLiteral ||
                    o instanceof Long) {

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

    public static boolean HasNonSymbolicBounds(Map<Expression, ?> LoopIterMap) {

        Set<Expression> LoopIndices = LoopIterMap.keySet();

        for (Expression e : LoopIndices) {

            if (!(LoopIterMap.get(e) instanceof IntegerLiteral ||
                    LoopIterMap.get(e) instanceof Long)) {

                return false;
            }
        }

        return true;

    }

    public static Map<Expression, ?> LoopIterationMap(Loop LoopNest) {
        Map<Expression, Long> LoopIterationCount = new HashMap<Expression, Long>();
        Map<Expression, Expression> SymbolicIterationCount = new HashMap<Expression, Expression>();

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

}
