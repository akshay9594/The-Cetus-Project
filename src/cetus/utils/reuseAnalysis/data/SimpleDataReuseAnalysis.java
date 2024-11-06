package cetus.utils.reuseAnalysis.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cetus.hir.Expression;
import cetus.hir.Loop;

public class SimpleDataReuseAnalysis implements DataReuseAnalysis {

    private Loop loopNest;
    private Map<Expression, ?> loopCosts = new HashMap<>();
    private List<Expression> loopNestMemoryOrder = new ArrayList<>();

    public SimpleDataReuseAnalysis(Loop loopNest, Map<Expression, ?> loopCosts,
            List<Expression> loopNestMemoryOrder) {
        this.loopNest = loopNest;
        this.loopCosts = loopCosts;
        this.loopNestMemoryOrder = loopNestMemoryOrder;
    }

    @Override
    public Loop getLoopNest() {
        return loopNest;
    }

    @Override
    public Map<Expression, ?> getLoopCosts() {
        return loopCosts;
    }

    @Override
    public List<Expression> getLoopNestMemoryOrder() {
        return loopNestMemoryOrder;
    }

    public boolean hasReuse() {

        boolean isReusable = true;

        isReusable &= loopCosts.size() > 0
                && loopNestMemoryOrder.size() > 0;

        return isReusable;
    }
}