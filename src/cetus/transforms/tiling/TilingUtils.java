package cetus.transforms.tiling;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.ConditionalExpression;
import cetus.hir.DFIterator;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Loop;
import cetus.hir.Statement;
import cetus.hir.Symbol;
import cetus.hir.SymbolTable;
import cetus.hir.Symbolic;
import cetus.hir.Traversable;
import cetus.utils.VariableDeclarationUtils;

public class TilingUtils {

    public final static String TILE_SUFFIX = "Tile";

    public static Loop createInStripLoop(ForLoop loop, Expression stripExpr, Identifier newIndexVariable)
            throws Exception {
        Statement originalInitStatement = loop.getInitialStatement();
        List<Traversable> originalInitStatements = originalInitStatement.getChildren();
        if (originalInitStatements.size() > 1) {
            throw new Exception("Loop has more than one initial statment");
        }

        if (originalInitStatements.size() == 0) {
            throw new Exception("Loop has no initial statment");
        }

        Expression originalInitExpr = (Expression) originalInitStatements.get(0);
        Expression originalCondition = loop.getCondition();

        if (!(originalInitExpr instanceof AssignmentExpression)) {
            throw new Exception("Loop's init statement is not an assignment expression");
        }

        if (!(originalCondition instanceof BinaryExpression)) {
            throw new Exception("Loop has no binary expression as original condition");
        }

        AssignmentExpression oriAssignmentExp = (AssignmentExpression) originalInitExpr;
        Expression initLHSExp = oriAssignmentExp.getLHS();
        AssignmentOperator assignmentOperator = oriAssignmentExp.getOperator();

        Expression newLoopInitExp = new AssignmentExpression(initLHSExp.clone(), assignmentOperator,
                newIndexVariable);

        BinaryExpression originalLoopCondition = (BinaryExpression) originalCondition;
        Expression condRHS = originalLoopCondition.getRHS();
        Expression condLHS = originalLoopCondition.getLHS();
        BinaryOperator condOperator = originalLoopCondition.getOperator();

        Symbol loopSymbol = LoopTools.getLoopIndexSymbol(loop);

        if (!loopSymbol.getSymbolName().equals(condLHS.toString())) {
            throw new Exception("LHS is not a symbol");
        }

        Expression stripCondition = Symbolic.subtract(stripExpr, new IntegerLiteral(1));
        stripCondition = Symbolic.add(stripCondition, newIndexVariable);
        Expression minExp = new ConditionalExpression(
                new BinaryExpression(stripCondition.clone(), BinaryOperator.COMPARE_LT, condRHS.clone()),
                stripCondition.clone(), condRHS.clone());

        Expression newLoopCondition = new BinaryExpression(
                condLHS.clone(), condOperator, minExp);

        Expression newLoopStepExp = loop.getStep().clone();

        Statement newLoopInitStm = loop.getInitialStatement().clone();
        ((Expression) newLoopInitStm.getChildren().get(0)).swapWith(newLoopInitExp);

        Statement newLoopBody = loop.getBody().clone(false);

        ForLoop inStripLoop = new ForLoop(newLoopInitStm, newLoopCondition, newLoopStepExp, newLoopBody);

        return inStripLoop;
    }

    public static Loop createCrossStripLoop(ForLoop loop, Expression stripExpr, Identifier newIndexVariable,
            Statement inStripLoop) throws Exception {

        Statement originalInitStatement = loop.getInitialStatement();
        List<Traversable> originalInitStatements = originalInitStatement.getChildren();
        if (originalInitStatements.size() > 1) {
            throw new Exception("Loop has more than one initial statment");
        }

        if (originalInitStatements.size() == 0) {
            throw new Exception("Loop has no initial statment");
        }

        Expression originalInitExpr = (Expression) originalInitStatements.get(0);
        Expression originalCondition = loop.getCondition();

        if (!(originalInitExpr instanceof AssignmentExpression)) {
            throw new Exception("Loop's init statement is not an assignment expression");
        }

        if (!(originalCondition instanceof BinaryExpression)) {
            throw new Exception("Loop has no binary expression as original condition");
        }

        AssignmentExpression oriAssignmentExp = (AssignmentExpression) originalInitExpr;
        Expression initRHSExp = oriAssignmentExp.getRHS();
        AssignmentOperator assignmentOperator = oriAssignmentExp.getOperator();

        Symbol loopSymbol = newIndexVariable.getSymbol();
        Expression newLoopInitExp = new AssignmentExpression(newIndexVariable.clone(), assignmentOperator,
                initRHSExp.clone());

        BinaryExpression originalLoopCondition = (BinaryExpression) originalCondition;
        Expression condRHS = originalLoopCondition.getRHS();
        Expression condLHS = originalLoopCondition.getLHS();
        BinaryOperator condOperator = originalLoopCondition.getOperator();

        if (loopSymbol.getSymbolName().equals(condLHS.toString())) {
            condLHS = newIndexVariable;

        } else if (loopSymbol.getSymbolName().equals(condRHS.toString())) {
            condRHS = newIndexVariable;

        }

        Expression newLoopCondition = new BinaryExpression(
                newIndexVariable.clone(), condOperator, condRHS.clone());

        Expression stepLHS = newIndexVariable;
        Expression stepRHS = stripExpr;
        Expression newLoopStepExp = new AssignmentExpression(stepLHS.clone(), AssignmentOperator.ADD, stepRHS.clone());

        Statement newLoopInitStm = loop.getInitialStatement().clone();
        ((Expression) newLoopInitStm.getChildren().get(0)).swapWith(newLoopInitExp);

        ForLoop crossStripLoop = new ForLoop(newLoopInitStm, newLoopCondition, newLoopStepExp, inStripLoop);

        return crossStripLoop;
    }

    public static void permuteLoop(ForLoop loop, ForLoop targetLoop) {

        Statement originalInitStm = targetLoop.getInitialStatement().clone();
        Expression originalCond = targetLoop.getCondition().clone();
        Expression originalStep = targetLoop.getStep().clone();

        targetLoop.setInitialStatement(loop.getInitialStatement().clone());
        targetLoop.setCondition(loop.getCondition().clone());
        targetLoop.setStep(loop.getStep().clone());

        loop.setInitialStatement(originalInitStm);
        loop.setCondition(originalCond);
        loop.setStep(originalStep);
    }

    public static Loop[] stripmining(SymbolTable variableDeclarationSpace, ForLoop loop, int strip) throws Exception {
        Statement loopInitialStatement = loop.getInitialStatement();
        if (loopInitialStatement.getChildren().size() > 1) {
            throw new Exception("Loop's initial statement has more than 2 expressions: " + loop.toString());
        }

        if (loopInitialStatement.getChildren().size() == 0) {
            throw new Exception("Loop's initial statement hasno expressions: " + loop.toString());
        }

        Symbol loopSymbol = LoopTools.getLoopIndexSymbol(loop);
        Identifier newIndexVariable = VariableDeclarationUtils.declareVariable(variableDeclarationSpace,
                loopSymbol.getSymbolName() + loopSymbol.getSymbolName());

        Identifier stripIdentifier = VariableDeclarationUtils.declareVariable(variableDeclarationSpace,
                loopSymbol.getSymbolName() + TILE_SUFFIX, new IntegerLiteral(strip));

        Loop inStripLoop = createInStripLoop(loop, stripIdentifier, newIndexVariable);
        Loop crossStripLoop = createCrossStripLoop(loop, stripIdentifier, newIndexVariable, (Statement) inStripLoop);

        return new Loop[] { crossStripLoop, inStripLoop };

    }

    public static TiledLoop tiling(SymbolTable variableDeclarationSpace, ForLoop outermostLoop, int strip,
            int targetLoopPos, Map<String, Boolean> stripmined, List<DependenceVector> dependenceVectors)
            throws Exception {

        ForLoop tiledLoop = null;
        List<ForLoop> loops = new ArrayList<>();
        new DFIterator<ForLoop>(outermostLoop, ForLoop.class).forEachRemaining(loops::add);

        ForLoop targetLoop = loops.get(targetLoopPos);
        Loop[] stripminedLoops = TilingUtils.stripmining(variableDeclarationSpace, targetLoop, strip);
        ForLoop crossStripLoop = (ForLoop) stripminedLoops[0];
        ForLoop inStripLoop = (ForLoop) stripminedLoops[1];

        stripmined.put(LoopTools.getLoopIndexSymbol(targetLoop).getSymbolName(), true);
        stripmined.put(LoopTools.getLoopIndexSymbol(crossStripLoop).getSymbolName(), true);

        tiledLoop = crossStripLoop;

        if (targetLoopPos - 1 >= 0) {
            ForLoop parentLoop = loops.get(targetLoopPos - 1);

            parentLoop.setBody(crossStripLoop);
            TilingUtils.permuteLoop(crossStripLoop, parentLoop);

            crossStripLoop = parentLoop;

            tiledLoop = outermostLoop;
        }

        List<DependenceVector> newDVS = calculateAfterTilingDVs(dependenceVectors, tiledLoop,
                (Loop) inStripLoop,
                (Loop) crossStripLoop);

        TiledLoop newTiledLoop = new TiledLoop(tiledLoop, newDVS);

        return newTiledLoop;
    }

    public static List<DependenceVector> calculateAfterTilingDVs(List<DependenceVector> originalDVs,
            Loop newLoopNest,
            Loop inStripLoop, Loop crossStripLoop) {

        List<DependenceVector> newDVS = new ArrayList<>();
        for (DependenceVector originalDV : originalDVs) {
            newDVS.addAll(calculateAfterTilingDV(originalDV, newLoopNest, inStripLoop, crossStripLoop));
        }
        return newDVS;
    }

    public static List<DependenceVector> calculateAfterTilingDV(DependenceVector originalDV,
            Loop newLoopNest,
            Loop inStripLoop, Loop crossStripLoop) {
        List<DependenceVector> newDVS = new ArrayList<>();

        LinkedList<Loop> nestedLoops = new LinkedList<>();
        new DFIterator<Loop>(newLoopNest, Loop.class).forEachRemaining(nestedLoops::add);

        // real object references inside nested loops
        Loop actualInStripLoop = inStripLoop;
        Loop actualCrossStripLoop = crossStripLoop;

        String inStripSymbol = LoopTools.getLoopIndexSymbol(inStripLoop).getSymbolName();
        String crossStripSymbol = LoopTools.getLoopIndexSymbol(crossStripLoop).getSymbolName();

        // find real object references inside nested loops
        for (Loop loop : nestedLoops) {

            String loopSymbol = LoopTools.getLoopIndexSymbol(loop).getSymbolName();

            if (loopSymbol.equals(inStripSymbol)) {
                actualInStripLoop = loop;
                continue;
            }
            if (loopSymbol.equals(crossStripSymbol)) {
                actualCrossStripLoop = loop;
                continue;
            }

            if (actualInStripLoop != null && actualCrossStripLoop != null) {
                break;
            }

        }

        DependenceVector newBaseDV = new DependenceVector(nestedLoops);

        int direction = findOriginalDirection(originalDV, actualInStripLoop);

        switch (direction) {
            case DependenceVector.equal: {
                DependenceVector newDV = new DependenceVector(newBaseDV);
                newDV.setDirection(actualInStripLoop, DependenceVector.equal);
                newDV.setDirection(actualCrossStripLoop, DependenceVector.equal);

                newDVS.add(newDV);
                break;
            }

            case DependenceVector.greater: {
                DependenceVector newDV = new DependenceVector(newBaseDV);
                newDV.setDirection(actualInStripLoop, DependenceVector.greater);
                newDV.setDirection(actualCrossStripLoop, DependenceVector.equal);

                DependenceVector newDV2 = new DependenceVector(newBaseDV);
                newDV2.setDirection(actualInStripLoop, DependenceVector.any);
                newDV2.setDirection(actualCrossStripLoop, DependenceVector.greater);

                newDVS.add(newDV);
                newDVS.add(newDV2);

                break;
            }

            case DependenceVector.less: {
                DependenceVector newDV = new DependenceVector(newBaseDV);
                newDV.setDirection(actualInStripLoop, DependenceVector.less);
                newDV.setDirection(actualCrossStripLoop, DependenceVector.equal);

                DependenceVector newDV2 = new DependenceVector(newBaseDV);
                newDV2.setDirection(actualInStripLoop, DependenceVector.any);
                newDV2.setDirection(actualCrossStripLoop, DependenceVector.less);

                newDVS.add(newDV);
                newDVS.add(newDV2);
                break;
            }

            default:
                break;
        }

        for (Loop loop : nestedLoops) {
            if (loop == actualInStripLoop || loop == actualCrossStripLoop) {
                continue;
            }

            for (DependenceVector newDV : newDVS) {
                int originalDirection = findOriginalDirection(originalDV, loop);
                newDV.setDirection(loop, originalDirection);
            }
        }

        return newDVS;
    }

    private static int findOriginalDirection(DependenceVector dv, Loop targetLoop) {
        int direction = DependenceVector.nil;

        String loopSymbol = LoopTools.getLoopIndexSymbol(targetLoop).getSymbolName();

        Loop origLoop = null;

        for (Loop loop : dv.getLoops()) {
            String origLoopSymbol = LoopTools.getLoopIndexSymbol(loop).getSymbolName();
            if (origLoopSymbol.equals(loopSymbol)) {
                origLoop = loop;
                break;
            }
        }

        if (origLoop != null) {
            direction = dv.getDirection(origLoop);
        }

        return direction;
    }
}
