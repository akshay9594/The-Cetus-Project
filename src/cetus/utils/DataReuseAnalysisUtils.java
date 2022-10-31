package cetus.utils;

import java.util.HashMap;
import java.util.Iterator;

import cetus.hir.Expression;

public class DataReuseAnalysisUtils {
    
    public static final String printLoopCosts(HashMap<Expression, ?> loopCostMap) {

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
}
