package omp2gpu.analysis; 

import java.util.*;

import cetus.hir.*;
import cetus.exec.*;
import cetus.analysis.*;

/**
 * Simplified version of LiveAnalysis.
 * LiveAnalysis0 performs varialbe-name-based, live-out analysis of the program;
 * for array variables or struct variables, base variable names are used for the
 * analyis.
 * Live-out is a union of Upward-Exposed-Use set of all successors of each node
 * <p>
 * Input  : CFGraph cfg
 * Output : LIVE set and USE set for each node in cfg
 *   USE(m) is Upward-Exposed-Use set of node m
 *   DEF(m) is DEF set of node m
 * <p>
 * LIVE(exit-node) = {} : only intra-procedural analysis
 * <p>
 * FOREACH successor node m 
 * 	LIVE(n) += { Use(m) + (LIVE(m) - DEF(m)) } = { LocalUSE(m) + LIVE(m) - DEF(m) }
 * END
 * <p>
 * NOTE: the run() method returns a CFGraph, which contains all LiveAnalysis results. 
 * 
 * @author Seyong Lee <lee222@purdue.edu>
 *         ParaMount Group 
 *         School of ECE, Purdue University
 */
public class LiveAnalysis0
{
	private int debug_level;
	private boolean debug_on = false;

	private Traversable input_code;
	private CFGraph cfg;
	private Set<Symbol> target_vars;
	private boolean condDEFChecking;

	/**
	 * 
	 * @param input
	 * @param i_cfg
	 * @param tarvars
	 * @param condDEF set true only when called in MemTrAnalysis.
	 */
	public LiveAnalysis0(Traversable input, CFGraph i_cfg, Set<Symbol> tarvars, boolean condDEF)
	{
		input_code = input;
		cfg = i_cfg;
		target_vars = tarvars;
		debug_level = PrintTools.getVerbosity();
		condDEFChecking = condDEF;
	}

	public String getPassName()
	{
		return new String("[LiveAnalysis0]");
	}
	
	public void run()
	{
/*		if( input_code instanceof Procedure ) {
			if( ((Procedure)input_code).getSymbolName().equals("mainLoop")) {
				System.out.println("\n[LiveAnalysis0 print]procedure specific debugging begin\n");
				debug_level = 6;
				debug_on = true;
			}
		}*/
		computeLive();

		display();
		
		if( debug_on ) {
			System.out.println("\n[LiveAnalysis print]procedure specific debugging end\n");
		}
	}
    
	private void computeLive()
	{
		int count = 0;

		PrintTools.println("[computeLive] strt *****************************", 3);
		PrintTools.print("              target variables in the input: ", 3);
		PrintTools.println("{" + PrintTools.collectionToString(target_vars, ",") + "}", 3);
		if( debug_on ) {
			PrintTools.print("              target variables in the input: ", 0);
			PrintTools.println("{" + PrintTools.collectionToString(target_vars, ",") + "}", 0);
		}

		Set<Symbol> tDef_symbols = DataFlowTools.getDefSymbol(input_code);
		Set<Symbol> def_symbols = AnalysisTools.getBaseSymbols(tDef_symbols);
		List<Symbol> remove_vars = new ArrayList<Symbol>();
		for(Symbol s : def_symbols) {
			String sname = s.getSymbolName();
			// PrintTools.println(sname, 0);
			if( sname.startsWith("sh__") || sname.startsWith("red__") || sname.startsWith("_bid") || 
					sname.startsWith("_gtid") || sname.startsWith("_ti_100") || sname.startsWith("row_temp_") ||
					sname.startsWith("lred__") || sname.endsWith("__extended") || sname.startsWith("const__") ) {
				remove_vars.add(s);
			}				
		}
		
		for(Symbol s : remove_vars) {
			def_symbols.remove(s);
			target_vars.remove(s);
		}
		
		
		TreeMap work_list = new TreeMap();

		// Enter the exit node in the work_list
		List<DFANode> exit_nodes = cfg.getExitNodes();
		if (exit_nodes.size() > 1)
		{
			PrintTools.println("[WARNING in computeLive()] multiple exits in the program", 2);
		}

		for ( DFANode exit_node : exit_nodes )
			work_list.put((Integer)exit_node.getData("top-order"), exit_node);

		// Do iterative steps
		while ( !work_list.isEmpty() )
		{
			if ( count++ > (cfg.size()*10) ) {
				PrintTools.println(cfg.toDot("tag,ir,ueuse", 3), 0);
				PrintTools.println("cfg size = " + cfg.size(), 0);
				Tools.exit("[computeLive] infinite loop!");
			}

			DFANode node = (DFANode)work_list.remove(work_list.lastKey());

			HashSet<Symbol> curr_liveout = new HashSet<Symbol>();

			// calculate the current live_out to check if there is any change
			for ( DFANode succ : node.getSuccs() )
			{
				HashSet<Symbol> succ_ueuse = (HashSet<Symbol>)succ.getData("ueuse");
				if( succ_ueuse != null ) {
					curr_liveout.addAll(succ_ueuse);
				}
			}

			// retrieve previous live_out
			HashSet<Symbol> prev_liveout = (HashSet<Symbol>)node.getData("live_out");

			if ( prev_liveout == null || !prev_liveout.equals(curr_liveout) )
			{
				// since live_out has been changed, we update it.
				node.putData("live_out", curr_liveout);

				// compute Upward-Exposed-Use set = LocalUEUse + (Live - DEF)
				computeUseSet(node, def_symbols);

				for ( DFANode pred : node.getPreds() )
					work_list.put(pred.getData("top-order"), pred);
			}
		}

		PrintTools.println("[computeLive] done *****************************", 3);
	}

	private boolean isKernelBoundaryBarrierNode(DFANode node)
	{
		String tag = (String)node.getData("tag");
		String type = (String)node.getData("type");
		if (tag != null && tag.equals("barrier"))
		{
			if (type != null && (type.equals("S2P") || type.equals("P2S")) ) return true;
		}
		return false;
	}

	// compute Upward-Exposed-Use set 
	// USE = UEUse+(LIVE-DEF) = (LocalUSE-DEF)+(LIVE-DEF) = (LocalUSE+LIVE)-DEF
	private void computeUseSet(DFANode node, Set<Symbol> def_symbols)
	{
		PrintTools.println("[computeUseSet] strt (node: " + node.getData("ir"), 5);
		if( debug_on ) {
			PrintTools.println("[computeUseSet] node: " + node.getData("ir"), 0);
		}

		HashSet<Symbol> ueuse = null;

		// live_out should not be null
		HashSet<Symbol> live_out = (HashSet<Symbol>)node.getData("live_out");

		if ( isKernelBoundaryBarrierNode(node) )
		{
			ueuse = new HashSet<Symbol>();
			node.putData("ueuse", ueuse);
		} else {
			// calculate upward-exposed-use set USE = (LocalUSE + LIVE)

			// calculate local USE set and local DEF set
			HashSet<Symbol> local_use, local_def;
			HashSet<Symbol> dipa_use = node.getData("use");
			if ( dipa_use == null )
				local_use = new HashSet<Symbol>();
			else
				local_use = dipa_use;

			HashSet<Symbol> dipa_def = node.getData("def");
			if ( dipa_def == null )
				local_def = new HashSet<Symbol>();
			else
				local_def = dipa_def;
			
			HashSet<Symbol> diffSet = new HashSet<Symbol>();

			Object o = CFGraph.getIR(node);
			if ( o instanceof Traversable )
			{
				for (Expression e : DataFlowTools.getUseSet((Traversable)o) )
				{
					Symbol use_symbol = SymbolTools.getSymbolOf(e);
					PrintTools.println("  locally found used symbol: " + use_symbol, 6);
					if( debug_on ) {
						PrintTools.println("  locally found used symbol: " + use_symbol, 0);
					}
					//DEBUG: Currently, if use_symbol is an AccessSymbol, only base symbol is used.
					if( use_symbol instanceof AccessSymbol ) {
						use_symbol = ((AccessSymbol)use_symbol).getIRSymbol();
						if( debug_on  && (use_symbol != null) ) {
							PrintTools.println("  base symbol of the AccessSymbol: " + use_symbol, 0);
						}
					}
					if (use_symbol != null && target_vars.contains(use_symbol) )
					{
						PrintTools.println("  locally found used symbol2: " + use_symbol, 6);
						if( debug_on ) {
							PrintTools.println("  locally found used symbol2: " + use_symbol, 0);
						}
						local_use.add(use_symbol);
					}
				}

				for (Expression e : DataFlowTools.getDefSet((Traversable)o) )
				{
					Symbol def_symbol = SymbolTools.getSymbolOf(e);
					PrintTools.println("  locally found def symbol: " + def_symbol, 6);
					if( debug_on ) {
						PrintTools.println("  locally found def symbol: " + def_symbol, 0);
					}
					//DEBUG: Currently, if def_symbol is an AccessSymbol, only base symbol is used.
					if( def_symbol instanceof AccessSymbol ) {
						def_symbol = ((AccessSymbol)def_symbol).getIRSymbol();
						if( debug_on  && (def_symbol != null) ) {
							PrintTools.println("  base symbol of the AccessSymbol: " + def_symbol, 0);
						}
					}
					if (def_symbol != null && def_symbols.contains(def_symbol) )
					{
						PrintTools.println("  locally found def symbol2: " + def_symbol, 6);
						if( debug_on ) {
							PrintTools.println("  locally found def symbol2: " + def_symbol, 0);
						}
						local_def.add(def_symbol);
					}
				}
				if( condDEFChecking ) {
					//////////////////////////////////////////////////////////////////////////////////////
					//If the current node is an entry node of IfStatement, then compare symbols defined //
					//in Then-Statement and in Else-Statement. Symbols defined only in one statement are//
					//added to ueuse set for conservative analysis.                                     //
					//////////////////////////////////////////////////////////////////////////////////////
					Object stmtO = node.getData("stmt");
					if( (stmtO != null) && (stmtO instanceof IfStatement) ) {
						HashSet<Symbol> def_in_then = new HashSet<Symbol>();
						HashSet<Symbol> def_in_else = new HashSet<Symbol>();
						HashSet<Symbol> unionSet = new HashSet<Symbol>();
						Statement thenStmt = ((IfStatement)stmtO).getThenStatement();
						Statement elseStmt = ((IfStatement)stmtO).getElseStatement();
						if( thenStmt != null ) {
							for (Expression e : DataFlowTools.getDefSet((Traversable)thenStmt) )
							{
								Symbol def_symbol = SymbolTools.getSymbolOf(e);
								if( debug_on ) {
									PrintTools.println("  def symbol in then statement: " + def_symbol, 0);
								}
								//DEBUG: Currently, if def_symbol is an AccessSymbol, only base symbol is used.
								if( def_symbol instanceof AccessSymbol ) {
									def_symbol = ((AccessSymbol)def_symbol).getIRSymbol();
									if( debug_on  && (def_symbol != null) ) {
										PrintTools.println("  base symbol of the AccessSymbol: " + def_symbol, 0);
									}
								}
								if (def_symbol != null && def_symbols.contains(def_symbol) )
								{
									if( debug_on ) {
										PrintTools.println("  locally found def symbol2: " + def_symbol, 0);
									}
									def_in_then.add(def_symbol);
								}
							}
						}
						if( elseStmt != null ) {
							for (Expression e : DataFlowTools.getDefSet((Traversable)elseStmt) )
							{
								Symbol def_symbol = SymbolTools.getSymbolOf(e);
								if( debug_on ) {
									PrintTools.println("  def symbol in then statement: " + def_symbol, 0);
								}
								//DEBUG: Currently, if def_symbol is an AccessSymbol, only base symbol is used.
								if( def_symbol instanceof AccessSymbol ) {
									def_symbol = ((AccessSymbol)def_symbol).getIRSymbol();
									if( debug_on  && (def_symbol != null) ) {
										PrintTools.println("  base symbol of the AccessSymbol: " + def_symbol, 0);
									}
								}
								if (def_symbol != null && def_symbols.contains(def_symbol) )
								{
									if( debug_on ) {
										PrintTools.println("  locally found def symbol2: " + def_symbol, 0);
									}
									def_in_else.add(def_symbol);
								}
							}
						}
						unionSet.addAll(def_in_then);
						unionSet.addAll(def_in_else);
						for(Symbol tSym : unionSet) {
							if( !def_in_then.contains(tSym) || !def_in_else.contains(tSym) ) {
								diffSet.add(tSym);
							}
						}
						if( debug_on ) {
							PrintTools.println("  diff set" + diffSet, 0);
						}
					}
				}
			}

			PrintTools.println("  local_use" + AnalysisTools.symbolsToString(local_use, ", "), 8);
			PrintTools.println("  local_def" + AnalysisTools.symbolsToString(local_def, ", "), 8);

			// upward-exposed USE = USE - DEF
			PrintTools.println("  live_out" + live_out, 6);
			if( debug_on ) {
				PrintTools.println("  live_out" + live_out, 0);
			}

			// debugged on May 11
			if (live_out.isEmpty())
			{
				ueuse = local_use;
			}
			else
			{	
				ueuse = new HashSet<Symbol>();
				ueuse.addAll(live_out);
				ueuse.removeAll(local_def);
				ueuse.addAll(local_use);
			}
			ueuse.addAll(diffSet);

			PrintTools.println("  ueuse " + AnalysisTools.symbolsToString(ueuse, ", "), 6);
			if( debug_on ) {
				PrintTools.println("  ueuse " + AnalysisTools.symbolsToString(ueuse, ", "), 0);
			}

			// insert upward-exposed USE set 
			node.putData("ueuse", ueuse);
		}

		PrintTools.println("  final ueuse: " + AnalysisTools.symbolsToString(ueuse, ", "), 5);
		if( debug_on ) {
			PrintTools.println("  final ueuse: " + AnalysisTools.symbolsToString(ueuse, ", "), 0);
		}

		PrintTools.println("[computeUseSet] done", 5);
	}

	private boolean isBarrierNode(DFANode node)
	{
		String tag = (String)node.getData("tag");
		if ( tag != null && tag.equals("barrier") )
			return true;
		else
			return false;
	}

	public void display()
	{
		if (debug_level < 5) return;

		for ( int i=0; i<cfg.size(); i++)
		{
			DFANode node = cfg.getNode(i);

			if ( isBarrierNode(node) )
			{
				PrintTools.println("\n" + node.toDot("tag,ir", 1), 2);

				HashSet<Symbol> live_out = (HashSet<Symbol>)node.getData("live_out");
				if (live_out != null) PrintTools.println("    live_out"+live_out, 5);

				HashSet<Symbol> ueuse = (HashSet<Symbol>)node.getData("ueuse");
				if (ueuse != null) PrintTools.println("    ueuse"+ueuse, 5);
			}
		}

		if( debug_on ) {
			PrintTools.println("[dot-input-file generation] begin\n" , 0);	
			PrintTools.println(cfg.toDot("tag,ir,ueuse", 3), 0);
			PrintTools.println("[dot-input-file generation] end\n" , 0);	
		} else {
			PrintTools.println("[dot-input-file generation] begin\n" , 2);	
			PrintTools.println(cfg.toDot("tag,ir,ueuse", 3), 2);
			PrintTools.println("[dot-input-file generation] end\n" , 2);	
		}
	}

}
