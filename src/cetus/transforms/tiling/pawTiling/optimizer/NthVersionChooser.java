package cetus.transforms.tiling.pawTiling.optimizer;

import java.util.List;

import cetus.analysis.DependenceVector;
import cetus.hir.ForLoop;
import cetus.hir.Loop;
import cetus.transforms.tiling.TiledLoop;

public class NthVersionChooser implements VersionChooser {

    private TiledLoop originalLoopNest;
    private TiledLoop choosenVersion;

    public NthVersionChooser(Loop loopNest, List<DependenceVector> dvs,
            TiledLoop choosenVersion) throws Exception {
        this.originalLoopNest = new TiledLoop((ForLoop) loopNest, dvs);
        this.choosenVersion = choosenVersion;
    }

    @Override
    public TiledLoop getOriginalLoopNest() {
        return originalLoopNest;
    }

    @Override
    public TiledLoop getChoosenVersion() {
        return choosenVersion;
    }

}