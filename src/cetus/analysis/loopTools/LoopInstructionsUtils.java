package cetus.analysis.loopTools;

import java.util.ArrayList;
import java.util.List;

import cetus.hir.BinaryExpression;
import cetus.hir.DFIterator;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Symbolic;

public final class LoopInstructionsUtils {

    public static Expression getTotalOfInstructions(ForLoop loopNest) throws Exception {
        List<ForLoop> loops = new ArrayList<>();
        new DFIterator<ForLoop>(loopNest, ForLoop.class).forEachRemaining(loops::add);

        Expression loopNestCond = loopNest.getCondition();
        if (!(loopNestCond instanceof BinaryExpression)) {
            throw new Exception("Condition should be a binary expression");
        }

        BinaryExpression cond = (BinaryExpression) loopNestCond;
        Expression totalOfInstructions = cond.getRHS();
        for (int i = 1; i < loops.size(); i++) {
            Expression loopCond = loops.get(i).getCondition();
            if ((loopCond instanceof BinaryExpression)) {
                totalOfInstructions = Symbolic.multiply(totalOfInstructions, ((BinaryExpression) loopCond).getRHS());
            }
        }

        return totalOfInstructions;
    }
}
