package omp2gpu.codegen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import cetus.codegen.*;
import cetus.hir.*;
import cetus.exec.*;
import cetus.analysis.AnalysisPass;
import cetus.transforms.*;
import omp2gpu.analysis.*;
import omp2gpu.hir.CudaAnnotation;
import omp2gpu.transforms.O2GTranslator;
import omp2gpu.transforms.SplitOmpPRegion;
import omp2gpu.transforms.CudaAnnotationParser;
import omp2gpu.transforms.CudaNormalizeReturn;
import omp2gpu.transforms.LoopCollapse;
import omp2gpu.transforms.ParallelLoopSwap;
import omp2gpu.transforms.TransformTools;
import omp2gpu.transforms.ConvertStatic2Global;
import omp2gpu.transforms.KernelCallingProcCloning;
import omp2gpu.transforms.UEPRemoval;
import omp2gpu.transforms.OmpCollapse;

/**
 * <b>omp2gpu</b> converts an OpenMP program into a CUDA GPU program
 * 
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 */
public class omp2gpu extends CodeGenPass
{
	private boolean OmpAnalysisOnly = false;
	private boolean kernelSplitOnly = false;
	private boolean ParallelLoopSwap = false;
	private boolean LocalityAnalysis = false;
	private boolean useGlobalGMalloc = false;
	private boolean globalGMallocOpt = false;
	private boolean addSafetyCheckingCode = false;
	private boolean showExtraOmpClauses = false;
	private boolean convertStatic2Global = false;
	private boolean doNotRemoveUnusedSymbols = false;
	private boolean kernelCallingProcCloning = false;
	private boolean MemTrOptOnLoops = false;
	private int MemTrOptLevel = 2;
	private int MallocOptLevel = 0;
	private int tuningLevel = 1;
	private int UEPRemovalOptLevel = 0;
	private HashMap<String, HashMap<String, Object>> userDirectives;
	private HashMap<String, Object> tuningConfigs;
	private String tuningParamFile= null;
	private String tuningConfDir = null;
	
	public omp2gpu(Program program, HashMap<String, HashMap<String, Object>> uDirectives,
			HashMap<String, Object> tConfigs)
	{
		super(program);
		userDirectives = uDirectives;
		tuningConfigs = tConfigs;
	}

	public String getPassName()
	{
		return new String("[omp2gpu]");
	}

	public void start()
	{

		/////////////////////////////////////////////////////////////////
		// Read command-line options and set corresponding parameters. //
		/////////////////////////////////////////////////////////////////
		String value = Driver.getOptionValue("OmpAnalysisOnly");
		if( value != null ) {
			//OmpAnalysisOnly = Boolean.valueOf(value).booleanValue();
			OmpAnalysisOnly = true;
		}
		value = Driver.getOptionValue("OmpKernelSplitOnly");
		if( value != null ) {
			//kernelSplitOnly = Boolean.valueOf(value).booleanValue();
			kernelSplitOnly = true;
		}
		value = Driver.getOptionValue("useParallelLoopSwap");
		if( value != null ) {
			ParallelLoopSwap = true;
		}
		value = Driver.getOptionValue("cudaMemTrOptLevel");
		if( value != null ) {
			MemTrOptLevel = Integer.valueOf(value).intValue();
		}
		value = Driver.getOptionValue("cudaMallocOptLevel");
		if( value != null ) {
			MallocOptLevel = Integer.valueOf(value).intValue();
		}
		value = Driver.getOptionValue("shrdSclrCachingOnReg");
		if( value != null ) {
			LocalityAnalysis = true;
		}
		value = Driver.getOptionValue("shrdArryElmtCachingOnReg");
		if( value != null ) {
			LocalityAnalysis = true;
		}
		value = Driver.getOptionValue("shrdSclrCachingOnSM");
		if( value != null ) {
			LocalityAnalysis = true;
		}
		value = Driver.getOptionValue("prvtArryCachingOnSM");
		if( value != null ) {
			LocalityAnalysis = true;
		}
		value = Driver.getOptionValue("shrdArryCachingOnTM");
		if( value != null ) {
			LocalityAnalysis = true;
		}
		value = Driver.getOptionValue("shrdSclrCachingOnConst");
		if( value != null ) {
			LocalityAnalysis = true;
		}
		value = Driver.getOptionValue("shrdArryCachingOnConst");
		if( value != null ) {
			LocalityAnalysis = true;
		}
		value = Driver.getOptionValue("useGlobalGMalloc");
		if( value != null ) {
			useGlobalGMalloc = true;
		}
		value = Driver.getOptionValue("globalGMallocOpt");
		if( value != null ) {
			globalGMallocOpt = true;
		}
		value = Driver.getOptionValue("genTuningConfFiles");
		if( value != null ) {
			if( value.equals("1") ) {
				PrintTools.println("[INFO] directory to store the generated Tuning configuration files is not specified;" +
						" default directory (tuning_conf) will be used.", 0);
				tuningConfDir="tuning_conf";
			} else {
				tuningConfDir=value;
			}
		}
		value = Driver.getOptionValue("extractTuningParameters");
		if( value != null ) {
			if( value.equals("1") ) {
				PrintTools.println("[INFO] file to store the extracted Tuning parameters is not specified;" +
						" default file name (TuningOptions.txt) will be used.", 0);
				tuningParamFile="TuningOptions.txt";
			} else {
				tuningParamFile=value;
			}
			//Enable passes needed to extract tuning parameters.
			LocalityAnalysis = true;
			ParallelLoopSwap = true;
		}
		value = Driver.getOptionValue("tuningLevel");
		if( value != null ) {
			tuningLevel = Integer.valueOf(value).intValue();
		}
		value = Driver.getOptionValue("addSafetyCheckingCode");
		if( value != null ) {
			addSafetyCheckingCode = true;
		}
		
		value = Driver.getOptionValue("showExtraOmpClauses");
		if( value != null ) {
			showExtraOmpClauses = true;
		}
		
		value = Driver.getOptionValue("convertStatic2Global");
		if( value != null ) {
				convertStatic2Global = true;
		}
		
		value = Driver.getOptionValue("doNotRemoveUnusedSymbols");
		if( value != null ) {
			doNotRemoveUnusedSymbols = true;
		}
		value = Driver.getOptionValue("UEPRemovalOptLevel");
		if( value != null ) {
			UEPRemovalOptLevel = Integer.valueOf(value).intValue();
		}
		value = Driver.getOptionValue("MemTrOptOnLoops");
		if( value != null ) {
			MemTrOptOnLoops = true;
		}
		///////////////////////////////////////////////////////////////////////////
		// DEBUG: For now, if both useGlobalGMalloc and globalGMallocOpt are on, //
		// the following two options are turned on too.                          //
		///////////////////////////////////////////////////////////////////////////
		if( useGlobalGMalloc && globalGMallocOpt ) {
			convertStatic2Global = true;
			kernelCallingProcCloning = true;
		}
		
		if( MemTrOptOnLoops ) {
			kernelCallingProcCloning = true;
		}
		
		/**************************************************************/
		/* cetus.transforms.AnnotationParser stores a Cuda annotation */
		/* as stand-alone PragmaAnnotation in an AnnotationStatement. */
		/* CudaAnnotationParser() converts this to CudaAnnotation and */
		/* attach it to the next annotatable object.                  */
		/**************************************************************/
		TransformPass.run(new CudaAnnotationParser(program));
		
		/* "int x, y,z;" becomes "int x; int y; int z;" */
		TransformPass.run(new SingleDeclarator(program));

		/* 
		 * Update Symbol links of each IDExpression. 
		 * SingleDeclarator modifies some Symbols, and thus affected
		 * IDExpression should be updated.
		 */
		SymbolTools.linkSymbol(program);
		
		TransformPass.run(new CudaNormalizeReturn(program));
		
		if( convertStatic2Global ) {
			TransformPass.run(new ConvertStatic2Global(program));
		}
		
		/////////////////////////////////////////////////////
		//Collapse omp-for loops with collapse(n) clauses. //
		/////////////////////////////////////////////////////
		TransformPass.run(new OmpCollapse(program));

		/* First, call OpenMP analysis pass to generate OpenMP annotations */
		AnalysisPass.run(new OmpAnalysis(program));
		
		if( OmpAnalysisOnly ) {
			////////////////////////////////////////////////
			// Clean extra Omp clauses to make the output //
			// conform to the OpenMP specification.       //
			////////////////////////////////////////////////
			if( !showExtraOmpClauses ) {
				SplitOmpPRegion.cleanExtraOmpClauses(program);
			}
			//////////////////////////////
			// Clean empty Omp clauses. //
			//////////////////////////////
			SplitOmpPRegion.cleanEmptyOmpClauses(program);
			return;
		}
		
		/////////////////////////////////////////////////////////
		// Wrap omp-parallel-for loops with CompoundStatements //
		// if the loop contains reduction clauses.             //
		/////////////////////////////////////////////////////////
		TransformTools.wrapOmpParallelForLoops(program);
		
		////////////////////////////////////////////////////
		// Split OpenMP parallel regions into sub-regions //
		// at every synchronization points.               //
		////////////////////////////////////////////////////
		TransformPass.run(new SplitOmpPRegion(program));
		
		if( kernelSplitOnly ) {
			////////////////////////////////////////////////
			// Clean extra Omp clauses to make the output //
			// conform to the OpenMP specification.       //
			////////////////////////////////////////////////
			if( showExtraOmpClauses ) {
				SplitOmpPRegion.cleanExtraBarriers(program, true);
			} else {
				SplitOmpPRegion.cleanExtraOmpClauses(program);
			}
			SplitOmpPRegion.addNowaitToOmpForLoops(program);
			////////////////////////////////////////////////////////////////////////
			// Apply transformation to remove upwardly-exposed private variables. //
			////////////////////////////////////////////////////////////////////////
			if( UEPRemovalOptLevel > 0 ) {
				TransformPass.run(new UEPRemoval(program));
			}
			///////////////////////////////////////////////////////////////////////////////
			// Clone procedures containing kernel regions and are called more than once. //
			// This cloning is needed for correct global optimizations.                  //
			///////////////////////////////////////////////////////////////////////////////
			if( kernelCallingProcCloning ) {
				TransformPass.run(new KernelCallingProcCloning(program));
			}
			////////////////////////////////////////////////////////////////
			// If GPU variables are globally allocated, the following     //
			// analysis computes resident GPU variables interprocedurally //
			// to reduce redundant cudaMalloc() and CPU-to-GPU memory     //
			// transfers.                                                 //
			////////////////////////////////////////////////////////////////
			if( globalGMallocOpt ) {
				AnalysisPass.run(new IpResidentGVariableAnalysis(program));
				AnalysisPass.run(new IpG2CMemTrAnalysis(program));
			}
			if( kernelCallingProcCloning ) {
				PrintTools.println("[removeUnusedProcedures] begin", 0);
				TransformTools.removeUnusedProcedures(program);
				PrintTools.println("[removeUnusedProcedures] end", 0);
			}
			//////////////////////////////////////////////////////////////
			// Annotate each kernel region with parent procedure name   //
			// and kernel id, and apply user directives if existing.    //
			//////////////////////////////////////////////////////////////
			AnalysisTools.annotateUserDirectives(program, userDirectives);
			////////////////////////////////////////////
			// Apply parallel loop swap optimization. //
			////////////////////////////////////////////
			if( ParallelLoopSwap ) {
				TransformPass.run(new ParallelLoopSwap(program));
			}
			/////////////////////////////////////////////////////
			// Analyze locality of shared variables to exploit //
			// GPU registers and shared memory as caches.      //
			/////////////////////////////////////////////////////
			if( LocalityAnalysis ) {
				AnalysisPass.run(new LocalityAnalysis(program));
			}
			//////////////////////////////////////////////
			// Intraprocedural Cuda Malloc optimization //
			//////////////////////////////////////////////
			if( MallocOptLevel > 0 ) {
				AnalysisPass.run(new CudaMallocAnalysis(program));
			}
			////////////////////////////////////////////////////////
			// Intraprocedural CPU-to-GPU memory transfer         //
			// analysis to identify unnecessary memory transfers. //
			////////////////////////////////////////////////////////
			if( MemTrOptLevel > 1 ) {
				AnalysisPass.run(new MemTrAnalysis(program));
			}
			/////////////////////////////////
			// Extract tunable parameters. //
			/////////////////////////////////
			if( tuningParamFile != null ) {
				List<HashMap> TuningOptions = storeTuningParameters();
				if( tuningConfDir != null ) {
					double timer = Tools.getTime();
					PrintTools.println("[genTuningConf] begin", 0);
					if( tuningLevel == 1 ) {
						genTuningConfs1(TuningOptions);
					} else {
						genTuningConfs2(TuningOptions);
					}
					PrintTools.println("[genTuningConf] end in " +
							String.format("%.2f seconds", Tools.getTime(timer)), 0);
				}
			}
			return;
		} else {
			//////////////////////////////
			// Clean extra Omp barriers //
			//////////////////////////////
			SplitOmpPRegion.cleanExtraBarriers(program, false);
			////////////////////////////////////////////////////////////////////////
			// Apply transformation to remove upwardly-exposed private variables. //
			////////////////////////////////////////////////////////////////////////
			if( UEPRemovalOptLevel > 0 ) {
				TransformPass.run(new UEPRemoval(program));
			}
			///////////////////////////////////////////////////////////////////////////////
			// Clone procedures containing kernel regions and are called more than once. //
			// This cloning is needed for correct global optimizations.                  //
			///////////////////////////////////////////////////////////////////////////////
			if( kernelCallingProcCloning ) {
				TransformPass.run(new KernelCallingProcCloning(program));
			}
			////////////////////////////////////////////////////////////////
			// If GPU variables are globally allocated, the following     //
			// analysis computes resident GPU variables interprocedurally //
			// to reduce redundant cudaMalloc() and CPU-to-GPU memory     //
			// transfers.                                                 //
			////////////////////////////////////////////////////////////////
			if( globalGMallocOpt ) {
				AnalysisPass.run(new IpResidentGVariableAnalysis(program));
				AnalysisPass.run(new IpG2CMemTrAnalysis(program));
			}
			if( kernelCallingProcCloning ) {
				PrintTools.println("[removeUnusedProcedures] begin", 0);
				TransformTools.removeUnusedProcedures(program);
				PrintTools.println("[removeUnusedProcedures] end", 0);
			}
			//////////////////////////////////////////////////////////////
			// Annotate each kernel region with parent procedure name   //
			// and kernel id, and apply user directives if existing.    //
			//////////////////////////////////////////////////////////////
			AnalysisTools.annotateUserDirectives(program, userDirectives);
			////////////////////////////////////////////
			// Apply parallel loop swap optimization. //
			////////////////////////////////////////////
			if( ParallelLoopSwap ) {
				TransformPass.run(new ParallelLoopSwap(program));
			}
			/////////////////////////////////////////////////////
			// Analyze locality of shared variables to exploit //
			// GPU registers and shared memory as caches.      //
			/////////////////////////////////////////////////////
			if( LocalityAnalysis ) {
				AnalysisPass.run(new LocalityAnalysis(program));
			}
			//////////////////////////////////////////////
			// Intraprocedural Cuda Malloc optimization //
			//////////////////////////////////////////////
			if( MallocOptLevel > 0 ) {
				AnalysisPass.run(new CudaMallocAnalysis(program));
			}
			////////////////////////////////////////////////////////
			// Intraprocedural CPU-to-GPU memory transfer         //
			// analysis to identify unnecessary memory transfers. //
			///////////////////////////////////////////////////////////////
			// FIXME: if MemTrOPtLevel <= 1, MemTrAnalysis will not      //
			// be executed. In this case, if the same variable is        //
			// passed for two separate parameters and one is R/O and     //
			// the other is R/W, R/W data should be transfered back      //
			// to CPU later than R/O one. However, current O2GTranslator //
			// does not consider the transfer order; possible wrong      //
			// output.                                                   //
			///////////////////////////////////////////////////////////////
			if( MemTrOptLevel > 1 ) {
				AnalysisPass.run(new MemTrAnalysis(program));
			}
			/////////////////////////////////
			// Extract tunable parameters. //
			/////////////////////////////////
			if( tuningParamFile != null ) {
				List<HashMap> TuningOptions = storeTuningParameters();
				if( tuningConfDir != null ) {
					double timer = Tools.getTime();
					PrintTools.println("[genTuningConf] begin", 0);
					if( tuningLevel == 1 ) {
						genTuningConfs1(TuningOptions);
					} else {
						genTuningConfs2(TuningOptions);
					}
					PrintTools.println("[genTuningConf] end in " +
							String.format("%.2f seconds", Tools.getTime(timer)), 0);
				}
			}
			///////////////////////////////////////////////
			// Do the actual OpenMP-to-CUDA translation. //
			///////////////////////////////////////////////
			PrintTools.println("[O2GTranslator] begin", 0);
			O2GTranslator.CUDAInitializer(program);

			//////////////////////////////////////////////////
			// extract OpenMP Parallel Loops into functions //
			//////////////////////////////////////////////////
			O2GTranslator.convParRegionToKernelFunc();
			PrintTools.println("[O2GTranslator] end", 0);
			
			///////////////////////////////////////////////////////////////////////////////
			// After kernel transformation, original functions called in a kernel region //
			// may not be used; delete these unused functions.                           //
			///////////////////////////////////////////////////////////////////////////////
			PrintTools.println("[removeUnusedProcedures] begin", 0);
			TransformTools.removeUnusedProcedures(program);
			PrintTools.println("[removeUnusedProcedures] end", 0);
			
			// Update symbol pointers again. 
			SymbolTools.linkSymbol(program);
			
			///////////////////////////////////////////////////////////////////////////////
			// After kernel transformation, kernel-containing procedures may have unused //
			// symbols; delete these unused symbols.                                     //
			///////////////////////////////////////////////////////////////////////////////
			if( !doNotRemoveUnusedSymbols ) {
				PrintTools.println("[removeUnusedSymbols] begin", 0);
				TransformTools.removeUnusedSymbols(program);
				PrintTools.println("[removeUnusedSymbols] end", 0);
			}

			/////////////////////////////////////////////////////////////////////////
			// Check whether a kernel function or a split parallel region contains //
			// upward-exposed private variables.                                   //
			/////////////////////////////////////////////////////////////////////////
			AnalysisPass.run( new UEPrivateAnalysis(program) );
			
			//////////////////////////////////////////////////////////////////////////////
			// Check whether a CUDA kernel function calls C standard library functions  //
			// that are not supported by CUDA runtime systems.                          //
			// If so, CUDA compiler will fail if they are not inlinable.                //
			//////////////////////////////////////////////////////////////////////////////
			PrintTools.println("[CheckKernelFunctions] begin", 0);
			AnalysisTools.checkKernelFunctions(program);
			PrintTools.println("[CheckKernelFunctions] end", 0);
			
			//////////////////////////////
			// Clean extra Omp barriers //
			//////////////////////////////
			SplitOmpPRegion.cleanExtraBarriers(program, true);
			
			//////////////////////////////////////////////
			// Rename output filenames from *.c to *.cu //
			//////////////////////////////////////////////
			renameOutputFiles(program);
		}
	}
	
	private void renameOutputFiles(Program program ) {
		for ( Traversable tt : program.getChildren() )
		{
			TranslationUnit tu = (TranslationUnit)tt;
			//DEBUG: tu.getInputFilename() may include path name, but default TranslationUnit.output_filename
			//does not have path name.
			//String iFileName = tu.getInputFilename();
			String iFileName = tu.getOutputFilename();
			int dot = iFileName.lastIndexOf(".c");
			if( dot == -1 ) {
				dot = iFileName.lastIndexOf(".h");
				if( dot != -1 ) {
					PrintTools.println("[WARNING] Translating a header file , " + iFileName + 
							", makes all macros in the header file inlined with values defined in this O2G tranlsation, " + 
							"which will cause problems if NVCC compiles the output codes with different macro definitions or " +
							"may cause redundant including of this header file.", 0);
				} else {
					PrintTools.println("[WARNING] Input file name, " + iFileName + 
							", does not end with C suffix (.c); " +
							"translated output file may behave incorrectly if macros used in " +
							"this O2G translation is different from the ones that will be used in NVCC.", 0);
				}
				continue;
			} else {
				String suffix = iFileName.substring(dot);
				if( !suffix.equalsIgnoreCase(".cu") ) {
					String fNameStem = iFileName.substring(0, dot);
					//DEBUG: In the new TranslationUnit, output_filename is a file name without path name.
					//String oFileName = Driver.getOptionValue("outdir") + File.separatorChar + 
					String oFileName = fNameStem.concat(".cu");
					tu.setOutputFilename(oFileName);
				}
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	//Array of local caching-related parameters inserted by LocalityAnalysis. //
	////////////////////////////////////////////////////////////////////////////
	String[] cachingParams = {"registerRO", "registerRW", "sharedRO", "sharedRW", "texture",
			"ROShSclrNL", "ROShSclr", "RWShSclr", "ROShArEl", "RWShArEl", "RO1DShAr",
			"PrvAr", "SclrConst", "ArryConst", "constant"};
	String[] uDirectives = {"registerRO", "registerRW", "sharedRO", "sharedRW", "texture", "constant"};
	
	protected List<HashMap> storeTuningParameters() {
		/////////////////////////////////////////////////////////////////////////////////////////
		//Assume that AnalysisTools.annotateUserDirectives() annotates each kernel region with //
		//a CudaAnnotaion containing "ainfo" clause.                                           // 
		/////////////////////////////////////////////////////////////////////////////////////////
		List<CudaAnnotation> cAnnotList = IRTools.collectPragmas(program, CudaAnnotation.class, "ainfo");
		if( cAnnotList == null ) {
			PrintTools.println("[WARNING in storeTuningParameters()] couldn't find any kernel region " +
					"containing Cuda ainfo clause.",0);
			return null;
		}
		
		HashSet<String> gOptionSet1 = new HashSet<String>();
		HashSet<String> gOptionSet2 = new HashSet<String>();
		HashSet<String> gOptionSet3 = new HashSet<String>();
		HashSet<String> gOptionSet4 = new HashSet<String>();
		HashSet<String> gOptionSet5 = new HashSet<String>();
		HashSet<String> gOptionSet6 = new HashSet<String>();
		HashMap<String, HashSet<String>> gOptionMap = new HashMap<String, HashSet<String>>();
		/////////////////////////////////////////////////////////////////////////////////////////////////
		// DEBUG: equal() and hashCode() methods of all PragmaAnnotations use Java.utilMap.entrySet(), //
		// and thus if two PragmaAnnotations contains same string entrySets, both hashCode() will      //
		// return the same output. However, below kOptionMap is OK, since CudaAnnotations used for     //
		// keys are unique; each kernel region has a unique ainfo directive.                           //
		/////////////////////////////////////////////////////////////////////////////////////////////////
		HashMap<CudaAnnotation, HashMap<String, Object>> kOptionMap = 
			new HashMap<CudaAnnotation, HashMap<String, Object>>();
		List<HashMap> TuningOptions = new LinkedList<HashMap>();
		TuningOptions.add(gOptionMap);
		TuningOptions.add(kOptionMap);
		LoopCollapse lcHandler = new LoopCollapse(program);
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(tuningParamFile));
			///////////////////////////
			// Check global options. //
			///////////////////////////
			boolean MatrixTransposeApplicable = false;
			boolean MallocPitchApplicable = false;
			boolean LoopCollapseApplicable = false;
			boolean PLoopSwapApplicable = false;
			boolean UnrollingReductionApplicable = false;
			boolean ReductionPatternExists = false;
			boolean shrdSclrCachingOnReg = false;
			boolean shrdArryElmtCachingOnReg = false;
			boolean shrdSclrCachingOnSM = false;
			boolean prvtArryCachingOnSM = false;
			boolean shrdArryCachingOnTM = false;
			boolean shrdSclrCachingOnConst = false;
			boolean shrdArryCachingOnConst = false;
			for( CudaAnnotation cAnnot : cAnnotList ) {
				Annotatable at = cAnnot.getAnnotatable();
				OmpAnnotation oAnnot = at.getAnnotation(OmpAnnotation.class, "shared");
				// Check whether MallocPitch is applicable.
				Set<Symbol> sharedSyms = oAnnot.get("shared");
				if( (sharedSyms != null) && !MallocPitchApplicable ) {
					for( Symbol sym : sharedSyms ) {
						if(SymbolTools.isArray(sym)) {
							List aspecs = sym.getArraySpecifiers();
							ArraySpecifier aspec = (ArraySpecifier)aspecs.get(0);
							if( aspec.getNumDimensions() == 2 ) {
								MallocPitchApplicable = true;
								break;
							}
						}
					}
				}
				// Check whether MatrixTranspose is applicable.
				Set<Symbol> thPrivSyms = oAnnot.get("threadprivate");
				if( (thPrivSyms != null) && !MatrixTransposeApplicable ) {
					for( Symbol sym : thPrivSyms ) {
						if(SymbolTools.isArray(sym)) {
							List aspecs = sym.getArraySpecifiers();
							ArraySpecifier aspec = (ArraySpecifier)aspecs.get(0);
							if( aspec.getNumDimensions() == 1 ) {
								MatrixTransposeApplicable = true;
								break;
							}
						}
					}
				}
			}
			////////////////////////////////////////////////////////////////////////
			// Check whether input program contains omp reduction clauses or not. //
			////////////////////////////////////////////////////////////////////////
			List<OmpAnnotation> oAnnotList = IRTools.collectPragmas(program, OmpAnnotation.class, "reduction");
			if( (oAnnotList != null) && !oAnnotList.isEmpty() ) {
				ReductionPatternExists = true;
			}
			StringBuilder str = new StringBuilder(1024);
			str.append("#############################\n");
			str.append("# Applicable global options #\n");
			str.append("##########################################\n");
			str.append("# Safe, always-beneficial options, but   #\n");
			str.append("# resource may limit their applications. #\n");
			str.append("##########################################\n");
			str.append("#pragma optionType1\n");
			str.append("useGlobalGMalloc\n");
			gOptionSet1.add("useGlobalGMalloc");
			//str.append("localRedVarConf = 1 (use 0 if shared memory overflows)\n");
			//gOptionSet1.add("localRedVarConf");
			if( MallocPitchApplicable ) {
				str.append("useMallocPitch\n");
				gOptionSet1.add("useMallocPitch");
			}
			if( MatrixTransposeApplicable ) {
				str.append("useMatrixTranspose\n");
				gOptionSet1.add("useMatrixTranspose");
			}
			gOptionMap.put("optionType1", gOptionSet1);
			////////////////////////////////////
			// Check kernel specific options. //
			////////////////////////////////////
			StringBuilder str2 = new StringBuilder(1024);
			str2.append("\n");
			str2.append("###########################\n");
			str2.append("# Kernel-specific options #\n");
			str2.append("###########################\n");
			for( CudaAnnotation cAnnot : cAnnotList ) {
				Annotatable at = cAnnot.getAnnotatable();
				CudaAnnotation aAnnot = at.getAnnotation(CudaAnnotation.class, "ainfo");
				str2.append(aAnnot.toString()+"\n");
				HashMap<String, Object> kMap = new HashMap<String, Object>();
				//Add maxnumofblocks(nblocks) parameters
				str2.append("maxnumofblocks(nblocks)\n");
				kMap.put("maxnumofblocks", "true");
				//Check LoopCollapse parameters.
				if( lcHandler.handleSMVP((Statement)at, true) ) {
					str2.append("loopcollapse\n");
					LoopCollapseApplicable = true;
					ReductionPatternExists = true;
					kMap.put("loopcollapse", "true");
				}
				//Check reduction-unrolling parameters
				Set<Symbol> redSyms = AnalysisTools.findReductionSymbols((Traversable)at);
				if( redSyms.size() > 0 ) {
					str2.append("noreductionunroll(" + AnalysisTools.symbolsToString(redSyms, ",") + ")\n");
					UnrollingReductionApplicable = true;
					kMap.put("noreductionunroll", AnalysisTools.symbolsToStringSet(redSyms));
				}
				//"tuningparameter" clause is used only internally .
				CudaAnnotation tAnnot = at.getAnnotation(CudaAnnotation.class, "tuningparameters");
				if( tAnnot != null ) {
					//Check ParallelLoopSwap parameters.
					Object obj = tAnnot.get("ploopswap");
					if( obj != null ) {
						str2.append("ploopswap\n");
						PLoopSwapApplicable = true;
						kMap.put("ploopswap", "true");
					}
					//Check local caching-related parameters.
					Set<String> clauses = new HashSet<String>(Arrays.asList(cachingParams));
					Set<String> uDirSet = new HashSet<String>(Arrays.asList(uDirectives));
					for( String clause : clauses ) {
						Set<String> symbols = tAnnot.get(clause); 
						if( symbols != null ) {
							if( uDirSet.contains(clause) ) {
								str2.append(clause + "(" + PrintTools.collectionToString(symbols, ",") + ")\n");
							} else {
								kMap.put(clause, new HashSet<String>(symbols));
								if( clause.equals("ROShSclrNL") ) {
									shrdSclrCachingOnSM = true;
								}
								if( clause.equals("ROShSclr") || clause.equals("RWShSclr") ) {
									shrdSclrCachingOnReg = true;
									shrdSclrCachingOnSM = true;
								}
								if( clause.equals("ROShArEl") || clause.equals("RWShArEl") ) {
									shrdArryElmtCachingOnReg = true;
								}
								if( clause.equals("RO1DShAr") ) {
									shrdArryCachingOnTM = true;
								}
								if( clause.equals("PrvAr") ) {
									prvtArryCachingOnSM = true;
								}
								if( clause.equals("SclrConst") ) {
									shrdSclrCachingOnConst = true;
								}
								if( clause.equals("ArryConst") ) {
									shrdArryCachingOnConst = true;
								}
							}
						}
					}
					//Remove CudaAnnotation containing tuning parameters.
					List<CudaAnnotation> cudaAnnots = at.getAnnotations(CudaAnnotation.class);
					at.removeAnnotations(CudaAnnotation.class);
					for( CudaAnnotation annot : cudaAnnots ) {
						if( !annot.containsKey("tuningparameters") ) {
							at.annotate(annot);
						}
					}
				}
				str2.append("\n");
				kOptionMap.put((CudaAnnotation)aAnnot.clone(), kMap);
			}
			str.append("\n");
			str.append("#################################\n");
			str.append("# May-beneficial options, which #\n");
			str.append("# interact with other options.  #\n");
			str.append("#################################\n");
			str.append("#pragma optionType2\n");
			str.append("cudaThreadBlockSize = number\n");
			gOptionSet2.add("cudaThreadBlockSize");
			str.append("maxNumOfCudaThreadBlocks = number\n");
			gOptionSet2.add("maxNumOfCudaThreadBlocks");
			if( ReductionPatternExists ) {
				str.append("localRedVarConf = 1 (use 0 if shared memory overflows)\n");
				gOptionSet2.add("localRedVarConf");
			}
			if( LoopCollapseApplicable ) {
				str.append("useLoopCollapse\n");
				gOptionSet2.add("useLoopCollapse");
			}
			if( PLoopSwapApplicable ) {
				str.append("useParallelLoopSwap\n");
				gOptionSet2.add("useParallelLoopSwap");
			}
			if( UnrollingReductionApplicable ) {
				str.append("useUnrollingOnReduction\n");
				gOptionSet2.add("useUnrollingOnReduction");
			}
			gOptionMap.put("optionType2", gOptionSet2);
			str.append("\n");
			str.append("#############################################\n");
			str.append("# Always-beneficial options, but inaccuracy #\n");
			str.append("# of the analysis may break correctness.    #\n");
			str.append("#############################################\n");
			str.append("#pragma optionType3\n");
			str.append("globalGMallocOpt\n");
			str.append("cudaMemTrOptLevel = N, where N = 4 (cf: N<4 is safe)\n");
			str.append("cudaMallocOptLevel = 1\n");
			gOptionSet3.add("globalGMallocOpt");
			gOptionSet3.add("cudaMemTrOptLevel");
			gOptionSet3.add("cudaMallocOptLevel");
			gOptionMap.put("optionType3", gOptionSet3);
			str.append("\n");
			str.append("#########################################\n");
			str.append("# Always-beneficial options, but user's #\n");
			str.append("# approval is required.                 #\n");
			str.append("#########################################\n");
			str.append("#pragma optionType4\n");
			str.append("assumeNonZeroTripLoops\n");
			gOptionSet4.add("assumeNonZeroTripLoops");
			gOptionMap.put("optionType4", gOptionSet4);
			str.append("\n");
			str.append("##############################################################\n");
			str.append("# May-beneficial options, which interact with other options. #\n");
			str.append("# These options are not needed if kernel-specific options    #\n");
			str.append("# are used.                                                  #\n");
			str.append("##############################################################\n");
			str.append("#pragma optionType5\n");
			if( shrdSclrCachingOnReg ) {
				str.append("shrdSclrCachingOnReg\n");
				gOptionSet5.add("shrdSclrCachingOnReg");
			}
			if( shrdSclrCachingOnSM ) {
				str.append("shrdSclrCachingOnSM\n");
				gOptionSet5.add("shrdSclrCachingOnSM");
			}
			if( shrdArryElmtCachingOnReg ) {
				str.append("shrdArryElmtCachingOnReg\n");
				gOptionSet5.add("shrdArryElmtCachingOnReg");
			}
			if( shrdArryCachingOnTM ) {
				str.append("shrdArryCachingOnTM\n");
				gOptionSet5.add("shrdArryCachingOnTM");
			}
			if( prvtArryCachingOnSM ) {
				str.append("prvtArryCachingOnSM\n");
				gOptionSet5.add("prvtArryCachingOnSM");
			}
			if( shrdSclrCachingOnConst ) {
				str.append("shrdSclrCachingOnConst\n");
				gOptionSet5.add("shrdSclrCachingOnConst");
			}
			if( shrdArryCachingOnConst ) {
				str.append("shrdArryCachingOnConst\n");
				gOptionSet5.add("shrdArryCachingOnConst");
			}
			gOptionMap.put("optionType5", gOptionSet5);
			str.append("\n");
			str.append("######################################################\n");
			str.append("# Non-tunable options, but user may need to apply    #\n");
			str.append("# some of these either to generate correct output    #\n");
			str.append("# code or to apply some user-assisted optimizations. #\n");
			str.append("######################################################\n");
			str.append("#pragma optionType6\n");
			str.append("disableCritical2ReductionConv\n");
			//gOptionSet6.add("disableCritical2ReductionConv");
			str.append("UEPRemovalOptLevel=N, where N=1,2,or 3\n");
			//gOptionSet6.add("UEPRemovalOptLevel");
			str.append("forceSyncKernelCall\n");
			//gOptionSet6.add("forceSyncKernelCall");
			str.append("doNotRemoveUnusedSymbols\n");
			//gOptionSet6.add("doNotRemoveUnusedSymbols");
			str.append("MemTrOptOnLoops\n");
			//gOptionSet6.add("MemTrOptOnLoops");
			gOptionMap.put("optionType6", gOptionSet6);
			out.write(str.toString());
			out.write(str2.toString());
			out.close();
		} catch (Exception e) {
			PrintTools.println("Creaing a file, "+ tuningParamFile + ", failed; " +
					"tuning parameters can not be saved.", 0);
		}
		return TuningOptions;
	}
	
	///////////////////////////////////////////////////////////////
	// Safe, always-beneficial global options, but resources may // 
	// limit their applications.                                 //
	///////////////////////////////////////////////////////////////
	static private String[] gOptions1 = {"useMatrixTranspose", "useMallocPitch",
		"useGlobalGMalloc"};
	
	///////////////////////////////////////////////////////////////
	// May-beneficial global options, which interact with other  //
	// options.                                                  // 
	///////////////////////////////////////////////////////////////
	static private String[] gOptions2 = {"cudaThreadBlockSize", "useLoopCollapse", 
		"useParallelLoopSwap", "useUnrollingOnReduction", "maxNumOfCudaThreadBlocks",
		"localRedVarConf"};
	
	//////////////////////////////////////////////////////////////////////
	// Always-beneficial global options, but inaccuracy of the analysis //
	// may break correctness.                                           //
	//////////////////////////////////////////////////////////////////////
	static private String[] gOptions3 = {"globalGMallocOpt", "cudaMallocOptLevel",
		"cudaMemTrOptLevel"};
	
	//////////////////////////////////////////////////////////////
	// Always-beneficial global options, but user's approval is // 
	// required.                                                //
	//////////////////////////////////////////////////////////////
	static private String[] gOptions4 = {"assumeNonZeroTripLoops"};
	
	///////////////////////////////////////////////////////////////
	// May-beneficial global options, which interact with other  //
	// options. These options are not needed if kernel-specific  // 
	// options are used.                                         //
	///////////////////////////////////////////////////////////////
	static private String[] gOptions5 = {"shrdSclrCachingOnReg", "shrdSclrCachingOnSM", 
		"shrdArryElmtCachingOnReg", "shrdArryCachingOnTM", "prvtArryCachingOnSM",
		"shrdSclrCachingOnConst", "shrdArryCachingOnConst"};
	
	/////////////////////////////////////////////////////////////////////////
	// Non-tunable options, but user may add these for correctness purpose //
	// or to apply user-assisted, unsafe optimizations                     //
	/////////////////////////////////////////////////////////////////////////
	static private String[] gOptions6 = {"disableCritical2ReductionConv",
		"UEPRemovalOptLevel", "forceSyncKernelCall", "doNotRemoveUnusedSymbols",
		"MemTrOptOnLoops"};
	
	///////////////////////////////////////////////////////////////////////
	// Global options that will be always applied unless explicitly      //
	// excluded by a user using excludedGOptionSet option.               //
	// These options are applied for both Program-level and Kernel-level //
	// tunings even if the user does not specify in defaultGOptionSet.   //
	///////////////////////////////////////////////////////////////////////
	static private String[] defaultGOptions0 = { 
		"cudaMallocOptLevel",  "cudaMemTrOptLevel"}; 
	/////////////////////////////////////////////////////////////////
	// Global options that will be applied by default if existing. //
	// (Below options are used for Program-level tuning.)          //
	/////////////////////////////////////////////////////////////////
	static private String[] defaultGOptions1 = {
		"useGlobalGMalloc", "globalGMallocOpt", "cudaMallocOptLevel", 
		"cudaMemTrOptLevel", "cudaThreadBlockSize"};
	//////////////////////////////////////////////////////////////////////////////
	// Global options that will be applied by default if existing.              //
	// (Below options are used for GPU-kernel-level tuning.)                    //
	// DEBUG: useLoopCollapse, useParallelLoopSwap, and useUnrollingOnReduction //
	// optimizations may not be always beneficial, but added by default, since  //
	// they can be controlled by user directives.                               //
	//////////////////////////////////////////////////////////////////////////////
	static private String[] defaultGOptions2 = {
		"useGlobalGMalloc", "globalGMallocOpt", "cudaMallocOptLevel", 
		"cudaMemTrOptLevel", "useLoopCollapse", "useParallelLoopSwap", 
		"useUnrollingOnReduction", "cudaThreadBlockSize"};
	////////////////////////////////////////////////////////////////////////
	// Global options that will be always applied if existing and unless  //
	// explicitly excluded by a user using excludedGOptionSet option.     //
	// These options are applied for Kernel-level tuning even if the user //
	// does not specify in defaultGOptionSet. These options are always    //
	// applied since these can be overwritten by user directives.         //
	////////////////////////////////////////////////////////////////////////
	static private String[] defaultGOptions3 = { "useLoopCollapse", 
		"useParallelLoopSwap", "useUnrollingOnReduction"};
	
	static private String[] TBSizes = {"64", "128", "256"};
	static private String[] BSizes = {};
	
	/**
	 * Program-level tuning configuration output generator.
	 * 
	 * @param TuningOptions applicable tuning options suggested by O2G translator
	 */
	protected void genTuningConfs1( List<HashMap> TuningOptions ) {
		if( TuningOptions.size() != 2 ) {
			PrintTools.println("[ERROR in genTuningConfs()] input TuningOptions list does not contain enough data.", 0);
			return;
		}
		
	    /* make sure the tuning-configuration directory exists */
	    File dir = null;
	    File fname = null;
	    try {
	      dir = new File(".");
	      fname = new File(dir.getCanonicalPath(), tuningConfDir);
	      if (!fname.exists())
	      {
	        if (!fname.mkdir())
	          throw new IOException("mkdir failed");
	      }
	    } catch (Exception e) {
	      System.err.println("cetus: could not create tuning-configuration directory, " + e);
	      System.exit(1);
	    }
	    
	    PrintTools.println("Generate configuration files for program-level tuning", 0);
	    String dirPrefix = fname.getAbsolutePath() + File.separatorChar;
	    
		HashMap<String, HashSet<String>> gOptionMap = 
			(HashMap<String, HashSet<String>>)TuningOptions.get(0);
		HashMap<CudaAnnotation, HashMap<String, Object>> kOptionMap = 
			(HashMap<CudaAnnotation, HashMap<String, Object>>)TuningOptions.get(1);
		HashMap<String, List<Boolean>> gOptMap = new HashMap<String, List<Boolean>>();
		HashSet<String> confSet = new HashSet<String>();
		HashSet<String> gOptions = new HashSet<String>();
		gOptions.addAll(Arrays.asList(gOptions1));
		gOptions.addAll(Arrays.asList(gOptions2));
		gOptions.addAll(Arrays.asList(gOptions3));
		gOptions.addAll(Arrays.asList(gOptions4));
		gOptions.addAll(Arrays.asList(gOptions5));
		gOptions.addAll(Arrays.asList(gOptions6));
		
		//////////////////////////////////
		// Check default configurations //
		//////////////////////////////////
		HashSet<String> defaultGOption = null;
		HashSet<String> excludedGOption = null;
		String memTrOptValue = "2";
		String mallocOptValue = "0";
		String UEPRemovalOptValue = "3";
		HashSet<String> TBSizeList = null;
		HashSet<String> maxNumBlockSet = null;
		if( tuningConfigs == null || tuningConfigs.isEmpty() ) {
			defaultGOption = new HashSet<String>(Arrays.asList(defaultGOptions1));
			excludedGOption = new HashSet<String>();
			TBSizeList = new HashSet<String>(Arrays.asList(TBSizes));
			maxNumBlockSet = new HashSet<String>(Arrays.asList(BSizes));
		} else {
			defaultGOption = (HashSet<String>)tuningConfigs.get("defaultGOptionSet");
			if( defaultGOption == null ) {
				defaultGOption = new HashSet<String>(Arrays.asList(defaultGOptions1));
			} else {
				//Check whether defaultGOption contains illegal options or not.
				for( String gOpt : defaultGOption ) {
					if( !gOptions.contains(gOpt) ) {
						PrintTools.println("[WARNING in genTuningConfs()] defaultGOptions set contains " +
								"unsupported option: " + gOpt, 0);
					}
				}
			}
			excludedGOption = (HashSet<String>)tuningConfigs.get("excludedGOptionSet");
			if( excludedGOption == null ) {
				excludedGOption = new HashSet<String>();
			} else {
				//Check whether excludedGOption contains illegal options or not.
				for( String gOpt : excludedGOption ) {
					if( !gOptions.contains(gOpt) ) {
						PrintTools.println("[WARNING in genTuningConfs()] excludedGOption set contains " +
								"unsupported option: " + gOpt, 0);
					}
				}
			}
			//////////////////////////////////////////////////////////////////
			// Options in defaultGOptions0 are always applied unless a user //
			// explicitly excludes using excludedGOptionSet option.         //
			//////////////////////////////////////////////////////////////////
			for( String gOpt : defaultGOptions0 ) {
				if( !excludedGOption.contains(gOpt) ) {
					defaultGOption.add(gOpt);
				}
			}
			TBSizeList = (HashSet<String>)tuningConfigs.get("cudaThreadBlockSet");
			if( TBSizeList == null ) {
				TBSizeList = new HashSet<String>(Arrays.asList(TBSizes));
			} else if( !excludedGOption.contains("cudaThreadBlockSize") ) {
				///////////////////////////////////////////////////////////////
				// If cudaThreadBlockSet option is used, cudaThreadBlockSize //
				// option is always included.                                //
				///////////////////////////////////////////////////////////////
				defaultGOption.add("cudaThreadBlockSize");
			}
			maxNumBlockSet = (HashSet<String>)tuningConfigs.get("maxnumofblocksSet");
			if( maxNumBlockSet == null ) {
				maxNumBlockSet = new HashSet<String>(Arrays.asList(BSizes));
			}
			memTrOptValue = (String)tuningConfigs.get("cudaMemTrOptLevel");
			if( memTrOptValue == null ) {
				memTrOptValue = "2";
			}
			mallocOptValue = (String)tuningConfigs.get("cudaMallocOptLevel");
			if( mallocOptValue == null ) {
				mallocOptValue = "0";
			}
			UEPRemovalOptValue = (String)tuningConfigs.get("UEPRemovalOptLevel");
			if( UEPRemovalOptValue == null ) {
				UEPRemovalOptValue = "3";
			}
		}
		
		Set<String> gKeySet = gOptionMap.keySet();
		for( String gKey : gKeySet ) {
			HashSet<String> gOptionSet = gOptionMap.get(gKey);
			for( String option : gOptionSet ) {
				if( excludedGOption.contains(option) ) {
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(new Boolean(false))));
				} else if( defaultGOption.contains(option) ) {
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(new Boolean(true))));
				} else {
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(
							new Boolean(false), new Boolean(true))));
				}
			}
		}
		gKeySet = gOptMap.keySet();
		for( String option : gOptions ) {
			if( !gKeySet.contains(option) ) {
				if( defaultGOption.contains(option) ) {
					//An option in gOptions6 set is included in the defaultGOption set.
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(new Boolean(true))));
				} else {
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(new Boolean(false))));
				}
			}
		}
		int confID = 0;
		for( boolean gOpt1 : gOptMap.get("assumeNonZeroTripLoops") ) {
			for( boolean gOpt2 : gOptMap.get("useGlobalGMalloc") ) {
				for( boolean gOpt3 : gOptMap.get("globalGMallocOpt") ) {
					for( boolean gOpt4 : gOptMap.get("cudaMallocOptLevel") ) {
						for( boolean gOpt5 : gOptMap.get("cudaMemTrOptLevel") ) {
							for( boolean gOpt6 : gOptMap.get("useMatrixTranspose") ) {
								for( boolean gOpt7 : gOptMap.get("useMallocPitch") ) {
									for( boolean gOpt8 : gOptMap.get("useLoopCollapse") ) {
										for( boolean gOpt9 : gOptMap.get("useParallelLoopSwap") ) {
											for( boolean gOpt10 : gOptMap.get("useUnrollingOnReduction") ) {
												for( boolean gOpt11 : gOptMap.get("shrdSclrCachingOnReg") ) {
													for( boolean gOpt12 : gOptMap.get("shrdArryElmtCachingOnReg") ) {
														for( boolean gOpt13 : gOptMap.get("shrdSclrCachingOnSM") ) {
															for( boolean gOpt14 : gOptMap.get("prvtArryCachingOnSM") ) {
																for( boolean gOpt15 : gOptMap.get("shrdArryCachingOnTM") ) {
																	for( boolean gOpt16 : gOptMap.get("cudaThreadBlockSize") ) {
																		for( boolean gOpt17 : gOptMap.get("maxNumOfCudaThreadBlocks") ) {
																			for( boolean gOpt18 : gOptMap.get("disableCritical2ReductionConv") ) {
																				for( boolean gOpt19 : gOptMap.get("UEPRemovalOptLevel") ) {
																					for( boolean gOpt20 : gOptMap.get("forceSyncKernelCall") ) {
																						for( boolean gOpt21 : gOptMap.get("doNotRemoveUnusedSymbols") ) {
																							for( boolean gOpt22 : gOptMap.get("shrdSclrCachingOnConst") ) {
																								for( boolean gOpt23 : gOptMap.get("shrdArryCachingOnConst") ) {
																									for( boolean gOpt24 : gOptMap.get("localRedVarConf") ) {
																										for( boolean gOpt25 : gOptMap.get("MemTrOptOnLoops") ) {
																											StringBuilder str1 = new StringBuilder(256);
																											if( addSafetyCheckingCode ) {
																												str1.append("addSafetyCheckingCode\n");
																											}
																											if( gOpt1 ) {
																												str1.append("assumeNonZeroTripLoops\n");
																											}
																											if( gOpt2 ) {
																												str1.append("useGlobalGMalloc\n");
																												if( gOpt3 ) {
																													str1.append("globalGMallocOpt\n");
																												}
																											}
																											if( gOpt4 ) {
																												str1.append("cudaMallocOptLevel="+mallocOptValue+"\n");
																											}
																											if( gOpt5 ) {
																												str1.append("cudaMemTrOptLevel="+memTrOptValue+"\n");
																											}
																											if( gOpt6 ) {
																												str1.append("useMatrixTranspose\n");
																											}
																											if( gOpt7 ) {
																												str1.append("useMallocPitch\n");
																											}
																											if( gOpt8 ) {
																												str1.append("useLoopCollapse\n");
																											}
																											if( gOpt9 ) {
																												str1.append("useParallelLoopSwap\n");
																											}
																											if( gOpt10 ) {
																												str1.append("useUnrollingOnReduction\n");
																											}
																											if( gOpt11 ) {
																												str1.append("shrdSclrCachingOnReg\n");
																											}
																											if( gOpt12 ) {
																												str1.append("shrdArryElmtCachingOnReg\n");
																											}
																											if( gOpt13 ) {
																												str1.append("shrdSclrCachingOnSM\n");
																											}
																											if( gOpt14 ) {
																												str1.append("prvtArryCachingOnSM\n");
																											}
																											if( gOpt15 ) {
																												str1.append("shrdArryCachingOnTM\n");
																											}
																											if( gOpt18 ) {
																												str1.append("disableCritical2ReductionConv\n");
																											}
																											if( gOpt19 ) {
																												str1.append("UEPRemovalOptLevel="+UEPRemovalOptValue+"\n");
																											}
																											if( gOpt20 ) {
																												str1.append("forceSyncKernelCall\n");
																											}
																											if( gOpt21 ) {
																												str1.append("doNotRemoveUnusedSymbols\n");
																											}
																											if( gOpt22 ) {
																												str1.append("shrdSclrCachingOnConst\n");
																											}
																											if( gOpt23 ) {
																												str1.append("shrdArryCachingOnConst\n");
																											}
																											if( gOpt24 ) {
																												str1.append("localRedVarConf=1\n");
																											} else {
																												str1.append("localRedVarConf=0\n");
																											}
																											if( gOpt25 ) {
																												str1.append("MemTrOptOnLoops\n");
																											}
																											String confString = str1.toString();
																											if( gOpt16 ) {
																												for( String tbSz : TBSizeList ) {
																													str1 = new StringBuilder(256);
																													str1.append(confString);
																													str1.append("cudaThreadBlockSize="+tbSz+"\n");
																													String confString2 = str1.toString();
																													if( gOpt17 ) {
																														for( String nBlocks : maxNumBlockSet ) {
																															str1 = new StringBuilder(256);
																															str1.append(confString2);
																															str1.append("maxNumOfCudaThreadBlocks="+nBlocks+"\n");
																															String confString3 = str1.toString();
																															if( !confSet.contains(confString3) ) {
																																confSet.add(confString3);
																																String confFile = "confFile"+confID+".txt";
																																try {
																																	BufferedWriter out1 = 
																																		new BufferedWriter(new FileWriter(dirPrefix+confFile));
																																	out1.write(confString3);
																																	out1.close();
																																} catch( Exception e ) {
																																	PrintTools.println("Creaing a file, "+ confFile + ", failed; " +
																																			"tuning parameters can not be saved.", 0);
																																}
																																confID++;
																															}
																														}
																													} else {
																														if( !confSet.contains(confString2) ) {
																															confSet.add(confString2);
																															String confFile = "confFile"+confID+".txt";
																															try {
																																BufferedWriter out1 = 
																																	new BufferedWriter(new FileWriter(dirPrefix+confFile));
																																out1.write(confString2);
																																out1.close();
																															} catch( Exception e ) {
																																PrintTools.println("Creaing a file, "+ confFile + ", failed; " +
																																		"tuning parameters can not be saved.", 0);
																															}
																															confID++;
																														}
																													}
																												}
																											} else {
																												if( gOpt17 ) {
																													for( String nBlocks : maxNumBlockSet ) {
																														str1 = new StringBuilder(256);
																														str1.append(confString);
																														str1.append("maxNumOfCudaThreadBlocks="+nBlocks+"\n");
																														String confString3 = str1.toString();
																														if( !confSet.contains(confString3) ) {
																															confSet.add(confString3);
																															String confFile = "confFile"+confID+".txt";
																															try {
																																BufferedWriter out1 = 
																																	new BufferedWriter(new FileWriter(dirPrefix+confFile));
																																out1.write(confString3);
																																out1.close();
																															} catch( Exception e ) {
																																PrintTools.println("Creaing a file, "+ confFile + ", failed; " +
																																		"tuning parameters can not be saved.", 0);
																															}
																															confID++;
																														}
																													}
																												} else {
																													if( !confSet.contains(confString) ) {
																														confSet.add(confString);
																														String confFile = "confFile"+confID+".txt";
																														try {
																															BufferedWriter out1 = 
																																new BufferedWriter(new FileWriter(dirPrefix+confFile));
																															out1.write(confString);
																															out1.close();
																														} catch( Exception e ) {
																															PrintTools.println("Creaing a file, "+ confFile + ", failed; " +
																																	"tuning parameters can not be saved.", 0);
																														}
																														confID++;
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		PrintTools.println("\n\n    Number of created tuning-configuration files: "+confID+"\n\n", 0);
	}
	
	/**
	 * GPU-kernel-level tuning configuration output generator.
	 * 
	 * @param TuningOptions applicable tuning options suggested by O2G translator
	 */
	protected void genTuningConfs2( List<HashMap> TuningOptions ) {
		if( TuningOptions.size() != 2 ) {
			PrintTools.println("[ERROR in genTuningConfs()] input TuningOptions list does not contain enough data.", 0);
			return;
		}
		
	    /* make sure the tuning-configuration directory exists */
	    File dir = null;
	    File fname = null;
	    try {
	      dir = new File(".");
	      fname = new File(dir.getCanonicalPath(), tuningConfDir);
	      if (!fname.exists())
	      {
	        if (!fname.mkdir())
	          throw new IOException("mkdir failed");
	      }
	    } catch (Exception e) {
	      System.err.println("cetus: could not create tuning-configuration directory, " + e);
	      System.exit(1);
	    }
	    PrintTools.println("Generate configuration files for GPU-kernel-level tuning", 0);
	    String dirPrefix = fname.getAbsolutePath() + File.separatorChar;
	    
		HashMap<String, HashSet<String>> gOptionMap = 
			(HashMap<String, HashSet<String>>)TuningOptions.get(0);
		HashMap<CudaAnnotation, HashMap<String, Object>> kOptionMap = 
			(HashMap<CudaAnnotation, HashMap<String, Object>>)TuningOptions.get(1);
		HashMap<String, List<Boolean>> gOptMap = new HashMap<String, List<Boolean>>();
		HashSet<String> confSet = new HashSet<String>();
		HashSet<String> gOptions = new HashSet<String>();
		gOptions.addAll(Arrays.asList(gOptions1));
		gOptions.addAll(Arrays.asList(gOptions2));
		gOptions.addAll(Arrays.asList(gOptions3));
		gOptions.addAll(Arrays.asList(gOptions4));
		gOptions.addAll(Arrays.asList(gOptions6));
		
		//////////////////////////////////
		// Check default configurations //
		//////////////////////////////////
		HashSet<String> defaultGOption = null;
		HashSet<String> excludedGOption = null;
		String memTrOptValue = "2";
		String mallocOptValue = "0";
		String UEPRemovalOptValue = "3";
		HashSet<String> TBSizeList = null;
		HashSet<String> maxNumBlockSet = null;
		if( tuningConfigs == null || tuningConfigs.isEmpty() ) {
			defaultGOption = new HashSet<String>(Arrays.asList(defaultGOptions1));
			excludedGOption = new HashSet<String>();
			TBSizeList = new HashSet<String>(Arrays.asList(TBSizes));
			maxNumBlockSet = new HashSet<String>(Arrays.asList(BSizes));
		} else {
			defaultGOption = (HashSet<String>)tuningConfigs.get("defaultGOptionSet");
			if( defaultGOption == null ) {
				defaultGOption = new HashSet<String>(Arrays.asList(defaultGOptions2));
			} else {
				//Check whether defaultGOption contains illegal options or not.
				HashSet<String> gOption5 = new HashSet<String>();
				gOption5.addAll(Arrays.asList(gOptions5));
				for( String gOpt : defaultGOption ) {
					if( !gOptions.contains(gOpt) && !gOption5.contains(gOpt) ) {
						PrintTools.println("[WARNING in genTuningConfs()] defaultGOptions set contains " +
								"unsupported option: " + gOpt, 0);
					}
				}
			}
			excludedGOption = (HashSet<String>)tuningConfigs.get("excludedGOptionSet");
			if( excludedGOption == null ) {
				excludedGOption = new HashSet<String>();
			} else {
				//Check whether defaultGOption contains illegal options or not.
				HashSet<String> gOption5 = new HashSet<String>();
				gOption5.addAll(Arrays.asList(gOptions5));
				for( String gOpt : excludedGOption ) {
					if( !gOptions.contains(gOpt) && !gOption5.contains(gOpt) ) {
						PrintTools.println("[WARNING in genTuningConfs()] excludedGOptions set contains " +
								"unsupported option: " + gOpt, 0);
					}
				}
			}
			//////////////////////////////////////////////////////////////////
			// Options in defaultGOptions0 are always applied unless a user //
			// explicitly excludes using excludedGOptionSet option.         //
			//////////////////////////////////////////////////////////////////
			for( String gOpt : defaultGOptions0 ) {
				if( !excludedGOption.contains(gOpt) ) {
					defaultGOption.add(gOpt);
				}
			}
			TBSizeList = (HashSet<String>)tuningConfigs.get("cudaThreadBlockSet");
			if( TBSizeList == null ) {
				TBSizeList = new HashSet<String>(Arrays.asList(TBSizes));
			} else if( !excludedGOption.contains("cudaThreadBlockSize") ) {
				///////////////////////////////////////////////////////////////
				// If cudaThreadBlockSet option is used, cudaThreadBlockSize //
				// option is always included.                                //
				///////////////////////////////////////////////////////////////
				defaultGOption.add("cudaThreadBlockSize");
			}
			maxNumBlockSet = (HashSet<String>)tuningConfigs.get("maxnumofblocksSet");
			if( maxNumBlockSet == null ) {
				maxNumBlockSet = new HashSet<String>(Arrays.asList(BSizes));
			}
			memTrOptValue = (String)tuningConfigs.get("cudaMemTrOptLevel");
			if( memTrOptValue == null ) {
				memTrOptValue = "2";
			}
			mallocOptValue = (String)tuningConfigs.get("cudaMallocOptLevel");
			if( mallocOptValue == null ) {
				mallocOptValue = "0";
			}
			UEPRemovalOptValue = (String)tuningConfigs.get("UEPRemovalOptLevel");
			if( UEPRemovalOptValue == null ) {
				UEPRemovalOptValue = "3";
			}
		}
		
		HashSet<String> defaultGOptSet3 = new HashSet<String>(Arrays.asList(defaultGOptions3));
		Set<String> gKeySet = gOptionMap.keySet();
		for( String gKey : gKeySet ) {
			HashSet<String> gOptionSet = gOptionMap.get(gKey);
			for( String option : gOptionSet ) {
				if( excludedGOption.contains(option) ) {
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(new Boolean(false))));
				} else if( defaultGOption.contains(option) ) {
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(new Boolean(true))));
				} else if( defaultGOptSet3.contains(option) ) {
					////////////////////////////////////////////////////////////////////////
					// Options in defaultGOptions3 are always applied if existing and     //
					// unless a user explicitly excludes using excludedGOptionSet option. //
					////////////////////////////////////////////////////////////////////////
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(new Boolean(true))));
				} else {
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(
							new Boolean(false), new Boolean(true))));
				}
			}
		}
		gKeySet = gOptMap.keySet();
		for( String option : gOptions ) {
			if( !gKeySet.contains(option) ) {
				if( defaultGOption.contains(option) ) {
					//An option in gOptions6 set is included in the defaultGOption set.
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(new Boolean(true))));
				} else {
					gOptMap.put(option, new ArrayList<Boolean>(Arrays.asList(new Boolean(false))));
				}
			}
		}
		int confID = 0;
		for( boolean gOpt1 : gOptMap.get("assumeNonZeroTripLoops") ) {
			for( boolean gOpt2 : gOptMap.get("useGlobalGMalloc") ) {
				for( boolean gOpt3 : gOptMap.get("globalGMallocOpt") ) {
					for( boolean gOpt4 : gOptMap.get("cudaMallocOptLevel") ) {
						for( boolean gOpt5 : gOptMap.get("cudaMemTrOptLevel") ) {
							for( boolean gOpt6 : gOptMap.get("useMatrixTranspose") ) {
								for( boolean gOpt7 : gOptMap.get("useMallocPitch") ) {
									for( boolean gOpt8 : gOptMap.get("useLoopCollapse") ) {
										for( boolean gOpt9 : gOptMap.get("useParallelLoopSwap") ) {
											for( boolean gOpt10 : gOptMap.get("useUnrollingOnReduction") ) {
												for( boolean gOpt11 : gOptMap.get("cudaThreadBlockSize") ) {
													for( boolean gOpt12 : gOptMap.get("maxNumOfCudaThreadBlocks") ) {
														for( boolean gOpt18 : gOptMap.get("disableCritical2ReductionConv") ) {
															for( boolean gOpt19 : gOptMap.get("UEPRemovalOptLevel") ) {
																for( boolean gOpt20 : gOptMap.get("forceSyncKernelCall") ) {
																	for( boolean gOpt21 : gOptMap.get("doNotRemoveUnusedSymbols") ) {
																		for( boolean gOpt22 : gOptMap.get("localRedVarConf") ) {
																			for( boolean gOpt23 : gOptMap.get("MemTrOptOnLoops") ) {
																				StringBuilder str1 = new StringBuilder(256);
																				if( addSafetyCheckingCode ) {
																					str1.append("addSafetyCheckingCode\n");
																				}
																				if( gOpt1 ) {
																					str1.append("assumeNonZeroTripLoops\n");
																				}
																				if( gOpt2 ) {
																					str1.append("useGlobalGMalloc\n");
																					if( gOpt3 ) {
																						str1.append("globalGMallocOpt\n");
																					}
																				}
																				if( gOpt4 ) {
																					str1.append("cudaMallocOptLevel="+mallocOptValue+"\n");
																				}
																				if( gOpt5 ) {
																					str1.append("cudaMemTrOptLevel="+memTrOptValue+"\n");
																				}
																				if( gOpt6 ) {
																					str1.append("useMatrixTranspose\n");
																				}
																				if( gOpt7 ) {
																					str1.append("useMallocPitch\n");
																				}
																				if( gOpt8 ) {
																					str1.append("useLoopCollapse\n");
																				}
																				if( gOpt9 ) {
																					str1.append("useParallelLoopSwap\n");
																				}
																				if( gOpt10 ) {
																					str1.append("useUnrollingOnReduction\n");
																				}
																				if( gOpt18 ) {
																					str1.append("disableCritical2ReductionConv\n");
																				}
																				if( gOpt19 ) {
																					str1.append("UEPRemovalOptLevel="+UEPRemovalOptValue+"\n");
																				}
																				if( gOpt20 ) {
																					str1.append("forceSyncKernelCall\n");
																				}
																				if( gOpt21 ) {
																					str1.append("doNotRemoveUnusedSymbols\n");
																				}
																				if( gOpt22 ) {
																					str1.append("localRedVarConf=1\n");
																				} else {
																					str1.append("localRedVarConf=0\n");
																				}
																				if( gOpt23 ) {
																					str1.append("MemTrOptOnLoops\n");
																				}
																				String confString = str1.toString();
																				if( gOpt11 ) {
																					for( String tbSz : TBSizeList ) {
																						str1 = new StringBuilder(256);
																						str1.append(confString);
																						str1.append("cudaThreadBlockSize="+tbSz+"\n");
																						String confString2 = str1.toString();
																						if( gOpt12 ) {
																							for( String nBlocks : maxNumBlockSet ) {
																								str1 = new StringBuilder(256);
																								str1.append(confString2);
																								str1.append("maxNumOfCudaThreadBlocks="+nBlocks+"\n");
																								String confString3 = str1.toString();
																								if( !confSet.contains(confString3) ) {
																									confSet.add(confString3);
																									Set<String> userDirectives = 
																										genKTuningConf(gOpt8, gOpt9, gOpt10, kOptionMap, maxNumBlockSet);
																									if( userDirectives == null ) {
																										return;
																									}
																									for( String uDir : userDirectives ) {
																										String confFile = "confFile"+confID+".txt";
																										String uDirFile = "userDirective"+confID+".txt";
																										str1 = new StringBuilder(256);
																										str1.append(confString3);
																										str1.append("cudaUserDirectiveFile="+uDirFile+"\n");
																										try {
																											BufferedWriter out1 = 
																												new BufferedWriter(new FileWriter(dirPrefix+confFile));
																											out1.write(str1.toString());
																											out1.close();
																											BufferedWriter out2 = 
																												new BufferedWriter(new FileWriter(dirPrefix+uDirFile));
																											out2.write(uDir);
																											out2.close();
																										} catch( Exception e ) {
																											PrintTools.println("Creaing a file, "+ confFile + ", failed; " +
																													"tuning parameters can not be saved.", 0);
																										}
																										confID++;
																									}
																								}
																							}
																						} else {
																							if( !confSet.contains(confString2) ) {
																								confSet.add(confString2);
																								Set<String> userDirectives = 
																									genKTuningConf(gOpt8, gOpt9, gOpt10, kOptionMap, maxNumBlockSet);
																								if( userDirectives == null ) {
																									return;
																								}
																								for( String uDir : userDirectives ) {
																									String confFile = "confFile"+confID+".txt";
																									String uDirFile = "userDirective"+confID+".txt";
																									str1 = new StringBuilder(256);
																									str1.append(confString2);
																									str1.append("cudaUserDirectiveFile="+uDirFile+"\n");
																									try {
																										BufferedWriter out1 = 
																											new BufferedWriter(new FileWriter(dirPrefix+confFile));
																										out1.write(str1.toString());
																										out1.close();
																										BufferedWriter out2 = 
																											new BufferedWriter(new FileWriter(dirPrefix+uDirFile));
																										out2.write(uDir);
																										out2.close();
																									} catch( Exception e ) {
																										PrintTools.println("Creaing a file, "+ confFile + ", failed; " +
																												"tuning parameters can not be saved.", 0);
																									}
																									confID++;
																								}
																							}
																						}
																					}
																				} else {
																					if( gOpt12 ) {
																						for( String nBlocks : maxNumBlockSet ) {
																							str1 = new StringBuilder(256);
																							str1.append(confString);
																							str1.append("maxNumOfCudaThreadBlocks="+nBlocks+"\n");
																							String confString2 = str1.toString();	
																							if( !confSet.contains(confString2) ) {
																								confSet.add(confString2);
																								Set<String> userDirectives = 
																									genKTuningConf(gOpt8, gOpt9, gOpt10, kOptionMap, maxNumBlockSet);
																								if( userDirectives == null ) {
																									return;
																								}
																								for( String uDir : userDirectives ) {
																									String confFile = "confFile"+confID+".txt";
																									String uDirFile = "userDirective"+confID+".txt";
																									str1 = new StringBuilder(256);
																									str1.append(confString2);
																									str1.append("cudaUserDirectiveFile="+uDirFile+"\n");
																									try {
																										BufferedWriter out1 = 
																											new BufferedWriter(new FileWriter(dirPrefix+confFile));
																										out1.write(str1.toString());
																										out1.close();
																										BufferedWriter out2 = 
																											new BufferedWriter(new FileWriter(dirPrefix+uDirFile));
																										out2.write(uDir);
																										out2.close();
																									} catch( Exception e ) {
																										PrintTools.println("Creaing a file, "+ confFile + ", failed; " +
																												"tuning parameters can not be saved.", 0);
																									}
																									confID++;
																								}
																							}
																						}
																					} else {
																						if( !confSet.contains(confString) ) {
																							confSet.add(confString);
																							Set<String> userDirectives = 
																								genKTuningConf(gOpt8, gOpt9, gOpt10, kOptionMap, maxNumBlockSet);
																							if( userDirectives == null ) {
																								return;
																							}
																							for( String uDir : userDirectives ) {
																								String confFile = "confFile"+confID+".txt";
																								String uDirFile = "userDirective"+confID+".txt";
																								str1 = new StringBuilder(256);
																								str1.append(confString);
																								str1.append("cudaUserDirectiveFile="+uDirFile+"\n");
																								try {
																									BufferedWriter out1 = 
																										new BufferedWriter(new FileWriter(dirPrefix+confFile));
																									out1.write(str1.toString());
																									out1.close();
																									BufferedWriter out2 = 
																										new BufferedWriter(new FileWriter(dirPrefix+uDirFile));
																									out2.write(uDir);
																									out2.close();
																								} catch( Exception e ) {
																									PrintTools.println("Creaing a file, "+ confFile + ", failed; " +
																											"tuning parameters can not be saved.", 0);
																								}
																								confID++;
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		PrintTools.println("\n\n    Number of created tuning-configuration files: "+confID+"\n\n", 0);
	}

	/**
	 * Kernel-level user directive output generator
	 * 
	 * @param useLoopCollapse true if loop collapse optimization is applied
	 * @param useParallelLoopSwap true if parallel loopswap optimization is applied
	 * @param useUnrolling true if unrolling-on-reduction optimization is applied
	 * @param kOptionMap hashMap of kernel-level options
	 * @return set of user directive outputs
	 */
	protected Set<String> genKTuningConf(boolean useLoopCollapse, boolean useParallelLoopSwap, boolean useUnrolling,
			HashMap<CudaAnnotation, HashMap<String, Object>> kOptionMap, HashSet<String> maxNumBlockSet) {
		Set<String> oldSet = new HashSet<String>();
		Set<String> newSet = new HashSet<String>();
		Set<CudaAnnotation> keySet = kOptionMap.keySet();
		for( CudaAnnotation cAnnot : keySet ) {
			StringBuilder id_str = new StringBuilder(32);
			id_str.append("kernelid("+cAnnot.get("kernelid")+") ");
			id_str.append("procname("+cAnnot.get("procname")+") ");
			String idString = id_str.toString();
			HashMap<String, Object> kMap = kOptionMap.get(cAnnot);
			Set<String> kSet = kMap.keySet();
			HashMap<String, List<String>> kOptMap = new HashMap<String, List<String>>();
			if( useLoopCollapse && kSet.contains("loopcollapse") ) {
				kOptMap.put("loopcollapse", new ArrayList<String>(Arrays.asList(
						"false", "true")));
			} else {
				kOptMap.put("loopcollapse", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( useParallelLoopSwap && kSet.contains("ploopswap") ) {
				kOptMap.put("ploopswap", new ArrayList<String>(Arrays.asList(
						"false", "true")));
			} else {
				kOptMap.put("ploopswap", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( useUnrolling && kSet.contains("noreductionunroll") ) {
				kOptMap.put("noreductionunroll", new ArrayList<String>(Arrays.asList(
						"false", "true")));
			} else {
				kOptMap.put("noreductionunroll", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("ROShSclrNL") ) {
				kOptMap.put("ROShSclrNL", new ArrayList<String>(Arrays.asList(
						"none", "sharedRO")));
			} else {
				kOptMap.put("ROShSclrNL", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("ROShSclr") ) {
				kOptMap.put("ROShSclr", new ArrayList<String>(Arrays.asList(
						"none", "registerRO", "sharedRO")));
			} else {
				kOptMap.put("ROShSclr", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("RWShSclr") ) {
				kOptMap.put("RWShSclr", new ArrayList<String>(Arrays.asList(
						"none", "registerRW", "sharedRW")));
			} else {
				kOptMap.put("RWShSclr", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("ROShArEl") ) {
				kOptMap.put("ROShArEl", new ArrayList<String>(Arrays.asList(
						"none", "registerRO")));
			} else {
				kOptMap.put("ROShArEl", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("RWShArEl") ) {
				kOptMap.put("RWShArEl", new ArrayList<String>(Arrays.asList(
						"none", "registerRW")));
			} else {
				kOptMap.put("RWShArEl", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("RO1DShAr") ) {
				kOptMap.put("RO1DShAr", new ArrayList<String>(Arrays.asList(
						"none", "texture")));
			} else {
				kOptMap.put("RO1DShAr", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("RO1DShAr") ) {
				kOptMap.put("RO1DShAr", new ArrayList<String>(Arrays.asList(
						"none", "texture")));
			} else {
				kOptMap.put("RO1DShAr", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("PrvAr") ) {
				kOptMap.put("PrvAr", new ArrayList<String>(Arrays.asList(
						"none", "sharedRW")));
			} else {
				kOptMap.put("PrvAr", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("SclrConst") ) {
				kOptMap.put("SclrConst", new ArrayList<String>(Arrays.asList(
						"none", "constant")));
			} else {
				kOptMap.put("SclrConst", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( kSet.contains("ArryConst") ) {
				kOptMap.put("ArryConst", new ArrayList<String>(Arrays.asList(
						"none", "constant")));
			} else {
				kOptMap.put("ArryConst", new ArrayList<String>(Arrays.asList(
						"none")));
			}
			if( maxNumBlockSet == null || maxNumBlockSet.isEmpty() ) {
				kOptMap.put("maxNumBlockSet", new ArrayList<String>(Arrays.asList(
				"none")));
			} else {
/*				kOptMap.put("maxNumBlockSet", new ArrayList<String>(Arrays.asList(
						"none", "maxnumofblocks")));*/
				//////////////////////////////////////////////////////////////////
				//DEBUG: for now, this option is applied to a program globally, //
				// and thus not applied for each kernel here.                   //
				//////////////////////////////////////////////////////////////////
				kOptMap.put("maxNumBlockSet", new ArrayList<String>(Arrays.asList(
				"none")));
			}
			for( String kOpt1 : kOptMap.get("loopcollapse") ) {
				for( String kOpt2 : kOptMap.get("ploopswap") ) {
					for( String kOpt3 : kOptMap.get("noreductionunroll") ) {
						for( String kOpt4 : kOptMap.get("ROShSclrNL") ) {
							for( String kOpt5 : kOptMap.get("ROShSclr") ) {
								for( String kOpt6 : kOptMap.get("RWShSclr") ) {
									for( String kOpt7 : kOptMap.get("ROShArEl") ) {
										for( String kOpt8 : kOptMap.get("RWShArEl") ) {
											for( String kOpt9 : kOptMap.get("RO1DShAr") ) {
												for( String kOpt10 :kOptMap.get("PrvAr") ) {
													for( String kOpt11 :kOptMap.get("maxNumBlockSet") ) {
														for( String kOpt12 :kOptMap.get("SclrConst") ) {
															for( String kOpt13 :kOptMap.get("ArryConst") ) {
																StringBuilder str1 = new StringBuilder(256);
																str1.append(idString);
																if( kOpt1.equals("false") ) {
																	str1.append("noloopcollapse ");
																}
																if( kOpt2.equals("false") ) {
																	str1.append("noploopswap ");
																}
																if( kOpt3.equals("false") ) {
																	Set<String> rSet = (Set<String>)kMap.get("noreductionunroll");
																	str1.append("noreductionunroll("+
																			PrintTools.collectionToString(rSet, ",")+") ");
																}
																HashSet<String> registerRO = new HashSet<String>();
																HashSet<String> registerRW = new HashSet<String>();
																HashSet<String> sharedRO = new HashSet<String>();
																HashSet<String> sharedRW = new HashSet<String>();
																HashSet<String> texture = new HashSet<String>();
																HashSet<String> constant = new HashSet<String>();
																if( kOpt4.equals("sharedRO") ) {
																	sharedRO.addAll((Set<String>)kMap.get("ROShSclrNL"));
																}
																if( kOpt5.equals("sharedRO") ) {
																	sharedRO.addAll((Set<String>)kMap.get("ROShSclr"));
																} else if( kOpt5.equals("registerRO") ) {
																	registerRO.addAll((Set<String>)kMap.get("ROShSclr"));
																}
																if( kOpt6.equals("sharedRW") ) {
																	sharedRW.addAll((Set<String>)kMap.get("RWShSclr"));
																} else if( kOpt5.equals("registerRW") ) {
																	registerRW.addAll((Set<String>)kMap.get("RWShSclr"));
																}
																if( kOpt9.equals("texture") ) {
																	texture.addAll((Set<String>)kMap.get("RO1DShAr"));
																}
																if( kOpt12.equals("constant") ) {
																	constant.addAll((Set<String>)kMap.get("SclrConst"));
																}
																if( kOpt13.equals("constant") ) {
																	constant.addAll((Set<String>)kMap.get("ArryConst"));
																}
																if( kOpt7.equals("registerRO") ) {
																	Set<String> sSet = new HashSet<String>((Set<String>)kMap.get("ROShArEl"));
																	Set<String> removeSet = new HashSet<String>();
																	///////////////////////////////////////////////////
																	// If an array element in ROShArEl set refers to //
																	// an array in texture set, the element should   //
																	// not be included in the registerRO set.        //
																	///////////////////////////////////////////////////
																	if( !texture.isEmpty() ) {
																		for( String element : sSet ) {
																			int bracket = element.indexOf('[');
																			if( bracket != -1 ) {
																				String sym = element.substring(0, bracket);
																				if( texture.contains(sym) ) {
																					removeSet.add(element);
																				}
																			}
																		}
																		sSet.removeAll(removeSet);
																	}
																	registerRO.addAll(sSet);
																}
																if( kOpt8.equals("registerRW") ) {
																	registerRW.addAll((Set<String>)kMap.get("RWShArEl"));
																}
																if( kOpt10.equals("sharedRW") ) {
																	sharedRW.addAll((Set<String>)kMap.get("PrvAr"));
																}
																if( !sharedRO.isEmpty() ) {
																	str1.append("sharedRO("+PrintTools.collectionToString(sharedRO, ",")+") ");
																}
																if( !sharedRW.isEmpty() ) {
																	str1.append("sharedRW("+PrintTools.collectionToString(sharedRW, ",")+") ");
																}
																if( !texture.isEmpty() ) {
																	str1.append("texture("+PrintTools.collectionToString(texture, ",")+") ");
																}
																if( !registerRO.isEmpty() ) {
																	str1.append("registerRO("+PrintTools.collectionToString(registerRO, ",")+") ");
																}
																if( !registerRW.isEmpty() ) {
																	str1.append("registerRW("+PrintTools.collectionToString(registerRW, ",")+") ");
																}
																if( !constant.isEmpty() ) {
																	str1.append("constant("+PrintTools.collectionToString(constant, ",")+") ");
																}
																str1.append("\n\n");
																String tuningConf = str1.toString();
																if( kOpt11.equals("maxnumofblocks") ) {
																	//////////////////////////////////////////////////////////////////
																	//DEBUG: for now, this option is applied to a program globally, //
																	// and thus not applied for each kernel here.                   //
																	//////////////////////////////////////////////////////////////////
																} else {
																	if( oldSet.isEmpty() ) {
																		newSet.add(tuningConf);
																	} else {
																		//DEBUG
																		//System.out.println("newSet size: " + newSet.size());
																		try {
																			for( String confS : oldSet ) {
																				String newStr = confS.concat(tuningConf);
																				newSet.add(newStr);
																			}
																		} catch( Exception e) {
																			Tools.exit("[ERROR in genKTuningConf()] there exist too many tuning " +
																			"configurations; file generation will be skipped.");
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			oldSet.clear();
			oldSet.addAll(newSet);
			// DEBUG
			//System.out.println("newSet size: " + newSet.size());
			newSet.clear();
		}
		return oldSet;
	}
}
