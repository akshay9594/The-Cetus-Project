package cetus.utils.reuseAnalysis.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cetus.analysis.LoopTools;
import cetus.hir.ArrayAccess;
import cetus.hir.DFIterator;
import cetus.hir.Expression;
import cetus.hir.FloatLiteral;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Loop;
import cetus.utils.VariableDeclarationUtils;
import cetus.utils.reuseAnalysis.data.DataReuseAnalysis;
import cetus.utils.reuseAnalysis.data.SimpleDataReuseAnalysis;

public class TemporalReuseAnalyzer implements ReuseAnalyzer {

    private static final int DEFAULT_CACHE_SIZE = 64;

    /**
     * It will use the
     * {@link TemporalReuseAnalyzer#getReuseAnalysis(Loop, int)} with the
     * default cache size value: {@value #DEFAULT_CACHE_SIZE}
     * 
     * @see SimpleReuseAnalysis#runReuseAnalysis()
     * 
     * @return - Order of the loops in the nest for max reusability. From the less
     *         reuse to the max.
     * 
     */

    @Override
    public DataReuseAnalysis getReuseAnalysis(Loop loopNest) {
        return getReuseAnalysis(loopNest, DEFAULT_CACHE_SIZE);
    }

    // Main function to sort array accesses by temporal reuse
    public Map<Expression, Integer> sortByReuse(ForLoop loopNest) {
        // Map to hold array accesses and their reuse scores
        Map<Expression, Integer> reuseScores = new HashMap<>();

        List<ArrayAccess> arrayAccesses = new DFIterator<ArrayAccess>(loopNest, ArrayAccess.class).getList();
        List<ForLoop> loopNestList = new DFIterator<ForLoop>(loopNest, ForLoop.class).getList();

        for (ArrayAccess access : arrayAccesses) {
            List<Expression> accessOrder = access.getIndices();
            for (Expression index : accessOrder) {
                int score = calculateReuseScoreForIndex(loopNestList, access, index);
                reuseScores.put(index, score);
            }
        }

        return reuseScores;
    }

    // Calculate reuse score for a single index based on its position and loop nest
    // structure
    private int calculateReuseScoreForIndex(List<ForLoop> loopNest, ArrayAccess access, Expression index) {
        int score = 0;
        int numLoops = loopNest.size();

        int numDimensions = access.getNumIndices();

        // Calculate index position in array access (left-most = outermost dimension)
        int indexPosition = access.getIndices().indexOf(index);

        // Add a base score based on index position (outer dimensions have more reuse)
        score += getPositionBasedScore(indexPosition, numDimensions);

        // Traverse loop nest in reverse (innermost loop first)
        for (int i = 0; i < numLoops; i++) {
            ForLoop currentLoop = loopNest.get(numLoops - 1 - i); // Innermost loop first
            boolean dependsOnIndex = dependsOnIndex(currentLoop, index);
            if (dependsOnIndex) {
                // Alignment with innermost dimension for high reuse
                score += 30;
            } else if (!dependsOnIndex) {
                // Partial invariance for moderate reuse
                score += 20;
            } else {
                // Lower score for partial dependency
                score += 5;
            }
        }

        // Extra boost for invariance in outermost loop (further iterations re-use)
        if (!dependsOnAccess(loopNest.get(0), access)) {
            score += 50;
        }

        return score;
    }

    // Determines the reuse score based on the index's position in the array access
    // (outermost = 0)
    private int getPositionBasedScore(int indexPosition, int numDimensions) {
        // Outermost dimensions (first indices in access) tend to have more reuse
        // opportunities
        if (indexPosition == 0) {
            return 40; // Outer-most index (more reuse across outer iterations)
        } else if (indexPosition == numDimensions - 1) {
            return -10; // Innermost index (less reuse)
        } else {
            return 20; // Middle index (moderate reuse)
        }
    }

    // Checks if the current loop index depends on the array index
    private boolean dependsOnIndex(ForLoop outerLoop, Expression index) {
        return LoopTools.getIndexVariable(outerLoop).toString().equals(index.toString());
    }

    private boolean dependsOnAccess(ForLoop outerLoop, ArrayAccess access) {
        for (Expression index : access.getIndices()) {
            if (dependsOnIndex(outerLoop, index)) {
                return true;
            }
        }
        return false;
    }

    public static Integer getArrayAccessSize(ArrayAccess arrayAccess) {
        int size = 1;
        for (Expression index : arrayAccess.getIndices()) {
            if (index instanceof IntegerLiteral) {
                size *= ((IntegerLiteral) index).getValue();
            } else if (index instanceof FloatLiteral) {
                size *= (int) ((FloatLiteral) index).getValue();
            } else if (index instanceof Identifier) {
                Expression declaredValue = VariableDeclarationUtils.getVariableDeclaredValue(
                        VariableDeclarationUtils.getVariableDeclarationSpace(index), (Identifier) index);
                if (declaredValue instanceof IntegerLiteral) {
                    size *= ((IntegerLiteral) declaredValue).getValue();
                } else if (declaredValue instanceof FloatLiteral) {
                    size *= (int) ((FloatLiteral) declaredValue).getValue();
                } else {
                    size *= 1;

                }
            } else if (index instanceof Expression) {
                size *= 1;
            } else {
                throw new RuntimeException("Unknown index type: " + index.getClass());
            }
        }
        return size;
    }

    private static List<ArrayAccess> getRelatedArrayAccesses(Loop loop) {
        List<ArrayAccess> arrayAccesses = new ArrayList<>();
        DFIterator<ArrayAccess> arrayAccessIterator = new DFIterator<ArrayAccess>(loop, ArrayAccess.class);
        while (arrayAccessIterator.hasNext()) {
            ArrayAccess arrayAccess = arrayAccessIterator.next();
            for (Expression index : arrayAccess.getIndices()) {
                Expression loopIndex = LoopTools.getIndexVariable(loop);
                boolean isRelated = index.equals(loopIndex);
                if (isRelated) {
                    arrayAccesses.add(arrayAccess);

                }
            }
        }
        return arrayAccesses;
    }

    @Override
    public DataReuseAnalysis getReuseAnalysis(Loop loopNest, int cacheSize) {
        DataReuseAnalysis reuseAnalysis = null;
        if (loopNest instanceof ForLoop) {
            Map<Expression, Integer> reuseScores = sortByReuse((ForLoop) loopNest);
            List<Expression> sortedIndices = new ArrayList<>(reuseScores.keySet());

            //Sort in ascending order
            sortedIndices.sort((a, b) -> reuseScores.get(b) - reuseScores.get(a));

            reuseAnalysis = new SimpleDataReuseAnalysis(loopNest, reuseScores, sortedIndices);
            return reuseAnalysis;
        }
        return null;
    }

    public DataReuseAnalysis getReuseAnalysisBackup(Loop loopNest, int cacheSize) {

        // for each loop identify array access using loop index variable
        Map<Expression, Integer> loopCosts = new HashMap<>();
        DFIterator<Loop> loopIterator = new DFIterator<Loop>(loopNest, Loop.class);
        while (loopIterator.hasNext()) {
            Loop loop = loopIterator.next();
            List<ArrayAccess> arrayAccesses = getRelatedArrayAccesses(loop);
            Integer loopSize = 1;
            for (ArrayAccess arrayAccess : arrayAccesses) {
                Integer arrayAccessSize = getArrayAccessSize(arrayAccess);
                loopSize += arrayAccessSize;
            }
            loopCosts.put(LoopTools.getIndexVariable(loop), loopSize);
        }

        // order loop cost
        List<Map.Entry<Expression, Integer>> loopCostsList = new ArrayList<>(loopCosts.entrySet());
        Collections.sort(loopCostsList, new Comparator<Map.Entry<Expression, Integer>>() {
            @Override
            public int compare(Map.Entry<Expression, Integer> o1, Map.Entry<Expression, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        // SimpleDataReuseAnalysis reuseAnalysis = new SimpleDataReuseAnalysis(loopNest,
        // loopCosts, LoopNestOrder);

        // return reuseAnalysis;

        return null;

    }

    public String calculateDataLoad(Loop outermostLoop, Expression index) {
        // calculate the data load per iteration of the outermostloop given the index
        int dataLoad = 0;
        int loopDepth = 0;
        List<ArrayAccess> arrayAccesses = getRelatedArrayAccesses(outermostLoop);
        for (ArrayAccess arrayAccess : arrayAccesses) {
            List<Expression> indices = arrayAccess.getIndices();
            int indexPosition = indices.indexOf(index);
            if (indexPosition != -1) {
                loopDepth = indexPosition + 1;
                int rows = 1;
                int cols = 1;
                for (int i = 0; i <= indexPosition; i++) {
                    rows *= getArrayAccessSize(arrayAccess);
                }
                for (int i = indexPosition + 1; i < indices.size(); i++) {
                    cols *= getArrayAccessSize(arrayAccess);
                }
                dataLoad += rows + cols;
            }
        }
        return loopDepth + "deep-" + index.toString() + "rows+" + dataLoad + "cols";
    }

}
