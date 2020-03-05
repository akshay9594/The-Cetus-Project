package omp2gpu.analysis; 

import java.util.*;
import cetus.hir.*;
import cetus.exec.*;
import cetus.analysis.*;
import omp2gpu.analysis.*;

/**
 *Simplified version of ReachAnalysis.
 * ReachingDef performs may & must reaching-definition analysis of the program
 *  <p> 
 * Input  : CFGraph cfg
 * Output : Must and May ReachDEF set for each node in cfg
 *  <p> 
 * ReachDEF(entry-node) = {}	: only intra-procedural analysis
 *  <p> 
 * for ( node m : predecessor nodes of node n )
 * 	ReachDEF(n) = ^ { DEDef(m) v (ReachDEF(m) ^ ~Killed(m)) }  : Must Reaching Def
 * 	ReachDEF(n) = v { DEDef(m) v (ReachDEF(m) ^ ~Killed(m)) }  : May Reaching Def
 * where,
 *   DEDef(m) is a Downward-Exposed-Def set of node m
 *   ~Killed(m) is a set of variables not defined_vars in node m
 *  <p> 
 *  Additionally, this analysis calculates whether variables of interest  are 
 *  modified by CPU or GPU, but to use this information, input procedure (icfg) 
 *  should be called by CPU.
 *  
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 *
 */
public class ReachAnalysis0
{
	private int debug_level;
	private boolean debug_on = false;

	private CFGraph cfg;
	private Traversable input_code;
	private Set<Symbol> target_vars;

	public ReachAnalysis0(Traversable icode, CFGraph icfg, Set<Symbol> tarvars)
	{
		input_code = icode;
		cfg = icfg;
		target_vars = tarvars;
		debug_level = PrintTools.getVerbosity();
	}

	public String getPassName()
	{
		return new String("[ReachAnalysis0]");
	}
	
	public void run()
	{
		/*if( input_code instanceof Procedure ) {
			if( ((Procedure)input_code).getSymbolName().equals("mainLoop")) {
				System.out.println("\n[ReachAnalysis0 print]procedure specific debugging begin\n");
				debug_level = 6;
				debug_on = true;
			}
		}*/
		ReachingDef();
		
		display();
		if( debug_on ) {
			System.out.println("\n[ReachAnalysis0 print]procedure specific debugging end\n");
		}
	}

	private boolean hasChangedRM(AnalysisTools.REGIONMAP prev, AnalysisTools.REGIONMAP curr)
	{
		if ( prev == null || curr == null || !prev.equals(curr) )
			return true;
		else
			return false;
	}

	private Set<Symbol> getDefinedVariables()
	{
		Set<Symbol> tDefined_vars = DataFlowTools.getDefSymbol(input_code);
		Set<Symbol> defined_vars = AnalysisTools.getBaseSymbols(tDefined_vars);
		List<Symbol> remove_vars = new ArrayList<Symbol>();
		for(Symbol s : defined_vars) {
			String sname = s.getSymbolName();
			if( sname.startsWith("sh__") || sname.startsWith("red__") || sname.startsWith("_bid") || 
					sname.startsWith("_gtid") || sname.startsWith("_ti_100") || sname.startsWith("row_temp_") ||
					sname.startsWith("lred__") || sname.endsWith("__extended") || sname.startsWith("const__")) {
				remove_vars.add(s);
			}				
		}

		for(Symbol s : remove_vars) {
			defined_vars.remove(s);
			target_vars.remove(s);
		}

		return defined_vars;
	}

	private void ReachingDef()
	{
		PrintTools.println("[ReachingDef] strt *****************************", 3);

		Set<Symbol> defined_vars = getDefinedVariables();
		PrintTools.print("              shared variables in the input: ", 3);
		PrintTools.println("{" + PrintTools.collectionToString(target_vars, ",") + "}", 3);
		PrintTools.print("              defined variables in the input: ", 3);
		PrintTools.println("{" + PrintTools.collectionToString(defined_vars, ",") + "}", 3);
		if( debug_on ) {
			PrintTools.print("              shared variables in the input: ", 0);
			PrintTools.println("{" + PrintTools.collectionToString(target_vars, ",") + "}", 0);
			PrintTools.print("              defined variables in the input: ", 0);
			PrintTools.println("{" + PrintTools.collectionToString(defined_vars, ",") + "}", 0);
		}
		TreeMap work_list = new TreeMap();

		// Enter the entry node in the work_list
		DFANode entry = cfg.getNodeWith("stmt", "ENTRY");
		entry.putData("may_def_inRM", new AnalysisTools.REGIONMAP());
		entry.putData("must_def_inRM", new AnalysisTools.REGIONMAP());
		work_list.put(entry.getData("top-order"), entry);
		
		////////////////////////////////////////////////////////////////////////////
		// [CAUTION] This analysis assumes that the procedure of interest is      //
		// called by CPU, even though it can contain kernel function calls.       //
		////////////////////////////////////////////////////////////////////////////
		String currentRegion = new String("CPU");

		// Do iterative steps
		while ( !work_list.isEmpty() )
		{
			DFANode node = (DFANode)work_list.remove(work_list.firstKey());
			String tag = (String)node.getData("tag");
			// Check whether the node is in the kernel region or not.
			if( tag != null && tag.equals("barrier") ) {
				String type = (String)node.getData("type");
				if( type != null ) {
					if( type.equals("S2P") ) {
						currentRegion = new String("GPU");
					} else if( type.equals("P2S") ) {
						currentRegion = new String("CPU");
					}
				}
			}

			PrintTools.println("\nnode = " + node.getData("ir"), 4);

			AnalysisTools.REGIONMAP may_def_inRM = null;
			AnalysisTools.REGIONMAP must_def_inRM = null;

			//FIXME: In a while loop, "back-edge-from" annotation does not exist.
			DFANode temp = (DFANode)node.getData("back-edge-from");
			for ( DFANode pred : node.getPreds() )
			{
				AnalysisTools.REGIONMAP pred_may_def_outRM = (AnalysisTools.REGIONMAP)pred.getData("may_def_outRM");
				AnalysisTools.REGIONMAP pred_must_def_outRM = (AnalysisTools.REGIONMAP)pred.getData("must_def_outRM");

				if ( may_def_inRM == null ) {
					may_def_inRM = (AnalysisTools.REGIONMAP)pred_may_def_outRM.clone();
				} else {
					if (temp != null && temp == pred)
					{
						// this data is from a back-edge, union it with the current data
						may_def_inRM = may_def_inRM.unionWith(pred_may_def_outRM, "multiple");
					}
					else
					{
						// this is an if-else branch.
						may_def_inRM = may_def_inRM.unionWith(pred_may_def_outRM, "conditional");
					}
				}

				if ( must_def_inRM == null ) {
					must_def_inRM = (AnalysisTools.REGIONMAP)pred_must_def_outRM.clone();
				} else {
					if (temp != null && temp == pred)
					{
						// this data is from a back-edge, union it with the current data
						must_def_inRM = must_def_inRM.unionWith(pred_must_def_outRM, "multiple");
					}
					else
					{
						// this is an if-else branch, thus intersect it with the current data
						must_def_inRM = must_def_inRM.intersectWith(pred_must_def_outRM, "conditional");
					}
				}
			}

			PrintTools.println("  curr must_def_inRM = " + must_def_inRM, 4);
			

			// previous may_def_in and previous must_def_in
			AnalysisTools.REGIONMAP p_may_def_inRM = (AnalysisTools.REGIONMAP)node.getData("may_def_inRM");
			AnalysisTools.REGIONMAP p_must_def_inRM = (AnalysisTools.REGIONMAP)node.getData("must_def_inRM");

			if ( hasChangedRM(p_may_def_inRM, may_def_inRM) || hasChangedRM(p_must_def_inRM, must_def_inRM))
			{
				node.putData("may_def_inRM", may_def_inRM);
				node.putData("must_def_inRM", must_def_inRM);

				// Handles data kill, union, etc.
				AnalysisTools.REGIONMAP may_def_outRM = 
					computeOutDefRM(node, may_def_inRM, defined_vars, currentRegion);
				node.putData("may_def_outRM", may_def_outRM);
				
				AnalysisTools.REGIONMAP must_def_outRM = 
					computeOutDefRM(node, must_def_inRM, defined_vars, currentRegion);
				node.putData("must_def_outRM", must_def_outRM);
				
				PrintTools.println("  curr must_def_outRM = " + must_def_outRM, 4);

				for ( DFANode succ : node.getSuccs() )
					work_list.put(succ.getData("top-order"), succ);
			}
			
		}

		PrintTools.println("[ReachingDef] done *****************************", 3);
	}

	private AnalysisTools.REGIONMAP computeOutDefRM(DFANode node, AnalysisTools.REGIONMAP in, 
			Set<Symbol> defined_vars, String region)
	{
		if( debug_on ) {
			PrintTools.println("[computeOutDefRM] node: " + node.getData("ir"), 0);
		}
		
		AnalysisTools.REGIONMAP out = null;

		if (in == null) in = new AnalysisTools.REGIONMAP();

		out = new AnalysisTools.REGIONMAP();

		Object o = CFGraph.getIR(node);

		if ( o instanceof Traversable )
		{
			Traversable tr = (Traversable)o;

			for ( Expression e : DataFlowTools.getDefSet(tr) )
			{
				Symbol def_symbol = SymbolTools.getSymbolOf(e);
				//DEBUG: Currently, if def_symbol is an AccessSymbol, only base symbol is used.
				if( def_symbol instanceof AccessSymbol ) {
					def_symbol = ((AccessSymbol)def_symbol).getIRSymbol();
				}
				if (def_symbol != null && target_vars.contains(def_symbol) )
				{
					out.put(def_symbol, region);
				}
			}

			// if functioncall, kill DEF variables being globals or actual params.
			in.removeSideAffected(tr);
		}
		if( debug_on ) {
			PrintTools.println("[computeOutDefRM] curr in = " + in, 0);
		}
		//out = in.unionWith(out);
		out = out.overwritingUnionWith(in); 
		if( debug_on ) {
			PrintTools.println("[computeOutDefRM] curr out = " + out, 0);
		}

		return out;
	}

	private boolean isBarrierNode(DFANode node)
	{
		String tag = (String)node.getData("tag");
		if (tag != null && tag.equals("barrier"))
		{
			return true;
		}
		return false;
	}

	public void display()
	{
		if (debug_level < 5) return;

		for ( int i=0; i<cfg.size(); i++)
		{
			DFANode node = cfg.getNode(i);

			if ( (isBarrierNode(node) && debug_level >= 5) || debug_level >= 8 )
			{
				PrintTools.println("\n" + node.toDot("tag,ir", 1), 5);

				AnalysisTools.REGIONMAP may_def_inRM = (AnalysisTools.REGIONMAP)node.getData("may_def_inRM");
				if (may_def_inRM != null) PrintTools.println("    may_def_inRM" + may_def_inRM, 9);

				AnalysisTools.REGIONMAP must_def_inRM = (AnalysisTools.REGIONMAP)node.getData("must_def_inRM");
				if (must_def_inRM != null) PrintTools.println("    must_def_inRM" + must_def_inRM, 9);

				AnalysisTools.REGIONMAP may_def_outRM = (AnalysisTools.REGIONMAP)node.getData("may_def_outRM");
				if (may_def_outRM != null) PrintTools.println("    may_def_outRM" + may_def_outRM, 5);

				AnalysisTools.REGIONMAP must_def_outRM = (AnalysisTools.REGIONMAP)node.getData("must_def_outRM");
				if (must_def_outRM != null) PrintTools.println("    must_def_outRM" + must_def_outRM, 5);
			}
		}

		if( debug_on ) {
			PrintTools.println("[dot-input-file generation] begin\n" , 0);	
			PrintTools.println(cfg.toDot("tag,ir,may_def_outRM,must_def_outRM", 5), 0);
			PrintTools.println("[dot-input-file generation] end\n" , 0);	
		} else {
			PrintTools.println("[dot-input-file generation] begin\n" , 5);	
			PrintTools.println(cfg.toDot("tag,ir,may_def_outRM,must_def_outRM", 5), 5);
			PrintTools.println("[dot-input-file generation] end\n" , 5);	
		}
	}

}
