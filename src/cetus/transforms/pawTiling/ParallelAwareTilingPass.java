package cetus.transforms.pawTiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import cetus.analysis.LoopTools;
import cetus.exec.CommandLineOptionSet;
import cetus.hir.ArrayAccess;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.ConditionalExpression;
import cetus.hir.DFIterator;
import cetus.hir.Declaration;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Loop;
import cetus.hir.NameID;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.Symbol;
import cetus.hir.SymbolTable;
import cetus.hir.Symbolic;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cetus.transforms.LoopInterchange;
import cetus.transforms.TransformPass;
import cetus.utils.DataDependenceUtils;

public class ParallelAwareTilingPass extends TransformPass {

    public final static String PARAM_NAME = "paw-tiling";

    private CommandLineOptionSet commandLineOptions;

    private int processors;

    private List<Loop> selectedOutermostLoops;

    private PawAnalysisData analysisData = new PawAnalysisData();

    private LoopInterchange loopInterchangePass = null;

    public ParallelAwareTilingPass(Program program) {
        this(program, null);
    }

    public ParallelAwareTilingPass(Program program, CommandLineOptionSet commandLineOptions) {
        super(program);
        this.commandLineOptions = commandLineOptions;
        if (commandLineOptions.getValue("verbosity").equals("1")) {
            System.out.println("Verbosity activated");
            analysisData.verbosity = true;
        }

        loopInterchangePass = new LoopInterchange(program);
    }

    @Override
    public String getPassName() {
        return "[Parallel-aware tiling]";
    }

    @Override
    public void start() {
        System.out.println(program);
        System.out.println("Printing DD Graph");
        DataDependenceUtils.printDependenceArcs(program);

        System.out.println("Printing direction matrix ");
        DataDependenceUtils.printDirectionMatrix(program);

        List<Loop> outermostLoops = LoopTools.getOutermostLoops(program);
        List<Loop> perfectLoops = filterValidLoops(outermostLoops);
        this.selectedOutermostLoops = perfectLoops;

        System.out.println("#### Selected loops: " + selectedOutermostLoops.size() + "\n");
        for (Loop outermostLoop : selectedOutermostLoops) {
            System.out.println(outermostLoop + "\n");
        }
        System.out.println("#### END Selected loops");

        System.out.println(analysisData);

        for (Loop outermostLoop : selectedOutermostLoops) {
            try {
                runPawTiling(outermostLoop);
            } catch (Exception e) {
                System.out.println(" ----");
                System.out.println("Error trying to run paw tiling");
                System.out.println("Loop:");
                System.err.println(outermostLoop);
                System.out.println("Error:");
                e.printStackTrace();
                System.out.println(" ---- ");
            }
        }

    }

    private void runPawTiling(Loop loopNest) {
        runDataReuseAnalysis(loopNest);
        createTiledVersions(loopNest);
    }

    private List<Expression> runDataReuseAnalysis(Loop loopNest) {

        loopInterchangePass.start();

        LinkedList<Loop> loopNestList = LoopTools.calculateInnerLoopNest(loopNest);

        List<Expression> memoryOrder = loopInterchangePass.ReusabilityAnalysis(program, loopNest,
                getLoopAssingmentExpressions(loopNest), getLoopArrayAccesses(loopNest), loopNestList);

        System.out.println(program.toString());

        System.out.println("### REUSABILITY ###");

        for (int i = 0; i < memoryOrder.size(); i++) {
            Expression expr = memoryOrder.get(i);
            System.out.println(expr);
        }

        System.out.println("### REUSABILITY END ###");

        return memoryOrder;
    }

    private void createTiledVersions(Loop loopNest) {

        DFIterator<ForLoop> nestedLoopsIter = new DFIterator<>(loopNest, ForLoop.class);
        List<ForLoop> nestedLoops = new ArrayList<>();
        while (nestedLoopsIter.hasNext()) {
            ForLoop loop = nestedLoopsIter.next();
            nestedLoops.add(loop);
        }

        ForLoop outermostLoop = nestedLoops.get(0);
        ForLoop outermostClone = outermostLoop.clone(false);

        ForLoop innermostLoop = nestedLoops.get(nestedLoops.size() - 1);
        ForLoop innermostClone = innermostLoop.clone(false);

        SymbolTable variableDeclarationSpace = getVariableDeclarationSpace(outermostLoop);

        try {

            // i,j
            // i,i1,j
            // i,i1,j,j1
            // i,j,j1

            // i,j,k

            // i,j,k,k1
            // i,j,j1,k,k1
            // i,i1,j,j1,k,k1

            // i,j,j1,k
            // i,i1,j,j1,k

            // i,i1,j,k
            // i,i1,j,k,k1

            // ForLoop innerClone = innermostLoop.clone(false);
            // Loop stripminnedLoop = stripmining(variableDeclarationSpace,
            // outermostLoop, 20);

            // Loop stripminnedLoop2 = stripmining(variableDeclarationSpace,
            // innermostLoop, 20);
            // ForLoop outerClone = outermostLoop.clone();

            List<ForLoop> tiledVersions = createTiledVersion(variableDeclarationSpace, nestedLoops, 20, true, 0);

            HashMap<String, Boolean> loopsByStringRepresentation = new HashMap<>();
            List<ForLoop> filteredTiledVersions = new ArrayList<>();
            for (int i = 0; i < tiledVersions.size(); i++) {
                ForLoop tiledVersion = tiledVersions.get(i);
                if (!loopsByStringRepresentation.containsKey(tiledVersion.toString())) {
                    filteredTiledVersions.add(tiledVersion);
                }
                loopsByStringRepresentation.put(tiledVersion.toString(), true);

            }

            System.out.println("########## PRINTING TILED VERSIONS\n");

            for (ForLoop tiledVersion : filteredTiledVersions) {
                System.out.println("######## TILED VERSION #########\n");
                System.out.println(tiledVersion);
                System.out.println("######## END TILED VERSION #########\n");
            }

            // System.out.println("PROGRAM BEFORE REPLACE");
            // System.out.println(program);

            // replaceLoop(innermostLoop, stripminnedLoop);
            // System.out.println("PROGRAM AFTER REPLACE");
            // System.out.println(program);
        } catch (Exception e) {
            System.out.println("It was not possible to do stripmining over loop:");
            System.out.println(loopNest);
            e.printStackTrace();
        }
    }

    private List<ForLoop> createTiledVersion(SymbolTable variableDeclarationSpace, List<ForLoop> loopNest, int strip,
            boolean stripmine, int loopIdx) throws Exception {

        List<ForLoop> retVersions = new ArrayList<>();

        if (loopIdx >= loopNest.size()) {
            return new ArrayList<>();
        }

        if (!stripmine) {
            List<ForLoop> loops = new ArrayList<>();
            ForLoop targetLoop = loopNest.get(loopIdx);
            ForLoop noStripminedLoop = new ForLoop(targetLoop.getInitialStatement().clone(),
                    targetLoop.getCondition().clone(),
                    targetLoop.getStep().clone(),
                    targetLoop.getBody().clone(false));
            loops.add(noStripminedLoop);

            return loops;
        }

        ForLoop targetLoop = loopNest.get(loopIdx);

        // retVersions.add(targetLoop);

        ForLoop stripminedLoop = (ForLoop) stripmining(variableDeclarationSpace, targetLoop, strip);
        retVersions.add(stripminedLoop);

        List<ForLoop> subStripminedVersions = createTiledVersion(variableDeclarationSpace, loopNest, strip, true,
                loopIdx + 1);

        List<ForLoop> noStripminedVersions = createTiledVersion(variableDeclarationSpace, loopNest, strip, false,
                loopIdx + 1);

        List<ForLoop> stripminedVersions = new ArrayList<>();
        stripminedVersions.addAll(subStripminedVersions);
        stripminedVersions.addAll(noStripminedVersions);

        for (ForLoop stripminedVersion : stripminedVersions) {

            if (stripminedVersion.toString() == null) {
                continue;
            }
            DFIterator<ForLoop> bodyIter = new DFIterator<>(stripminedLoop, ForLoop.class);
            List<ForLoop> nestedStripminedLoops = new ArrayList<>();
            while (bodyIter.hasNext()) {
                nestedStripminedLoops.add(bodyIter.next());
            }

            ForLoop inStripLoop = nestedStripminedLoops.get(nestedStripminedLoops.size() - 2);

            Statement oriStatement = inStripLoop.getInitialStatement();
            Expression oriCond = inStripLoop.getCondition();
            Expression oriStep = inStripLoop.getStep();

            ForLoop newInStripLoop = new ForLoop(oriStatement.clone(),
                    oriCond.clone(),
                    oriStep.clone(),
                    stripminedVersion.clone(false));

            oriStatement = stripminedLoop.getInitialStatement();
            oriCond = stripminedLoop.getCondition();
            oriStep = stripminedLoop.getStep();

            ForLoop newCrossStripLoop = new ForLoop(oriStatement.clone(),
                    oriCond.clone(),
                    oriStep.clone(),
                    newInStripLoop);

            retVersions.add(newCrossStripLoop);
        }

        return retVersions;

    }

    private void replaceLoop(ForLoop originalLoop, Loop newLoop) {
        Traversable originalParent = originalLoop.getParent();

        int originalLoopIdx = -1;
        for (int i = 0; i < originalParent.getChildren().size(); i++) {
            if (originalParent.getChildren().get(i) == originalLoop) {
                originalLoopIdx = i;
                break;
            }
        }

        originalParent.setChild(originalLoopIdx, newLoop);
    }

    private Loop stripmining(SymbolTable variableDeclarationSpace, ForLoop loop, int strip) throws Exception {
        Statement loopInitialStatement = loop.getInitialStatement();
        if (loopInitialStatement.getChildren().size() > 1) {
            throw new Exception("Loop's initial statement has more than 2 expressions: " + loop.toString());
        }

        if (loopInitialStatement.getChildren().size() == 0) {
            throw new Exception("Loop's initial statement hasno expressions: " + loop.toString());
        }

        Symbol loopSymbol = LoopTools.getLoopIndexSymbol(loop);
        Identifier newIndexVariable = declareIndexVariable(variableDeclarationSpace,
                loopSymbol.getSymbolName() + "_tiling");

        Loop inStripLoop = createInStripLoop(loop, strip, newIndexVariable);
        Loop crossStripLoop = createCrossStripLoop(loop, strip, newIndexVariable, (Statement) inStripLoop);

        return crossStripLoop;

    }

    private Loop createInStripLoop(ForLoop loop, int strip, Identifier newIndexVariable) throws Exception {
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

        Expression stripExp = new IntegerLiteral(strip);
        Expression stripCondition = Symbolic.add(newIndexVariable, stripExp);
        stripCondition = Symbolic.subtract(stripCondition, new IntegerLiteral(1));
        Expression minExp = new ConditionalExpression(
                new BinaryExpression(stripCondition.clone(), BinaryOperator.COMPARE_LT, condRHS.clone()),
                stripCondition.clone(), condRHS.clone());

        Expression newLoopCondition = new BinaryExpression(
                condLHS.clone(), condOperator, minExp);

        Expression newLoopStepExp = loop.getStep().clone();

        Statement newLoopInitStm = loop.getInitialStatement().clone();
        ((Expression) newLoopInitStm.getChildren().get(0)).swapWith(newLoopInitExp);

        Statement newLoopBody = loop.getBody().clone();

        DFIterator<ForLoop> bodyIter = new DFIterator<>(loop.getBody(), ForLoop.class);

        boolean isBodyALoop = bodyIter.hasNext();

        if (isBodyALoop) {

            List<ForLoop> childLoops = new ArrayList<>();
            while (bodyIter.hasNext()) {
                childLoops.add(bodyIter.next());
            }

            ForLoop outermostChildLoop = childLoops.get(childLoops.size() - 1);
            newLoopBody = outermostChildLoop.clone(false);
        }

        ForLoop inStripLoop = new ForLoop(newLoopInitStm, newLoopCondition, newLoopStepExp, newLoopBody);

        return inStripLoop;
    }

    private Loop createCrossStripLoop(ForLoop loop, int strip, Identifier newIndexVariable,
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
        Expression stepRHS = new IntegerLiteral(strip);
        Expression newLoopStepExp = new AssignmentExpression(stepLHS.clone(), AssignmentOperator.ADD, stepRHS.clone());

        Statement newLoopInitStm = loop.getInitialStatement().clone();
        ((Expression) newLoopInitStm.getChildren().get(0)).swapWith(newLoopInitExp);

        ForLoop crossStripLoop = new ForLoop(newLoopInitStm, newLoopCondition, newLoopStepExp, inStripLoop);

        return crossStripLoop;
    }

    private SymbolTable getVariableDeclarationSpace(Traversable traversable) {
        Traversable auxTraversable = traversable.getParent();
        while (auxTraversable != null && auxTraversable.getParent() != null
                && !(auxTraversable instanceof SymbolTable)) {
            auxTraversable = auxTraversable.getParent();
        }

        return (SymbolTable) auxTraversable;
    }

    private Identifier declareIndexVariable(SymbolTable variableDeclarationSpace, String varName) {

        NameID variableNameID = new NameID(varName);
        VariableDeclarator varDeclarator = new VariableDeclarator(variableNameID);
        Declaration varDeclaration = new VariableDeclaration(Specifier.INT, varDeclarator);

        variableDeclarationSpace.addDeclaration(varDeclaration);

        Identifier varIdentifier = new Identifier(varDeclarator);

        return varIdentifier;
    }

    private List<AssignmentExpression> getLoopAssingmentExpressions(Loop loop) {
        List<AssignmentExpression> assignmentExpressions = new ArrayList<>();
        DepthFirstIterator<Traversable> statementsIterator = new DepthFirstIterator<>(loop);
        while (statementsIterator.hasNext()) {
            Traversable stm = statementsIterator.next();
            if (stm instanceof AssignmentExpression) {
                assignmentExpressions.add((AssignmentExpression) stm);
            }
        }
        return assignmentExpressions;
    }

    public List<ArrayAccess> getLoopArrayAccesses(Loop loop) {
        List<ArrayAccess> arrayAccesses = new ArrayList<>();
        DepthFirstIterator<Traversable> statementsIterator = new DepthFirstIterator<>(loop);
        while (statementsIterator.hasNext()) {
            Traversable stm = statementsIterator.next();
            if (stm instanceof ArrayAccess) {
                arrayAccesses.add((ArrayAccess) stm);
            }
        }
        return arrayAccesses;
    }

    public List<Loop> filterValidLoops(List<Loop> loops) {
        List<Loop> validLoops = new ArrayList<>();
        for (Loop loop : loops) {
            if (isCanonical(loop)
                    && isPerfectNest(loop)
                    && !containsFunctionCalls(loop)
                    && isIncreasingOrder(loop)) {
                validLoops.add(loop);
            }
        }

        return validLoops;
    }

    public boolean isIncreasingOrder(Loop loop) {

        if (LoopTools.getIncrementExpression(loop).toString().equals("-1")) {
            analysisData.nonIncreasingOrderLoops.add(loop);
            return false;
        }

        return true;
    }

    public boolean isCanonical(Loop loop) {
        if (!LoopTools.isCanonical(loop)) {
            analysisData.nonCanonicalLoops.add(loop);
            return false;
        }
        return true;
    }

    public boolean isPerfectNest(Loop loop) {
        if (!LoopTools.isPerfectNest(loop)) {
            analysisData.nonPerfectNestLoops.add(loop);
            return false;
        }
        return true;
    }

    public boolean containsFunctionCalls(Loop loop) {
        if (LoopTools.containsFunctionCall(loop)) {
            analysisData.withFunctionCallLoops.add(loop);
            return true;
        }
        return false;
    }

    // public void stripmining() {
    // chooseStrip();
    // }

    // public int chooseStrip(Loop loop) {

    // }

}
