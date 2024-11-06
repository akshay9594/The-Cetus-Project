package cetus.transforms.tiling.pawTiling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import cetus.analysis.AnalysisPass;
import cetus.analysis.ArrayPrivatization;
import cetus.analysis.DDGraph;
import cetus.analysis.DDTDriver;
import cetus.analysis.DependenceVector;
import cetus.analysis.LoopParallelizationPass;
import cetus.analysis.LoopTools;
import cetus.analysis.Reduction;
import cetus.analysis.DDGraph.Arc;
import cetus.analysis.loopTools.LoopInstructionsUtils;
import cetus.codegen.CodeGenPass;
import cetus.codegen.ompGen;
import cetus.exec.CommandLineOptionSet;
import cetus.exec.Driver;
import cetus.hir.Annotation;
import cetus.hir.ArrayAccess;
import cetus.hir.AssignmentExpression;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CodeAnnotation;
import cetus.hir.CompoundStatement;
import cetus.hir.DFIterator;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.FloatLiteral;
import cetus.hir.ForLoop;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.Literal;
import cetus.hir.Loop;
import cetus.hir.NameID;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Symbol;
import cetus.hir.SymbolTable;
import cetus.hir.Symbolic;
import cetus.hir.Traversable;
import cetus.transforms.TransformPass;
import cetus.transforms.tiling.TiledLoop;
import cetus.transforms.tiling.TilingUtils;
import cetus.transforms.tiling.pawTiling.logging.PawTilingFormatter;
import cetus.transforms.tiling.pawTiling.optimizer.VersionChooser;
import cetus.transforms.tiling.pawTiling.optimizer.providers.ComplexChooserProvider;
import cetus.transforms.tiling.pawTiling.optimizer.providers.NthGuidedChooserProvider;
import cetus.transforms.tiling.pawTiling.optimizer.providers.VersionChooserProvider;
import cetus.utils.ArrayUtils;
import cetus.utils.CacheUtils;
import cetus.utils.DataReuseAnalysisUtils;
import cetus.utils.VariableDeclarationUtils;
import cetus.utils.reuseAnalysis.algorithm.ReuseAnalyzer;
import cetus.utils.reuseAnalysis.algorithm.TemporalReuseAnalyzer;
import cetus.utils.reuseAnalysis.data.DataReuseAnalysis;

public class ParallelAwareTilingPass extends TransformPass {

    public final static String PAW_TILING = "paw-tiling";

    public final static int DISTRIBUTED_CACHE_OPTION = 1;
    public final static int NON_DISTRIBUTED_CACHE_OPTION = 0;

    public final static String CORES_PARAM_NAME = "cores";
    public final static String CACHE_PARAM_NAME = "cacheSizeInKiB";

    public final static int DEFAULT_PROCESSORS = 4;
    public final static int DEFAULT_CACHE_SIZE = 32 * 1024; // 32 KiBi = 32 * 1024 bits
    public final static int DEFAULT_CACHE_ALIGNMENT = 16; // common cache alignment

    public final static int MAX_ITERATIONS_TO_PARALLELIZE = 100000;
    public final static String BALANCED_TILE_SIZE_NAME = "balancedTileSize";

    public static final String NTH_ORDER_PARAM = "tiling-level";
    public static final String NO_PERFECT_NEST_FLAG = "paw-tiling-no-perfect-nest";
    public static final int DEFAULT_NTH_ORDER = -1;

    private Logger logger = Logger.getLogger("PawTiling");

    private int numOfProcessors = DEFAULT_PROCESSORS;
    private int cacheSizeInKB = DEFAULT_CACHE_SIZE;
    private int cacheAlignment = DEFAULT_CACHE_ALIGNMENT;

    private List<Loop> selectedOutermostLoops;

    private PawAnalysisUtils pawAnalysisUtils = new PawAnalysisUtils();

    private ReuseAnalyzer reuseAnalyzer;

    private VersionChooserProvider chooserProvider;

    public ParallelAwareTilingPass(Program program) {
        this(program, null);
    }

    public ParallelAwareTilingPass(Program program, CommandLineOptionSet commandLineOptions) {
        this(program, commandLineOptions, new TemporalReuseAnalyzer());
    }

    public ParallelAwareTilingPass(Program program, CommandLineOptionSet commandLineOptions,
            ReuseAnalyzer reuseAnalysisFactory) {

        this(program, commandLineOptions, reuseAnalysisFactory,
                commandLineOptions.getValue(PAW_TILING) != null && commandLineOptions.getValue(PAW_TILING).equals("1")
                        ? new ComplexChooserProvider()
                        : new NthGuidedChooserProvider(commandLineOptions));
    }

    public ParallelAwareTilingPass(Program program, CommandLineOptionSet commandLineOptions,
            ReuseAnalyzer reuseAnalysisFactory, VersionChooserProvider chooserFactory) {
        super(program);

        if (commandLineOptions.getValue("verbosity").equals("1")) {
            pawAnalysisUtils.verbosity = true;
            setLogger(true);
        } else {
            setLogger(false);
        }

        if (chooserFactory instanceof ComplexChooserProvider) {
            logger.info("Complex provider selected");
        } else {
            logger.info("Depth-first provider selected");
        }

        this.reuseAnalyzer = reuseAnalysisFactory;
        this.chooserProvider = chooserFactory;

        try {
            numOfProcessors = Integer.parseInt(commandLineOptions.getValue(CORES_PARAM_NAME));
            assert numOfProcessors > 0;
        } catch (Exception e) {
            logger.warning(
                    "Error on setting num of processors. The default value: " + DEFAULT_PROCESSORS + " will be used");
        }

        try {
            cacheSizeInKB = Integer.parseInt(commandLineOptions.getValue(CACHE_PARAM_NAME));
            assert cacheSizeInKB > 0;
        } catch (Exception e) {
            logger.warning(
                    "Error on setting cache size. The default value: " + DEFAULT_CACHE_SIZE + " will be used");
        }

    }

    private void setLogger(boolean verbosity) {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new PawTilingFormatter());

        logger.addHandler(consoleHandler);
        logger.setUseParentHandlers(true);

        if (verbosity) {
            logger.setLevel(Level.ALL);
            logger.log(Level.INFO, "VERBOSITY_SET=ALL");
        } else {
            logger.setLevel(Level.INFO);
        }

    }

    @Override
    public String getPassName() {
        return "[PawTiling]";
    }

    @Override
    public void start() {

        prepare();

        List<Loop> outermostLoops = LoopTools.getOutermostLoops(program);

        List<Loop> perfectLoops = pawAnalysisUtils.filterValidLoops(outermostLoops);
        this.selectedOutermostLoops = perfectLoops;

        logger.log(Level.FINE, "#### Selected loops: " + selectedOutermostLoops.size() + "\n");
        for (Loop outermostLoop : selectedOutermostLoops) {
            logger.log(Level.FINE, outermostLoop + "\n");
        }
        logger.log(Level.FINE, "#### END Selected loops");

        logger.log(Level.FINE, pawAnalysisUtils.toString());

        // In case of rollback
        List<Loop> originalLoopNestCopy = new ArrayList<>();
        outermostLoops.forEach(loop -> {
            originalLoopNestCopy.add(((ForLoop) loop).clone(false));
        });

        if (selectedOutermostLoops.size() == 0) {
            return;
        }

        for (Loop outermostLoop : selectedOutermostLoops) {
            try {

                Expression cores = createCoresVariable(
                        VariableDeclarationUtils
                                .getVariableDeclarationSpace(outermostLoop.getParent()));

                // Expression cache = createCacheVariable(
                // VariableDeclarationUtils
                // .getVariableDeclarationSpace(outermostLoop.getParent()));

                runPawTiling((ForLoop) outermostLoop, cores, cacheSizeInKB);
            } catch (Exception e) {
                logger.log(Level.SEVERE, " ----");
                logger.log(Level.SEVERE, "Error trying to run paw tiling");
                logger.log(Level.SEVERE, "Loop:");
                logger.log(Level.SEVERE, outermostLoop.toString());
                logger.log(Level.SEVERE, "ERROR", e);
                logger.log(Level.SEVERE, " ---- ");
            }
        }

        reRunPasses();

    }

    private void prepare() {
        // TransformPass.run(new LoopInterchange(program));
    }

    public void reRunPasses() {

        // TODO: need to change the way of doing things. First I need to
        // run everything when I just have the tiled version in the program
        // once the tiled version is parallelized I can add the if statement
        // with the default version.!!!!!!!!!
        // #WARNING:

        LoopTools.addLoopName(program, true);

        AnalysisPass.run(new DDTDriver(program));
        AnalysisPass.run(new ArrayPrivatization(program));

        try {
            AnalysisPass.run(new Reduction(program));

        } catch (Exception e) {
            logger.severe(e.getMessage());
            // e.printStackTrace(logger);
        }

        AnalysisPass.run(new LoopParallelizationPass(program));

        String profitableOmpCopy = Driver.getOptionValue("profitable-omp");

        Driver.setOptionValue("profitable-omp", "0");
        CodeGenPass.run(new ompGen(program));

        Driver.setOptionValue("profitable-omp", profitableOmpCopy);

    }

    private void runPawTiling(ForLoop loopNest, Expression cores, int cacheSizeInKB) throws Exception {

        DataReuseAnalysis reuseAnalysis = null;

        reuseAnalysis = runReuseAnalysis(loopNest);

        if (!hasReuse(reuseAnalysis)) {
            return;
        }

        LinkedList<Loop> nestedLoops = new LinkedList<>();
        new DFIterator<Loop>(loopNest, Loop.class).forEachRemaining(nestedLoops::add);

        DDGraph subGraph = program.getDDGraph().getSubGraph(loopNest);
        List<Arc> loopCarriedArcs = subGraph.getLoopCarriedDependencesForGraph();
        List<Arc> allArcs = subGraph.getAllArcs();

        List<DependenceVector> dependenceVectors = program.getDDGraph().getDirectionMatrix(nestedLoops);

        logger.log(Level.FINE, "### ORIGINAL VERSION ###");
        logger.log(Level.FINE, loopNest.toString());

        logger.log(Level.FINE, ("#### ORIGINAL DVs ####"));
        logger.log(Level.FINE, dependenceVectors.toString());
        logger.log(Level.FINE, "#### ORIGINAL DVs END ####");

        logger.log(Level.FINE, "### ORIGINAL VERSION END ###");

        Expression totalOfInstructions = LoopInstructionsUtils.getTotalOfInstructions(loopNest);

        long cacheInElements = getCacheInElementsTerm(loopNest, cacheSizeInKB);

        Expression rowSize = computeRowDataSize(loopNest);
        Expression dataFullSize = computeDataSize(loopNest);

        Expression rawTileSize = computeRawTileSize(rowSize, cacheInElements);

        long totalIns = ((IntegerLiteral) totalOfInstructions).getValue();
        long rawTile = ((IntegerLiteral) rawTileSize).getValue();
        long balancedTileSize = computeBalancedCrossStripSize(totalIns, rawTile, numOfProcessors);

        SymbolTable variableDeclarations = new CompoundStatement();
        variableDeclarations = VariableDeclarationUtils.getVariableDeclarationSpace(loopNest.getParent());

        setTileSizeVar(variableDeclarations, balancedTileSize, loopNest);

        VersionChooser optimizer = chooserProvider.chooseOptimalVersion(variableDeclarations, loopNest,
                dependenceVectors, reuseAnalysis, balancedTileSize);

        TiledLoop leastCostTiledVersion = optimizer.getChoosenVersion();

        logger.log(Level.FINE, "### TILED VERSION SELECTION ###");
        logger.log(Level.FINE, leastCostTiledVersion.toString());
        logger.log(Level.FINE, "### SELECTED VERSION ###");

        replaceTileSize(variableDeclarations, balancedTileSize);

        // create two versions if iterations isn't enough to parallelize

        ForLoop newLoopNest = loopNest.clone(false);

        logger.log(Level.FINE, "DATA_FULL_SIZE_IN_KB", rowSize.toString());

        Expression cacheValueInElements = new IntegerLiteral(cacheInElements);
        // cacheValue = VariableDeclarationUtils.getVariableDeclaredValue(
        // VariableDeclarationUtils.getVariableDeclarationSpace(loopNest),
        // (IDExpression) cache);

        Statement twoVersionsStm = createTwoVersionsStm(totalOfInstructions, cacheValueInElements, dataFullSize,
                newLoopNest,
                leastCostTiledVersion);

        replaceLoop(loopNest, twoVersionsStm);
        Traversable targetSymbolTable = VariableDeclarationUtils
                .getVariableDeclarationSpace(twoVersionsStm.getParent());
        if (twoVersionsStm instanceof IfStatement) {
            targetSymbolTable = ((IfStatement) twoVersionsStm).getElseStatement();
        }

        // VariableDeclarationUtils.addNewVariableDeclarations(targetSymbolTable,
        // filterValidDeclarations(variableDeclarations, leastCostTiledVersion));

        logger.log(Level.FINE, "### Updated attributes ###");

    }

    private void setTileSizeVar(SymbolTable variableDeclarations, long balancedTileSize, ForLoop loop) {
        IDExpression tileSizeVar = VariableDeclarationUtils.getIdentifier(variableDeclarations,
                BALANCED_TILE_SIZE_NAME);
        if (tileSizeVar == null) {
            tileSizeVar = VariableDeclarationUtils.declareVariable(variableDeclarations,
                    BALANCED_TILE_SIZE_NAME,
                    new IntegerLiteral(balancedTileSize));
        }

        String loopName = LoopTools.getLoopName(loop);
        int indexOfHash = loopName.indexOf("#");
        String loopNumberStr = "-1";
        if (indexOfHash != -1) {
            loopNumberStr = "" + loopName.charAt(indexOfHash + 1);
        }

        if (loopNumberStr.equals("-1")) {
            return;
        }

        if (loopNumberStr.equals("0")) {
            return;
        }
        CodeAnnotation annotation = new CodeAnnotation(String.format("%s=%d;", tileSizeVar, balancedTileSize));
        loop.annotateBefore(annotation);

    }

    private Expression createCoresVariable(SymbolTable variableDeclarations) {
        Identifier coresIdentifier = VariableDeclarationUtils.declareVariable(variableDeclarations, CORES_PARAM_NAME,
                new IntegerLiteral(numOfProcessors));

        return ((Expression) coresIdentifier);
    }

    private boolean hasReuse(DataReuseAnalysis reuseAnalysis) {

        boolean isReusable = true;

        isReusable &= reuseAnalysis != null
                && reuseAnalysis.hasReuse();

        return isReusable;
    }

    private Statement createTwoVersionsStm(Expression maxOfInstructions, Expression cache, Expression dataFullSize,
            Statement noTiledCode, Statement tiledCode) {

        Expression instructionsCondition = new BinaryExpression(maxOfInstructions.clone(), BinaryOperator.COMPARE_LE,
                new IntegerLiteral(MAX_ITERATIONS_TO_PARALLELIZE));
        Expression cacheCond = new BinaryExpression(cache.clone(), BinaryOperator.COMPARE_GT, dataFullSize.clone());
        Expression condition = new BinaryExpression(instructionsCondition.clone(), BinaryOperator.LOGICAL_AND,
                cacheCond.clone());

        CompoundStatement leastCostVersionStm = new CompoundStatement();
        leastCostVersionStm.addStatement(tiledCode);

        if (!(maxOfInstructions instanceof Literal)
                || !(cache instanceof Literal)
                || !(dataFullSize instanceof Literal)) {
            IfStatement ifStm = new IfStatement(condition, noTiledCode, leastCostVersionStm);
            return ifStm;

        } else {
            boolean isProfitableParallelIterations = false;
            boolean isEnoughCache = false;
            if (maxOfInstructions instanceof IntegerLiteral) {
                long inst = ((IntegerLiteral) maxOfInstructions).getValue();
                isProfitableParallelIterations = inst >= MAX_ITERATIONS_TO_PARALLELIZE;
            } else if (maxOfInstructions instanceof FloatLiteral) {
                double inst = ((FloatLiteral) maxOfInstructions).getValue();
                isProfitableParallelIterations = inst <= MAX_ITERATIONS_TO_PARALLELIZE;
            }

            long cacheSize = ((IntegerLiteral) cache).getValue();

            if (dataFullSize instanceof IntegerLiteral) {
                long fullSizeData = ((IntegerLiteral) dataFullSize).getValue();
                isEnoughCache = cacheSize > fullSizeData;

            } else if (dataFullSize instanceof FloatLiteral) {
                double fullSizeData = ((FloatLiteral) dataFullSize).getValue();
                isEnoughCache = cacheSize > fullSizeData;

            }
            if (isProfitableParallelIterations && isEnoughCache) {
                return tiledCode.clone();
            } else {
                return noTiledCode.clone();
            }
        }
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
            long newTileSize) {

        Identifier balancedTileVariable = VariableDeclarationUtils.declareVariable(variableDeclarationSpace,
                BALANCED_TILE_SIZE_NAME,
                new IntegerLiteral(newTileSize));

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

    // TODO: INtegrate
    private Expression computeRawTileSize(Expression matrixSize, long cacheInElements) {
        if (matrixSize instanceof IntegerLiteral) {
            long N = ((IntegerLiteral) matrixSize).getValue();
            return new IntegerLiteral(TilingUtils.findLRWBlock(N, cacheInElements));

        }
        return new IntegerLiteral("-1");

    }

    // TODO: INtegrate
    private Expression computeBalancedCrossStripSize(Expression numOfInstructions, Expression numOfProcessors,
            Expression rawTileSize) {

        // (numOfInstructions / (processors * rawSize)) * processors
        Expression divisor = Symbolic.multiply(
                Symbolic.divide(numOfInstructions, Symbolic.multiply(numOfProcessors, rawTileSize)),
                numOfProcessors);

        // instructions / ( ( instructions / (processors * rawSize) ) * processors )
        Expression strip = Symbolic.divide(numOfInstructions, divisor);
        return strip;
    }

    private long computeBalancedCrossStripSize(long totalInstructions, long rawTileSize,
            long numOfProcessors) {

        // Making rawTile size multiple of num of processors
        long balancingTerm = (long) Math.ceil(totalInstructions / (numOfProcessors * rawTileSize));
        long tileSize = (totalInstructions) / (balancingTerm * numOfProcessors);
        return tileSize;
    }

    private Expression computeDataSize(Loop loop) {
        List<ArrayAccess> arrayAccesses = new ArrayList<>();
        new DFIterator<ArrayAccess>(loop, ArrayAccess.class).forEachRemaining(arrayAccesses::add);

        return ArrayUtils.getFullSize(VariableDeclarationUtils.getVariableDeclarationSpace(loop.getParent()),
                arrayAccesses);
    }

    /**
     * Compute the data full size in elements
     * 
     * @param loop the loop where to obtain the array accesses to calculate the
     *             block size
     * @return the block size in bits to use from the cache
     */
    private Expression computeRowDataSize(Loop loop) {

        // // Accessing only using loop bounds
        // Expression upperBound = LoopTools.getUpperBoundExpression(loop);

        // return Symbolic.multiply(upperBound, new IntegerLiteral(cacheAlignment));

        // Full matrices
        List<ArrayAccess> arrayAccesses = new ArrayList<>();

        Set<String> alreadyAdded = new HashSet<>();
        new DFIterator<ArrayAccess>(loop, ArrayAccess.class).forEachRemaining(access -> {
            String accessName = access.getArrayName().toString();
            if (!alreadyAdded.contains(accessName)) {
                arrayAccesses.add(access);
                alreadyAdded.add(accessName);

            }
        });

        // return CacheUtils.getRawBlockSize(cache, arrayAccesses);
        return ArrayUtils.getMaxSize(VariableDeclarationUtils.getVariableDeclarationSpace(loop.getParent()),
                arrayAccesses);

    }

    private long getCacheInElementsTerm(Loop loop, int cache) {
        List<ArrayAccess> arrayAccesses = new ArrayList<>();
        new DFIterator<ArrayAccess>(loop, ArrayAccess.class).forEachRemaining(arrayAccesses::add);

        return CacheUtils.getCacheInArrayElements(cache, arrayAccesses);
    }

    private DataReuseAnalysis runReuseAnalysis(Loop loopNest) {

        DataReuseAnalysis reuseAnalysis = reuseAnalyzer.getReuseAnalysis(loopNest);
        List<Expression> memoryOrder = reuseAnalysis.getLoopNestMemoryOrder();

        logger.log(Level.FINE, "### REUSE ANALYSIS ###");

        logger.log(Level.FINE, "#### LOOP COSTS:");
        logger.log(Level.FINE, DataReuseAnalysisUtils.printLoopCosts(reuseAnalysis.getLoopCosts()));
        logger.log(Level.FINE, "#### LOOP COSTS END");

        logger.log(Level.FINE, "#### Memory order:");

        for (int i = 0; i < memoryOrder.size(); i++) {
            Expression expr = memoryOrder.get(i);
            logger.log(Level.FINE, expr + "\n");
        }

        logger.log(Level.FINE, "#### Memory order END");
        logger.log(Level.FINE, "### REUSABILITY END ###");

        return reuseAnalysis;

    }

    private void cloneCodeAnnotations(ForLoop originalLoop, Statement newLoop) {
        List<Annotation> annotations = originalLoop.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof CodeAnnotation) {
                newLoop.annotateBefore(annotation.clone());
            }
        }
    }

    private void replaceLoop(ForLoop originalLoop, Statement newLoop) {
        Traversable originalParent = originalLoop.getParent();

        int originalLoopIdx = -1;
        for (int i = 0; i < originalParent.getChildren().size(); i++) {
            if (originalParent.getChildren().get(i) == originalLoop) {
                originalLoopIdx = i;
                break;
            }
        }

        cloneCodeAnnotations(originalLoop, newLoop);

        originalParent.setChild(originalLoopIdx, newLoop);
    }

}
