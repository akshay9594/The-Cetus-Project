package cetus.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import cetus.analysis.LoopTools;
import cetus.hir.ArrayAccess;
import cetus.hir.BinaryExpression;
import cetus.hir.Expression;
import cetus.hir.Loop;

public class DataReuseAnalysis {

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

    public static List<List<ArrayAccess>> RefGroup(Loop candidateLoop, List<ArrayAccess> loopBodyArrays,
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

    public static void printRefGroup(List<List<ArrayAccess>> refGroup) {
        System.out.println("#### Ref group ####\n");

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

            System.out.println("{ " + referencesStr + " }");

        }

        System.out.println("\n#### END RefGroup ####\n");
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

    public static void printLoopCosts(HashMap<Expression, ?> loopCostMap) {

        System.out.println("#### Loop costs ####");

        Iterator<Expression> loopCostIter = loopCostMap.keySet().iterator();
        while (loopCostIter.hasNext()) {
            Expression keyExpr = loopCostIter.next();
            Object loopCost = loopCostMap.get(keyExpr);
            System.out.println("#### Loop:");
            System.out.println(keyExpr);
            System.out.println("#### Loop cost: " + loopCost);
        }

        System.out.println("#### end loop costs ####");

    }

}
