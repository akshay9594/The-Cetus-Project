package cetus.transforms.tiling.pawTiling.optimizer;

import java.util.List;

import cetus.analysis.DependenceVector;
import cetus.hir.ForLoop;
import cetus.hir.Loop;
import cetus.transforms.tiling.TiledLoop;

public class ComplexVersionChooser implements VersionChooser {

    private TiledLoop originalLoopNest;
    private List<TiledLoop> tiledVersions;
    private TiledLoop choosenVersion;

    public ComplexVersionChooser(Loop loopNest, List<DependenceVector> dvs, List<TiledLoop> tiledVersions,
            TiledLoop choosenVersion) throws Exception {
        this.originalLoopNest = new TiledLoop((ForLoop) loopNest, dvs);
        this.tiledVersions = tiledVersions;
        this.choosenVersion = choosenVersion;
    }

    public List<TiledLoop> getTiledVersions() {
        return tiledVersions;
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