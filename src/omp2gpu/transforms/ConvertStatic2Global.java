package omp2gpu.transforms;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeMap;

import omp2gpu.analysis.AnalysisTools;
import omp2gpu.hir.CudaAnnotation;
import cetus.hir.Annotatable;
import cetus.hir.AnnotationDeclaration;
import cetus.hir.ChainedList;
import cetus.hir.CompoundStatement;
import cetus.hir.OmpAnnotation;
import cetus.hir.DeclarationStatement;
import cetus.hir.Declaration;
import cetus.hir.DepthFirstIterator;
import cetus.hir.IRTools;
import cetus.hir.Identifier;
import cetus.hir.PragmaAnnotation;
import cetus.hir.PrintTools;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.Statement;
import cetus.hir.Symbol;
import cetus.hir.SymbolTools;
import cetus.hir.TranslationUnit;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cetus.transforms.TransformPass;

/**
 * Convert static variables in procedures except for main into global variables.
 * 
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 */
public class ConvertStatic2Global extends TransformPass {

	public ConvertStatic2Global(Program program) {
		super(program);
	}

	@Override
	public String getPassName() {
		return new String("[convertStatic2Global]");
	}

	@Override
	public void start() {
		List<Procedure> procList = IRTools.getProcedureList(program);
		HashMap<String, String> static2globalMap = new HashMap<String, String>();
		for( Procedure proc : procList ) {
			Declaration orgFirstDecl = null;
			String pName = proc.getSymbolName();
			if (pName.equals("main") || pName.equals("MAIN__")) {
				// Skip main procedure.
				continue;
			}
			static2globalMap.clear();
			CompoundStatement pBody = proc.getBody();
			TranslationUnit tu = (TranslationUnit)proc.getParent();
			Set<Symbol> symSet = SymbolTools.getVariableSymbols(pBody);
			Set<Symbol> staticSyms = AnalysisTools.getStaticVariables(symSet);
			//for( Symbol sSym : staticSyms ) {
		    TreeMap<String, Symbol> sortedMap = new TreeMap<String, Symbol>();
			for( Symbol static_var : staticSyms ) {
				String symName = static_var.getSymbolName();
				sortedMap.put(symName, static_var);
			}
			Collection<Symbol> sortedSet = sortedMap.values();
			for( Symbol sSym : sortedSet ) {
				if( !(sSym instanceof VariableDeclarator) ) {
					PrintTools.println("[WARNING in convertStatic2Global()] unexpected type of symbol : "+sSym,0);
					continue;
				}
				VariableDeclarator declr = (VariableDeclarator)sSym;
				VariableDeclaration decl = (VariableDeclaration)declr.getDeclaration();
				/* 
				 * Create a cloned Declaration of the static variable.
				 */
				VariableDeclarator cloned_declarator = 
					(VariableDeclarator)declr.clone();
				String oldName = sSym.getSymbolName();
				String newName = oldName.concat("__").concat(proc.getSymbolName());
				static2globalMap.put(oldName, newName);
				//cloned_declarator.setName(newName);
				List<Specifier> clonedspecs = new ChainedList<Specifier>();
				clonedspecs.addAll(decl.getSpecifiers());
				VariableDeclaration cloned_decl = new VariableDeclaration(clonedspecs, cloned_declarator);
				// Remove current static symbol from the procedure.
				DeclarationStatement dStmt = (DeclarationStatement)decl.getParent();
				pBody.removeChild(dStmt);
				// Create a new global variable in the enclosing translation unit.
				Declaration firstDecl = tu.getFirstDeclaration();
				if( orgFirstDecl == null ) {
					orgFirstDecl = firstDecl;
				}
				tu.addDeclarationBefore(firstDecl, cloned_decl);
				cloned_declarator.setName(newName);
				// Replace old ID with the new ID.
				Identifier orgID = new Identifier(declr);
				Identifier cloned_ID = new Identifier(cloned_declarator);
				IRTools.replaceAll(pBody, orgID, cloned_ID);
			}
			if( !static2globalMap.isEmpty() ) {
				// Update OmpAnnoation or CudaAnnotation contained in the current procedure.
				Set<Statement> removeSet = new HashSet<Statement>();
				DepthFirstIterator itr = new DepthFirstIterator(pBody);
				while(itr.hasNext())
				{
					Object obj = itr.next();

					if ( (obj instanceof Annotatable) && (obj instanceof Statement) )
					{
						Annotatable at = (Annotatable)obj;
						List<PragmaAnnotation> aList = at.getAnnotations(PragmaAnnotation.class);
						if( aList != null ) {
							for( PragmaAnnotation pAnnot : aList ) {
								if( pAnnot.containsKey("threadprivate") ) {
									Object oVal = pAnnot.get("threadprivate");
									if( oVal instanceof Set ) {
										Set valSet = (Set)oVal;
										Set newSet = new HashSet<String>();
										for( String oName : static2globalMap.keySet() ) {
											if( valSet.contains(oName) ) {
												valSet.remove(oName);
												newSet.add(static2globalMap.get(oName));
											}
										}
										if( valSet.isEmpty() ) {
											//////////////////////////////////////////////////////////////
											// If old omp threadprivate annotation is empty, remove it. //
											//////////////////////////////////////////////////////////////
											removeSet.add((Statement)obj);
										}
										if( !newSet.isEmpty() ) {
											////////////////////////////////////////////////////////////////////
											// Insert the new omp threadprivate annotation into the enclosing //
											// TranslationUnit.                                               //
											////////////////////////////////////////////////////////////////////
											OmpAnnotation new_annot = new OmpAnnotation("threadprivate", newSet);
											AnnotationDeclaration annot_container = new AnnotationDeclaration(new_annot);
											tu.addDeclarationBefore(orgFirstDecl, annot_container);
										}
									} else {
										PrintTools.println("[ERROR in ConvertStatic2Global()] wrong value type in an omp " +
												"threadprivate annotation: " + obj, 0);
									}
								} else {
									for( String key : pAnnot.keySet() ) {
										Object oVal = pAnnot.get(key);
										if( oVal instanceof Set ) {
											Set valSet = (Set)oVal;
											for( String oName : static2globalMap.keySet() ) {
												if( valSet.contains(oName) ) {
													valSet.remove(oName);
													valSet.add(static2globalMap.get(oName));
												}
											}
										} else if( oVal instanceof Map ) {
											//reduction clause has maps as values.
											Map ValMap = (Map)oVal;
											for( Object key2 : ValMap.keySet() ) {
												Object oVal2 = ValMap.get(key2);
												if( oVal2 instanceof Set ) {
													Set valSet2 = (Set)oVal2;
													for( String oName : static2globalMap.keySet() ) {
														if( valSet2.contains(oName) ) {
															valSet2.remove(oName);
															valSet2.add(static2globalMap.get(oName));
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
				if( !removeSet.isEmpty() ) {
					for( Statement rStmt : removeSet ) {
						pBody.removeStatement(rStmt);
					}
				}
			}
		}
	}
	
}
