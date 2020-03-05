/**
 * 
 */
package omp2gpu.transforms;

import java.util.*;

import omp2gpu.analysis.AnalysisTools;
import omp2gpu.hir.CudaAnnotation;
import cetus.analysis.CallGraph;
import cetus.hir.*;
import cetus.transforms.TransformPass;

/**
 * <b>KernelCallingProcCloning</b> clones procedures that are called in kernel regions.
 * 
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 */
public class KernelCallingProcCloning extends TransformPass {

	/**
	 * @param program
	 */
	public KernelCallingProcCloning(Program program) {
		super(program);
	}

	/* (non-Javadoc)
	 * @see cetus.transforms.TransformPass#getPassName()
	 */
	@Override
	public String getPassName() {
		return new String("[Kernel-Calling-Procedure Cloning]");
	}

	/* (non-Javadoc)
	 * @see cetus.transforms.TransformPass#start()
	 */
	@Override
	public void start() {
		AnalysisTools.markIntervalForKernelRegions(program);

		// generate a list of procedures in post-order traversal
		CallGraph callgraph = new CallGraph(program);
		// procedureList contains Procedure in ascending order; the last one is main
		List<Procedure> procedureList = callgraph.getTopologicalCallList();

		List<FunctionCall> funcCallList = IRTools.getFunctionCalls(program);
		// cloneProcMap contains procedures containing kernel regions and called multiple times.
		TreeMap<Integer, Procedure> cloneProcMap = new TreeMap<Integer, Procedure>();
		// MayCloneProcMap contains procedures containing kernel regions but called once; these 
		// may be cloned too if parent procedures are cloned.
		TreeMap<Integer, Procedure> MayCloneProcMap = new TreeMap<Integer, Procedure>();

		/* drive the engine; visit every procedure */
		for (Procedure proc : procedureList)
		{
			boolean kernelExists = false;
			int numOfCalls = 0;
			LinkedList<Procedure> callingProcs = new LinkedList<Procedure>();
			HashSet<Procedure> visitedProcs = new HashSet<Procedure>();
			String name = proc.getName().toString();
			/* f2c code uses MAIN__ */
			if (name.equals("main") || name.equals("MAIN__")) {
				continue;
			}
			List<OmpAnnotation> bBarrier_annots = (List<OmpAnnotation>)
			IRTools.collectPragmas(proc.getBody(), OmpAnnotation.class, "barrier");
			if( bBarrier_annots.isEmpty() ) {
				// Current procedure does not contain any kernel region; skip it.
				continue;
			}
			for( OmpAnnotation omp_annot : bBarrier_annots ) {
				String type = (String)omp_annot.get("barrier");
				if( type.equals("S2P") ) {
					kernelExists = true;
					break;
				} else {
					continue;
				}
			}
			if( !kernelExists ) {
				// Current procedure does not contain any kernel region; skip it.
				continue;
			}
			callingProcs.add(proc);
			while( !callingProcs.isEmpty() ) {
				Procedure c_proc = callingProcs.removeFirst();
				numOfCalls = 0;
				for( FunctionCall funcCall : funcCallList ) {
					if(c_proc.getName().equals(funcCall.getName())) {
						numOfCalls++;
						Traversable t = funcCall.getStatement();
						while( (t != null) && !(t instanceof Procedure) ) {
							t = t.getParent();
						}
						Procedure p_proc = (Procedure)t;
						name = p_proc.getName().toString();
						if (!name.equals("main") && !name.equals("MAIN__")) {
							if( !visitedProcs.contains(p_proc) ) {
								callingProcs.add(p_proc);
								visitedProcs.add(p_proc);
							}
						}
					}
				}
				int ind = procedureList.indexOf(c_proc);
				if( ind > -1 ) {
					if( numOfCalls > 1 ) {
						// Current procedure contains a kernel region and is called more than once.
						cloneProcMap.put(new Integer(ind), c_proc);
					}
					else {
						// Current procedure contains a kernel region but is called only once.
						MayCloneProcMap.put(new Integer(ind), c_proc);
					}
				}
			}
		}

		Integer lastKey = null;
		Procedure c_proc = null;
		// Clone procedures in clonedProcMap and MayCloneProcMap.
		while( !cloneProcMap.isEmpty() || !MayCloneProcMap.isEmpty() ) {
			if( !cloneProcMap.isEmpty() ) {
				lastKey = cloneProcMap.lastKey();
				c_proc = cloneProcMap.remove(lastKey);
			} else if( !MayCloneProcMap.isEmpty() ) {
				lastKey = MayCloneProcMap.lastKey();
				c_proc = MayCloneProcMap.remove(lastKey);
			}
			funcCallList = IRTools.getFunctionCalls(program);
			int numOfCalls = 0;
			for( FunctionCall funcCall : funcCallList ) {
				if(c_proc.getName().equals(funcCall.getName())) {
					if( numOfCalls > 0 ) {
						// Clone current procedure 
						List<Specifier> return_types = c_proc.getReturnType();
						List<VariableDeclaration> oldParamList = 
							(List<VariableDeclaration>)c_proc.getParameters();
						CompoundStatement body = (CompoundStatement)c_proc.getBody().clone();
						String new_proc_name = c_proc.getSymbolName() + "_clnd" + numOfCalls;
						Procedure new_proc = new Procedure(return_types,
								new ProcedureDeclarator(new NameID(new_proc_name),
										new LinkedList()), body);	
						if( oldParamList != null ) {
							for( VariableDeclaration param : oldParamList ) {
								Symbol param_declarator = (Symbol)param.getDeclarator(0);
								VariableDeclaration cloned_decl = (VariableDeclaration)param.clone();
								Identifier paramID = new Identifier(param_declarator);
								Identifier cloned_ID = new Identifier((Symbol)cloned_decl.getDeclarator(0));
								new_proc.addDeclaration(cloned_decl);
								IRTools.replaceAll((Traversable) body, paramID, cloned_ID);
							}
						}
						TranslationUnit tu = (TranslationUnit)c_proc.getParent();
						////////////////////////////
						// Add the new procedure. //
						////////////////////////////
						tu.addDeclarationAfter(c_proc, new_proc);
						//////////////////////////////////////////////////////////////////
						//If declaration statement exists for the original procedure,   //
						//create a new declaration statement for the new procedure too. //
						//////////////////////////////////////////////////////////////////
						FlatIterator Fiter = new FlatIterator(program);
						while (Fiter.hasNext())
						{
							TranslationUnit cTu = (TranslationUnit)Fiter.next();
							BreadthFirstIterator iter = new BreadthFirstIterator(cTu);
							iter.pruneOn(ProcedureDeclarator.class);
							for (;;)
							{
								ProcedureDeclarator procDeclr = null;

								try {
									procDeclr = (ProcedureDeclarator)iter.next(ProcedureDeclarator.class);
								} catch (NoSuchElementException e) {
									break;
								}
								if( procDeclr.getID().equals(c_proc.getName()) ) {
									//Found function declaration.
									VariableDeclaration procDecl = (VariableDeclaration)procDeclr.getParent();
									//Create a new function declaration.
									VariableDeclaration newProcDecl = 
										new VariableDeclaration(procDecl.getSpecifiers(), new_proc.getDeclarator().clone());
									//Insert the new function declaration.
									cTu.addDeclarationAfter(procDecl, newProcDecl);
									break;
								}
							}
						}
						/////////////////////////////////////////////////////////////////////////
						// Update the newly cloned procedure:                                  //
						//     1) Update symbols in the new procedure, including symbols       //
						//        in OmpAnnoations.                                            //
						/////////////////////////////////////////////////////////////////////////
						SymbolTools.linkSymbol(new_proc);
						TransformTools.updateAnnotationsInRegion(new_proc, true);
						//////////////////////////////////////////////////////////
						// Create a new function call for the cloned procedure. //
						//////////////////////////////////////////////////////////
						if( funcCall != null ) {
							FunctionCall new_funcCall = new FunctionCall(new NameID(new_proc_name));
							List<Expression> argList = (List<Expression>)funcCall.getArguments();
							if( argList != null ) {
								for( Expression exp : argList ) {
									new_funcCall.addArgument(exp.clone());
								}
							}
							funcCall.swapWith(new_funcCall);
						}
					}
					numOfCalls++;
				}
			}
		}

		SplitOmpPRegion.cleanExtraBarriers(program, false);
	}

}
