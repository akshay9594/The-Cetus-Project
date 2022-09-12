package cetus.transforms.tiling.pawTiling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.hir.DFIterator;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Loop;
import cetus.hir.SymbolTable;
import cetus.transforms.tiling.TiledLoop;
import cetus.transforms.tiling.TilingUtils;
import cetus.utils.reuseAnalysis.DataReuseAnalysis;

public class Optimizer {

    public final static int MAX_NEST_LEVEL = 20;

    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    private TiledLoop originalLoopNest;
    private List<TiledLoop> tiledVersions;
    private TiledLoop choosenVersion;

    public Optimizer(SymbolTable symbolTable, Loop loopNest, List<DependenceVector> dvs) throws Exception {
        this.originalLoopNest = new TiledLoop((ForLoop) loopNest, dvs);

        this.tiledVersions = createTiledVersions(loopNest, symbolTable, dvs);
    }

    public List<TiledLoop> getTiledVersions() {
        return tiledVersions;
    }

    public TiledLoop chooseOptimalVersion(DataReuseAnalysis reuseAnalysis) {
        this.choosenVersion = chooseOptimalVersion(originalLoopNest, tiledVersions, reuseAnalysis);
        return choosenVersion;
    }

    private List<TiledLoop> createTiledVersions(Loop loopNest, SymbolTable symbolTable,
            List<DependenceVector> dependenceVectors) throws Exception {

        List<ForLoop> nestedLoops = new ArrayList<>();
        new DFIterator<ForLoop>(loopNest, ForLoop.class).forEachRemaining(nestedLoops::add);

        List<TiledLoop> tiledVersions = new ArrayList<>();
        Map<String, Boolean> stripminedBySymbol = new HashMap<>();

        if (nestedLoops.size() < MAX_NEST_LEVEL) {
            createTiledVersions(symbolTable, (ForLoop) loopNest, 20,
                    stripminedBySymbol,
                    tiledVersions, dependenceVectors);

            int originalLoopNestIdx = -1;
            ForLoop originalLoopNestNoAnnotations = ((ForLoop) loopNest).clone(false);
            for (int i = 0; i < tiledVersions.size(); i++) {
                if (tiledVersions.get(i).equals(originalLoopNestNoAnnotations)) {
                    originalLoopNestIdx = i;
                    break;
                }
            }

            if (originalLoopNestIdx != -1) {
                tiledVersions.remove(originalLoopNestIdx);
            }

            logger.info("########## PRINTING TILED VERSIONS\n");

            for (ForLoop tiledVersion : tiledVersions) {
                logger.info("######## TILED VERSION #########\n");
                logger.info(tiledVersion + "\n");
                logger.info("######## END TILED VERSION #########\n");
            }
        }

        return tiledVersions;

    }

    private void createTiledVersions(SymbolTable variableDeclarationSpace, ForLoop loopNest,
            int strip,
            Map<String, Boolean> stripmined, List<TiledLoop> versions, List<DependenceVector> dependenceVectors)
            throws Exception {

        List<ForLoop> nestedLoops = new ArrayList<>();
        new DFIterator<ForLoop>(loopNest, ForLoop.class).forEachRemaining(nestedLoops::add);
        int loopNestSize = nestedLoops.size();

        for (int i = loopNestSize - 1; i >= 0; i--) {

            String loopSymbolName = LoopTools.getLoopIndexSymbol(nestedLoops.get(i)).getSymbolName();
            if (stripmined.containsKey(loopSymbolName)) {
                continue;
            }

            TiledLoop stripminedLoop = (TiledLoop) TilingUtils.tiling(variableDeclarationSpace, loopNest.clone(false),
                    strip, i, stripmined, dependenceVectors);
            versions.add(stripminedLoop);

            HashMap<String, Boolean> newStripmined = new HashMap<>();
            for (String symbol : stripmined.keySet()) {
                newStripmined.put(symbol, true);
            }

            createTiledVersions(variableDeclarationSpace, stripminedLoop.clone(false),
                    strip,
                    newStripmined, versions, stripminedLoop.getDependenceVectors());

        }

    }

    private TiledLoop chooseOptimalVersion(TiledLoop originalLoopNest, List<TiledLoop> tiledVersions,
            DataReuseAnalysis reuseAnalysis) {

        int outerParallelLoopIndex = -1;

        TiledLoop choosenVersion = originalLoopNest;

        if (tiledVersions.size() == 0) {
            return originalLoopNest;
        }

        TiledLoop firstTiledVersion = tiledVersions.get(0);

        List<DependenceVector> firstTiledVersionDVs = calculateDependenceVectors(firstTiledVersion);
        outerParallelLoopIndex = getOuterParallelLoopIdx(firstTiledVersion, firstTiledVersionDVs);

        List<TiledLoop> loopsWithSameParallelization = new ArrayList<>();

        loopsWithSameParallelization.add(firstTiledVersion);

        // decide version based on parallelizability
        for (int i = 1; i < tiledVersions.size(); i++) {
            TiledLoop tiledVersion = tiledVersions.get(i);

            List<DependenceVector> dependenceVectors = calculateDependenceVectors(tiledVersion);
            int auxOuterParallelLoopIdx = getOuterParallelLoopIdx(tiledVersion, dependenceVectors);

            logger.info("### Tiled version: ");
            logger.info(tiledVersion + "\n");
            logger.info("### Outer parallelizable loop position: " + auxOuterParallelLoopIdx);

            if (auxOuterParallelLoopIdx == -1) {
                continue;
            }
            if (auxOuterParallelLoopIdx == outerParallelLoopIndex) {
                loopsWithSameParallelization.add(tiledVersion);
            }

            else if (auxOuterParallelLoopIdx < outerParallelLoopIndex) {
                outerParallelLoopIndex = auxOuterParallelLoopIdx;
                choosenVersion = tiledVersion;
                loopsWithSameParallelization.clear();
                loopsWithSameParallelization.add(choosenVersion);
            }
        }

        // there is no need to analyze the reuse analysis data
        if (loopsWithSameParallelization.size() == 0 || reuseAnalysis == null
                || reuseAnalysis.getLoopNestMemoryOrder().size() == 0) {

            choosenVersion.setOutermostParallelizableLoop(outerParallelLoopIndex);
            return choosenVersion;
        }

        // init expresion for the most reusable loop
        List<Expression> memoryOrder = reuseAnalysis.getLoopNestMemoryOrder();
        Expression initExpr = memoryOrder.get(memoryOrder.size() - 1);
        int positionOfMostReusableInnerLoop = getByIndexInLoopNestPosition(choosenVersion, initExpr);

        for (TiledLoop loop : loopsWithSameParallelization) {
            int auxPosition = getByIndexInLoopNestPosition(loop, initExpr);
            if (auxPosition == -1 || auxPosition == positionOfMostReusableInnerLoop) {
                continue;
            }

            if (auxPosition > positionOfMostReusableInnerLoop) {
                positionOfMostReusableInnerLoop = auxPosition;
                choosenVersion = loop;
            }
        }

        choosenVersion.setOutermostParallelizableLoop(outerParallelLoopIndex);

        return choosenVersion;
    }

    private int getByIndexInLoopNestPosition(ForLoop loopNest, Expression indexExpression) {
        List<ForLoop> loops = new ArrayList<>();
        LoopTools.calculateInnerLoopNest(loopNest).descendingIterator()
                .forEachRemaining((loop) -> loops.add((ForLoop) loop));

        if (loops.get(0) != loopNest) {
            Collections.reverse(loops);
        }

        int position = -1;

        for (int i = 0; i < loops.size(); i++) {
            ForLoop loop = loops.get(i);
            Expression initStm = loop.getStep();
            if (initStm.findExpression(indexExpression).size() == 0) {
                continue;
            }

            position = i;
        }

        return position;
    }

    private List<DependenceVector> calculateDependenceVectors(TiledLoop tiledVersion) {

        List<DependenceVector> dependenceVectors = tiledVersion.getDependenceVectors();
        logger.info("### Tiled version ###");
        logger.info(tiledVersion + "\n");

        logger.info("### Direction vector");
        logger.info(dependenceVectors + "\n");

        return dependenceVectors;

    }

    private int getOuterParallelLoopIdx(ForLoop loopNest, List<DependenceVector> dependenceVectors) {
        int position = -1;

        List<ForLoop> loops = new ArrayList<>();
        new DFIterator<ForLoop>(loopNest, ForLoop.class).forEachRemaining(loops::add);

        if (loops.size() == 0) {
            return -1;
        }

        if (loops.get(0) != loopNest) {
            // to have a list with ordered loops from the outermost to the innermost
            Collections.reverse(loops);
        }

        for (int i = 0; i < loops.size(); i++) {
            ForLoop loop = loops.get(i);
            for (DependenceVector dependenceVector : dependenceVectors) {
                int direction = dependenceVector.getDirection(loop);
                if (direction == DependenceVector.less) {
                    position = i;
                }
            }
        }

        return position;
    }

}
