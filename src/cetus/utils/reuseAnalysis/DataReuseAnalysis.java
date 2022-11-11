package cetus.utils.reuseAnalysis;

import java.util.HashMap;
import java.util.List;

import cetus.hir.Expression;
import cetus.hir.Loop;

public interface DataReuseAnalysis {

    public Loop getLoopNest();

    public HashMap<Expression, ?> getLoopCosts();

    /**
     * Returns the indexes of each loop in the loop nest, starting from the less
     * reusable to the most reusable loop. i.e., for a given loop nest:
     * 
     * <pre>{@code 
     *  for(int i=0;i<n;i++) //most reusable
     *      for(int j=0;j<n;j++) //less reusable
     *          for(int k=0;k<n;k++) 
     * }</pre>
     * 
     * it will return the following list: {@code [j, k, i]} because by being i the
     * most reusable loop in must be in the innermost position
     * 
     * @return list with sorted index expressions,
     */
    public List<Expression> getLoopNestMemoryOrder();

    public boolean hasReuse();
}
