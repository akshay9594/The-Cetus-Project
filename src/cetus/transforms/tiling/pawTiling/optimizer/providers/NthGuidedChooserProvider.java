package cetus.transforms.tiling.pawTiling.optimizer.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.analysis.exceptions.IllegalDependenceVector;
import cetus.exec.CommandLineOptionSet;
import cetus.hir.DFIterator;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Loop;
import cetus.hir.Symbol;
import cetus.hir.SymbolTable;
import cetus.transforms.tiling.TiledLoop;
import cetus.transforms.tiling.TilingUtils;
import cetus.transforms.tiling.pawTiling.ParallelAwareTilingPass;
import cetus.transforms.tiling.pawTiling.optimizer.NthVersionChooser;
import cetus.transforms.tiling.pawTiling.optimizer.VersionChooser;
import cetus.utils.reuseAnalysis.DataReuseAnalysis;

public class NthGuidedChooserProvider implements VersionChooserProvider {

    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    private int nthOrder;

    public NthGuidedChooserProvider() {
        this(ParallelAwareTilingPass.DEFAULT_NTH_ORDER);
    }

    public NthGuidedChooserProvider(CommandLineOptionSet commandLineOptions) {

        this(commandLineOptions.getValue(ParallelAwareTilingPass.NTH_ORDER_PARAM) == null
                || commandLineOptions.getValue(ParallelAwareTilingPass.NTH_ORDER_PARAM).equals("")
                        ? ParallelAwareTilingPass.DEFAULT_NTH_ORDER
                        : Integer.parseInt(commandLineOptions.getValue(ParallelAwareTilingPass.NTH_ORDER_PARAM)));

    }

    public NthGuidedChooserProvider(int nthOrder) {
        if (nthOrder <= 0) {
            this.nthOrder = ParallelAwareTilingPass.DEFAULT_NTH_ORDER;
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

        List<Loop> loops = new ArrayList<>();
        new DFIterator<Loop>(loopNest, Loop.class).forEachRemaining(loop -> loops.add(loop));

        int nthLevel = nthOrder;
        if (nthLevel == ParallelAwareTilingPass.DEFAULT_NTH_ORDER) {
            int targetLevel = loops.size() - 1;
            nthLevel = targetLevel > 0 ? 1 : targetLevel;
        }

        int maxTilingLvl = nthLevel < reusableLoops ? nthLevel : reusableLoops;

        TiledLoop optimalTileVersion = null;

        TiledLoop curLoop = new TiledLoop(((ForLoop) loopNest), dvs);
        List<DependenceVector> curDvs = dvs;
        for (int i = 0; i < loops.size() && maxTilingLvl > 0; i++) {
            int targetLoopPos = getLoopPosByIndex(curLoop, reusableOrder.get(i));

            try {
                curLoop = TilingUtils.tiling(symbolTable, ((ForLoop) curLoop).clone(false), DEFAULT_STRIP,
                        targetLoopPos,
                        curDvs);
                maxTilingLvl--;
            } catch (IllegalDependenceVector exception) {
                logger.info("Illegal DV on tiling curLoop:");
                logger.info("" + exception.loopNest);
                logger.info("" + exception.dependenceVector);

            }
            curDvs = curLoop.getDependenceVectors();
        }

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
