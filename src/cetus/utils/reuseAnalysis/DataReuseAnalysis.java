package cetus.utils.reuseAnalysis;

import java.util.HashMap;
import java.util.List;

import cetus.hir.Expression;
import cetus.hir.Loop;

public interface DataReuseAnalysis {

    public Loop getLoopNest();
    public HashMap<Expression, ?> getLoopCosts();
    public List<Expression> getLoopNestMemoryOrder();
    public boolean hasReuse();
}
