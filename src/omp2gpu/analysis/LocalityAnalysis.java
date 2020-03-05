package omp2gpu.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import omp2gpu.hir.CudaAnnotation;
import omp2gpu.transforms.SplitOmpPRegion;
import cetus.analysis.AnalysisPass;
import cetus.analysis.CallGraph;
import cetus.exec.Driver;
import cetus.hir.ArraySpecifier;
import cetus.hir.CompoundStatement;
import cetus.hir.DepthFirstIterator;
import cetus.hir.Expression;
import cetus.hir.OmpAnnotation;
import cetus.hir.Procedure;
import cetus.hir.ProcedureDeclarator;
import cetus.hir.Program;
import cetus.hir.Statement;
import cetus.hir.Symbol;
import cetus.hir.SymbolTools;
import cetus.hir.DataFlowTools;
import cetus.hir.IRTools;
import cetus.hir.PrintTools;
import cetus.hir.ArrayAccess;
import cetus.hir.Traversable;

/**
 * Locality Analysis (outdated).
 * If shrdSclrCachingOnReg == true,
 *     - if R/O shared scalar variables have locality, 
 *         add them into cuda registerRO clause. 
 *     - if R/W shared scalar variables have locality,
 *         add them into cuda registerRW clause. 
 * If shrdSclrCachingOnSM == true,
 *     - if R/O shared scalar variables exist, 
 *         add them into cuda sharedRO clause. 
 *     - if R/W shared scalar variables have locality,
 *         add them into cuda sharedRW clause. 
 *         (not yet implemented). 
 * If both shrdSclrCachingOnReg and shrdSclrCachingOnSM are on,        
 *     - R/O shared scalar variables with locality will be put
 *       into sharedRO clause.
 *     - R/W shared scalar variables with locality will be put
 *       into registerRW clause.
 * If shrdArryElmtCachingOnReg == true,
 *     - if R/O shared array elements have locality, 
 *         add them into cuda registerRO clause. 
 *     - if R/W shared array elements have locality,
 *         add them into cuda registerRW clause. 
 * If prvtArryCachingOnSM == true,
 *     - add private array variables into sharedRW clause.
 * If shrdArryCachingOnTM == true,
 *     - If one-dimensional, R/O shared arrays have locality,
 *         add them into cuda texture clause.
 *
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 */
public class LocalityAnalysis extends AnalysisPass {
	private boolean shrdSclrCachingOnReg;
	private boolean shrdSclrCachingOnSM;
	private boolean shrdArryElmtCachingOnReg;
	private boolean prvtArryCachingOnSM;
	private boolean shrdArryCachingOnTM;
	private boolean shrdSclrCachingOnConst;
	private boolean shrdArryCachingOnConst;
	private boolean extractTuningParameters;

	/**
	 * @param program
	 */
	public LocalityAnalysis(Program program) {
		super(program);
	}

	/* (non-Javadoc)
	 * @see cetus.analysis.AnalysisPass#getPassName()
	 */
	@Override
	public String getPassName() {
		return new String("[LocalityAnalysis]");
	}

	/* (non-Javadoc)
	 * @see cetus.analysis.AnalysisPass#start()
	 */
	@Override
	public void start() {
		shrdSclrCachingOnReg = false;
		String value = Driver.getOptionValue("shrdSclrCachingOnReg");
		if( value != null ) {
			shrdSclrCachingOnReg = true;
		}
		shrdArryElmtCachingOnReg = false;
		value = Driver.getOptionValue("shrdArryElmtCachingOnReg");
		if( value != null ) {
			shrdArryElmtCachingOnReg = true;
		}
		shrdSclrCachingOnSM = false;
		value = Driver.getOptionValue("shrdSclrCachingOnSM");
		if( value != null ) {
			shrdSclrCachingOnSM = true;
		}
		prvtArryCachingOnSM = false;
		value = Driver.getOptionValue("prvtArryCachingOnSM");
		if( value != null ) {
			prvtArryCachingOnSM = true;
		}
		shrdArryCachingOnTM = false;
		value = Driver.getOptionValue("shrdArryCachingOnTM");
		if( value != null ) {
			shrdArryCachingOnTM = true;
		}
		shrdSclrCachingOnConst = false;
		value = Driver.getOptionValue("shrdSclrCachingOnConst");
		if( value != null ) {
			shrdSclrCachingOnConst = true;
		}
		shrdArryCachingOnConst = false;
		value = Driver.getOptionValue("shrdArryCachingOnConst");
		if( value != null ) {
			shrdArryCachingOnConst = true;
		}
		extractTuningParameters = false;
		value = Driver.getOptionValue("extractTuningParameters");
		if( value != null ) {
			extractTuningParameters = true;
		}
		
		AnalysisTools.markIntervalForKernelRegions(program);
		
		/////////////////////////////////////////////////////////////////////////////////////
		//DEBUG: CallGraph.getTopologicalCallList() returns only procedures reachable from //
		// the main procedure. To access all procedures, use an iterator.                  //
		/////////////////////////////////////////////////////////////////////////////////////
/*		// generate a list of procedures in post-order traversal
		CallGraph callgraph = new CallGraph(program);
		// procedureList contains Procedure in ascending order; the last one is main
		List<Procedure> procedureList = callgraph.getTopologicalCallList();*/
		/* iterate to search for all Procedures */
		DepthFirstIterator proc_iter = new DepthFirstIterator(program);
		Set<Procedure> procedureList = (Set<Procedure>)(proc_iter.getSet(Procedure.class));

		/* drive the engine; visit every procedure */
		for (Procedure proc : procedureList)
		{
			//DEBUG: if a share symbol is written in a previous kernel in the same procedure,
			//the symbol will not be cached in CUDA constant memory conservatively.
			//prevDefSymSet is used for this checking.
			Set<Symbol> prevDefSymSet = new HashSet<Symbol>();
			boolean constSetAdded = false;
			List<Statement> modifiedConstRegionList = new LinkedList<Statement>();
			List<OmpAnnotation> bBarrier_annots = (List<OmpAnnotation>)
			IRTools.collectPragmas(proc.getBody(), OmpAnnotation.class, "barrier");
			//FIXME: Filling prevDefSymSet will work correctly only if the below 
			//for-loop iterates omp_annots in a lexical order. 
			for( OmpAnnotation omp_annot : bBarrier_annots ) {
				String type = (String)omp_annot.get("barrier");
				Statement bstmt = null;
				Statement pstmt = null;
				if( type.equals("S2P") ) {
					bstmt = (Statement)omp_annot.getAnnotatable();
					pstmt = AnalysisTools.getStatementAfter((CompoundStatement)bstmt.getParent(), 
							bstmt);
				} else {
					continue;
				}
				OmpAnnotation annot = pstmt.getAnnotation(OmpAnnotation.class, "shared");
				if( annot != null ) {
					Set<Symbol> sharedVars = annot.get("shared");
					Set<Symbol> privVars = annot.get("private");
					Map<Expression, Set<Integer>> useExpMap = DataFlowTools.getUseMap(pstmt);
					Map<Expression, Set<Integer>> defExpMap = DataFlowTools.getDefMap(pstmt);
					Map<Symbol, Set<Integer>> useSymMap = DataFlowTools.convertExprMap2SymbolMap(useExpMap);
					Map<Symbol, Set<Integer>> defSymMap = DataFlowTools.convertExprMap2SymbolMap(defExpMap);
					Set<Symbol> useSymSet = useSymMap.keySet();
					Set<Symbol> defSymSet = defSymMap.keySet();
					Set<Expression> useExpSet = useExpMap.keySet();
					Set<Expression> defExpSet = defExpMap.keySet();
					HashSet<String> regROSet = null;
					HashSet<String> cudaRegROSet = new HashSet<String>();
					CudaAnnotation regROAnnot = null;
					HashSet<String> regRWSet = null;
					HashSet<String> cudaRegRWSet = new HashSet<String>();
					HashSet<String> noRegSet = new HashSet<String>();
					CudaAnnotation regRWAnnot = null;
					HashSet<String> sharedROSet = null;
					HashSet<String> cudaSharedROSet = new HashSet<String>();
					CudaAnnotation sharedROAnnot = null;
					HashSet<String> sharedRWSet = null;
					HashSet<String> cudaSharedRWSet = new HashSet<String>();
					HashSet<String> noSharedSet = new HashSet<String>();
					CudaAnnotation sharedRWAnnot = null;
					HashSet<String> textureSet = null;
					HashSet<String> cudaTextureSet = new HashSet<String>();
					HashSet<String> noTextureSet = new HashSet<String>();
					CudaAnnotation textureAnnot = null;
					HashSet<String> constSet = null;
					HashSet<String> cudaConstSet = new HashSet<String>();
					HashSet<String> noConstSet = new HashSet<String>();
					CudaAnnotation constAnnot = null;
					////////////////////////////
					// Tunable parameter sets //
					////////////////////////////
					HashSet<String> tRegisterROSet = new HashSet<String>();
					HashSet<String> tRegisterRWSet = new HashSet<String>();
					HashSet<String> tSharedROSet = new HashSet<String>();
					HashSet<String> tSharedRWSet = new HashSet<String>();
					HashSet<String> tTextureSet = new HashSet<String>();
					HashSet<String> tConstantSet = new HashSet<String>();
					HashSet<String> tSclrConstSet = new HashSet<String>();
					HashSet<String> tArryConstSet = new HashSet<String>();
					HashSet<String> tROShSclrNL = new HashSet<String>();
					HashSet<String> tROShSclr = new HashSet<String>();
					HashSet<String> tRWShSclr = new HashSet<String>();
					HashSet<String> tROShArEl = new HashSet<String>();
					HashSet<String> tRWShArEl = new HashSet<String>();
					HashSet<String> tRO1DShAr = new HashSet<String>();
					HashSet<String> tPrvAr = new HashSet<String>();
					CudaAnnotation aInfoAnnot = pstmt.getAnnotation(CudaAnnotation.class, "ainfo");
					List<CudaAnnotation> cudaAnnots = pstmt.getAnnotations(CudaAnnotation.class);
					if( cudaAnnots != null ) {
						for( CudaAnnotation cannot : cudaAnnots ) {
							HashSet<String> dataSet = (HashSet<String>)cannot.get("registerRO");
							if( dataSet != null ) {
								regROSet = dataSet;
								regROAnnot = cannot;
								continue;
							}
							dataSet = (HashSet<String>)cannot.get("registerRW");
							if( dataSet != null ) {
								regRWSet = dataSet;
								regRWAnnot = cannot;
								continue;
							}
							dataSet = (HashSet<String>)cannot.get("noregister");
							if( dataSet != null ) {
								noRegSet.addAll(dataSet);
								continue;
							}
							dataSet = (HashSet<String>)cannot.get("sharedRO");
							if( dataSet != null ) {
								sharedROSet = dataSet;
								sharedROAnnot = cannot;
								continue;
							}
							dataSet = (HashSet<String>)cannot.get("sharedRW");
							if( dataSet != null ) {
								sharedRWSet = dataSet;
								sharedRWAnnot = cannot;
								continue;
							}
							dataSet = (HashSet<String>)cannot.get("noshared");
							if( dataSet != null ) {
								noSharedSet.addAll(dataSet);
								continue;
							}
							dataSet = (HashSet<String>)cannot.get("texture");
							if( dataSet != null ) {
								textureSet = dataSet;
								textureAnnot = cannot;
								continue;
							}
							dataSet = (HashSet<String>)cannot.get("notexture");
							if( dataSet != null ) {
								noTextureSet.addAll(dataSet);
								continue;
							}
							dataSet = (HashSet<String>)cannot.get("constant");
							if( dataSet != null ) {
								constSet = dataSet;
								constAnnot = cannot;
								continue;
							}
							dataSet = (HashSet<String>)cannot.get("noconstant");
							if( dataSet != null ) {
								noConstSet.addAll(dataSet);
								continue;
							}
						}
					}
					int useCnt = 0;
					int defCnt = 0;
					for( Symbol sym: sharedVars ) {
						//////////////////////////////////////////////////////////////////////////////////////////
						// DEBUG: This analysis assumes that input shared variables are either scalar variables //
						// or array variables. However, SymbolTools.isScalar(sym) does not distinguish scalar   //
						// variables from pointer variables. Therefore, we add pointer-type checking at the     //
						// beginning.                                                                           //
						//////////////////////////////////////////////////////////////////////////////////////////
						if( SymbolTools.isPointer(sym) ) {
							continue;
						}
						boolean isStruct = false;
						///////////////////////////////////////////////////////////////////////////////////////////
						// DEBUG: Parameter symbol is a child of ProcedureDeclarator, which is a member field    //
						// of the enclosing Procedure, but not a child. Therefore, traversing from the parameter //
						// symbol can not reach the Procedure.                                                   //
						///////////////////////////////////////////////////////////////////////////////////////////
						if( proc.containsSymbol(sym) ) {
							isStruct = SymbolTools.isStruct(sym, proc);
						} else {
							isStruct = SymbolTools.isStruct(sym, (Traversable)sym);
						}
						useCnt = 0;
						defCnt = 0;
						if( useSymSet.contains(sym) ) {
							useCnt = useSymMap.get(sym).size();
						}
						if( defSymSet.contains(sym) ) {
							defCnt = defSymMap.get(sym).size();
							prevDefSymSet.add(sym);
						}
						if( (defCnt == 0) && AnalysisTools.ipaIsDefined(sym, pstmt) ) {
							defCnt = 2; //Assume that there is locality.
							prevDefSymSet.add(sym);
						}
						if( (useCnt <= 1) && (defCnt <= 1) ) {
							////////////////////////
							//No locality exists. //
							///////////////////////////////////////////////////////
							//Even if there is no locality, passing R/O shared   //
							// scalar variable as kernel parameter can save GPU  //
							// global memory access.                             //
							///////////////////////////////////////////////////////
							if( defCnt == 0 ) { 
								if( SymbolTools.isScalar(sym) ) {
									tSharedROSet.add(sym.getSymbolName());
									tROShSclrNL.add(sym.getSymbolName());
									tSclrConstSet.add(sym.getSymbolName());
									tConstantSet.add(sym.getSymbolName());
									if( isStruct ) {
										if( shrdSclrCachingOnConst ) {
											if( !prevDefSymSet.contains(sym) ) {
												cudaConstSet.add(sym.getSymbolName());
												constSetAdded = true;
											}
										} else if( shrdSclrCachingOnSM ) {
											cudaSharedROSet.add(sym.getSymbolName());
										} else {
											continue;
										}
									} else {
										if( shrdSclrCachingOnSM ) {
											cudaSharedROSet.add(sym.getSymbolName());
										} else if( shrdSclrCachingOnConst ) {
											if( !prevDefSymSet.contains(sym) ) {
												cudaConstSet.add(sym.getSymbolName());
												constSetAdded = true;
											}
										} else {
											continue;
										}
									}
								} else {
									tArryConstSet.add(sym.getSymbolName());
									tConstantSet.add(sym.getSymbolName());
									tRO1DShAr.add(sym.getSymbolName());
									List aspecs = sym.getArraySpecifiers();
									ArraySpecifier aspec = (ArraySpecifier)aspecs.get(0);
									int dimsize = aspec.getNumDimensions();
									if( (dimsize == 1) && !isStruct ) {
										tTextureSet.add(sym.getSymbolName());
									}
									if (shrdArryCachingOnTM) {
										if( (dimsize == 1) && !isStruct ) {
											cudaTextureSet.add(sym.getSymbolName());
										} else if( shrdArryCachingOnConst ) {
											if( !prevDefSymSet.contains(sym) ) {
												cudaConstSet.add(sym.getSymbolName());
												constSetAdded = true;
											}
										} else {
											continue;
										}
									} else if( shrdArryCachingOnConst ) {
											if( !prevDefSymSet.contains(sym) ) {
												cudaConstSet.add(sym.getSymbolName());
												constSetAdded = true;
											}
									} else {
										continue;
									}
								}
							} else {
								continue;
							}
						} else if ( defCnt == 0 ) {
							//R/O shared variable
							if( SymbolTools.isScalar(sym) ) {
								if( isStruct ) {
									/////////////////////////////////////////////
									// For R/O shared scalar struct variables, //
									// caching on Const is preferred method.   //
									/////////////////////////////////////////////
									if( shrdSclrCachingOnConst ) {
										if( !prevDefSymSet.contains(sym) ) {
											cudaConstSet.add(sym.getSymbolName());
											constSetAdded = true;
										}
									} else if( shrdSclrCachingOnSM ) {
										cudaSharedROSet.add(sym.getSymbolName());
									}
									tSharedROSet.add(sym.getSymbolName());
									tROShSclr.add(sym.getSymbolName());
									tSclrConstSet.add(sym.getSymbolName());
									tConstantSet.add(sym.getSymbolName());
								} else {
									////////////////////////////////////////
									// For R/O shared scalar variables,   //
									// caching on SM is preferred method. //
									////////////////////////////////////////
									if( shrdSclrCachingOnSM ) {
										cudaSharedROSet.add(sym.getSymbolName());
									} else if( shrdSclrCachingOnConst ) {
										if( !prevDefSymSet.contains(sym) ) {
											cudaConstSet.add(sym.getSymbolName());
											constSetAdded = true;
										}
									} else if( shrdSclrCachingOnReg ) {
										cudaRegROSet.add(sym.getSymbolName());
									}
									tSharedROSet.add(sym.getSymbolName());
									tRegisterROSet.add(sym.getSymbolName());
									tROShSclr.add(sym.getSymbolName());
									tSclrConstSet.add(sym.getSymbolName());
									tConstantSet.add(sym.getSymbolName());
								}
							} else if( SymbolTools.isArray(sym) ) {
								List aspecs = sym.getArraySpecifiers();
								ArraySpecifier aspec = (ArraySpecifier)aspecs.get(0);
								int dimsize = aspec.getNumDimensions();
								if( (dimsize == 1) && !isStruct ) {
									if( shrdArryCachingOnTM ) {
										cudaTextureSet.add(sym.getSymbolName());
									}
									tTextureSet.add(sym.getSymbolName());
									tRO1DShAr.add(sym.getSymbolName());
								}
								tArryConstSet.add(sym.getSymbolName());
								tConstantSet.add(sym.getSymbolName());
								for( Expression exp : useExpSet ) {
									if( exp instanceof ArrayAccess ) {
										ArrayAccess aa = (ArrayAccess)exp;
										if( IRTools.containsSymbol(aa.getArrayName(), sym) ) {
											if( useExpMap.get(exp).size() > 1 ) {
												if( shrdArryElmtCachingOnReg && !cudaTextureSet.contains(sym.getSymbolName()) ) {
													cudaRegROSet.add(aa.toString());
												}
												tRegisterROSet.add(aa.toString());
												tROShArEl.add(aa.toString());
											}
										}
									}
								}
								if( !cudaTextureSet.contains(sym.getSymbolName()) && shrdArryCachingOnConst ) {
									if( !prevDefSymSet.contains(sym) ) {
										cudaConstSet.add(sym.getSymbolName());
										constSetAdded = true;
									}
								}
							}
						} else {
							//R/W shared variable
							if( SymbolTools.isScalar(sym) ) {
								///////////////////////////////////////
								// For R/W shared scalar variables,  //
								// caching on Register is preferred. //
								///////////////////////////////////////
								if( shrdSclrCachingOnReg && !isStruct ) {
									cudaRegRWSet.add(sym.getSymbolName());
								} else if( shrdSclrCachingOnSM ) {
									cudaSharedRWSet.add(sym.getSymbolName());
								}
								if(!isStruct ) {
									tRegisterRWSet.add(sym.getSymbolName());
								}
								tRWShSclr.add(sym.getSymbolName());
								tSharedRWSet.add(sym.getSymbolName());
							} else if( SymbolTools.isArray(sym) && !isStruct ) {
								for( Expression exp : defExpSet ) {
									if( exp instanceof ArrayAccess ) {
										ArrayAccess aa = (ArrayAccess)exp;
										if( IRTools.containsSymbol(aa.getArrayName(), sym) ) {
											if( (defExpMap.get(exp).size() > 1) || 
													(useExpSet.contains(exp) && (useExpMap.get(exp).size() > 1)) ) {
												if( shrdArryElmtCachingOnReg ) {
													////////////////////////////////////
													// Remove any '(', ')', or space. //
													////////////////////////////////////////////////////
													// FIXME: this comparison assumes that            //
													// array access strings in cuda noregister clause //
													// do not have any '(', ')', or space.            //
													////////////////////////////////////////////////////
													String aAccessString = aa.toString();
													StringBuilder strB = new StringBuilder(aAccessString);
													int index = strB.toString().indexOf('(');
													while ( index != -1 ) {
														strB = strB.deleteCharAt(index);
														index = strB.toString().indexOf('(');
													}
													index = strB.toString().indexOf(')');
													while ( index != -1 ) {
														strB = strB.deleteCharAt(index);
														index = strB.toString().indexOf(')');
													}
													index = strB.toString().indexOf(' ');
													while ( index != -1 ) {
														strB = strB.deleteCharAt(index);
														index = strB.toString().indexOf(' ');
													}
													String aAccessString2 = strB.toString();
													if( !noRegSet.contains(aAccessString2) ) {
														cudaRegRWSet.add(aa.toString());
													}
												}
												tRegisterRWSet.add(aa.toString());
												tRWShArEl.add(aa.toString());
											}
										}
									}
								}
							}
						}
					}
					for( Symbol sym: privVars ) {
						if( SymbolTools.isArray(sym) ) {
							if( prvtArryCachingOnSM ) {
								cudaSharedRWSet.add(sym.getSymbolName());
							}
							tSharedRWSet.add(sym.getSymbolName());
							tPrvAr.add(sym.getSymbolName());
						}
					}
					cudaRegROSet.removeAll(noRegSet);
					if( cudaRegROSet.size() > 0 ) {
						if( regROAnnot == null ) {
							regROAnnot = new CudaAnnotation("gpurun", "true");
							regROAnnot.put("registerRO", cudaRegROSet);
							pstmt.annotate(regROAnnot);
						} else {
							regROSet.addAll(cudaRegROSet);
						}
					}
					cudaRegRWSet.removeAll(noRegSet);
					if( cudaRegRWSet.size() > 0 ) {
						if( regRWAnnot == null ) {
							regRWAnnot = new CudaAnnotation("gpurun", "true");
							regRWAnnot.put("registerRW", cudaRegRWSet);
							pstmt.annotate(regRWAnnot);
						} else {
							regRWSet.addAll(cudaRegRWSet);
						}
					}
					cudaSharedROSet.removeAll(noSharedSet);
					if( cudaSharedROSet.size() > 0 ) {
						if( sharedROAnnot == null ) {
							sharedROAnnot = new CudaAnnotation("gpurun", "true");
							sharedROAnnot.put("sharedRO", cudaSharedROSet);
							pstmt.annotate(sharedROAnnot);
						} else {
							sharedROSet.addAll(cudaSharedROSet);
						}
					}
					cudaSharedRWSet.removeAll(noSharedSet);
					if( cudaSharedRWSet.size() > 0 ) {
						if( sharedRWAnnot == null ) {
							sharedRWAnnot = new CudaAnnotation("gpurun", "true");
							sharedRWAnnot.put("sharedRW", cudaSharedRWSet);
							pstmt.annotate(sharedRWAnnot);
						} else {
							sharedRWSet.addAll(cudaSharedRWSet);
						}
					}
					cudaTextureSet.removeAll(noTextureSet);
					if( cudaTextureSet.size() > 0 ) {
						if( textureAnnot == null ) {
							textureAnnot = new CudaAnnotation("gpurun", "true");
							textureAnnot.put("texture", cudaTextureSet);
							pstmt.annotate(textureAnnot);
						} else {
							textureSet.addAll(cudaTextureSet);
						}
					}
					cudaConstSet.removeAll(noConstSet);
					if( cudaConstSet.size() > 0 ) {
						if( constAnnot == null ) {
							constAnnot = new CudaAnnotation("gpurun", "true");
							constAnnot.put("constant", cudaConstSet);
							pstmt.annotate(constAnnot);
						} else {
							constSet.addAll(cudaConstSet);
						}
						if( constSetAdded ) {
							modifiedConstRegionList.add(pstmt);
							constSetAdded = false;
						}
					}
					if( extractTuningParameters && (aInfoAnnot != null) ) {
						//"tuningparameter" clause is used only internally .
						CudaAnnotation tAnnot = pstmt.getAnnotation(CudaAnnotation.class, "tuningparameters");
						if( tAnnot == null ) {
							tAnnot = new CudaAnnotation("tuningparameters", "true");
							pstmt.annotate(tAnnot);
						}
						if( tRegisterROSet.size() > 0 ) {
							tAnnot.put("registerRO", tRegisterROSet);
						}
						if( tRegisterRWSet.size() > 0 ) {
							tAnnot.put("registerRW", tRegisterRWSet);
						}
						if( tSharedROSet.size() > 0 ) {
							tAnnot.put("sharedRO", tSharedROSet);
						}
						if( tSharedRWSet.size() > 0 ) {
							tAnnot.put("sharedRW", tSharedRWSet);
						}
						if( tTextureSet.size() > 0 ) {
							tAnnot.put("texture", tTextureSet);
						}
						if( tConstantSet.size() > 0 ) {
							tAnnot.put("constant", tConstantSet);
						}
						if( tSclrConstSet.size() > 0 ) {
							tAnnot.put("SclrConst", tSclrConstSet);
						}
						if( tArryConstSet.size() > 0 ) {
							tAnnot.put("ArryConst", tArryConstSet);
						}
						if( tROShSclrNL.size() > 0 ) {
							tAnnot.put("ROShSclrNL", tROShSclrNL);
						}
						if( tROShSclr.size() > 0 ) {
							tAnnot.put("ROShSclr", tROShSclr);
						}
						if( tRWShSclr.size() > 0 ) {
							tAnnot.put("RWShSclr", tRWShSclr);
						}
						if( tROShArEl.size() > 0 ) {
							tAnnot.put("ROShArEl", tROShArEl);
						}
						if( tRWShArEl.size() > 0 ) {
							tAnnot.put("RWShArEl", tRWShArEl);
						}
						if( tRO1DShAr.size() > 0 ) {
							tAnnot.put("RO1DShAr", tRO1DShAr);
						}
						if( tPrvAr.size() > 0 ) {
							tAnnot.put("PrvAr", tPrvAr);
						}
					}
				}
			}
			//DEBUG: if a shared variable is R/W in a kernel region and R/O in other kernel region
			//in the same procedure, caching the shared variable on CUDA constant memory for the
			//R/O kernel may be incorrect depending on other memory-transfer optimizations.
			//Therefore, if the shared variable is R/W in any of a kernel region in a procedure,
			//conservatively remove it from the CUDA constant clause.
			if( (!prevDefSymSet.isEmpty()) && (!modifiedConstRegionList.isEmpty()) ) {
				Set<String> defSymStringSet = AnalysisTools.symbolsToStringSet(prevDefSymSet);
				for( Statement pstmt : modifiedConstRegionList ) {
					List<CudaAnnotation> cudaAnnots = pstmt.getAnnotations(CudaAnnotation.class);
					if( cudaAnnots != null ) {
						boolean emptyAnnot =false;
						for( CudaAnnotation cannot : cudaAnnots ) {
							HashSet<String> dataSet = (HashSet<String>)cannot.get("constant");
							if( dataSet != null ) {
								dataSet.removeAll(defSymStringSet);
								if( dataSet.isEmpty() ) {
									cannot.remove("constant");
									emptyAnnot = true;
								}
							}
						}
						if( emptyAnnot ) {
							List<CudaAnnotation> newList = new LinkedList<CudaAnnotation>();
							for( CudaAnnotation cAnnot : cudaAnnots ) {
								if( !cAnnot.isEmpty() ) {
									newList.add(cAnnot);
								}
							}
							pstmt.removeAnnotations(CudaAnnotation.class);
							if( newList.size() > 0 ) {
								for( CudaAnnotation newAnnot : newList ) {
									pstmt.annotate(newAnnot);
								}
							} 
						}
					}	
				}
			}
		}
		
		SplitOmpPRegion.cleanExtraBarriers(program, false);

	}

}
