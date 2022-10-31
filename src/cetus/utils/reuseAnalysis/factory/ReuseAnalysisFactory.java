package cetus.utils.reuseAnalysis.factory;

import cetus.hir.Loop;
import cetus.utils.reuseAnalysis.DataReuseAnalysis;

public interface ReuseAnalysisFactory {
    public DataReuseAnalysis getReuseAnalysis(Loop loopNest);
    public DataReuseAnalysis getReuseAnalysis(Loop loopNest, int cacheSize);
}
