package cetus.utils.reuseAnalysis.algorithm;

import cetus.hir.Loop;
import cetus.utils.reuseAnalysis.data.DataReuseAnalysis;

public interface ReuseAnalyzer {
    public DataReuseAnalysis getReuseAnalysis(Loop loopNest);
    public DataReuseAnalysis getReuseAnalysis(Loop loopNest, int cacheSize);
}
