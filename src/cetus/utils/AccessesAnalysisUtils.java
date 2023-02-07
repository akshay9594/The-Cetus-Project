package cetus.utils;

import cetus.hir.BinaryExpression;
import cetus.hir.Expression;

public final class AccessesAnalysisUtils {
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
}
