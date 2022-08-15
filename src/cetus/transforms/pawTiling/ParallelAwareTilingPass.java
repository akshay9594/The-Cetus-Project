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
import cetus.hir.CompoundStatement;
import cetus.hir.ConditionalExpression;
import cetus.hir.DFIterator;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Initializer;
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
    public final static int DEFAULT_PROCESSORS = 4;
    public final static int MAX_NEST_LEVEL = 20;
    public final static String TILE_SUFFIX = "Tile";
    public final static String BALANCED_TILE_SIZE_NAME = "balancedTileSize";

    private int numOfProcessors;

    private boolean verbosity = false;

    private List<Loop> selectedOutermostLoops;

    private PawAnalysisData analysisData = new PawAnalysisData();

    public ParallelAwareTilingPass(Program program) {
        this(program, null);
    }

    public ParallelAwareTilingPass(Program program, CommandLineOptionSet commandLineOptions) {
        super(program);

        try {
            numOfProcessors = Integer.parseInt(commandLineOptions.getValue(PARAM_NAME));
            assert numOfProcessors > 0;
        } catch (Exception e) {
            System.out.println(
                    "Error on setting num of processors. The default value: " + DEFAULT_PROCESSORS + " will be used");
            numOfProcessors = DEFAULT_PROCESSORS;
        }
        verbosity = commandLineOptions.getValue("verbosity").equals("1");
        analysisData.verbosity = verbosity;
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

        if (verbosity) {
            System.out.println("#### Selected loops: " + selectedOutermostLoops.size() + "\n");
            for (Loop outermostLoop : selectedOutermostLoops) {
                System.out.println(outermostLoop + "\n");
            }
            System.out.println("#### END Selected loops");

            System.out.println(analysisData);
        }

        for (Loop outermostLoop : selectedOutermostLoops) {
            try {
                runPawTiling((ForLoop) outermostLoop);
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

    private void runPawTiling(ForLoop loopNest) throws Exception {
        try {
            runLoopInterchange(loopNest);
        } catch (Exception e) {
            if (verbosity) {
                System.out.println(
                        "It was not possible to perform loop interchange. However, paw tiling will continue. Error:");
                e.printStackTrace();
            }
        }

        runReuseAnalysis(loopNest);

        CompoundStatement variableDeclarations = new CompoundStatement();
        List<ForLoop> tiledVersions = createTiledVersions(loopNest, variableDeclarations);
        for (ForLoop tiledVersion : tiledVersions) {

        }

        Expression totalOfInstructions = getTotalOfInstructions(loopNest);
        BinaryExpression condition = new BinaryExpression(totalOfInstructions, BinaryOperator.COMPARE_LE,
                new IntegerLiteral(100000));

        ForLoop leastCostTiledVersion = tiledVersions.get(0);
        Expression rawTileSize = computeRawTileSize();

        Expression balancedTileSize = computeBalancedTileSize(totalOfInstructions, numOfProcessors,
                rawTileSize);

        // create two versions if iterations isn't enough to parallelize
        CompoundStatement leastCostVersionStm = new CompoundStatement();
        leastCostVersionStm.addStatement(leastCostTiledVersion.clone(false));

        IfStatement ifStm = new IfStatement(condition, loopNest.clone(false), leastCostVersionStm);

        replaceTileSize(variableDeclarations, balancedTileSize);

        addNewVariableDeclaratios(leastCostVersionStm,
                filterValidDeclarations(variableDeclarations, leastCostTiledVersion));

        updateAttributes(loopNest);

        replaceLoop(loopNest, ifStm);

    }

    // TODO: Update reduction/private variable attributes
    private void updateAttributes(ForLoop loopNest) {

    }

    private List<Declaration> filterValidDeclarations(CompoundStatement variableDeclarations,
            ForLoop loopNest) {

        List<Declaration> declarations = new ArrayList<>();

        List<ForLoop> loops = new ArrayList<>();
        new DFIterator<ForLoop>(loopNest, ForLoop.class).forEachRemaining(loops::add);

        IDExpression balancedTileSize = new NameID(BALANCED_TILE_SIZE_NAME);
        declarations.add(variableDeclarations.findSymbol(balancedTileSize));

        for (ForLoop loop : loops) {
            Symbol symbol = LoopTools.getLoopIndexSymbol(loop);

            if (symbol == null) {
                continue;
            }

            NameID name = new NameID(symbol.getSymbolName());

            Declaration declaration = variableDeclarations.findSymbol(name);

            if (declaration != null) {
                declarations.add(declaration);
            }

            Expression rawStepExpr = loop.getStep();
            if (!(rawStepExpr instanceof AssignmentExpression)) {
                continue;
            }

            AssignmentExpression stepExpr = (AssignmentExpression) rawStepExpr;
            Expression RHSExpr = stepExpr.getRHS();

            NameID assignationName = new NameID(RHSExpr.toString());

            Declaration assignationDeclaration = variableDeclarations.findSymbol(assignationName);

            if (assignationDeclaration != null) {
                declarations.add(assignationDeclaration);
            }
        }

        return declarations;
    }

    private Expression getTotalOfInstructions(ForLoop loopNest) throws Exception {
        List<ForLoop> loops = new ArrayList<>();
        new DFIterator<ForLoop>(loopNest, ForLoop.class).forEachRemaining(loops::add);

        Expression loopNestCond = loopNest.getCondition();
        if (!(loopNestCond instanceof BinaryExpression)) {
            throw new Exception("Condition should be a binary expression");
        }

        BinaryExpression cond = (BinaryExpression) loopNestCond;
        Expression totalOfInstructions = cond.getRHS();
        for (int i = 1; i < loops.size(); i++) {
            Expression loopCond = loops.get(i).getCondition();
            if ((loopCond instanceof BinaryExpression)) {
                totalOfInstructions = Symbolic.multiply(totalOfInstructions, ((BinaryExpression) loopCond).getRHS());
            }
        }

        return totalOfInstructions;
    }

    private void replaceTileSize(SymbolTable variableDeclarationSpace,
            Expression newTileSize) {

        Identifier balancedTileVariable = declareVariable(variableDeclarationSpace, BALANCED_TILE_SIZE_NAME,
                newTileSize);

        for (Declaration declaration : variableDeclarationSpace.getDeclarations()) {
            List<Traversable> children = declaration.getChildren();
            Declarator declarator = null;

            for (Traversable child : children) {
                if (!(child instanceof Declarator)) {
                    continue;
                }

                declarator = (Declarator) child;
                break;
            }

            if (declarator == null) {
                continue;
            }

            IDExpression id = declarator.getID();
            if (!id.getName().contains(TILE_SUFFIX)) {
                continue;
            }

            if (id.getName().equals(BALANCED_TILE_SIZE_NAME)) {
                continue;
            }
            Initializer init = new Initializer(balancedTileVariable.clone());
            declarator.setInitializer(init);
        }
    }

    private Expression computeBalancedTileSize(Expression numOfInstructions, int numOfProcessors,
            Expression rawTileSize) {

        Expression numOfProcessorsExp = new IntegerLiteral(numOfProcessors);

        // (numOfInstructions / (processors * rawSize)) * processors
        Expression divisor = Symbolic.multiply(
                Symbolic.divide(numOfInstructions, Symbolic.multiply(numOfProcessorsExp, rawTileSize)),
                numOfProcessorsExp);

        // instructions / ( ( instructions / (processors * rawSize) ) * processors )
        return Symbolic.divide(numOfInstructions, divisor);
    }

    // TODO
    private Expression computeRawTileSize() {
        return new IntegerLiteral(2);
    }

    private void runReuseAnalysis(ForLoop loopNest) {

    }

    private void addNewVariableDeclaratios(Traversable loopNest, List<Declaration> variableDeclarations) {

        SymbolTable variableDeclarationSpace = getVariableDeclarationSpace(loopNest);
        for (Declaration declaration : variableDeclarations) {
            variableDeclarationSpace.addDeclaration(declaration.clone());
        }

    }

    private List<Expression> runLoopInterchange(Loop loopNest) {

        LoopInterchange loopInterchangePass = new LoopInterchange(program);
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

    private List<ForLoop> createTiledVersions(Loop loopNest, SymbolTable variableDeclarationSpace) throws Exception {

        List<ForLoop> nestedLoops = new ArrayList<>();
        new DFIterator<ForLoop>(loopNest, ForLoop.class).forEachRemaining(nestedLoops::add);

        List<ForLoop> tiledVersions = new ArrayList<>();
        HashMap<String, Boolean> stripminedBySymbol = new HashMap<>();

        if (nestedLoops.size() < MAX_NEST_LEVEL) {
            createTiledVersions(variableDeclarationSpace, (ForLoop) loopNest, 20,
                    stripminedBySymbol,
                    tiledVersions);

            if (verbosity) {
                System.out.println("########## PRINTING TILED VERSIONS\n");

                for (ForLoop tiledVersion : tiledVersions) {
                    System.out.println("######## TILED VERSION #########\n");
                    System.out.println(tiledVersion);
                    System.out.println("######## END TILED VERSION #########\n");
                }
            }

        }

        return tiledVersions;

    }

    private void createTiledVersions(SymbolTable variableDeclarationSpace, ForLoop loopNest,
            int strip,
            HashMap<String, Boolean> stripmined, List<ForLoop> versions) throws Exception {

        List<ForLoop> nestedLoops = new ArrayList<>();
        new DFIterator<ForLoop>(loopNest, ForLoop.class).forEachRemaining(nestedLoops::add);
        int loopNestSize = nestedLoops.size();

        for (int i = loopNestSize - 1; i >= 0; i--) {

            String loopSymbolName = LoopTools.getLoopIndexSymbol(nestedLoops.get(i)).getSymbolName();
            if (stripmined.containsKey(loopSymbolName)) {
                continue;
            }

            ForLoop stripminedLoop = (ForLoop) tiling(variableDeclarationSpace, loopNest.clone(false),
                    strip, i, stripmined);
            versions.add(stripminedLoop);

            HashMap<String, Boolean> newStripmined = new HashMap<>();
            for (String symbol : stripmined.keySet()) {
                newStripmined.put(symbol, true);
            }

            createTiledVersions(variableDeclarationSpace, stripminedLoop.clone(false),
                    strip,
                    newStripmined, versions);

        }

    }

    private void replaceLoop(ForLoop originalLoop, IfStatement ifStm) {
        Traversable originalParent = originalLoop.getParent();

        int originalLoopIdx = -1;
        for (int i = 0; i < originalParent.getChildren().size(); i++) {
            if (originalParent.getChildren().get(i) == originalLoop) {
                originalLoopIdx = i;
                break;
            }
        }

        originalParent.setChild(originalLoopIdx, ifStm);
    }

    private Loop tiling(SymbolTable variableDeclarationSpace, ForLoop outermostLoop, int strip,
            int targetLoopPos, HashMap<String, Boolean> stripmined) throws Exception {
        List<ForLoop> loops = new ArrayList<>();
        new DFIterator<ForLoop>(outermostLoop, ForLoop.class).forEachRemaining(loops::add);

        ForLoop targetLoop = loops.get(targetLoopPos);
        ForLoop crossStripLoop = (ForLoop) stripmining(variableDeclarationSpace, targetLoop, strip);

        stripmined.put(LoopTools.getLoopIndexSymbol(targetLoop).getSymbolName(), true);
        stripmined.put(LoopTools.getLoopIndexSymbol(crossStripLoop).getSymbolName(), true);

        if (targetLoopPos - 1 >= 0) {
            ForLoop parentLoop = loops.get(targetLoopPos - 1);
            parentLoop.setBody(crossStripLoop);
            permuteLoop(crossStripLoop, parentLoop);
        }

        return outermostLoop;
    }

    private void permuteLoop(ForLoop loop, ForLoop targetLoop) {

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

    private Loop stripmining(SymbolTable variableDeclarationSpace, ForLoop loop, int strip) throws Exception {
        Statement loopInitialStatement = loop.getInitialStatement();
        if (loopInitialStatement.getChildren().size() > 1) {
            throw new Exception("Loop's initial statement has more than 2 expressions: " + loop.toString());
        }

        if (loopInitialStatement.getChildren().size() == 0) {
            throw new Exception("Loop's initial statement hasno expressions: " + loop.toString());
        }

        Symbol loopSymbol = LoopTools.getLoopIndexSymbol(loop);
        Identifier newIndexVariable = declareVariable(variableDeclarationSpace,
                loopSymbol.getSymbolName() + loopSymbol.getSymbolName());

        Identifier stripIdentifier = declareVariable(variableDeclarationSpace,
                loopSymbol.getSymbolName() + TILE_SUFFIX, new IntegerLiteral(strip));

        Loop inStripLoop = createInStripLoop(loop, stripIdentifier, newIndexVariable);
        Loop crossStripLoop = createCrossStripLoop(loop, stripIdentifier, newIndexVariable, (Statement) inStripLoop);

        return crossStripLoop;

    }

    private Loop createInStripLoop(ForLoop loop, Expression stripExpr, Identifier newIndexVariable) throws Exception {
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

    private Loop createCrossStripLoop(ForLoop loop, Expression stripExpr, Identifier newIndexVariable,
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

    private SymbolTable getVariableDeclarationSpace(Traversable traversable) {

        if (traversable instanceof SymbolTable) {
            return (SymbolTable) traversable;
        }
        Traversable auxTraversable = traversable.getParent();
        while (auxTraversable != null && auxTraversable.getParent() != null
                && !(auxTraversable instanceof SymbolTable)) {
            auxTraversable = auxTraversable.getParent();
        }

        return (SymbolTable) auxTraversable;
    }

    private Identifier declareVariable(SymbolTable variableDeclarationSpace, String varName) {

        return declareVariable(variableDeclarationSpace, varName, null);
    }

    private Identifier declareVariable(SymbolTable variableDeclarationSpace, String varName,
            Expression value) {

        NameID variableNameID = new NameID(varName);

        VariableDeclarator varDeclarator = new VariableDeclarator(variableNameID);

        if (value != null) {
            Initializer initializer = new Initializer(value);
            varDeclarator.setInitializer(initializer);
        }
        Declaration varDeclaration = new VariableDeclaration(Specifier.INT, varDeclarator);

        if (variableDeclarationSpace.findSymbol(variableNameID) == null) {
            variableDeclarationSpace.addDeclaration(varDeclaration);
        }

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

}
