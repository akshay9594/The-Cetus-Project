package omp2gpu.transforms;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.lang.Math;

import omp2gpu.analysis.AnalysisTools;
import omp2gpu.analysis.OmpAnalysis;
import omp2gpu.hir.CUDASpecifier;
import omp2gpu.hir.CudaAnnotation;

import cetus.analysis.LoopTools;
import cetus.hir.*;

/**
 * <b>TransformTools</b> provides tools for various transformation tools for OpenMP-to-CUDA translation.
 * 
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 */
public abstract class TransformTools {
	
	/**
	 * Java doesn't allow a class to be both abstract and final,
	 * so this private constructor prevents any derivations.
	 */
	private TransformTools()
	{
	}

	/**
	 * Get a temporary integer variable that can be used as a loop index variable 
	 * or other temporary data holder. The name of the variable is decided by
	 * using the trailer value, and if the variable with the given name exists
	 * in a region, where, this function returns the existing variable.
	 * Otherwise, this function create a new variable with the given name.
	 * This function differs from Tools.getTemp() in two ways; first if the 
	 * temporary variable exits in the region, this function returns existing
	 * one, but Tools.getTemp() creates another new one. 
	 * Second, if the temporary variable does not exist in the region, this 
	 * function creates the new variable, but Tools.getTemp() searches parents
	 * of the region and creates the new variable only if none of parents contains
	 * the temporary variable.
	 * 
	 * @param where code region from where temporary variable is searched or 
	 *        created. 
	 * @param trailer integer trailer that is used to create/search a variable name
	 * @return
	 */
	public static Identifier getTempIndex(Traversable where, int trailer) {
	    Traversable t = where;
	    while ( !(t instanceof SymbolTable) )
	      t = t.getParent();
	    // Traverse to the parent of a loop statement
	    if (t instanceof ForLoop || t instanceof DoLoop || t instanceof WhileLoop) {
	      t = t.getParent();
	      while ( !(t instanceof SymbolTable) )
	        t = t.getParent();
	    }
	    SymbolTable st = (SymbolTable)t;
	    String header = "_ti_100";
	    String name = header+"_"+trailer;
	    Identifier ret = null;
	    ///////////////////////////////////////////////////////////////////////////
	    // SymbolTable.findSymbol(IDExpression name) can not be used here, since //
	    // it will search parent tables too.                                     //
	    ///////////////////////////////////////////////////////////////////////////
	    Set<String> symNames = AnalysisTools.symbolsToStringSet(st.getSymbols());
	    if( symNames.contains(name) ) {
	    	VariableDeclaration decl = (VariableDeclaration)st.findSymbol(new NameID(name));
	    	ret = new Identifier((VariableDeclarator)decl.getDeclarator(0));
	    } else {
	    	//ret = SymbolTools.getTemp(t, Specifier.INT, header);
	    	///////////////////////////////////////////////////////////////////
	    	//SymbolTools.getTemp() may cause a problem if parent symbol tables    //
	    	//contain a variable whose name is the same as the one of ret.   //
	    	//To avoid this problem, a new temp variable is created directly //
	    	//here without using SymbolTools.getTemp().                           //
	    	///////////////////////////////////////////////////////////////////
	    	VariableDeclarator declarator = new VariableDeclarator(new NameID(name));
	        VariableDeclaration decl = new VariableDeclaration(Specifier.INT, declarator);
	        st.addDeclaration(decl);
	    	ret = new Identifier(declarator);
	    }
	    return ret;
	}

	/**
	 * Get a new temporary integer variable, which has not been created 
	 * by getTempIndex() method.
	 * 
	 * @param where
	 * @return
	 */
	public static Identifier getNewTempIndex(Traversable where) {
	    Traversable t = where;
	    while ( !(t instanceof SymbolTable) )
	      t = t.getParent();
	    // Traverse to the parent of a loop statement
	    if (t instanceof ForLoop || t instanceof DoLoop || t instanceof WhileLoop) {
	      t = t.getParent();
	      while ( !(t instanceof SymbolTable) )
	        t = t.getParent();
	    }
	    SymbolTable st = (SymbolTable)t;
	    String header = "_ti_100";
	    int trailer = 0;
		Identifier ret = null;
	   	Set<String> symNames = AnalysisTools.symbolsToStringSet(st.getSymbols());
	    while ( true ) {
	    	String name = header+"_"+trailer;
	    	if( symNames.contains(name) ) {
	    		trailer++;
	    	} else {
	    		//ret = SymbolTools.getTemp(t, Specifier.INT, header);
	    		///////////////////////////////////////////////////////////////////
	    		//SymbolTools.getTemp() may cause a problem if parent symbol tables    //
	    		//contain a variable whose name is the same as the one of ret.   //
	    		//To avoid this problem, a new temp variable is created directly //
	    		//here without using SymbolTools.getTemp().                            //
	    		///////////////////////////////////////////////////////////////////
	    		VariableDeclarator declarator = new VariableDeclarator(new NameID(name));
	    		VariableDeclaration decl = new VariableDeclaration(Specifier.INT, declarator);
	    		st.addDeclaration(decl);
	    		ret = new Identifier(declarator);
	    		break;
	    	}
	    }
	    return ret;
	}

	/**
	 * If shared variable in a parallel region is used as private/firstprivate variable 
	 * in a omp-for loop in the parallel region, the shared variable is privatized 
	 * in the omp-for loop and initialization statement is added if the shared variable
	 * is used as a firstprivate variable.
	 * 
	 * @param map
	 * @param region
	 */
	public static void privatizeSharedData(HashMap map, CompoundStatement region) {
		HashSet<Symbol> OmpSharedSet = null;
		if (map.keySet().contains("shared"))
			OmpSharedSet = (HashSet<Symbol>) map.get("shared");
		
		List<OmpAnnotation> omp_annots = IRTools.collectPragmas(region, OmpAnnotation.class, "for");
		for ( OmpAnnotation fannot : omp_annots ) {
			Statement target_stmt = (Statement)fannot.getAnnotatable();
			HashSet<Symbol> ForPrivSet = null; 
			HashSet<Symbol> ForFirstPrivSet = null; 
			HashSet<Symbol> PrivSet = new HashSet<Symbol>();
			if (fannot.keySet().contains("private") || fannot.keySet().contains("firstprivate")) {
				ForPrivSet = (HashSet<Symbol>) fannot.get("private");
				if( ForPrivSet != null ) {
					PrivSet.addAll(ForPrivSet);
				}
				ForFirstPrivSet = (HashSet<Symbol>) fannot.get("firstprivate");
				if( ForFirstPrivSet != null ) {
					PrivSet.addAll(ForFirstPrivSet);
				}
				for( Symbol privSym : PrivSet ) {
					if( AnalysisTools.containsSymbol(OmpSharedSet, privSym.getSymbolName()) ) {
						/* 
						 * Create a new temporary variable for the shared variable.
						 */
						VariableDeclaration decl = (VariableDeclaration)
							((VariableDeclarator)privSym).getParent();
						VariableDeclarator cloned_declarator = 
							(VariableDeclarator)((VariableDeclarator)privSym).clone();
						/////////////////////////////////////////////////////////////////////////////////
						// __device__ and __global__ functions can not declare static variables inside //
						// their body.                                                                 //
						/////////////////////////////////////////////////////////////////////////////////
						List<Specifier> clonedspecs = new ChainedList<Specifier>();
						clonedspecs.addAll(decl.getSpecifiers());
						clonedspecs.remove(Specifier.STATIC);
						Identifier cloned_ID = SymbolTools.getArrayTemp(region, clonedspecs, 
								cloned_declarator.getArraySpecifiers(), privSym.getSymbolName());
						/////////////////////////////////////////////////////////////////////////////
						// Replace the symbol pointer of the shared variable with this new symbol. //
						/////////////////////////////////////////////////////////////////////////////
						IRTools.replaceAll(target_stmt, new Identifier((VariableDeclarator)privSym), cloned_ID);
						
						/////////////////////////////////////////////////////////////////////
						// Load the value of shared variable to the firstprivate variable. //
						/////////////////////////////////////////////////////////////////////
						if( (ForFirstPrivSet != null) && ForFirstPrivSet.contains(privSym) ) {
							Symbol sharedSym = AnalysisTools.findsSymbol(OmpSharedSet, privSym.getSymbolName());
							Identifier shared_ID = new Identifier(sharedSym);
							CompoundStatement parentStmt = (CompoundStatement)target_stmt.getParent();
							if( SymbolTools.isScalar(sharedSym) && !SymbolTools.isPointer(sharedSym) ) {
								Statement estmt = new ExpressionStatement(new AssignmentExpression(cloned_ID.clone(), 
										AssignmentOperator.NORMAL, shared_ID.clone()));
								parentStmt.addStatementBefore(target_stmt,estmt);
							} else if( SymbolTools.isArray(sharedSym) ) {
								List aspecs = sharedSym.getArraySpecifiers();
								ArraySpecifier aspec = (ArraySpecifier)aspecs.get(0);
								int dimsize = aspec.getNumDimensions();
								//////////////////////////////
								// Sample loading statement //
								///////////////////////////////////////////////////////
								// Ex: for(i=0; i<SIZE1; i++) {                      //
								//         for(k=0; k<SIZE2; k++) {                  //
								//             fpriv_var[i][k] = shared_var[i][k];   //
								//         }                                         //
								//      }                                            //
								///////////////////////////////////////////////////////
								//////////////////////////////////////// //////
								// Create or find temporary index variables. // 
								//////////////////////////////////////// //////
								List<Identifier> index_vars = new LinkedList<Identifier>();
								for( int i=0; i<dimsize; i++ ) {
									index_vars.add(TransformTools.getTempIndex(region, i));
								}
								Identifier index_var = null;
								Expression assignex = null;
								Statement loop_init = null;
								Expression condition = null;
								Expression step = null;
								CompoundStatement loop_body = null;
								ForLoop innerLoop = null;
								for( int i=dimsize-1; i>=0; i-- ) {
									index_var = index_vars.get(i);
									assignex = new AssignmentExpression((Identifier)index_var.clone(),
											AssignmentOperator.NORMAL, new IntegerLiteral(0));
									loop_init = new ExpressionStatement(assignex);
									condition = new BinaryExpression(index_var.clone(),
											BinaryOperator.COMPARE_LT, aspec.getDimension(i).clone());
									step = new UnaryExpression(UnaryOperator.POST_INCREMENT, 
											(Identifier)index_var.clone());
									loop_body = new CompoundStatement();
									if( i == (dimsize-1) ) {
										List<Expression> indices1 = new LinkedList<Expression>();
										List<Expression> indices2 = new LinkedList<Expression>();
										for( int k=0; k<dimsize; k++ ) {
											indices1.add((Expression)index_vars.get(k).clone());
											indices2.add((Expression)index_vars.get(k).clone());
										}
										assignex = new AssignmentExpression(new ArrayAccess(
												cloned_ID.clone(), indices1), 
												AssignmentOperator.NORMAL, 
												new ArrayAccess(shared_ID.clone(), indices2)); 
										loop_body.addStatement(new ExpressionStatement(assignex));
									} else {
										loop_body.addStatement(innerLoop);
									}
									innerLoop = new ForLoop(loop_init, condition, step, loop_body);
								}	
								parentStmt.addStatementBefore(target_stmt,innerLoop);
							}
						}
							
					}
				}
			}
		}
	}

	/**
	 * Calculate the iteration space sizes of omp-for loops existing in a parallel region.
	 * This calculation should be done before the parallel region is transformed into a
	 * kernel function.
	 * 
	 * @param region parallel region to be searched
	 * @param map annotation map beloning to the parallel region, region
	 * @return set of symbols used in calculating iteration space
	 */
	public static Set<Symbol> calcLoopItrSize(Statement region, HashMap map) {
		ForLoop ploop = null;
		Expression iterspace = null;
		Set<Symbol> usedSymbols = new HashSet<Symbol>();
		if( region instanceof ForLoop ) {
			ploop = (ForLoop)region;
			// check for a canonical loop
			if ( !LoopTools.isCanonical(ploop) ) {
				Tools.exit("[Error in calcLoopItrSize()] Parallel Loop is not " +
						"a canonical loop; compiler can not determine iteration space of " +
						"the following loop: \n" +  ploop);
			}
			// check whether loop stride is 1.
			Expression incr = LoopTools.getIncrementExpression(ploop);
			if( incr instanceof IntegerLiteral ) {
				long IntIncr = ((IntegerLiteral)incr).getValue();
				if( Math.abs(IntIncr) != 1 ) {
					Tools.exit("[Error in calcLoopItrSize()] A parallel loop with a stride > 1 is found;" +
							" current O2G translator can not handle this loop: \n" + ploop);
				}
			} else {
				Tools.exit("[Error in calcLoopItrSize()] The stride of the following parallel loop is not constant;"
						+ " current O2G translator can not handle this loop: \n" + ploop);
				
			}
			// identify the loop index variable 
			Expression ivar = LoopTools.getIndexVariable(ploop);
			Expression lb = LoopTools.getLowerBoundExpression(ploop);
			Expression ub = LoopTools.getUpperBoundExpression(ploop);
			iterspace = Symbolic.add(Symbolic.subtract(ub,lb),new IntegerLiteral(1));
			// Insert the calculated iteration size into the annotation map.
			map.put("iterspace", iterspace);
			usedSymbols.addAll(SymbolTools.getAccessedSymbols(iterspace));
		} else if( region instanceof CompoundStatement ){
			List<OmpAnnotation>
			omp_annots = IRTools.collectPragmas(region, OmpAnnotation.class, "for");
			for ( OmpAnnotation annot : omp_annots ) {
				Statement target_stmt = (Statement)annot.getAnnotatable();
				if( target_stmt instanceof ForLoop ) {
					ploop = (ForLoop)target_stmt;
					if ( !LoopTools.isCanonical(ploop) ) {
						Tools.exit("[Error in calLoopItrSize()] Parallel Loop is not " +
								"a canonical loop; compiler can not determine iteration space of the " +
								"following loop: \n" +  ploop);
					}
					// check whether loop stride is 1.
					Expression incr = LoopTools.getIncrementExpression(ploop);
					if( incr instanceof IntegerLiteral ) {
						long IntIncr = ((IntegerLiteral)incr).getValue();
						if( Math.abs(IntIncr) != 1 ) {
							Tools.exit("[Error in calcLoopItrSize()] A parallel loop with a stride > 1 is found;" +
									" current O2G translator can not handle this loop: \n" + ploop);
						}
					} else {
						Tools.exit("[Error in calcLoopItrSize()] The stride of the following parallel loop is not constant;"
								+ " current O2G translator can not handle this loop: \n" + ploop);

					}
					Expression ivar = LoopTools.getIndexVariable(ploop);
					Expression lb = LoopTools.getLowerBoundExpression(ploop);
					Expression ub = LoopTools.getUpperBoundExpression(ploop);
					iterspace = Symbolic.add(Symbolic.subtract(ub,lb),new IntegerLiteral(1));
					annot.put("iterspace", iterspace);
					usedSymbols.addAll(SymbolTools.getAccessedSymbols(iterspace));
				}
			}
		}
		return usedSymbols;
	}

	/**
	 * Find appropriate initialization value for a given reduction operator
	 * and variable type.
	 * @param redOp reductioin operator
	 * @param specList list containing type specifiers of the reduction variable
	 * @return initialization value for the reduction variable
	 */
	public static Expression getRInitValue(BinaryOperator redOp, List specList) {
		///////////////////////////////////////////////////////
		// Operator		Initialization value                 //
		///////////////////////////////////////////////////////
		//	+			0
		//	*			1
		//	-			0
		//	&			~0
		//	|			0
		//	^			0
		//	&&			1
		//	||			0
		///////////////////////////////////////////////////////
		Expression initValue = null;
		if( redOp.equals(BinaryOperator.ADD) || redOp.equals(BinaryOperator.SUBTRACT) ) {
			if(specList.contains(Specifier.FLOAT) || specList.contains(Specifier.DOUBLE)) {
				initValue = new FloatLiteral(0.0f, "F");
			} else {
				initValue = new IntegerLiteral(0);
			}
		} else if( redOp.equals(BinaryOperator.BITWISE_INCLUSIVE_OR)
				|| redOp.equals(BinaryOperator.BITWISE_EXCLUSIVE_OR)
				|| redOp.equals(BinaryOperator.LOGICAL_OR) ) {
			initValue = new IntegerLiteral(0);
		} else if( redOp.equals(BinaryOperator.MULTIPLY) ) {
			if(specList.contains(Specifier.FLOAT) || specList.contains(Specifier.DOUBLE)) {
				initValue = new FloatLiteral(1.0f, "F");
			} else {
				initValue = new IntegerLiteral(1);
			}
		} else if( redOp.equals(BinaryOperator.LOGICAL_AND) ) {
			initValue = new IntegerLiteral(1);
		} else if( redOp.equals(BinaryOperator.BITWISE_AND) ) {
			initValue = new UnaryExpression(UnaryOperator.BITWISE_COMPLEMENT, 
					new IntegerLiteral(0));
		}
		return initValue;
	}

	/**
	 * Create a reduction assignment expression for the given reduction operator.
	 * This function is used to perform both in-block partial reduction and across-
	 * block final reduction.
	 * [CAUTION] the partial results of a subtraction reduction are added to form the 
	 * final value.
	 * 
	 * @param RedExp expression of reduction variable/array
	 * @param redOp reduction operator
	 * @param Rexp right-hand-side expression
	 * @return reduction assignment expression
	 */
	public static AssignmentExpression RedExpression(Expression RedExp, BinaryOperator redOp,
			Expression Rexp) {
		AssignmentExpression assignExp = null;
		if( redOp.equals(BinaryOperator.ADD) ) {
			assignExp = new AssignmentExpression( RedExp, AssignmentOperator.ADD,
					Rexp);
		}else if( redOp.equals(BinaryOperator.SUBTRACT) ) {
			assignExp = new AssignmentExpression( RedExp, AssignmentOperator.ADD,
					Rexp);
		}else if( redOp.equals(BinaryOperator.BITWISE_INCLUSIVE_OR) ) {
			assignExp = new AssignmentExpression( RedExp, AssignmentOperator.BITWISE_INCLUSIVE_OR,
					Rexp);
		}else if( redOp.equals(BinaryOperator.BITWISE_EXCLUSIVE_OR) ) {
			assignExp = new AssignmentExpression( RedExp, AssignmentOperator.BITWISE_EXCLUSIVE_OR,
					Rexp);
		}else if( redOp.equals(BinaryOperator.MULTIPLY) ) {
			assignExp = new AssignmentExpression( RedExp, AssignmentOperator.MULTIPLY,
					Rexp);
		}else if( redOp.equals(BinaryOperator.BITWISE_AND) ) {
			assignExp = new AssignmentExpression( RedExp, AssignmentOperator.BITWISE_AND,
					Rexp);
		}else if( redOp.equals(BinaryOperator.LOGICAL_AND) ) {
			assignExp = new AssignmentExpression( RedExp, AssignmentOperator.NORMAL,
					new BinaryExpression((Expression)RedExp.clone(), redOp, Rexp));
		}else if( redOp.equals(BinaryOperator.LOGICAL_OR) ) {
			assignExp = new AssignmentExpression( RedExp, AssignmentOperator.NORMAL,
					new BinaryExpression((Expression)RedExp.clone(), redOp, Rexp));
		}
		return assignExp;
			
	}

	/**
	 * Update information of OmpAnnotations contained in the region. 
	 * If input region is a cloned one, cloning of the original OmpAnnotations
	 * will do a shallow copy of the HashMap instance: the keys and values themselves 
	 * are not cloned. 
	 * For each OmpAnnotation in the input region,
	 *     - shared, private, reduction, and threadprivate data sets are updated.
	 * 
	 * [CAUTION] this method does not update symbol pointers of Identifiers
	 * in the input region. 
	 *     
	 * @param region code region where OmpAnnotations will be updated.
	 * @param useAccessedSymbols use symbols accessed in the input region when updating 
	 * old symbols in OmpAnnotations. If Identifiers in the input region have correct symbol pointers,
	 * set useAccessSymbols true. Otherwise, set this false.
	 */
	static public void updateAnnotationsInRegion( Traversable region, boolean useAccessedSymbols ) {
		HashSet<Symbol> old_set = null;
		HashSet<Symbol> new_set = null;
		
/*		Procedure parentProc = IRTools.getParentProcedure(region);
		PrintTools.println("[updateAnnotationsInRegion] start for region in procedure: "+parentProc.getSymbolName(), 0);*/
	
		/* iterate over everything, with particular attention to annotations */
		DepthFirstIterator iter = new DepthFirstIterator(region);
	
		while(iter.hasNext())
		{
			Object obj = iter.next();
	
			if ( (obj instanceof Annotatable) && (obj instanceof Statement) )
			{
				Annotatable at = (Annotatable)obj;
				Statement atstmt = (Statement)obj;
				OmpAnnotation omp_annot = null;
				Collection tCollect = null;
				/////////////////////////////////////////////////////////////////////////
				// Update omp shared, private, reduction, and threadprivate data sets. //
				/////////////////////////////////////////////////////////////////////////
				omp_annot = at.getAnnotation(OmpAnnotation.class, "shared");
				if( omp_annot != null ) {
					old_set = new HashSet<Symbol>();
					tCollect = (Collection)omp_annot.remove("shared");
					old_set.addAll(tCollect);
					/*					PrintTools.println("[updateAnnotationsInRegion] old shared set: "+ 
							AnalysisTools.symbolsToString(old_set, ","), 0);*/
					new_set = new HashSet<Symbol>();
					if( useAccessedSymbols ) {
						AnalysisTools.updateSymbols2((Traversable)obj, old_set, new_set, true);
					} else {
						AnalysisTools.updateSymbols((Traversable)obj, old_set, new_set, true);
					}
					omp_annot.put("shared", new_set);
				}
				omp_annot = at.getAnnotation(OmpAnnotation.class, "private");
				if( omp_annot != null ) {
					old_set = new HashSet<Symbol>();
					tCollect = (Collection)omp_annot.remove("private");
					old_set.addAll(tCollect);
					new_set = new HashSet<Symbol>();
					if( useAccessedSymbols ) {
						AnalysisTools.updateSymbols2((Traversable)obj, old_set, new_set, false);
					} else {
						AnalysisTools.updateSymbols((Traversable)obj, old_set, new_set, false);
					}
					omp_annot.put("private", new_set);
					//////////////////////////////////////////////////////////
					// If a shared variable is included in the private set, //
					// remove the variable from the shared set.             //
					//////////////////////////////////////////////////////////
					if( omp_annot.keySet().contains("shared") ) {
						old_set = (HashSet<Symbol>)omp_annot.get("shared");
						old_set.removeAll(new_set);
					}
				}
				omp_annot = at.getAnnotation(OmpAnnotation.class, "firstprivate");
				if( omp_annot != null ) {
					old_set = new HashSet<Symbol>();
					tCollect = (Collection)omp_annot.remove("firstprivate");
					old_set.addAll(tCollect);
					new_set = new HashSet<Symbol>();
					if( useAccessedSymbols ) {
						AnalysisTools.updateSymbols2((Traversable)obj, old_set, new_set, false);
					} else {
						AnalysisTools.updateSymbols((Traversable)obj, old_set, new_set, false);
					}
					omp_annot.put("firstprivate", new_set);
					//////////////////////////////////////////////////////////
					// If a shared variable is included in the private set, //
					// remove the variable from the shared set.             //
					//////////////////////////////////////////////////////////
					if( omp_annot.keySet().contains("shared") ) {
						old_set = (HashSet<Symbol>)omp_annot.get("shared");
						old_set.removeAll(new_set);
					}
				}
				omp_annot = at.getAnnotation(OmpAnnotation.class, "threadprivate");
				if( omp_annot != null ) {
					////////////////////////////////////////////////////////////////////////
					// DEBUG: "#pragma omp threadprivate(list)" annotation is standalone  //
					// annotation, where the list is a list of strings, and thus we don't //
					// have to update string list.                                        //
					////////////////////////////////////////////////////////////////////////
					if( (at.getAnnotation(OmpAnnotation.class, "parallel") != null) ||
							(at.getAnnotation(OmpAnnotation.class, "for") != null) ) {
						old_set = new HashSet<Symbol>();
						tCollect = (Collection)omp_annot.remove("threadprivate");
						old_set.addAll(tCollect);
						new_set = new HashSet<Symbol>();
						if( useAccessedSymbols ) {
							AnalysisTools.updateSymbols2((Traversable)obj, old_set, new_set, true);
						} else {
							AnalysisTools.updateSymbols((Traversable)obj, old_set, new_set, true);
						}
						omp_annot.put("threadprivate", new_set);
					}
				}
				omp_annot = at.getAnnotation(OmpAnnotation.class, "reduction");
				if( omp_annot != null ) {
					OmpAnalysis.updateReductionClause((Traversable)obj, omp_annot);
				}
				////////////////////////////////////////////////////////
				// Update CudaAnnotations so that each annotation     //
				// contains HashSets as values; this update is needed //
				// only if CudaAnnotations are cloned.                //
				// FIXME: if Annotation.clone() allows HashSet as     //
				// values, we don't need this update.                 //
				////////////////////////////////////////////////////////
				List<CudaAnnotation> cuda_annots = at.getAnnotations(CudaAnnotation.class);
				if( (cuda_annots != null) && (cuda_annots.size() > 0) ) {
					for( CudaAnnotation cannot : cuda_annots ) {
						Set<String> keySet = cannot.keySet();
						for( String cudaClause : keySet ) {
							Object vObj = cannot.get(cudaClause);
							if( vObj instanceof Collection ) {
								Collection<String> dataSet = (Collection<String>)vObj;
								if( cudaClause.equals("enclosingloops") ) {
									LinkedHashSet<String> lnewSet = new LinkedHashSet<String>();
									if( dataSet != null ) {
										lnewSet.addAll(dataSet);
										cannot.put("enclosingloops", lnewSet);
									}
								} else {
									HashSet<String> newSet = new HashSet<String>();
									if( dataSet != null ) {
										newSet.addAll(dataSet);
										cannot.put(cudaClause, newSet);
									}
								}
							}
						}
					}
				}
			} 
		}
/*		PrintTools.println("[updateAnnotationsInRegion] end for region in procedure: "+parentProc.getSymbolName(), 0);*/
	}
	
	/**
	 * Add a statement before the ref_stmt in the parent CompoundStatement.
	 * This method can be used to insert declaration statement before the ref_stmt,
	 * which is not allowed in CompoundStatement.addStatementBefore() method.
	 * 
	 * @param parent parent CompoundStatement containing the ref_stmt as a child
	 * @param ref_stmt reference statement
	 * @param new_stmt new statement to be added
	 */
	public static void addStatementBefore(CompoundStatement parent, Statement ref_stmt, Statement new_stmt) {
		List<Traversable> children = parent.getChildren();
		int index = Tools.indexByReference(children, ref_stmt);
		if (index == -1)
			throw new IllegalArgumentException();
		if (new_stmt.getParent() != null)
			throw new NotAnOrphanException();
		children.add(index, new_stmt);
		new_stmt.setParent(parent);
		if( new_stmt instanceof DeclarationStatement ) {
			Declaration decl = ((DeclarationStatement)new_stmt).getDeclaration();
			SymbolTools.addSymbols(parent, decl);
		}
	}

	/**
	 * Add a statement after the ref_stmt in the parent CompoundStatement.
	 * This method can be used to insert declaration statement after the ref_stmt,
	 * which is not allowed in CompoundStatement.addStatementAfter() method.
	 * 
	 * @param parent parent CompoundStatement containing the ref_stmt as a child
	 * @param ref_stmt reference statement
	 * @param new_stmt new statement to be added
	 */
	public static void addStatementAfter(CompoundStatement parent, Statement ref_stmt, Statement new_stmt) {
		List<Traversable> children = parent.getChildren();
		int index = Tools.indexByReference(children, ref_stmt);
		if (index == -1)
			throw new IllegalArgumentException();
		if (new_stmt.getParent() != null)
			throw new NotAnOrphanException();
		children.add(index+1, new_stmt);
		new_stmt.setParent(parent);
		if( new_stmt instanceof DeclarationStatement ) {
			Declaration decl = ((DeclarationStatement)new_stmt).getDeclaration();
			SymbolTools.addSymbols(parent, decl);
		}
	}
	
	/**
	 * Remove a child from a parent; this method is used to delete ProcedureDeclaration
	 * when both Procedure and ProcedureDeclaration need to be deleted. TranslationUnit
	 * symbol table contains only one entry for both, and thus TranslationUnit.removeChild()
	 * complains an error when trying to delete both of them. 
	 * 
	 * 
	 * @param parent parent traversable containing the child
	 * @param child child traversable to be removed
	 */
	public static void removeChild(Traversable parent, Traversable child)
	{
		List<Traversable> children = parent.getChildren();
		int index = Tools.indexByReference(children, child);

		if (index == -1)
			throw new NotAChildException();

		child.setParent(null);
		children.remove(index);
	}

	/**
	 * Create a HashMap which contains updated shared, reduction, private, and threadprivate data sets
	 * for the function called in a Omp parallel region. Depending on the sharing attributes of
	 * the actual arguments of the called function, corresponding formal parameters are 
	 * assigned to one of HashSets (shared, reduction, private, and threadprivate sets) in the HashMap.
	 * In addition, shared data that are accessed in the called function, but not passed 
	 * as parameters are added into the new shared set, and all local variables are added to 
	 * the new private set.
	 * 
	 * @param par_map HashMap of an enclosing parallel region.
	 * @param argList List of actual arguments passed into the function proc.
	 * @param proc Procedure that is called in a parallel region.
	 * @return New HashMap that contains updated shared, private, and threadprivate data sets.
	 */
	static public HashMap updateOmpMapForCalledFunc(HashMap par_map, List<Expression> argList, Procedure proc) {
		HashSet<Symbol> old_set = null;
		HashSet<Symbol> new_set = null;
		HashMap new_map = new HashMap();
/*		PrintTools.println("[updateOmpMapForCalledFunc] called func: "+proc.getSymbolName(), 0);*/
		// Copy all hash mapping except for shared, private, firstprivate, and threadprivate data sets
		new_map.putAll(par_map); 
		new_map.remove("shared");
		new_map.remove("reduction");
		new_map.remove("private");
		new_map.remove("firstprivate");
		new_map.remove("threadprivate");
		
		Set<Symbol> accessedSymbols = SymbolTools.getAccessedSymbols(proc.getBody());
		// Remove procedure symbols from accessedSymbols. //
		Set<Symbol> tSet = new HashSet<Symbol>();
		for( Symbol sm : accessedSymbols ) {
			if( (sm instanceof Procedure) || (sm instanceof ProcedureDeclarator) ) {
				tSet.add(sm);
			}
		}
		accessedSymbols.removeAll(tSet);
		Set<Symbol> IpAccessedSymbols = AnalysisTools.getIpAccessedVariableSymbols(proc.getBody());
		IpAccessedSymbols = AnalysisTools.getBaseSymbols(IpAccessedSymbols);
		// DEBUG Print
/*		PrintTools.println("[updateOmpMapForCalledFunc] accessedSymbols: "+ 
				AnalysisTools.symbolsToString(accessedSymbols, ","), 0);
		PrintTools.println("[updateOmpMapForCalledFunc] IpAccessedSymbols: "+ 
				AnalysisTools.symbolsToString(IpAccessedSymbols, ","), 0);*/
		
		List paramList = proc.getParameters();
		int list_size = paramList.size();
		//////////////////////////////////////////////////////////////////////////////////////////
		// DEBUG: Procedure may include void specifier as a parameter (ex: foo(void))instead of //
		// having empty one (ex: foo()). In this case, list_size should be reset to 0.          //
		//////////////////////////////////////////////////////////////////////////////////////////
		if( list_size == 1 ) {
			Object obj = paramList.get(0);
			String paramS = obj.toString();
			// Remove any leading or trailing whitespace.
			paramS = paramS.trim();
			if( paramS.equals(Specifier.VOID.toString()) ) {
				list_size = 0;
			}
		}
		
		if( par_map.keySet().contains("shared") ) {
			old_set = (HashSet<Symbol>)par_map.get("shared");
			new_set = new HashSet<Symbol>();
			// If actual argument is shared, put corresponding parameter into the new shared set.
			for(int i=0; i<list_size; i++) {
				Expression arg = argList.get(i);
				Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
				for( Expression exp : UseSet) {
					Symbol sm = SymbolTools.getSymbolOf(exp);
					//DEBUG: Currently, if sm is an AccessSymbol, only base symbol is used.
					if( sm instanceof AccessSymbol ) {
						sm = ((AccessSymbol)sm).getIRSymbol();
					}
					if(old_set.contains(sm)) {
						Object obj = paramList.get(i);
						if( obj instanceof VariableDeclaration ) {
							VariableDeclarator vdecl = 
								(VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
							new_set.add(vdecl);
						} 
						break;
					}
				}
			}
			// Put other shared variables in the old_set, which are accessed 
			// in the called function, into the new set.
			for( Symbol ssm : old_set ) {
				if( IpAccessedSymbols.contains(ssm) ) {
					new_set.add(ssm);
				}
			}
			new_map.put("shared", new_set);
		}
/*		PrintTools.println("[updateOmpMapForCalledFunc] New shared set: "+ 
				AnalysisTools.symbolsToString(new_set, ","), 0);*/
		
		if( par_map.keySet().contains("reduction") ) {
			HashMap reduction_map = (HashMap)par_map.get("reduction");
			HashMap newreduction_map = new HashMap(4);
			HashSet<Symbol> allItemsSet = new HashSet<Symbol>();
			for (String ikey : (Set<String>)(reduction_map.keySet())) {
				HashSet<Symbol> o_set = (HashSet<Symbol>)reduction_map.get(ikey);
				HashSet<Symbol> n_set = new HashSet<Symbol>();
				// If actual argument is a reduction variable, put corresponding 
				// parameter into the new reduction set.
				for(int i=0; i<list_size; i++) {
					Expression arg = argList.get(i);
					Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
					for( Expression exp : UseSet) {
						Symbol sm = SymbolTools.getSymbolOf(exp);
						if(o_set.contains(sm)) {
							Object obj = paramList.get(i);
							if( obj instanceof VariableDeclaration ) {
								VariableDeclarator vdecl = 
									(VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
								n_set.add(vdecl);
							} 
							break;
						}
					}
				}
				// Put other reduction variables in the o_set, which are accessed 
				// in the called function, into the n_set.
				for( Symbol ssm : o_set ) {
					if( accessedSymbols.contains(ssm) ) {
						n_set.add(ssm);
					}
				}
				newreduction_map.put(ikey, n_set);
			}
			new_map.put("reduction", newreduction_map);
		}
		/*
		 * FIXME: What if a private variable is passed as a reference?
		 *        What if an argument consists of both shared and private variables?
		 */
		if( par_map.keySet().contains("private") ) {
			old_set = (HashSet<Symbol>)par_map.get("private");
			new_set = new HashSet<Symbol>();
			// If actual argument is private, put corresponding parameter into the new private set.
			for(int i=0; i<list_size; i++) {
				Expression arg = argList.get(i);
				Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
				for( Expression exp : UseSet) {
					Symbol sm = SymbolTools.getSymbolOf(exp);
					if(old_set.contains(sm)) {
						Object obj = paramList.get(i);
						if( obj instanceof VariableDeclaration ) {
							VariableDeclarator vdecl = (VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
							if( SymbolTools.isScalar(vdecl) && !SymbolTools.isPointer(vdecl) ) {
								new_set.add(vdecl);
							} else {
								PrintTools.println("[WARNING] private variable, "+vdecl.getSymbolName()+", " +
										"is passed as a reference in procedure, " +  proc.getSymbolName() + 
										"(); splitting parallel region in "+proc.getSymbolName()+"() may result in "
										+ "incorrect output codes if "+vdecl.getSymbolName()+" upwardly exposed " +
										"in "+proc.getSymbolName()+"().", 0);
								new_set.add(vdecl);
							}
						} 
						break;
					}
				}
			}
			// Put other private variables in the old_set, which are accessed 
			// in the called function, into the new set.
			for( Symbol ssm : old_set ) {
				if( accessedSymbols.contains(ssm) ) {
					new_set.add(ssm);
				}
			}
			// Put other private variables that are declared within this function call.
			Set<Symbol> localSymbols = new HashSet<Symbol>();
			DepthFirstIterator iter = new DepthFirstIterator(proc.getBody());
			while(iter.hasNext())
			{
				Object obj = iter.next();	
				if( obj instanceof SymbolTable ) {
					localSymbols.addAll(SymbolTools.getVariableSymbols((SymbolTable)obj));
				}
			}
			Set<Symbol> StaticLocalSet = AnalysisTools.getStaticVariables(localSymbols);
			new_set.addAll(localSymbols);
			new_set.removeAll(StaticLocalSet);
			new_map.put("private", new_set);
			////////////////////////////////////////////////////////////////////////////////
			//If shared variable is used as a private variable, it should be removed from //
			// the shared set.                                                            //
			////////////////////////////////////////////////////////////////////////////////
			old_set = (HashSet<Symbol>) new_map.get("shared");
			old_set.removeAll(new_set);
		}
		
		if( par_map.keySet().contains("firstprivate") ) {
			old_set = (HashSet<Symbol>)par_map.get("firstprivate");
			new_set = new HashSet<Symbol>();
			// If actual argument is firstprivate, put corresponding parameter into the new firstprivate set.
			for(int i=0; i<list_size; i++) {
				Expression arg = argList.get(i);
				Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
				for( Expression exp : UseSet) {
					Symbol sm = SymbolTools.getSymbolOf(exp);
					if(old_set.contains(sm)) {
						Object obj = paramList.get(i);
						if( obj instanceof VariableDeclaration ) {
							VariableDeclarator vdecl = (VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
							if( SymbolTools.isScalar(vdecl) && !SymbolTools.isPointer(vdecl) ) {
								new_set.add(vdecl);
							} else {
								PrintTools.println("[WARNING] firstprivate variable, "+vdecl.getSymbolName()+", " +
										"is passed as a reference in procedure, " +  proc.getSymbolName() + 
										"(); splitting parallel region in "+proc.getSymbolName()+"() may result in "
										+ "incorrect output codes if "+vdecl.getSymbolName()+" upwardly exposed " +
										"in "+proc.getSymbolName()+"().", 0);
								new_set.add(vdecl);
							}
						} 
						break;
					}
				}
			}
			/////////////////////////////////////////////////////////////////////////////////////////////
			// Formal arguments of called routines in the region that are passed by value are private, //
			// and if they are accessed, it is very likely that these should be firstprivate.          //
			/////////////////////////////////////////////////////////////////////////////////////////////
			for( Object obj : paramList) {
				if( obj instanceof VariableDeclaration ) {
					VariableDeclarator vdecl = (VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
					if( SymbolTools.isScalar(vdecl) && !SymbolTools.isPointer(vdecl) 
							&& accessedSymbols.contains(vdecl) ) {
						new_set.add(vdecl);
					}
				}
			}
			// Put other firstprivate variables in the old_set, which are accessed 
			// in the called function, into the new set.
			for( Symbol ssm : old_set ) {
				if( accessedSymbols.contains(ssm) ) {
					new_set.add(ssm);
				}
			}
			new_map.put("firstprivate", new_set);
			////////////////////////////////////////////////////////////////////////////////
			//If shared variable is used as a private variable, it should be removed from //
			// the shared set.                                                            //
			////////////////////////////////////////////////////////////////////////////////
			old_set = (HashSet<Symbol>) new_map.get("shared");
			old_set.removeAll(new_set);
		}
		
		if( par_map.keySet().contains("threadprivate") ) {
			old_set = (HashSet<Symbol>)par_map.get("threadprivate");
			new_set = new HashSet<Symbol>();
			for(int i=0; i<list_size; i++) {
				Expression arg = argList.get(i);
				Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
				for( Expression exp : UseSet) {
					Symbol sm = SymbolTools.getSymbolOf(exp);
					//DEBUG: Currently, if sm is an AccessSymbol, only base symbol is used.
					if( sm instanceof AccessSymbol ) {
						sm = ((AccessSymbol)sm).getIRSymbol();
					}
					if(old_set.contains(sm)) {
						Object obj = paramList.get(i);
						if( obj instanceof VariableDeclaration ) {
							VariableDeclarator vdecl = (VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
							new_set.add(vdecl);
						} 
						break;
					}
				}
			}
			// Put other shared variables in the old_set, which are accessed 
			// in the called function, into the new set.
			for( Symbol ssm : old_set ) {
				if( IpAccessedSymbols.contains(ssm) ) {
					new_set.add(ssm);
				}
			}
			new_map.put("threadprivate", new_set);
		}
/*		PrintTools.println("[updateOmpMapForCalledFunc] New threadprivate set: "+ 
				AnalysisTools.symbolsToString(new_set, ","), 0);*/
		
		
		// Update annotations of omp-for loops enclosed by the called procedure, proc.
		List<OmpAnnotation> ompfor_annotList = 
			IRTools.collectPragmas(proc.getBody(), OmpAnnotation.class, "for");
		for( OmpAnnotation ompfor_annot : ompfor_annotList ) {
			Statement atstmt = (Statement)ompfor_annot.getAnnotatable();
			accessedSymbols = SymbolTools.getAccessedSymbols(atstmt);
			if( ompfor_annot.keySet().contains("shared") ) {
				HashSet<Symbol> o_set = (HashSet<Symbol>)ompfor_annot.remove("shared");
				HashSet<Symbol> n_set = new HashSet<Symbol>();
				new_set = (HashSet<Symbol>)new_map.get("shared");
				for( Symbol sm : new_set ) {
					if( accessedSymbols.contains(sm)) {
						n_set.add(sm);
					}
				}
				for( Symbol sm : o_set ) {
					if( !n_set.contains(sm)) {
						n_set.add(sm);
					}
				}
				ompfor_annot.put("shared", n_set);
			}
		}
		return new_map;
	}
	
	/**
	 * Create a HashMap which contains updated shared, reduction, private, and threadprivate data sets
	 * for the function called in a Omp parallel region. Depending on the sharing attributes of
	 * the actual arguments of the called function, corresponding formal parameters are 
	 * assigned to one of HashSets (shared, reduction, private, and threadprivate sets) in the HashMap.
	 * In addition, shared data that are accessed in the called function, but not passed 
	 * as parameters are added into the new shared set, and all local variables are added to 
	 * the new private set.
	 * 
	 * This method differs from updateOmpMapForCalledFunc() in that this method uses symbol-name-based
	 * analysis while the other uses symbol-based analysis.
	 * 
	 * @param par_map HashMap of an enclosing parallel region.
	 * @param argList List of actual arguments passed into the function proc.
	 * @param proc Procedure that is called in a parallel region.
	 * @return New HashMap that contains updated shared, private, and threadprivate data sets.
	 */
	static public HashMap updateOmpMapForCalledFunc2(HashMap par_map, List<Expression> argList, Procedure proc) {
		HashSet<Symbol> old_set = null;
		HashSet<Symbol> new_set = null;
		HashMap new_map = new HashMap();
/*		PrintTools.println("[updateOmpMapForCalledFunc] called func: "+proc.getSymbolName(), 0);*/
		// Copy all hash mapping except for shared, private, firstprivate, and threadprivate data sets
		new_map.putAll(par_map); 
		new_map.remove("shared");
		new_map.remove("reduction");
		new_map.remove("private");
		new_map.remove("firstprivate");
		new_map.remove("threadprivate");
		
		Set<Symbol> accessedSymbols = SymbolTools.getAccessedSymbols(proc.getBody());
		// Remove procedure symbols from accessedSymbols. //
		Set<Symbol> tSet = new HashSet<Symbol>();
		for( Symbol sm : accessedSymbols ) {
			if( (sm instanceof Procedure) || (sm instanceof ProcedureDeclarator) ) {
				tSet.add(sm);
			}
		}
		accessedSymbols.removeAll(tSet);
		Set<Symbol> IpAccessedSymbols = AnalysisTools.getIpAccessedVariableSymbols(proc.getBody());
		IpAccessedSymbols = AnalysisTools.getBaseSymbols(IpAccessedSymbols);
		// DEBUG Print
/*		PrintTools.println("[updateOmpMapForCalledFunc] accessedSymbols: "+ 
				AnalysisTools.symbolsToString(accessedSymbols, ","), 0);
		PrintTools.println("[updateOmpMapForCalledFunc] IpAccessedSymbols: "+ 
				AnalysisTools.symbolsToString(IpAccessedSymbols, ","), 0);*/
		
		List paramList = proc.getParameters();
		int list_size = paramList.size();
		if( list_size == 1 ) {
			Object obj = paramList.get(0);
			String paramS = obj.toString();
			// Remove any leading or trailing whitespace.
			paramS = paramS.trim();
			if( paramS.equals(Specifier.VOID.toString()) ) {
				list_size = 0;
			}
		}
		
		if( par_map.keySet().contains("shared") ) {
			old_set = new HashSet<Symbol>((HashSet<Symbol>)par_map.get("shared"));
			tSet.clear();
			new_set = new HashSet<Symbol>();
			// If actual argument is shared, put corresponding parameter into the new shared set.
			for(int i=0; i<list_size; i++) {
				Expression arg = argList.get(i);
				Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
				for( Expression exp : UseSet) {
					Symbol sm = SymbolTools.getSymbolOf(exp);
					//DEBUG: Currently, if sm is an AccessSymbol, only base symbol is used.
					if( sm instanceof AccessSymbol ) {
						sm = ((AccessSymbol)sm).getIRSymbol();
					}
					if( (sm != null) && AnalysisTools.containsSymbol(old_set, sm.getSymbolName())) {
						Object obj = paramList.get(i);
						if( obj instanceof VariableDeclaration ) {
							VariableDeclarator vdecl = 
								(VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
							new_set.add(vdecl);
							tSet.add(AnalysisTools.findsSymbol(old_set, sm.getSymbolName()));
						} 
						break;
					}
				}
			}
			old_set.removeAll(tSet);
			// Put other shared variables in the old_set, which are accessed 
			// in the called function, into the new set.
			for( Symbol ssm : old_set ) {
				if( AnalysisTools.containsSymbol(IpAccessedSymbols, ssm.getSymbolName()) ) {
					new_set.add(ssm);
				}
			}
			new_map.put("shared", new_set);
		}
/*		PrintTools.println("[updateOmpMapForCalledFunc] New shared set: "+ 
				AnalysisTools.symbolsToString(new_set, ","), 0);*/
		
		if( par_map.keySet().contains("reduction") ) {
			HashMap reduction_map = (HashMap)par_map.get("reduction");
			HashMap newreduction_map = new HashMap(4);
			HashSet<Symbol> allItemsSet = new HashSet<Symbol>();
			for (String ikey : (Set<String>)(reduction_map.keySet())) {
				HashSet<Symbol> o_set = new HashSet<Symbol>((HashSet<Symbol>)reduction_map.get(ikey));
				HashSet<Symbol> n_set = new HashSet<Symbol>();
				tSet.clear();
				// If actual argument is a reduction variable, put corresponding 
				// parameter into the new reduction set.
				for(int i=0; i<list_size; i++) {
					Expression arg = argList.get(i);
					Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
					for( Expression exp : UseSet) {
						Symbol sm = SymbolTools.getSymbolOf(exp);
						if( (sm != null) && AnalysisTools.containsSymbol(o_set,sm.getSymbolName())) {
							Object obj = paramList.get(i);
							if( obj instanceof VariableDeclaration ) {
								VariableDeclarator vdecl = 
									(VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
								n_set.add(vdecl);
								tSet.add(AnalysisTools.findsSymbol(o_set, sm.getSymbolName()));
							} 
							break;
						}
					}
				}
				o_set.removeAll(tSet);
				// Put other reduction variables in the o_set, which are accessed 
				// in the called function, into the n_set.
				for( Symbol ssm : o_set ) {
					if( AnalysisTools.containsSymbol(accessedSymbols,ssm.getSymbolName()) ) {
						n_set.add(ssm);
					}
				}
				newreduction_map.put(ikey, n_set);
			}
			new_map.put("reduction", newreduction_map);
		}
		/*
		 * FIXME: What if a private variable is passed as a reference?
		 *        What if an argument consists of both shared and private variables?
		 */
		if( par_map.keySet().contains("private") ) {
			old_set = new HashSet<Symbol>((HashSet<Symbol>)par_map.get("private"));
			new_set = new HashSet<Symbol>();
			tSet.clear();
			// If actual argument is private, put corresponding parameter into the new private set.
			for(int i=0; i<list_size; i++) {
				Expression arg = argList.get(i);
				Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
				for( Expression exp : UseSet) {
					Symbol sm = SymbolTools.getSymbolOf(exp);
					if( (sm != null) && AnalysisTools.containsSymbol(old_set,sm.getSymbolName())) {
						Object obj = paramList.get(i);
						if( obj instanceof VariableDeclaration ) {
							VariableDeclarator vdecl = (VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
							if( SymbolTools.isScalar(vdecl) && !SymbolTools.isPointer(vdecl) ) {
								new_set.add(vdecl);
								tSet.add(AnalysisTools.findsSymbol(old_set, sm.getSymbolName()));
							} else {
								PrintTools.println("[WARNING] private variable, "+vdecl.getSymbolName()+", " +
										"is passed as a reference in procedure, " +  proc.getSymbolName() + 
										"(); splitting parallel region in "+proc.getSymbolName()+"() may result in "
										+ "incorrect output codes if "+vdecl.getSymbolName()+" upwardly exposed " +
										"in "+proc.getSymbolName()+"().", 0);
								new_set.add(vdecl);
							}
						} 
						break;
					}
				}
			}
			old_set.removeAll(tSet);
			// Put other private variables in the old_set, which are accessed 
			// in the called function, into the new set.
			for( Symbol ssm : old_set ) {
				if( AnalysisTools.containsSymbol(accessedSymbols,ssm.getSymbolName()) ) {
					new_set.add(ssm);
				}
			}
			// Put other private variables that are declared within this function call.
			Set<Symbol> localSymbols = new HashSet<Symbol>();
			DepthFirstIterator iter = new DepthFirstIterator(proc.getBody());
			while(iter.hasNext())
			{
				Object obj = iter.next();	
				if( obj instanceof SymbolTable ) {
					localSymbols.addAll(SymbolTools.getVariableSymbols((SymbolTable)obj));
				}
			}
			Set<Symbol> StaticLocalSet = AnalysisTools.getStaticVariables(localSymbols);
			new_set.addAll(localSymbols);
			new_set.removeAll(StaticLocalSet);
			new_map.put("private", new_set);
			////////////////////////////////////////////////////////////////////////////////
			//If shared variable is used as a private variable, it should be removed from //
			// the shared set.                                                            //
			////////////////////////////////////////////////////////////////////////////////
			old_set = (HashSet<Symbol>) new_map.get("shared");
			old_set.removeAll(new_set);
		}
		
		if( par_map.keySet().contains("firstprivate") ) {
			old_set = new HashSet<Symbol>((HashSet<Symbol>)par_map.get("firstprivate"));
			new_set = new HashSet<Symbol>();
			tSet.clear();
			// If actual argument is firstprivate, put corresponding parameter into the new firstprivate set.
			for(int i=0; i<list_size; i++) {
				Expression arg = argList.get(i);
				Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
				for( Expression exp : UseSet) {
					Symbol sm = SymbolTools.getSymbolOf(exp);
					if( (sm != null) && AnalysisTools.containsSymbol(old_set,sm.getSymbolName())) {
						Object obj = paramList.get(i);
						if( obj instanceof VariableDeclaration ) {
							VariableDeclarator vdecl = (VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
							if( SymbolTools.isScalar(vdecl) && !SymbolTools.isPointer(vdecl) ) {
								new_set.add(vdecl);
								tSet.add(AnalysisTools.findsSymbol(old_set, sm.getSymbolName()));
							} else {
								PrintTools.println("[WARNING] firstprivate variable, "+vdecl.getSymbolName()+", " +
										"is passed as a reference in procedure, " +  proc.getSymbolName() + 
										"(); splitting parallel region in "+proc.getSymbolName()+"() may result in "
										+ "incorrect output codes if "+vdecl.getSymbolName()+" upwardly exposed " +
										"in "+proc.getSymbolName()+"().", 0);
								new_set.add(vdecl);
							}
						} 
						break;
					}
				}
			}
			/////////////////////////////////////////////////////////////////////////////////////////////
			// Formal arguments of called routines in the region that are passed by value are private, //
			// and if they are accessed, it is very likely that these should be firstprivate.          //
			/////////////////////////////////////////////////////////////////////////////////////////////
			for( Object obj : paramList) {
				if( obj instanceof VariableDeclaration ) {
					VariableDeclarator vdecl = (VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
					if( SymbolTools.isScalar(vdecl) && !SymbolTools.isPointer(vdecl) 
							&& accessedSymbols.contains(vdecl) ) {
						new_set.add(vdecl);
					}
				}
			}
			old_set.removeAll(tSet);
			// Put other firstprivate variables in the old_set, which are accessed 
			// in the called function, into the new set.
			for( Symbol ssm : old_set ) {
				if( AnalysisTools.containsSymbol(accessedSymbols, ssm.getSymbolName()) ) {
					new_set.add(ssm);
				}
			}
			new_map.put("firstprivate", new_set);
			////////////////////////////////////////////////////////////////////////////////
			//If shared variable is used as a private variable, it should be removed from //
			// the shared set.                                                            //
			////////////////////////////////////////////////////////////////////////////////
			old_set = (HashSet<Symbol>) new_map.get("shared");
			old_set.removeAll(new_set);
		}
		
		if( par_map.keySet().contains("threadprivate") ) {
			old_set = new HashSet<Symbol>((HashSet<Symbol>)par_map.get("threadprivate"));
			new_set = new HashSet<Symbol>();
			tSet.clear();
			for(int i=0; i<list_size; i++) {
				Expression arg = argList.get(i);
				Set<Expression> UseSet = DataFlowTools.getUseSet(arg);
				for( Expression exp : UseSet) {
					Symbol sm = SymbolTools.getSymbolOf(exp);
					//DEBUG: Currently, if sm is an AccessSymbol, only base symbol is used.
					if( sm instanceof AccessSymbol ) {
						sm = ((AccessSymbol)sm).getIRSymbol();
					}
					if( (sm != null) && AnalysisTools.containsSymbol(old_set,sm.getSymbolName())) {
						Object obj = paramList.get(i);
						if( obj instanceof VariableDeclaration ) {
							VariableDeclarator vdecl = (VariableDeclarator)((VariableDeclaration)obj).getDeclarator(0);
							new_set.add(vdecl);
							tSet.add(AnalysisTools.findsSymbol(old_set, sm.getSymbolName()));
						} 
						break;
					}
				}
			}
			old_set.removeAll(tSet);
			// Put other shared variables in the old_set, which are accessed 
			// in the called function, into the new set.
			for( Symbol ssm : old_set ) {
				if( AnalysisTools.containsSymbol(IpAccessedSymbols,ssm.getSymbolName()) ) {
					new_set.add(ssm);
				}
			}
			new_map.put("threadprivate", new_set);
		}
/*		PrintTools.println("[updateOmpMapForCalledFunc] New threadprivate set: "+ 
				AnalysisTools.symbolsToString(new_set, ","), 0);*/
		
		
		// Update annotations of omp-for loops enclosed by the called procedure, proc.
		List<OmpAnnotation> ompfor_annotList = 
			IRTools.collectPragmas(proc.getBody(), OmpAnnotation.class, "for");
		for( OmpAnnotation ompfor_annot : ompfor_annotList ) {
			Statement atstmt = (Statement)ompfor_annot.getAnnotatable();
			accessedSymbols = SymbolTools.getAccessedSymbols(atstmt);
			if( ompfor_annot.keySet().contains("shared") ) {
				HashSet<Symbol> o_set = (HashSet<Symbol>)ompfor_annot.remove("shared");
				HashSet<Symbol> n_set = new HashSet<Symbol>();
				tSet.clear();
				new_set = (HashSet<Symbol>)new_map.get("shared");
				for( Symbol sm : new_set ) {
					if( AnalysisTools.containsSymbol(accessedSymbols,sm.getSymbolName())) {
						n_set.add(sm);
						tSet.add(AnalysisTools.findsSymbol(new_set, sm.getSymbolName()));
					}
				}
				o_set.removeAll(tSet);
				for( Symbol sm : o_set ) {
					if( !AnalysisTools.containsSymbol(n_set,sm.getSymbolName())) {
						n_set.add(sm);
					}
				}
				ompfor_annot.put("shared", n_set);
			}
		}
		return new_map;
	}
	
	static public void removeUnusedProcedures(Program prog) {
		boolean mayContainUnusedOnes = true;
		while ( mayContainUnusedOnes ) {
			List<Procedure> procList = IRTools.getProcedureList(prog);
			List<FunctionCall> funcCallList = IRTools.getFunctionCalls(prog);
			HashSet<String> funcCallSet = new HashSet<String>();
			HashSet<String> deletedSet = new HashSet<String>();
			for( FunctionCall fCall : funcCallList ) {
				funcCallSet.add(fCall.getName().toString());
			}
			for( Procedure proc : procList ) {
				String pName = proc.getSymbolName();
				if (pName.equals("main") || pName.equals("MAIN__")) {
					// Skip main procedure.
					continue;
				}
				if( !funcCallSet.contains(pName) ) {
					//Procedure is never used in this program.
					TranslationUnit tu = (TranslationUnit)proc.getParent();
					//Delete the unused procedure.
					/////////////////////////////////////////////////////////////////////////////
					// FIXME: When a procedure, proc, is added to a TranslationUnit, tu,       //
					// tu.containsSymbol(proc) returns false, but tu.containsDeclaration(proc) //
					// returns true.                                                           //
					/////////////////////////////////////////////////////////////////////////////
					//if( tu.containsSymbol(proc) ) {
					if( tu.containsDeclaration(proc) ) {
						tu.removeChild(proc);
						deletedSet.add(pName);
					} else {
						PrintTools.println("[WARNING in removeUnusedProcedures()] Can't delete procedure, " + pName, 0);
					}
					// Check whether corresponding ProcedureDeclarator exists and delete it. 
					FlatIterator Fiter = new FlatIterator(prog);
					while (Fiter.hasNext())
					{
						TranslationUnit cTu = (TranslationUnit)Fiter.next();
						List<Traversable> children = cTu.getChildren();
						Traversable child = null;
						do {
							child = null;
							for( Traversable t : children ) {
								if( t instanceof VariableDeclaration ) {
									Declarator declr = ((VariableDeclaration)t).getDeclarator(0);
									if( declr instanceof ProcedureDeclarator ) {
										if( ((ProcedureDeclarator)declr).getSymbolName().equals(pName) ) {
											child = t;
											break;
										}
									}
								}
							}
							if( child != null ) {
								//cTu.removeChild(child);
								children.remove(child);
								child.setParent(null);
							}
						} while (child != null);
					}
				}
			}
			if( !deletedSet.isEmpty() ) {
				mayContainUnusedOnes = true;
				PrintTools.println("[INFO in removeUnusedProcedures()] list of deleted procedures: " +
						PrintTools.collectionToString(deletedSet, ", "), 2);
			} else {
				mayContainUnusedOnes = false;
			}
		}
	}
	
	static private String[] dummyVars = {"gpuNumThreads", "gpuNumBlocks", "gpuBytes",
	"gpuNumBlocks1", "gpuNumBlocks2", "totolNumThreads", "gpuGmemSize", "gpuSmemSize",
	"totalNumThreads"};
	
	static public void removeUnusedSymbols(Program prog) {
		Set<TranslationUnit> skipTrList = new HashSet<TranslationUnit>();
		List<Procedure> procList = IRTools.getProcedureList(prog);
		for( Procedure proc : procList ) {
			List returnTypes = proc.getTypeSpecifiers();
			if( returnTypes.contains(CUDASpecifier.CUDA_GLOBAL) 
				|| returnTypes.contains(CUDASpecifier.CUDA_DEVICE) ) {
				skipTrList.add(IRTools.getParentTranslationUnit(proc));
			}
			CompoundStatement pBody = proc.getBody();
			Set<Symbol> accessedSymbols = AnalysisTools.getAccessedVariables(pBody);
			Set<Symbol> declaredSymbols = SymbolTools.getVariableSymbols(pBody);
			HashSet<Symbol> unusedSymbolSet = new HashSet<Symbol>();
			for( Symbol sym : declaredSymbols ) {
				String sname = sym.getSymbolName();
				if( sname.startsWith("dimBlock") || sname.startsWith("dimGrid") ||
					sname.startsWith("_gtid") || sname.startsWith("_bid") ||
					sname.startsWith("row_temp_") || sname.startsWith("lred__") ||
					sname.endsWith("__extended") || sname.startsWith("gpu__") ||
					sname.startsWith("red__") || sname.startsWith("param__") ||
					sname.startsWith("pitch__") || sname.startsWith("sh__") ) {
					continue;
				}
				if( !accessedSymbols.contains(sym) ) {
					if( sym instanceof VariableDeclarator ) {
						VariableDeclarator declr = (VariableDeclarator)sym;
						if( !AnalysisTools.isClassMember(declr) ) {
							Declaration decl = declr.getDeclaration();
							unusedSymbolSet.add(sym);
							pBody.removeChild(decl.getParent());
						}
					}
				}
			}
			if( !unusedSymbolSet.isEmpty() ) {
				PrintTools.println(" Declarations removed from a procedure, " + 
						proc.getSymbolName() + ": " + AnalysisTools.symbolsToString(unusedSymbolSet, ", "), 2);
			}
		}
		
		//DEBUG: removing unused dummy variables are temporarily disabled, since it may cause
		//problems when one file is manually included in another file.
/*		List<String> dummyVarList = Arrays.asList(dummyVars);
		for ( Traversable tt : prog.getChildren() )
		{
			HashSet<Symbol> unusedSymbolSet = new HashSet<Symbol>();
			TranslationUnit tu = (TranslationUnit)tt;
			String iFileName = tu.getInputFilename();
			int dot = iFileName.lastIndexOf(".h");
			if( dot >= 0 ) {
				continue;
			} else if( skipTrList.contains(tu)) {
				continue;
			}
			Set<Symbol> accessedSymbols = SymbolTools.getAccessedSymbols(tu);
			if( accessedSymbols == null ) {
				continue;
			}
			for(String var : dummyVarList) {
				Declaration symDecl = SymbolTools.findSymbol(tu, var);
				if( ( symDecl != null ) && ( symDecl instanceof VariableDeclaration ) ) {
					VariableDeclarator sym = (VariableDeclarator)((VariableDeclaration)symDecl).getDeclarator(0);
					if( !accessedSymbols.contains(sym)) {
						unusedSymbolSet.add(sym);
						tu.removeChild(symDecl);
					}
				}
			}
			if( !unusedSymbolSet.isEmpty() ) {
				PrintTools.println(" Declarations removed from a translation unit, " + 
						iFileName + ": " + AnalysisTools.symbolsToString(unusedSymbolSet, ", "), 2);
			}
		}*/
	}
	
	/**
	 * Wrap omp-parallel-for loops using compoundStatement if the parallel-for loops
	 * contain reduction clauses. This wrapping is needed to prevent a possible deadlock 
	 * caused by reduction transformation in O2GTranslator. 
	 * 
	 * @param prog
	 */
	static public void wrapOmpParallelForLoops(Program prog) {
		DepthFirstIterator iter = new DepthFirstIterator(prog);

		while ( iter.hasNext() )
		{
			Object obj = iter.next();

			if( (obj instanceof Annotatable) && (obj instanceof Statement) )
			{
				Annotatable at = (Annotatable)obj;

				if ( at.containsAnnotation(OmpAnnotation.class, "parallel") &&
						at.containsAnnotation(OmpAnnotation.class, "for") &&
						at.containsAnnotation(OmpAnnotation.class, "reduction"))
				{
					//Create an empty CompoundStatement.
					CompoundStatement cStmt = new CompoundStatement();
					//Move OmpAnnotations to the wrapping compound statement. //
					List<OmpAnnotation> omp_annots = at.getAnnotations(OmpAnnotation.class);
					if( omp_annots != null ) {
						OmpAnnotation forAnnot = new OmpAnnotation("for", "true");
						for(OmpAnnotation omp_annot : omp_annots) {
							if( omp_annot.containsKey("for") ) {
								omp_annot.remove("for");
							}
							if( omp_annot.containsKey("reduction") ) {
								forAnnot.put("reduction", omp_annot.remove("reduction"));
							}
							cStmt.annotate(omp_annot);
						}
						at.removeAnnotations(OmpAnnotation.class);
						forAnnot.put("nowait", "true");
						at.annotate(forAnnot);
					}
					//If CudaAnnotations exist, move them too. //
					List<CudaAnnotation> cuda_annots = at.getAnnotations(CudaAnnotation.class);
					if( cuda_annots != null ) {
						for(CudaAnnotation cuda_annot : cuda_annots) {
							cStmt.annotate(cuda_annot);
						}
						at.removeAnnotations(CudaAnnotation.class);
					}
					//CommentAnnotation may exist for debugging; move them too. //
					List<CommentAnnotation> comment_annots = at.getAnnotations(CommentAnnotation.class);
					if( comment_annots != null ) {
						for(CommentAnnotation comment_annot : comment_annots) {
							cStmt.annotate(comment_annot);
						}
						at.removeAnnotations(CommentAnnotation.class);
					}
					
					//Swap the parallel-for loop with compoundstatement.
					Statement atstmt = (Statement)at;
					atstmt.swapWith(cStmt);
					cStmt.addStatement(atstmt);
					
				} else {
					continue;
				}
			}
		}
	}
}
