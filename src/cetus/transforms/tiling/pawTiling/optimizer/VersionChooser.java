package cetus.transforms.tiling.pawTiling.optimizer;

import cetus.transforms.tiling.TiledLoop;

public interface VersionChooser {

    public final static int MAX_NEST_LEVEL = 20;

    public TiledLoop getOriginalLoopNest();

    public TiledLoop getChoosenVersion();
}
