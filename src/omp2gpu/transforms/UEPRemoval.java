/**
 * 
 */
package omp2gpu.transforms;

import java.util.*;

import omp2gpu.analysis.AnalysisTools;
import omp2gpu.analysis.LiveAnalysis0;
import omp2gpu.analysis.OCFGraph;
import omp2gpu.analysis.ReachAnalysis0;

import cetus.analysis.CFGraph;
import cetus.analysis.CallGraph;
import cetus.analysis.DFANode;
import cetus.analysis.LoopTools;
import cetus.exec.Driver;
import cetus.hir.*;
import cetus.transforms.TransformPass;

/**
 * <b>UEPRemoval</b> solves upwardly-exposed private variable problems.
 * 
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 */
public class UEPRemoval extends TransformPass {
	
	private int UEPRemovalOptLevel = 0;
	private int verbosity = 0;

	/**
	 * @param program
	 */
	public UEPRemoval(Program program) {
		super(program);
		verbosity =
	      Integer.valueOf(Driver.getOptionValue("verbosity")).intValue();
	}

	/* (non-Javadoc)
	 * @see cetus.transforms.TransformPass#getPassName()
	 */
	@Override
	public String getPassName() {
		return new String("[Upwardly-Exposed Private Variable Removal]");
	}

	/* (non-Javadoc)
	 * @see cetus.transforms.TransformPass#start()
	 */
	@Override
	public void start() {
		boolean assumeNonZeroTripLoops = false;
		String value = Driver.getOptionValue("assumeNonZeroTripLoops");
		if( value != null ) {
			//assumeNonZeroTripLoops = Boolean.valueOf(value).booleanValue();
			assumeNonZeroTripLoops = true;
		}
		value = Driver.getOptionValue("UEPRemovalOptLevel");
		if( value != null ) {
			UEPRemovalOptLevel = Integer.valueOf(value).intValue();
		}
		if( UEPRemovalOptLevel == 0 ) {
			return;
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
		DepthFirstIterator proc_iter = new DepthFirstIterator(program);
		Set<Procedure> procedureList = (Set<Procedure>)(proc_iter.getSet(Procedure.class));
		HashSet<Procedure> visitedProcedures = new HashSet<Procedure>();
		

		/* drive the engine; visit every procedure */
		for (Procedure proc : procedureList)
		{
			//Mapping between UEP symbols and their enclosing kernel regions
			HashMap<Symbol, HashSet<Statement>> sym2StmtMap = new HashMap<Symbol, HashSet<Statement>>();
			///////////////////////////////////////////////////////////////////////////////////////////
			//Private symbols to their enclosing kernel regions mapping, where DEF-statements of the //
			//symbols should be copied before the kernel regions.                                    //
			///////////////////////////////////////////////////////////////////////////////////////////
			HashMap<Symbol, HashSet<Statement>> sym2StmtMap2 = new HashMap<Symbol, HashSet<Statement>>();
			HashSet<Statement> stmtSet = new HashSet<Statement>();
			HashMap<Statement, Statement> pRMap = new HashMap<Statement, Statement>();
			HashSet<Statement> kRegionSet = new HashSet<Statement>();
			HashMap<Statement, Set<Symbol>> accessedSymMap = new HashMap<Statement, Set<Symbol>>();
			HashMap<Statement, Set<Symbol>> defSymMap = new HashMap<Statement, Set<Symbol>>();
			List<OmpAnnotation> bBarrier_annots = (List<OmpAnnotation>)
			IRTools.collectPragmas(proc.getBody(), OmpAnnotation.class, "barrier");
			for( OmpAnnotation omp_annot : bBarrier_annots ) {
				String type = (String)omp_annot.get("barrier");
				Statement bstmt = null;
				Statement pstmt = null;
				if( type.equals("S2P") ) {
					bstmt = (Statement)omp_annot.getAnnotatable();
					pstmt = AnalysisTools.getStatementAfter((CompoundStatement)bstmt.getParent(), 
							bstmt);
					kRegionSet.add(pstmt);
				} else {
					continue;
				}
			}
			if( kRegionSet.isEmpty() ) {
				continue;
			} else {
				for( Statement pstmt : kRegionSet ) {
					if( pstmt instanceof ForLoop) {
						pRMap.put(((ForLoop)pstmt).getInitialStatement(), pstmt);
					} else if( pstmt instanceof CompoundStatement ) {
						List<Traversable> childList = pstmt.getChildren();
						for( Traversable child : childList ) {
							//Find the first statement that is neither AnnotationStatement
							//and nor DeclarationStatement.
							if( !(child instanceof DeclarationStatement) &&
									!(child instanceof AnnotationStatement) ) {
								Statement fstmt = null;
								if( child instanceof ForLoop ) {
									fstmt = ((ForLoop)child).getInitialStatement();
								} else {
									fstmt = (Statement)child;
								}
								pRMap.put(fstmt, pstmt);
								break;
							}
						}
					} 	
				}
				stmtSet.addAll(pRMap.keySet());
			}
			
			// Find a set of private variables in a current procedure.
			Set<Symbol> private_vars = SymbolTools.getLocalSymbols(proc.getBody());
			// Remove local procedure symbols from private_vars. //
			Set<Symbol> tSet = new HashSet<Symbol>();
			for( Symbol sm : private_vars ) {
				if( (sm instanceof Procedure) || (sm instanceof ProcedureDeclarator) ) {
					tSet.add(sm);
				}
			}
			private_vars.removeAll(tSet);
			
			//Find the first non-declaration statement in the procedure body.
			Statement firstNonDeclStmt = IRTools.getFirstNonDeclarationStatement(proc.getBody());
			
			OCFGraph.setNonZeroTripLoops(assumeNonZeroTripLoops);
			CFGraph cfg = new OCFGraph(proc, null);

			// sort the control flow graph
			cfg.topologicalSort(cfg.getNodeWith("stmt", "ENTRY"));

			//DEBUG: ReachAnalysis0 is not needed.
/*			// perform reaching-definition analysis (It should come before LiveAnalysis)
			ReachAnalysis0 reach_analysis = new ReachAnalysis0(proc, cfg, private_vars);
			reach_analysis.run();*/

			// perform live-out analysis (It should come after ReachAnalysis)
			LiveAnalysis0 live_analysis = new LiveAnalysis0(proc, cfg, private_vars, false);
			live_analysis.run();
			
			//Section.MAP ueuse = null;
			Set<Symbol> ueuse = null;
			Set<Symbol> ueuse_set = null;
			
			Iterator<DFANode> iter = cfg.iterator();
			while ( iter.hasNext() )
			{
				DFANode node = iter.next();
				if( stmtSet.size() == 0 ) {
					// All parallel regions of interest are searched.
					break;
				}
				Statement IRStmt = null;
				Object obj = node.getData("ir");
				if( obj instanceof Statement ) {
					IRStmt = (Statement)obj;
				} else {
					continue;
				}

				boolean found_pRegion = false;
				Statement foundStmt = null;
				for( Statement stmt : stmtSet ) {
					if( stmt.equals(IRStmt) ) {
						found_pRegion = true;
						foundStmt = stmt;
						break;
					}
				}
				if( found_pRegion ) {
					ueuse_set = new HashSet<Symbol>();
					ueuse = (Set<Symbol>)node.getData("ueuse");
					Statement pstmt = pRMap.get(foundStmt);
					Set<Symbol> accessedSymbols = SymbolTools.getAccessedSymbols(pstmt);
					accessedSymMap.put(pstmt, accessedSymbols);
					OmpAnnotation oAnnot = pstmt.getAnnotation(OmpAnnotation.class, "shared");
					Set<Symbol> sharedSymbols = new HashSet<Symbol>();
					if( oAnnot != null ) {
						sharedSymbols.addAll((Set<Symbol>)oAnnot.get("shared"));
					}
					Set<Symbol> redSymbols = new HashSet<Symbol>();
					oAnnot = pstmt.getAnnotation(OmpAnnotation.class, "reduction");
					if( oAnnot != null ) {
						HashMap reduction_map = oAnnot.get("reduction");
						if( reduction_map != null ) {
							for (String ikey : (Set<String>)(reduction_map.keySet())) {
								redSymbols.addAll((Collection<Symbol>)reduction_map.get(ikey));
							}
						}
					}
					for( Symbol sym : ueuse ) {
						String sname = sym.getSymbolName();
						if( !sname.startsWith("_gtid") && 
								!sname.startsWith("pitch__") && 
								!sname.startsWith("red__") && 
								!sname.startsWith("lred__") && 
								!sname.endsWith("__extended") &&
								!sname.startsWith("const__") && 
								!sname.startsWith("gpu__") ) {
							///////////////////////////////////////////////////////////////////////////
							//Depending on the OpenMP default clause setting, local variables can be //
							//shared variables. In this case, no UEP variable problem occurs.        //
							///////////////////////////////////////////////////////////////////////////
							if( accessedSymbols.contains(sym) && !sharedSymbols.contains(sym) 
									&& !redSymbols.contains(sym) ) {
								ueuse_set.add(sym);
							}
						}		
					}
					if( !ueuse_set.isEmpty() ) {
						HashSet<Statement> UEStmtSet = null;
						for( Symbol pSym : ueuse_set ) {
							// Current implementation handles scalar private variables only.
							if( SymbolTools.isScalar(pSym) && !SymbolTools.isPointer(pSym) ) {
								if( sym2StmtMap.keySet().contains(pSym) ) {
									UEStmtSet = sym2StmtMap.get(pSym);
								} else {
									UEStmtSet = new HashSet<Statement>();
									sym2StmtMap.put(pSym, UEStmtSet);
								}
								UEStmtSet.add(pstmt);
							}
						}
					}
					//////////////////////////////////////////////////////////////
					//Check whether omp-for loops contain private symbols in    //
					// initial statement, condition, or step expression.        //
					//If so, and if the private symbols are not in ueuse_set,   //
					//the DEF-statements of the symbols should be copied before //
					//this kernel region to remove UEP problem.                 //
					//This conversion is needed since the private symbols are   //
					//used for calculating the iteration space of the omp-for   //
					//loops.                                                    //
					//////////////////////////////////////////////////////////////
					List<OmpAnnotation> ompfor_annots = (List<OmpAnnotation>)
					IRTools.collectPragmas(pstmt, OmpAnnotation.class, "for");
					for( OmpAnnotation fAnnot : ompfor_annots ) {
						Annotatable at = fAnnot.getAnnotatable();
						if( at instanceof ForLoop ) {
							ForLoop fLoop = (ForLoop)at;
							Set<Symbol> accSyms = new HashSet<Symbol>();
							accSyms.addAll(SymbolTools.getAccessedSymbols(fLoop.getInitialStatement()));
							accSyms.addAll(SymbolTools.getAccessedSymbols(fLoop.getCondition()));
							accSyms.addAll(SymbolTools.getAccessedSymbols(fLoop.getStep()));
							Symbol indexSym = LoopTools.getLoopIndexSymbol(fLoop);
							accSyms.remove(indexSym);
							accSyms.retainAll(private_vars);
							accSyms.removeAll(ueuse_set);
							accSyms.removeAll(sharedSymbols);
							if( !accSyms.isEmpty() ) {
								HashSet<Statement> cpStmtSet = null;
								HashSet<Statement> UEStmtSet = null;
								for( Symbol pSym : accSyms ) {
									// Current implementation handles scalar private variables only.
									if( SymbolTools.isScalar(pSym) && !SymbolTools.isPointer(pSym) ) {
										if( sym2StmtMap2.keySet().contains(pSym) ) {
											cpStmtSet = sym2StmtMap2.get(pSym);
										} else {
											cpStmtSet = new HashSet<Statement>();
											sym2StmtMap2.put(pSym, cpStmtSet);
										}
										cpStmtSet.add(pstmt);
										if( sym2StmtMap.keySet().contains(pSym) ) {
											UEStmtSet = sym2StmtMap.get(pSym);
										} else {
											UEStmtSet = new HashSet<Statement>();
											sym2StmtMap.put(pSym, UEStmtSet);
										}
										UEStmtSet.add(pstmt);
									}
								}
							}
						} else {
							Tools.exit("[ERROR in UEPRemoval()] omp-for annotation is attached to " +
									"wrong type of statement.\n" + at);
						}
					}
					stmtSet.remove(foundStmt);
				}
			}
			if( !sym2StmtMap.keySet().isEmpty() ) {
				//Upwardly-exposed private symbols exist.
				PrintTools.println("UEP sybmols in a procedure, " + proc.getSymbolName() + ": "
						+ AnalysisTools.symbolsToString(sym2StmtMap.keySet(), ", "), 1);
				List<OmpAnnotation> pRegion_annots = (List<OmpAnnotation>)
				IRTools.collectPragmas(proc.getBody(), OmpAnnotation.class, "parallel");
				// Calculate DEF-symbol sets for each kernel region.
				for( Statement kStmt : kRegionSet ) {
					Set<Symbol> tDefSyms = DataFlowTools.getDefSymbol(kStmt);
					Set<Symbol> defSyms = AnalysisTools.getBaseSymbols(tDefSyms);
					defSymMap.put(kStmt, defSyms);
				}
				HashSet<Statement> UEStmtSet = null;
				HashSet<Statement> cpStmtSet = null;
				int convType = 0;
				///////////////////////////////////////////////////////////////////////////////////////////////////////
				// DEBUG: Iteration order of HashSet may change over time, and thus each compilation with the same   //
				// input may generate different output code; bad for debugging. To allow reproducible output, sorted //
				// set (TreeSet) is used instead of the original HashSet.                                            //
				///////////////////////////////////////////////////////////////////////////////////////////////////////
				//for( Symbol pSym : sym2StmtMap.keySet() ) {
			    TreeMap<String, Symbol> sortedMap = new TreeMap<String, Symbol>();
				for( Symbol pSym : sym2StmtMap.keySet() ) {
					String symName = pSym.getSymbolName();
					sortedMap.put(symName, pSym);
				}
				Collection<Symbol> sortedSet = sortedMap.values();
				for( Symbol pSym : sortedSet ) {
					boolean ROInKRegions = true;
					UEStmtSet = sym2StmtMap.get(pSym); 
					cpStmtSet = sym2StmtMap2.get(pSym); 
					if( cpStmtSet == null ) {
						cpStmtSet = new HashSet<Statement>();
					}
					HashMap<Statement, CompoundStatement> removeStmtMap = 
						new HashMap<Statement, CompoundStatement>();
					convType = 0;
					/////////////////////////////////////////////////////////////////////////////
					// Check whether the upwardly-exposed private (UEP) symbol is written in   //
					// other kernel regions in the current procedure. If written, add DEF-     //
					// statements to remveStmtMap.                                             //
					// DEBUG: This conversion assumes that UEP symbols have thread-independent //
					// values; users should verify this assumption manually.                   //
					/////////////////////////////////////////////////////////////////////////////
					for( Statement kStmt : kRegionSet ) {
						if( convType < 0 ) {
							///////////////////////////////////////////////////////////////////
							// Previously-checked kernel region contains a case that current //
							// analysis can not handle.                                      //
							///////////////////////////////////////////////////////////////////
							break;
						}
						if( defSymMap.get(kStmt).contains(pSym) ) {
							ROInKRegions = false;
							if( kStmt instanceof CompoundStatement ) {
								CompoundStatement cStmt = (CompoundStatement)kStmt;
								List<Traversable> children = cStmt.getChildren();
								for( Traversable child : children ) {
									Set<Symbol> tDefSyms = DataFlowTools.getDefSymbol(child);
									Set<Symbol> defSyms = AnalysisTools.getBaseSymbols(tDefSyms);
									if( defSyms.contains(pSym) ) {
										if( child instanceof ExpressionStatement ) {
											if( UEStmtSet.contains(kStmt) && !cpStmtSet.contains(kStmt) ) {
												/////////////////////////////////////////////////////////////////
												//Upwardly-exposed kernel region contains simple DEF-statement.//
												//Current analysis can not handle this case.                   //
												/////////////////////////////////////////////////////////////////
												convType = -3;
												break;
											} else {
												removeStmtMap.put((Statement)child, cStmt);
												UEStmtSet.add(kStmt);
											}
										} else if( child instanceof ForLoop ) {
											//////////////////////////////////////////////////
											//If UEP symbol is not used as an index symbol, //
											//the symbol may have thread-dependent value.   //
											//////////////////////////////////////////////////
											DepthFirstIterator itr = new DepthFirstIterator(child);
											boolean usedAsLoopIndex = false;
											while (itr.hasNext())
											{
												Object obj = itr.next();
												if (obj instanceof ForLoop )
												{
													Symbol indexSym = LoopTools.getLoopIndexSymbol((ForLoop)obj);
													if( indexSym.equals(pSym) ) {
														usedAsLoopIndex = true;
														break;
													}
												}
											}
											if( !usedAsLoopIndex ) {
												if( UEPRemovalOptLevel < 3 ) {
													///////////////////////////////////////////////////////////
													//Check whether this symbol is used as reduction symbol. //
													//If so, UEP variable problem is false one.              //
													///////////////////////////////////////////////////////////
													boolean usedAsRedVariable = false;
													OmpAnnotation oAnnot = ((ForLoop)child).getAnnotation(OmpAnnotation.class, "for");
													HashMap reduction_map = null;
													if( oAnnot != null ) {
														reduction_map = oAnnot.get("reduction");
														if( reduction_map != null ) {
															for (String ikey : (Set<String>)(reduction_map.keySet())) {
																if( ((Collection<Symbol>)reduction_map.get(ikey)).contains(pSym) ) {
																	usedAsRedVariable = true;
																	break;
																}
															}
														}
													}
													if( !usedAsRedVariable ) {
														oAnnot = kStmt.getAnnotation(OmpAnnotation.class, "reduction");
														if( oAnnot != null ) {
															reduction_map = oAnnot.get("reduction");
															if( reduction_map != null ) {
																for (String ikey : (Set<String>)(reduction_map.keySet())) {
																	if( ((Collection<Symbol>)reduction_map.get(ikey)).contains(pSym) ) {
																		usedAsRedVariable = true;
																		break;
																	}
																}
															}
														}
													}
													if( !usedAsRedVariable ) {
														convType = -1;
														if( verbosity > 1 ) {
															PrintTools.println("[current kernel region] \n" + kStmt + "\n", 2);
														}
														break;
													}
												}
											}
										} else {
											// UEP symbols is written in unexpected statement types.
											convType = -2;
											break;
										}
									}
								}
							} else if( kStmt instanceof ForLoop ) {
								//////////////////////////////////////////////////
								//If UEP symbol is not used as an index symbol, //
								//the symbol may have thread-dependent value.   //
								//////////////////////////////////////////////////
								DepthFirstIterator itr = new DepthFirstIterator(kStmt);
								boolean usedAsLoopIndex = false;
								while (itr.hasNext())
								{
									Object obj = itr.next();
									if (obj instanceof ForLoop )
									{
										Symbol indexSym = LoopTools.getLoopIndexSymbol((ForLoop)obj);
										if( indexSym.equals(pSym) ) {
											usedAsLoopIndex = true;
											break;
										}
									}
								}
								if( !usedAsLoopIndex ) {
									if( UEPRemovalOptLevel < 3 ) {
										convType = -1;
										if( verbosity > 1 ) {
											PrintTools.println("[current kernel region] \n" + kStmt + "\n", 2);
										}
										break;
									}
								}
							} else {
								PrintTools.println("[WARNING in UEPRemoval()] unexpected type of kernel region found: "
										+ kStmt + "\n", 0);
								convType = -4;
								break;
							}
						}
					}
					if( convType < 0 ) {
						if( verbosity == 0 ) {
							PrintTools.println("ConvType of " + pSym.getSymbolName() + " in a procedure, "+ proc.getSymbolName() 
									+ " : " + convType, 0);
						} else {
							PrintTools.println("ConvType of " + pSym.getSymbolName() + " : " + convType, 1);
						}
						if( convType == -1 ) {
							PrintTools.println("    [INFO] UEPRemoval pass may not be able to handle this type correctly; skipped.", 0);
							PrintTools.println("    [INFO] The symbol is written in a for-loop body; if the symbol has" +
									" thread-independent value, try UEPRemovalOptLevel > 2 ", 0);
						} else {
							PrintTools.println("    [INFO] UEPRemoval pass can not handle this type; skipped.", 0);
						}
						continue;
					}
					if( ROInKRegions ) {
						//////////////////////////////////////////////////////////////////////
						//Conversion type 0: upwardly-exposed private symbol is not written //
						//in any kernel regions in the current procedure.                   //
						//////////////////////////////////////////////////////////////////////
						convType = 0;
					} else if( removeStmtMap.isEmpty() ) {
						//////////////////////////////////////////////////////////////////////////////
						//Conversion type 1: upwardly-exposed private symbol is written in some     //
						//kernel regions in the current procedure, but used as loop-index variable  //
						//or reduction variable.                                                    //
						//////////////////////////////////////////////////////////////////////////////
						convType = 1;
					} else {
						/////////////////////////////////////////////////////////////////////////////
						//Conversion type 2: upwardly-exposed private symbol is written in some    //
						//kernel regions in the current procedure, but not as loop-index variable. //
						/////////////////////////////////////////////////////////////////////////////
						convType = 2;
					}
					PrintTools.println("ConvType of " + pSym.getSymbolName() + " : " + convType, 1);
					
					/////////////////////////////////////////////
					// Conversion steps to remove UEP variable //
					/////////////////////////////////////////////////////////////////////////////
					// If convType > 1                                                         //
					//    1) Move DEF-statements in kernel regions before the enclosing kernel //
					//       regions.                                                          //
					//    2) Add "static" specifier to the declaration of the UEP symbol.      //
					//        - If the symbol declaration has initialization part, create a    //
					//          separate initialization statement.                             //
					//    3) For each upwardly-exposed kernel region in the current procedure, //
					//        - Remove the symbol from omp private clause.                     //
					//        - Add the symbol to omp-firstprivate clause.                     //
					// If convType == 0 or 1, do steps 2) and 3) above.                        //
					/////////////////////////////////////////////////////////////////////////////
					if( convType > 1 ) {
						if( UEPRemovalOptLevel < 2 ) {
							if( verbosity == 0 ) {
								PrintTools.println("ConvType of " + pSym.getSymbolName() + " in a procedure, "+ proc.getSymbolName() 
									+ " : " + convType, 0);
							}
							PrintTools.println("    [INFO] Current opt-level does not handle the conversion for " + pSym.getSymbolName() + 
									"; try with UEPRemovalOptLevel > 1.", 0);
							continue;
						}
						// Move DEF-statements out of the enclosing kernel regions.
						for( Statement rStmt : removeStmtMap.keySet() ) {
							CompoundStatement cStmt = removeStmtMap.get(rStmt);
							cStmt.removeStatement(rStmt);
							Traversable p = cStmt.getParent();
							if( p instanceof CompoundStatement ) {
								((CompoundStatement)p).addStatementBefore(cStmt, rStmt);
							} else {
								Tools.exit("[ERROR in UEPRemoval()] unexpected parent; to remove this error," +
								" set UEPRemovalOptLevel < 2.");
							}
						}
					}
					if( convType >= 0 ) {
						Declaration decl = pSym.getDeclaration();
						//////////////////////////////////////////////////////////////////////////////////////
						//FIXME: For valid OpenMP semantic, a firstprivate variable should not be a local   //
						//variable. However, converting a local variable to a static one may cause problems //
						//in handling context-sensitive, interprocedural analyses, since it prevents        //
						//procedure cloning. For now, therefore, leave the local variable as it is.         //
						//////////////////////////////////////////////////////////////////////////////////////
/*						if( decl instanceof VariableDeclaration ) {
							List<Specifier> specs = ((VariableDeclaration)decl).getSpecifiers();
							if( !specs.contains(Specifier.STATIC) ) {
								specs.add(0, Specifier.STATIC);
								////////////////////////////////////////////////////////////////////////////////
								//If declarator has initial value, create a separate initialization statement //
								//and add it to the enclosing procedure body.                                 //
								////////////////////////////////////////////////////////////////////////////////
								if( pSym instanceof VariableDeclarator ) {
									VariableDeclarator vDeclr = (VariableDeclarator)pSym;
									Initializer init = vDeclr.getInitializer();
									//////////////////////////////////////////////////////////////////////////
									//DEBUG: It seems that Cetus parser uses Initializer for all types of   //
									//initialization, instead of using ListInitializer or ValueInitializer. //
									//////////////////////////////////////////////////////////////////////////
									//if( (init != null) && (init instanceof ValueInitializer) ) {
									if( init != null ) {
										List initList = init.getChildren();
										if( initList.size() == 1 ) {
											Expression initExp = (Expression)initList.get(0);
											AssignmentExpression aExp = new AssignmentExpression( new Identifier(pSym), 
													AssignmentOperator.NORMAL, initExp.clone());
											ExpressionStatement expStmt = new ExpressionStatement(aExp);
											proc.getBody().addStatementBefore(firstNonDeclStmt, expStmt);
											vDeclr.setInitializer(null);
										}
									}
								}
							}
						}*/
						for( Statement kStmt : UEStmtSet ) {
							OmpAnnotation oAnnot = kStmt.getAnnotation(OmpAnnotation.class, "parallel");
							Set<Symbol> privSet = (Set<Symbol>)oAnnot.get("private");
							Set<Symbol> fPrivSet = null;
							if( (privSet != null) && privSet.contains(pSym) ) {
								privSet.remove(pSym);
								if( privSet.isEmpty() ) {
									oAnnot.remove("private");
								}
								if( oAnnot.containsKey("firstprivate") ) {
									fPrivSet = (Set<Symbol>)oAnnot.get("firstprivate");
								} else {
									fPrivSet = new HashSet<Symbol>();
									oAnnot.put("firstprivate", fPrivSet);
								}
								fPrivSet.add(pSym);
							}
						}
					}
				}
			}
		}

		SplitOmpPRegion.cleanExtraBarriers(program, false);
	}

}
