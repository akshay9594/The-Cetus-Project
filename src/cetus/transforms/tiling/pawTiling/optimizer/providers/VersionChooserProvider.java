package cetus.transforms.tiling.pawTiling.optimizer.providers;

import java.util.List;

import cetus.analysis.DependenceVector;
import cetus.hir.Loop;
import cetus.hir.SymbolTable;
import cetus.transforms.tiling.pawTiling.optimizer.VersionChooser;
import cetus.utils.reuseAnalysis.DataReuseAnalysis;

public interface VersionChooserProvider {

    public static final int DEFAULT_STRIP = 20;

    public VersionChooser chooseOptimalVersion(SymbolTable symbolTable, Loop loopNest, List<DependenceVector> dvs,
            DataReuseAnalysis reuseAnalysis) throws Exception;
}
