/**
 * 
 */
package omp2gpu.transforms;

import java.util.*;

import cetus.hir.*;
import cetus.analysis.LoopTools;
import cetus.transforms.TransformPass;

/**
 * <b>OmpCollapse</b> performs source-level transformation to collapse
 * the iterations of all associated loops into one larger iteration space.
 * 
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 */
public class OmpCollapse extends TransformPass {

	/**
	 * @param program
	 */
	public OmpCollapse(Program program) {
		super(program);
	}

	/* (non-Javadoc)
	 * @see cetus.transforms.TransformPass#getPassName()
	 */
	@Override
	public String getPassName() {
		return new String("[Omp-collapse transformation]");
	}

	/* (non-Javadoc)
	 * @see cetus.transforms.TransformPass#start()
	 */
	@Override
	public void start() {
		List<ForLoop> outer_loops = new ArrayList<ForLoop>();
		// Find omp-for loops containing collapse clauses with parameter value > 1.
		List<OmpAnnotation> forAnnotList = IRTools.collectPragmas(program, OmpAnnotation.class, "for");
		for( OmpAnnotation fAnnot : forAnnotList ) {
			Annotatable at = fAnnot.getAnnotatable();
			if( at instanceof ForLoop && at.containsAnnotation(OmpAnnotation.class, "collapse")) {
				OmpAnnotation collapseAnnot = at.getAnnotation(OmpAnnotation.class, "collapse");
				int collapseLevel = Integer.parseInt((String)collapseAnnot.get("collapse"));
				if( collapseLevel > 1 ) {
					outer_loops.add((ForLoop)at);
				}
			}
		}
		if( outer_loops.isEmpty() ) {
			return;
		} else {
			PrintTools.println("Found omp-collapse clauses that associate more than one loop.",0);
			int collapsedLoops = 0;
			for( ForLoop ompLoop : outer_loops ) {
				Traversable t = (Traversable)ompLoop;
				while(true) {
					if (t instanceof Procedure) break;
					t = t.getParent(); 
				}
				Procedure proc = (Procedure)t;
/*				if( !LoopTools.isPerfectNest(ompLoop) ) {
					PrintTools.println("[INFO] OmpCollapse implementation can handle perfectly nested loops only; skip the loop in procedure, "
							+ proc.getSymbolName() + ".",0);
					PrintTools.println("omp-for loop\n" + ompLoop + "\n", 2);
					continue;
				}*/
				ArrayList<Symbol> indexSymbols = new ArrayList<Symbol>();
				ArrayList<ForLoop> indexedLoops = new ArrayList<ForLoop>();
				OmpAnnotation collapseAnnot = ompLoop.getAnnotation(OmpAnnotation.class, "collapse");
				int collapseLevel = Integer.parseInt((String)collapseAnnot.get("collapse"));
				ForLoop currLoop = ompLoop;
				int i = 0;
				boolean pnest = true;
				while( i<collapseLevel ) {
					if( currLoop != null ) {
						indexedLoops.add(i, currLoop);
						indexSymbols.add(i, LoopTools.getLoopIndexSymbol(currLoop));
					} else {
						break;
					}
					i++;
					if( i<collapseLevel ) {
						//Find a nested loop.
						Statement fBody = currLoop.getBody();
						currLoop = null;
						FlatIterator iter = new FlatIterator((Traversable)fBody);
						Object o = null;
						if (iter.hasNext())
						{
							boolean skip = false;
							do
							{
								o = (Statement)iter.next(Statement.class);
								
								if (o instanceof AnnotationStatement)
									skip = true;
								else
									skip = false;
								
							} while ((skip) && (iter.hasNext()));
							
							if (o instanceof ForLoop)
							{
								currLoop = (ForLoop)o;
								
								/* The ForLoop contains additional statements after the end
								 * of the first ForLoop. This is interpreted as
								 * a non-perfect nest for dependence testing
								 */
								if (iter.hasNext())
									pnest = false;
							}
							else if (o instanceof CompoundStatement)
							{
								List children = ((Statement)o).getChildren();
								Statement s = (Statement)children.get(0);
								if (s instanceof ForLoop)
									currLoop = (ForLoop)o;
								else
									pnest = false;
							}
							else if (IRTools.containsClass(fBody, ForLoop.class))
							{
								PrintTools.println("Loop is not perfectly nested", 8);
								pnest = false;
							}
						}
						if( !pnest ) {
							break;
						}
					}
				}
				if( !pnest ) {
					PrintTools.println("[INFO] OmpCollapse implementation can handle perfectly nested loops only; skip the loop in procedure, "
							+ proc.getSymbolName() + ".",0);
					PrintTools.println("omp-for loop\n" + ompLoop + "\n", 2);
					continue;
				}
				if( indexedLoops.size() < collapseLevel ) {
					PrintTools.println("[WARNING] Number of found loops (" + indexedLoops.size() + 
							") is smaller then collapse parameter (" + collapseLevel + "); skip the loop in procedure " 
							+ proc.getSymbolName() + ".",0);
					PrintTools.println("omp-for loop\n" + ompLoop + "\n", 2);
					continue;
				}
				collapsedLoops++;
				ForLoop innerLoop = indexedLoops.get(collapseLevel-1);
				Statement fBody = innerLoop.getBody();
				CompoundStatement cStmt = null;
				Statement firstNonDeclStmt = null;
				if( fBody instanceof CompoundStatement ) {
					cStmt = (CompoundStatement)fBody;
					firstNonDeclStmt = IRTools.getFirstNonDeclarationStatement(fBody);
				} else {
					cStmt = new CompoundStatement();
					cStmt.addStatement(fBody);
					firstNonDeclStmt = fBody;
				}
				ArrayList<Expression> iterspaceList = new ArrayList<Expression>();
				ArrayList<Expression> lbList = new ArrayList<Expression>();
				Expression collapsedIterSpace = null;
				for( i=0; i<collapseLevel; i++ ) {
					ForLoop loop = indexedLoops.get(i);
					Expression lb = LoopTools.getLowerBoundExpression(loop);
					lbList.add(i, lb);
					Expression ub = LoopTools.getUpperBoundExpression(loop);
					Expression itrSpace = Symbolic.add(Symbolic.subtract(ub,lb),new IntegerLiteral(1));
					iterspaceList.add(i, itrSpace);
					if( i==0 ) {
						collapsedIterSpace = itrSpace;
					} else {
						collapsedIterSpace = Symbolic.multiply(collapsedIterSpace, itrSpace);
					}
				}
				//Create a new index variable for the newly collapsed loop.
				CompoundStatement procBody = proc.getBody();
				Identifier newIndex = TransformTools.getTempIndex(procBody, 0);
				/////////////////////////////////////////////////////////////////////////////////
				//Swap initialization statement, condition, and step of the omp-for loop with  //
				//those of the new, collapsed loop.                                            //
				/////////////////////////////////////////////////////////////////////////////////
				Expression expr1 = new AssignmentExpression(newIndex.clone(), AssignmentOperator.NORMAL,
						new IntegerLiteral(0));
				Statement initStmt = new ExpressionStatement(expr1);
				ompLoop.getInitialStatement().swapWith(initStmt);
				expr1 = new BinaryExpression((Identifier)newIndex.clone(), BinaryOperator.COMPARE_LT,
						collapsedIterSpace);
				ompLoop.getCondition().swapWith(expr1);
				expr1 = new UnaryExpression(
						UnaryOperator.POST_INCREMENT, (Identifier)newIndex.clone());
				ompLoop.getStep().swapWith(expr1);
				Identifier ompforIndex = new Identifier(indexSymbols.get(0));
				i = collapseLevel-1;
				while( i>0 ) {
					Identifier tIndex = new Identifier(indexSymbols.get(i));
					if( i == (collapseLevel-1) ) {
						expr1 = new BinaryExpression(newIndex.clone(), BinaryOperator.MODULUS, 
							iterspaceList.get(i).clone());
					} else {
						expr1 = new BinaryExpression(ompforIndex.clone(), BinaryOperator.MODULUS, 
							iterspaceList.get(i).clone());
					}
					Expression lbExp = lbList.get(i).clone();
					if( !(lbExp instanceof Literal) || !lbExp.toString().equals("0") ) {
						expr1 = new BinaryExpression(expr1, BinaryOperator.ADD, lbExp);
					}
					Statement stmt = new ExpressionStatement(new AssignmentExpression(tIndex.clone(), 
							AssignmentOperator.NORMAL, expr1));
					cStmt.addStatementBefore(firstNonDeclStmt, stmt);
					if( i == (collapseLevel-1) ) {
						expr1 = new BinaryExpression(newIndex.clone(), BinaryOperator.DIVIDE, 
							iterspaceList.get(i).clone());
					} else {
						expr1 = new BinaryExpression(ompforIndex.clone(), BinaryOperator.DIVIDE, 
							iterspaceList.get(i).clone());
					}
					if( i == 1 ) {
						lbExp = lbList.get(0).clone();
						if( !(lbExp instanceof Literal) || !lbExp.toString().equals("0") ) {
							expr1 = new BinaryExpression(expr1, BinaryOperator.ADD, lbExp);
						}
					}
					stmt = new ExpressionStatement(new AssignmentExpression(ompforIndex.clone(), 
						AssignmentOperator.NORMAL, expr1));
					cStmt.addStatementBefore(firstNonDeclStmt, stmt);
					i--;
				}
				/////////////////////////////////////////////////////////////////////////
				//Swap the body of the omp-for loop with the one of the innermost loop //
				//among associated loops.                                              //
				/////////////////////////////////////////////////////////////////////////
				ompLoop.getBody().swapWith(cStmt);
			}
			PrintTools.println("[INFO] Number of collapsed omp-for loops: " + collapsedLoops, 0);
		}
	}

}
