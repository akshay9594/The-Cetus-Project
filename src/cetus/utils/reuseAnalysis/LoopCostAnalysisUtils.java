package cetus.utils.reuseAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cetus.hir.Expression;
import cetus.hir.IntegerLiteral;
import cetus.hir.Symbolic;

public final class LoopCostAnalysisUtils {

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

    public static long LoopCost(List<Long> referenceCosts, Map<Expression, ?> loopNestIterCount,
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

    public static Expression SymbolicLoopCost(List<Expression> ReferenceCosts,
            Map<Expression, ?> LoopNestIterCount, Expression CurrentLoop) {

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
