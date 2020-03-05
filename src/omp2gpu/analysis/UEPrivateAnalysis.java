package omp2gpu.analysis;

import java.util.*;

import cetus.exec.Driver;
import cetus.hir.*;
import cetus.analysis.*;
import omp2gpu.hir.*;
import omp2gpu.transforms.SplitOmpPRegion;

/**
 * <b>UEPrivateAnalysis</b> detects possible Upwardly-Exposed-Private variables.
 * 
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 */
public class UEPrivateAnalysis extends AnalysisPass {
	
	private int debug_level;
	// post-order traversal of Procedures in the Program
	private List<Procedure> procedureList;
	private boolean initStmtPrinted = false;

	/**
	 * @param program
	 */
	public UEPrivateAnalysis(Program program) {
		super(program);
		debug_level = PrintTools.getVerbosity();
	}

	/* (non-Javadoc)
	 * @see cetus.analysis.AnalysisPass#getPassName()
	 */
	@Override
	public String getPassName() {
		return new String("[UEPrivateAnalysis]");
	}

	/* (non-Javadoc)
	 * @see cetus.analysis.AnalysisPass#start()
	 */
	@Override
	public void start() {
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
		
		StringBuilder initStr = new StringBuilder(512);
		initStr.append("///////////////////////////////////////////////////////////////////////////\n");	
		initStr.append("// [WARNING] Be sure to check the following warning messages on possible //\n");
		initStr.append("// Upwardly-Exposed Private Variable problem, which means that some      //\n");
		initStr.append("// private variables are read before they are written. This problem may  //\n");
		initStr.append("// occur due to incorrect kernel-region splitting.                       //\n");
		initStr.append("// If this problem occurs, a programmer should fix it by modifying input //\n");
		initStr.append("// OpenMP program. Here are some suggestions:                            //\n");
		initStr.append("//   1) Try a built-in UEP Removal optimization (UEPRemovalOptLevel=N,   //\n");
		initStr.append("//      where N = 1, 2, or 3). Because this optimization may be unsafe,  //\n");
		initStr.append("//      the programmer should verify the correctness manually.           //\n");
		initStr.append("//   2) Remove unnecessary synchronizations such as removing unnecessary //\n");
		initStr.append("//      barriers or adding nowait clause to omp-for-loop annotations if  //\n");
		initStr.append("//      applicable.                                                      //\n"); 
		initStr.append("//   3) If all participating threads have the same value for the private //\n"); 
		initStr.append("//      variable,                                                        //\n");
		initStr.append("//      - Either add firstprivate OpenMP clause to related omp parallel  //\n");
		initStr.append("//        regions.                                                       //\n");
		initStr.append("//      - Or if the first method is not applicable, change the private   //\n");
		initStr.append("//        variable to  shared variable, and enclose statements that      //\n");
		initStr.append("//        write the variable with omp single directive.                  //\n");
		initStr.append("// CF: Due to the inaccuray of a compiler analysis, the following warning//\n");
		initStr.append("//     may be false ones. Here are some reasons causing wrong warning:   //\n");
		initStr.append("//   1) Current UEPrivateAnalysis conducts an intraprocedural analysis;  //\n");
		initStr.append("//      if a function is called inside of the target procedure, the      //\n"); 
		initStr.append("//      analysis conservatively assumes all variables of interest are    //\n");
		initStr.append("//      accessed in the called function, and thus analysis may result in //\n");
		initStr.append("//      overly estimated, false outputs.                                 //\n");
		initStr.append("//   2) Private arrays can be falsely included to the UEUSE set; even if //\n");
		initStr.append("//      they are initialized in for-loops, compiler may not be sure of   //\n");
		initStr.append("//     their initialization due to the possibility of zero-trip loops.   //\n");
		initStr.append("//   3) Current UEPrivateAnalysis handles scalar and array expressions,  //\n");
		initStr.append("//      but not pointer expressions. Therefore, if a memory region is    //\n");
		initStr.append("//      accessed both by a pointer expression and by an array expression,//\n ");
		initStr.append("//      the analysis may not be able to return accurate results.         //\n");
		initStr.append("///////////////////////////////////////////////////////////////////////////\n\n");	

		///////////////////////
		// DEBUG: deprecated //
		///////////////////////
		//RangeAnalysis range = new RangeAnalysis(program);
		
		boolean assumeNonZeroTripLoops = false;
		String value = Driver.getOptionValue("assumeNonZeroTripLoops");
		if( value != null ) {
			//assumeNonZeroTripLoops = Boolean.valueOf(value).booleanValue();
			assumeNonZeroTripLoops = true;
		}

		/* drive the engine; visit every procedure */
		for (Procedure proc : procedureList)
		{
			////////////////////////////////////////////////////////////////////////
			// This analysis is conducted on kernel functions and other functions // 
			// containing omp parallel region.                                    //
			////////////////////////////////////////////////////////////////////////
			List returnTypes = proc.getTypeSpecifiers();
			PrintTools.println("Procedure is "+returnTypes+" "+proc.getName(), 3);
			boolean is_Kernel_Func = false;
			boolean containsKernelFunc = false;
			if( returnTypes.contains(CUDASpecifier.CUDA_GLOBAL) ) {
				is_Kernel_Func = true;
			}
			List<OmpAnnotation> pRegion_annots = (List<OmpAnnotation>)
			IRTools.collectPragmas(proc.getBody(), OmpAnnotation.class, "parallel");
			if( AnalysisTools.containsKernelFunctionCall(proc.getBody()) ) {
				containsKernelFunc = true;
			}
			if( (!is_Kernel_Func) && (pRegion_annots.size() == 0) && (!containsKernelFunc) ) {
				PrintTools.println("The procedure, " + proc.getName() + ", is skipped.", 3);
				continue;
			}
		
			// Find a set of private variables in the kernel function.
			Set<Symbol> private_vars = SymbolTools.getLocalSymbols(proc.getBody());
			// Remove local procedure symbols from private_vars. //
			Set<Symbol> tSet = new HashSet<Symbol>();
			for( Symbol sm : private_vars ) {
				if( (sm instanceof Procedure) || (sm instanceof ProcedureDeclarator) ) {
					tSet.add(sm);
				}
			}
			private_vars.removeAll(tSet);
			//Remove static symbols.
			Set<Symbol> staticSymbols = AnalysisTools.getStaticVariables(private_vars);
			private_vars.removeAll(staticSymbols);
			/////////////////////////////////////////////////////////////////////////////////
			// FIXME: If proc is not a kernel function, and if a scalar function parameter //
			// of the procedure is used as a private variable, the above private_vars set  //
			// will not include the private variable. However, this omission is OK as long //
			// as this procedure is executed sequentially by CPU; if this procedure is     //
			// executed by multiple OpenMP threads, the function parameter variable should //
			// be included to the private_vars set to check whether it is upwardly exposed.//
			/////////////////////////////////////////////////////////////////////////////////
			PrintTools.print("Private variable symbols in a fucntion " + proc.getName() + " = ", 5);
			PrintTools.println("{" + PrintTools.collectionToString(private_vars, ",") + "}", 5);

			// get the range map
			// RangeAnalysis is not needed LiveAnalysis0 and ReachAnalysis0.
			//Map<Statement, RangeDomain> rmap = RangeAnalysis.getRanges(proc);

			OCFGraph.setNonZeroTripLoops(assumeNonZeroTripLoops);
			CFGraph cfg = new OCFGraph(proc, null);
			//CFGraph cfg = new CFGraph(proc, null);
			// get the parallel version of control flow graph
			//PCFGraph cfg = new PCFGraph(proc, null);

			// sort the control flow graph
			cfg.topologicalSort(cfg.getNodeWith("stmt", "ENTRY"));

			// attach the range information to the control flow graph
			//AnalysisTools.addRangeDomainToCFG(cfg, rmap);

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
			if( is_Kernel_Func || containsKernelFunc ) {
				// Enter the entry node in the work_list
				ueuse_set = new HashSet<Symbol>();
				List<DFANode> entry_nodes = cfg.getEntryNodes();
				if (entry_nodes.size() > 1)
				{
					PrintTools.println("[WARNING in UEPrivateAnalysis()] multiple entries in the funcion, " 
							+ proc.getName(), 2);
				}

				for ( DFANode entry_node : entry_nodes ) {
					//ueuse = (Section.MAP)entry_node.getData("ueuse");
					ueuse = (Set<Symbol>)entry_node.getData("ueuse");
					//for( Symbol sym : ueuse.keySet() ) {
					for( Symbol sym : ueuse ) {
						String sname = sym.getSymbolName();
						if( !sname.startsWith("_gtid") && 
							!sname.startsWith("_bid") &&
							!sname.startsWith("_ti_100_") &&
							!sname.startsWith("row_temp_") &&
							!sname.endsWith("__extended") &&
							!sname.startsWith("gpu__") &&
							!sname.startsWith("red__") &&
							!sname.startsWith("lred__") &&
							!sname.startsWith("param__") &&
							!sname.startsWith("pitch__") &&
							!sname.startsWith("const__") &&
							!sname.startsWith("sh__") ) {
							ueuse_set.add(sym);
						}		
					}
				}
				////////////////////////////////////////////////////////////////////////////////////////////
				// Check whether private variables in ueuse_set are initialized in declaration statement. //
				////////////////////////////////////////////////////////////////////////////////////////////
				Set<Symbol> removeSet = new HashSet<Symbol>();
				for( Symbol sym : ueuse_set ) {
					if( (sym instanceof VariableDeclarator) && 
							((VariableDeclarator)sym).getInitializer() != null ) {
						removeSet.add(sym);
					}
				}
				ueuse_set.removeAll(removeSet);
				
				if( ueuse_set.size() > 0 ) {
					if( !initStmtPrinted ) {
						PrintTools.println(initStr.toString(), 0);
						initStmtPrinted = true;
					}
					StringBuilder str = new StringBuilder(512);
					str.append("///////////////////////////////////////////////////////////////////////////\n");	
					str.append("// [WARNING] Upward-Exposed Use of private variables in the following\n");
					str.append("// function: " + proc.getName() + "\n");	
					str.append("// The following private variables seem to be used before written. \n");
					str.append("// UEUSE: " + ueuse_set + "\n");
					str.append("///////////////////////////////////////////////////////////////////////////\n");	
					PrintTools.println(str.toString(), 0);
				}
			} else {
				PrintTools.println("Number of cetus parallel annotations in this procedure: "
						+ pRegion_annots.size() , 3);

				HashSet<Statement> stmtSet = new HashSet<Statement>();
				HashMap<Statement, Statement> pRMap = new HashMap<Statement, Statement>();
				HashMap<Statement, Annotation> pAMap = new HashMap<Statement, Annotation>();
				HashSet<Statement> kRegionSet = new HashSet<Statement>();
				HashMap<Statement, Set<Symbol>> accessedSymMap = new HashMap<Statement, Set<Symbol>>();
				List<OmpAnnotation> bBarrier_annots = (List<OmpAnnotation>)
				IRTools.collectPragmas(proc.getBody(), OmpAnnotation.class, "barrier");
				for( OmpAnnotation omp_annot : bBarrier_annots ) {
					String type = (String)omp_annot.get("barrier");
					Statement bstmt = null;
					Statement pstmt = null;
					OmpAnnotation oAnnot = null;
					if( type.equals("S2P") ) {
						bstmt = (Statement)omp_annot.getAnnotatable();
						pstmt = AnalysisTools.getStatementAfter((CompoundStatement)bstmt.getParent(), 
								bstmt);
						kRegionSet.add(pstmt);
						oAnnot = pstmt.getAnnotation(OmpAnnotation.class, "parallel");
						pAMap.put(pstmt, oAnnot);
						
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
						Statement pstmt = pRMap.get(foundStmt);
						PrintTools.println("Found parallel region of interest!", 3);
						PrintTools.println("====> " + pstmt + "\n", 3);
						ueuse_set = new HashSet<Symbol>();
						ueuse = (Set<Symbol>)node.getData("ueuse");
						Set<Symbol> accessedSymbols = SymbolTools.getAccessedSymbols(pstmt);
						//for( Symbol sym : ueuse.keySet() ) {
						for( Symbol sym : ueuse ) {
							String sname = sym.getSymbolName();
							if( !sname.startsWith("_gtid") && 
									!sname.startsWith("pitch__") && 
									!sname.startsWith("red__") && 
									!sname.startsWith("lred__") && 
									!sname.endsWith("__extended") &&
									!sname.startsWith("gpu__") ) {
								if( accessedSymbols.contains(sym) ) {
									ueuse_set.add(sym);
								}
							}		
						}
						if( ueuse_set.size() > 0 ) {
							if( !initStmtPrinted ) {
								PrintTools.println(initStr.toString(), 0);
								initStmtPrinted = true;
							}
							StringBuilder str = new StringBuilder(512);
							str.append("///////////////////////////////////////////////////////////////////////////\n");	
							str.append("// [WARNING] Upward-Exposed Use of local variables in the following\n");
							str.append("// function: " + proc.getName() + "\n");	
							str.append("// The following local variables seem to be used before written in  \n");
							str.append("// the parallel region annotated by this cetus annotation: \n");
							str.append("// Annotation: "+pAMap.get(pstmt) + "\n");
							str.append("// UEUSE: " + ueuse_set + "\n");
							str.append("///////////////////////////////////////////////////////////////////////////\n");	
							PrintTools.println(str.toString(), 0);
						}
						stmtSet.remove(foundStmt);}
				}
			}

			AnalysisTools.displayCFG(cfg, debug_level);
		}
		SplitOmpPRegion.cleanExtraBarriers(program, false);
	}
	


}
