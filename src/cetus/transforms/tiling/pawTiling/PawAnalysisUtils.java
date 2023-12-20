package cetus.transforms.tiling.pawTiling;

import java.util.ArrayList;
import java.util.List;

import cetus.analysis.LoopTools;
import cetus.hir.Loop;

/**
 * PawAnalysisData
 */
public class PawAnalysisUtils {

    protected boolean verbosity = false;

    protected List<Loop> nonCanonicalLoops = new ArrayList<>();
    protected List<Loop> nonPerfectNestLoops = new ArrayList<>();
    protected List<Loop> withFunctionCallLoops = new ArrayList<>();
    protected List<Loop> nonIncreasingOrderLoops = new ArrayList<>();

    public List<Loop> filterValidLoops(List<Loop> loops) {
        return filterValidLoops(loops, false);
    }

    public List<Loop> filterValidLoops(List<Loop> loops, boolean shouldCheckPerfectNests) {
        List<Loop> validLoops = new ArrayList<>();
        for (Loop loop : loops) {
            if (isCanonical(loop)
                    && (isPerfectNest(loop) || !shouldCheckPerfectNests)
                    && !containsFunctionCalls(loop)
                    && isIncreasingOrder(loop)) {
                validLoops.add(loop);
            }
        }

        return validLoops;
    }

    public boolean isIncreasingOrder(Loop loop) {

        if (LoopTools.getIncrementExpression(loop).toString().equals("-1")) {
            nonIncreasingOrderLoops.add(loop);
            return false;
        }

        return true;
    }

    public boolean isCanonical(Loop loop) {
        if (!LoopTools.isCanonical(loop)) {
            nonCanonicalLoops.add(loop);
            return false;
        }
        return true;
    }

    public boolean isPerfectNest(Loop loop) {
        if (!LoopTools.isPerfectNest(loop)) {
            nonPerfectNestLoops.add(loop);
            return false;
        }
        return true;
    }

    public boolean containsFunctionCalls(Loop loop) {
        if (LoopTools.containsFunctionCall(loop)) {
            withFunctionCallLoops.add(loop);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String format = "Non canonical loops: %s\nNon perfect nest loops: %s\nWith function calls: %s\n";
        if (verbosity) {
            format += "#### Non canonical loops ####\n%s\n#### Non perfect nest loops ####\n%s\n#### With function calls ####\n%s\n#### Non increasing order loops ####\n%s\n";
        }
        return String.format(format, nonCanonicalLoops.size(), nonPerfectNestLoops.size(),
                withFunctionCallLoops.size(), getFormattedLoops(nonCanonicalLoops),
                getFormattedLoops(nonPerfectNestLoops), getFormattedLoops(withFunctionCallLoops),
                getFormattedLoops(nonIncreasingOrderLoops));
    }

    private String getFormattedLoops(List<Loop> loops) {
        StringBuilder textBuilder = new StringBuilder();
        for (int i = 0; i < loops.size(); i++) {
            Loop loop = loops.get(i);
            textBuilder.append("#### LOOP " + i + " ####\n\n");
            textBuilder.append(loop.toString() + "\n\n");
            textBuilder.append("#### END LOOP " + i + " ####\n\n");
        }

        return textBuilder.toString();
    }

}