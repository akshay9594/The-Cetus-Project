package cetus.transforms.tiling;

import java.util.ArrayList;
import java.util.List;

import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.hir.DFIterator;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Loop;
import cetus.hir.Statement;

public class TiledLoop extends ForLoop {

    private List<DependenceVector> dependeceVectors = new ArrayList<>();
    private Loop outermostParallelizableLoop;

    public TiledLoop(ForLoop loopNest, List<DependenceVector> dvs) throws Exception {
        this(loopNest.getInitialStatement().clone(false),
                loopNest.getCondition().clone(),
                loopNest.getStep().clone(),
                loopNest.getBody().clone(false));

        for (DependenceVector dv : dvs) {

            DependenceVector newDV = new DependenceVector();

            for (Loop dvLoop : dv.getLoops()) {

                Loop loopInNest = lookupLoop(dvLoop, this);
                if (loopInNest == null) {
                    throw new Exception(
                            "Error on setting dvs, a loop in one of the DVs does not correspond with any loop in the loopnest");
                }
                newDV.setDirection(loopInNest, dv.getDirection(dvLoop));

            }

            dependeceVectors.add(newDV);
        }
    }

    public void setOutermostParallelizableLoop(int positionOfParallelizableLoop) {
        List<Loop> nestedLoops = new ArrayList<>();
        new DFIterator<Loop>(this, Loop.class).forEachRemaining(nestedLoops::add);

        if (positionOfParallelizableLoop < 0 || positionOfParallelizableLoop >= nestedLoops.size()) {
            return;
        }
        this.outermostParallelizableLoop = nestedLoops.get(positionOfParallelizableLoop);
    }

    public Loop getOutermostParallelizableLoop() {
        return outermostParallelizableLoop;
    }

    private Loop lookupLoop(Loop loopInDV, ForLoop loopNest) {

        List<Loop> nestedLoops = new ArrayList<>();
        new DFIterator<Loop>(loopNest, Loop.class).forEachRemaining(nestedLoops::add);

        for (Loop loop : nestedLoops) {
            if (areEquals(loopInDV, loop)) {
                return loop;
            }
        }

        return null;
    }

    private boolean areEquals(Loop loop, Loop target) {
        String symbolName = LoopTools.getLoopIndexSymbol(loop).getSymbolName();
        String targetSymbolName = LoopTools.getLoopIndexSymbol(target).getSymbolName();

        return symbolName.equals(targetSymbolName);
    }

    public TiledLoop(Statement init, Expression condition, Expression step, Statement body) {
        super(init, condition, step, body);
    }

    public List<DependenceVector> getDependenceVectors() {
        return dependeceVectors;
    }

    @Override
    public TiledLoop clone(boolean mustHaveAnnotations) {
        ForLoop origLoop = super.clone(mustHaveAnnotations);

        TiledLoop clon = new TiledLoop(origLoop.getInitialStatement().clone(mustHaveAnnotations),
                origLoop.getCondition().clone(),
                origLoop.getStep().clone(),
                origLoop.getBody().clone(mustHaveAnnotations));

        return clon;
    }

    @Override
    public TiledLoop clone() {

        return clone(false);
    }

}
