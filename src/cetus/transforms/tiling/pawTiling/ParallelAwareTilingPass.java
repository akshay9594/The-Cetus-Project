package cetus.transforms.tiling.pawTiling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import cetus.analysis.AnalysisPass;
import cetus.analysis.ArrayPrivatization;
import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.analysis.Reduction;
import cetus.analysis.loopTools.LoopInstructionsUtils;
import cetus.codegen.CodeGenPass;
import cetus.codegen.ompGen;
import cetus.exec.CommandLineOptionSet;
import cetus.hir.Annotatable;
import cetus.hir.AssignmentExpression;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CetusAnnotation;
import cetus.hir.CompoundStatement;
import cetus.hir.DFIterator;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
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
import cetus.hir.Statement;
import cetus.hir.Symbol;
import cetus.hir.SymbolTable;
import cetus.hir.Symbolic;
import cetus.hir.Traversable;
import cetus.transforms.LoopInterchange;
import cetus.transforms.TransformPass;
import cetus.transforms.tiling.TiledLoop;
import cetus.transforms.tiling.TilingUtils;
import cetus.utils.VariableDeclarationUtils;
import cetus.utils.reuseAnalysis.DataReuseAnalysis;

public class ParallelAwareTilingPass extends TransformPass {

    public final static String PAW_TILING = "paw-tiling";

    public final static int DISTRIBUTED_CACHE_OPTION = 1;
    public final static int NON_DISTRIBUTED_CACHE_OPTION = 0;

    public final static String CORES_PARAM_NAME = "cores";
    public final static String CACHE_PARAM_NAME = "cache-size";

    public final static int DEFAULT_PROCESSORS = 4;
    public final static int DEFAULT_CACHE_SIZE = 4096;

    public final static int MAX_ITERATIONS_TO_PARALLELIZE = 100000;
    public final static String BALANCED_TILE_SIZE_NAME = "balancedTileSize";

    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    private int numOfProcessors = DEFAULT_PROCESSORS;
    private int cacheSize = DEFAULT_CACHE_SIZE;

    // TODO: non implemented yet
    private boolean isCacheDistributed = false;

    private List<Loop> selectedOutermostLoops;

    private PawAnalysisUtils pawAnalysisUtils = new PawAnalysisUtils();

    public ParallelAwareTilingPass(Program program) {
        this(program, null);
    }

    public ParallelAwareTilingPass(Program program, CommandLineOptionSet commandLineOptions) {
        super(program);

        if (commandLineOptions.getValue("verbosity").equals("1")) {
            setLogger(true);
            pawAnalysisUtils.verbosity = true;
        }

        try {
            numOfProcessors = Integer.parseInt(commandLineOptions.getValue(CORES_PARAM_NAME));
            assert numOfProcessors > 0;
        } catch (Exception e) {
            logger.warning(
                    "Error on setting num of processors. The default value: " + DEFAULT_PROCESSORS + " will be used");
        }

        try {
            cacheSize = Integer.parseInt(commandLineOptions.getValue(CACHE_PARAM_NAME));
            assert cacheSize > 0;
        } catch (Exception e) {
            logger.warning(
                    "Error on setting cache size. The default value: " + DEFAULT_CACHE_SIZE + " will be used");
        }

        try {
            int cacheType = Integer.parseInt(commandLineOptions.getValue(PAW_TILING));
            if (cacheType == DISTRIBUTED_CACHE_OPTION) {
                isCacheDistributed = true;
            }
        } catch (Exception e) {
            logger.warning(
                    "Error on setting cache type. The cache will be considered as non distributed");
        }

    }

    private void setLogger(boolean verbosity) {
        if (verbosity) {
            logger.setLevel(Level.ALL);
        } else {
            logger.setLevel(Level.WARNING);
        }

        FileHandler handler;
        try {
            handler = new FileHandler("./out/" + this.getClass().getSimpleName() + ".log");
            handler.setFormatter(new SimpleFormatter());
            handler.setEncoding("utf-8");
            logger.addHandler(handler);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPassName() {
        return "[Parallel-aware tiling]";
    }

    @Override
    public void start() {

        List<Loop> outermostLoops = LoopTools.getOutermostLoops(program);
        List<Loop> perfectLoops = pawAnalysisUtils.filterValidLoops(outermostLoops);
        this.selectedOutermostLoops = perfectLoops;

        logger.info("#### Selected loops: " + selectedOutermostLoops.size() + "\n");
        for (Loop outermostLoop : selectedOutermostLoops) {
            logger.info(outermostLoop + "\n");
        }
        logger.info("#### END Selected loops");

        logger.info(pawAnalysisUtils.toString());

        // In case of rollback
        List<Loop> originalLoopNestCopy = new ArrayList<>();
        outermostLoops.forEach(loop -> {
            originalLoopNestCopy.add(((ForLoop) loop).clone(false));
        });

        for (Loop outermostLoop : selectedOutermostLoops) {
            try {
                runPawTiling((ForLoop) outermostLoop);
            } catch (Exception e) {
                logger.info(" ----");
                logger.info("Error trying to run paw tiling");
                logger.info("Loop:");
                System.err.println(outermostLoop);
                logger.info("Error:");
                e.printStackTrace();
                logger.info(" ---- ");
            }
        }

    }

    private void runPawTiling(ForLoop loopNest) throws Exception {

        DataReuseAnalysis reuseAnalysis = null;
        try {
            reuseAnalysis = runReuseAnalysis(loopNest);
        } catch (Exception e) {
            logger.severe(
                    "It was not possible to perform reuse analysis. Only parallelism will take into account for deciding over tiled versions. Error:");
            e.printStackTrace();

        }

        if (!hasReuse(reuseAnalysis)) {
            return;
        }

        try {
            // TODO: Need a rollback if we want to conserve the original loopNest
            // runLoopInterchage(loopNest);
        } catch (Exception e) {
            logger.severe(
                    "It was not possible to perform loop interchange. However, paw tiling will continue. Error:");
            e.printStackTrace();
        }

        LinkedList<Loop> nestedLoops = new LinkedList<>();
        new DFIterator<Loop>(loopNest, Loop.class).forEachRemaining(nestedLoops::add);

        CompoundStatement variableDeclarations = new CompoundStatement();

        List<DependenceVector> dependenceVectors = program.getDDGraph().getDirectionMatrix(nestedLoops);

        logger.info("### ORIGINAL VERSION ###");
        logger.info(loopNest.toString());

        logger.info("#### DVs ####");
        logger.info(dependenceVectors.toString());
        logger.info("#### DVs END ####");

        logger.info("### ORIGINAL VERSION END ###");

        Optimizer optimizer = new Optimizer(variableDeclarations, loopNest, dependenceVectors);

        TiledLoop leastCostTiledVersion = optimizer.chooseOptimalVersion(reuseAnalysis);

        logger.info("### TILED VERSION SELECTION ###");
        logger.info(leastCostTiledVersion.toString());
        logger.info("### SELECTED VERSION ###");

        Expression totalOfInstructions = LoopInstructionsUtils.getTotalOfInstructions(loopNest);

        Expression rawTileSize = new IntegerLiteral(computeRawTileSize(MAX_ITERATIONS_TO_PARALLELIZE * 2, cacheSize));

        Expression balancedTileSize = computeBalancedTileSize(totalOfInstructions, numOfProcessors,
                rawTileSize);

        replaceTileSize(variableDeclarations, balancedTileSize);

        // create two versions if iterations isn't enough to parallelize

        CompoundStatement leastCostVersionStm = new CompoundStatement();
        leastCostVersionStm.addStatement(leastCostTiledVersion);

        VariableDeclarationUtils.addNewVariableDeclarations(leastCostVersionStm,
                filterValidDeclarations(variableDeclarations, leastCostTiledVersion));

        ForLoop newLoopNest = loopNest.clone(false);

        Statement twoVersionsStm = createTwoVersionsStm(totalOfInstructions, newLoopNest,
                leastCostVersionStm);

        replaceLoop(loopNest, twoVersionsStm);

        updateAttributes(leastCostTiledVersion.getOutermostParallelizableLoop());

    }

    private boolean hasReuse(DataReuseAnalysis reuseAnalysis) {

        boolean isReusable = true;

        isReusable &= reuseAnalysis != null
                && reuseAnalysis.getLoopCosts().size() > 0
                && reuseAnalysis.getLoopNestMemoryOrder().size() > 0;

        if (isReusable) {

        }

        return isReusable;
    }

    private Statement createTwoVersionsStm(Expression maxOfInstructions, Statement trueClause, Statement falseClause) {

        BinaryExpression condition = new BinaryExpression(maxOfInstructions, BinaryOperator.COMPARE_LE,
                new IntegerLiteral(MAX_ITERATIONS_TO_PARALLELIZE));

        IfStatement ifStm = new IfStatement(condition, trueClause, falseClause);

        return ifStm;

    }

    // TODO: Update reduction/private variable attributes
    private void updateAttributes(Loop parallelLoop) {
        LoopTools.addLoopName(program, true);
        AnalysisPass.run(new ArrayPrivatization(program));
        AnalysisPass.run(new Reduction(program));
        addCetusAnnotation(parallelLoop, true);
        CodeGenPass.run(new ompGen(program));
        // new ompGen(program).genOmpParallelLoops((ForLoop) parallelLoop);

    }

    /**
     * Inserts cetus annotation if the given loop is proven to be parallel.
     */
    private void addCetusAnnotation(Loop loop, boolean parallel) {
        CetusAnnotation note = new CetusAnnotation();

        if (parallel) {
            note.put("parallel", "true");
        }
        ((Annotatable) loop).annotate(note);
    }

    private List<Declaration> filterValidDeclarations(CompoundStatement variableDeclarations,
            ForLoop loopNest) {

        List<Declaration> declarations = new ArrayList<>();

        List<ForLoop> loops = new ArrayList<>();
        new DFIterator<ForLoop>(loopNest, ForLoop.class).forEachRemaining(loops::add);

        IDExpression balancedTileSize = new NameID(BALANCED_TILE_SIZE_NAME);

        Declaration balancedTileSizeDeclaration = variableDeclarations.findSymbol(balancedTileSize);
        if (balancedTileSizeDeclaration != null) {
            declarations.add(balancedTileSizeDeclaration);
        }

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

    private void replaceTileSize(SymbolTable variableDeclarationSpace,
            Expression newTileSize) {

        Identifier balancedTileVariable = VariableDeclarationUtils.declareVariable(variableDeclarationSpace,
                BALANCED_TILE_SIZE_NAME,
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
            if (!id.getName().contains(TilingUtils.TILE_SUFFIX)) {
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

    // Implements the algorithm to find the block sized proposed in the paper the
    // Cache Performance and Optimization of Blocked Algorithms.
    // By Monica D. Lam, Edward E. Rothberg, Michael E. Wolf
    private int computeRawTileSize(int matrixSize, int cacheSize) {

        // int addr, di, dj, maxWidth;

        // maxWidth = Math.min(matrixSize, cacheSize);
        // addr = matrixSize / 2;
        // while (true) {
        // addr += cacheSize;
        // di = (int) Math.round((double) matrixSize / (double) addr);
        // dj = Math.abs((addr % matrixSize) - matrixSize / 2);
        // int auxMinMaxWidth = Math.min(maxWidth, dj);
        // if (di >= auxMinMaxWidth) {
        // return Math.round(Math.min(maxWidth, di));
        // }
        // maxWidth = auxMinMaxWidth;
        // }

        return 1000;
    }

    public void runLoopInterchage(Loop loopNest) {
        LoopInterchange loopInterchangePass = new LoopInterchange(program);

        LinkedList<Loop> loops = new LinkedList<>();

        List<Expression> originalOrder = new ArrayList<>();

        new DFIterator<Loop>(loopNest, Loop.class).forEachRemaining(loop -> {

            originalOrder.add(LoopTools.getIndexVariable(loop));
            loops.add(loop);
        });

        Map<ForLoop, String> loopMap = new HashMap<>();

        loopInterchangePass.interchangeLoops(loopNest, loops.get(loops.size() - 1), loops, loopMap, originalOrder);

        logger.info(program.toString());
    }

    private DataReuseAnalysis runReuseAnalysis(Loop loopNest) {

        DataReuseAnalysis reuseAnalysis = new DataReuseAnalysis(loopNest);
        List<Expression> memoryOrder = reuseAnalysis.getLoopNestMemoryOrder();

        logger.info("### REUSE ANALYSIS ###");

        logger.info("#### LOOP COSTS:");
        logger.info(DataReuseAnalysis.printLoopCosts(reuseAnalysis.getLoopCosts()));
        logger.info("#### LOOP COSTS END");

        logger.info("#### Memory order:");

        for (int i = 0; i < memoryOrder.size(); i++) {
            Expression expr = memoryOrder.get(i);
            logger.info(expr + "\n");
        }

        logger.info("#### Memory order END");
        logger.info("### REUSABILITY END ###");

        return reuseAnalysis;

    }

    private void replaceLoop(ForLoop originalLoop, Statement ifStm) {
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

}
