package cetus.transforms.tiling.pawTiling.optimizer.providers;

import java.util.Collections;
import java.util.List;

import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.hir.DFIterator;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Loop;
import cetus.hir.Symbol;
import cetus.hir.SymbolTable;
import cetus.transforms.tiling.TiledLoop;
import cetus.transforms.tiling.TilingUtils;
import cetus.transforms.tiling.pawTiling.optimizer.NthVersionChooser;
import cetus.transforms.tiling.pawTiling.optimizer.VersionChooser;
import cetus.utils.reuseAnalysis.DataReuseAnalysis;

public class NthGuidedChooserProvider implements VersionChooserProvider {

    public static final int DEFAULT_NTH_ORDER = 1;

    private int nthOrder;

    public NthGuidedChooserProvider() {
        this(DEFAULT_NTH_ORDER);
    }

    public NthGuidedChooserProvider(int nthOrder) {
        if (nthOrder <= 0) {
            this.nthOrder = DEFAULT_NTH_ORDER;
        } else {
            this.nthOrder = nthOrder;
        }
    }

    @Override
    public VersionChooser chooseOptimalVersion(SymbolTable symbolTable, Loop loopNest, List<DependenceVector> dvs,
            DataReuseAnalysis reuseAnalysis) throws Exception {

        List<Expression> reusableOrder = reuseAnalysis.getLoopNestMemoryOrder();
        Collections.reverse(reusableOrder);

        int reusableLoops = reusableOrder.size();
        int maxTilingLvl = nthOrder < reusableLoops ? nthOrder : reusableLoops;

        TiledLoop optimalTileVersion = null;

        TiledLoop curLoop = new TiledLoop(((ForLoop) loopNest), dvs);
        List<DependenceVector> curDvs = dvs;
        for (int i = 0; i < maxTilingLvl; i++) {
            int targetLoopPos = getLoopPosByIndex(loopNest, reusableOrder.get(i));
            curLoop = TilingUtils.tiling(symbolTable, ((ForLoop) curLoop).clone(false), DEFAULT_STRIP, targetLoopPos,
                    curDvs);
            curDvs = curLoop.getDependenceVectors();
        }

        // TODO: Review line 57 - 93 of ComplexChooserProvider. I need the step here for
        // setting the outermost parallelizable loop
        optimalTileVersion = curLoop;

        return new NthVersionChooser(loopNest, dvs, optimalTileVersion);
    }

    private int getLoopPosByIndex(Loop loopNest, Expression index) throws Exception {
        int foundPos = -1;
        DFIterator<Loop> loopIter = new DFIterator<Loop>(loopNest, Loop.class);

        int curPos = 0;
        while (loopIter.hasNext() && foundPos == -1) {
            Loop curLoop = loopIter.next();
            Symbol curSymbol = LoopTools.getLoopIndexSymbol(curLoop);

            if (curSymbol == null || !curSymbol.getSymbolName().equals(index.toString())) {
                curPos++;
                continue;
            }

            foundPos = curPos;
            break;

        }

        if (foundPos == -1) {
            throw new Exception("Index does not exist in the given loop nest");
        }

        return foundPos;
    }

}
