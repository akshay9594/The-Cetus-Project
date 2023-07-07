
package cetus.analysis;
import cetus.hir.*;
import cetus.hir.PragmaAnnotation.Range;

import java.util.*;


public class SubscriptedSubscriptAnalysis extends AnalysisPass{

    private static final String tag = "[SubscriptedSubscriptAnalysis]";

    private static RangeExpression increment_expression = null;

    private static Expression Class3_PNN_term = null;

    private static Set<Symbol> Killed_symbols = new HashSet<>();

    private static Map<String, RangeDomain> Loop_agg_ranges = new HashMap<>();

    private static Map<Symbol, String> variable_property = new HashMap<>();

    private static Map<Symbol,Object> Loop_agg_subscripts = new HashMap<>();


    public SubscriptedSubscriptAnalysis(Program program){
        super(program);
    }

    @Override
    public String getPassName() {
        return tag;
    }

    public void start() {

        LoopTools.addLoopName(program);

        ProcInfo proc_info = new ProcInfo();

        DFIterator<Procedure> iter =
                new DFIterator<Procedure>(program, Procedure.class);
        iter.pruneOn(Procedure.class);
        iter.pruneOn(Declaration.class);

        while (iter.hasNext()) {
            Procedure procedure = iter.next();

            CFGraph ProcedureGraph = new CFGraph(procedure);

            variable_property = new HashMap<>();
            Loop_agg_subscripts = new HashMap<>();
            Loop_agg_ranges = new HashMap<>();

            wrapper(ProcedureGraph);


            // //Since the pass is not Interprocedural, delete entries which are not
            // //global

            proc_info.setProcedureProps(procedure, variable_property);
            proc_info.setProcedureSubRanges(procedure, Loop_agg_subscripts);
            proc_info.setProcedureAggRangeVals(procedure, Loop_agg_ranges);

           // System.out.println( "proc: " + procedure.getName()+ ", proc props: " + proc_info.getProcedureAggRangeVals(procedure) +"\n");
            
        }
        
    
    }


    /**
     * Following routine is the Driver algorithm for Subscripted Subscript Analysis
     * 1. It takes as input the control flow graph of the procedure and traverses it.
     * 2. As soon as a Loop statement is encountered, Phase 1 is triggered
     * 3. The modified ranges are stored in the range dictionaries corresponding to each 
     *    statement or node in the control flow graph.   
     * @param - Control Flow Graph of the subroutine
    */

private void wrapper(CFGraph SubroutineGraph){


    normalizeCFG(SubroutineGraph);
    SubroutineGraph.topologicalSort(SubroutineGraph.getNodeWith("stmt", "ENTRY"));
    Map<String, RangeDomain> RangeValuesBeforeCurrentLoop = new HashMap<String, RangeDomain>();
    Integer visits = 0;
    
    ForLoop outermost_for_loop = null;
  
    TreeMap<Integer, DFANode> work_list = new TreeMap<Integer, DFANode> ();

    DFANode entry = SubroutineGraph.getNodeWith("stmt", "ENTRY");
            if (entry.getData("ranges") == null)
                entry.putData("ranges", new RangeDomain());
            work_list.put((Integer) entry.getData("top-order"), entry);

    //Traverse the CFG successor by successor
    
    while(work_list.size() > 0){
        
         // Get the first node in topological order from the work list.
        Integer node_num = work_list.firstKey();
        DFANode node = work_list.remove(node_num);


         visits = node.getData("num-visits");     
        if (visits == null) {
            node.putData("num-visits", 1);
           
        } else {
            node.putData("num-visits", visits + 1);
        }

        //  if(node.getData("ir") != null)
        // System.out.println("node: " + node.getData("ir")  +"\n");
        // else
        // System.out.println("node: " + node.getData("tag")  +"\n");


        if((Integer)node.getData("num-visits") > 2){

            node.putData("ranges", null);

            for(DFANode predecessor : node.getPreds()){

                if(predecessor.getData("symbol-exit") != null )
                    node.putData("ranges", predecessor.getData("ranges"));

            }
         
            for(DFANode successor : node.getSuccs()){
                
                if(successor.getData("num-visits") == null){
                    successor.putPredData(node, node.getData("ranges"));
                    //if(!(node.getData("stmt") instanceof IfStatement))
                    work_list.put((Integer)successor.getData("top-order"), successor);
                    
                }
                
            }
            continue;

        }

    
        // Merge incoming states from predecessors.
        RangeDomain curr_ranges = null;
        for (DFANode pred : node.getPreds()) {
            RangeDomain pred_range_out = node.getPredData(pred);
            // Skip BOT-state predecessors that has not been visited.
                    
            if (pred_range_out == null) {
                continue;
            }
            if (curr_ranges == null) {
                curr_ranges = new RangeDomain(pred_range_out);
               
            } else {
                   
                curr_ranges.unionRanges(pred_range_out);
    
            }
        
        }


        /**
         * If a For loop is encountered, store the range values of variables before the loop,
         * initialize LVVs to lambda at the loop header node and then analyze the loop body.
         */

          
           if(node.getData("stmt") instanceof ForLoop){

                outermost_for_loop = (ForLoop)node.getData("stmt");

                //Store the range dictionary of the node immediately before the loop header
                RangeValuesBeforeCurrentLoop.put(LoopTools.getLoopName(outermost_for_loop), new RangeDomain(curr_ranges));
                
        
                if(!IRTools.containsClass(outermost_for_loop.getBody(), WhileLoop.class)){

                    LinkedList<Loop>Loops_in_Nest =  new LinkedList();
                    //Initializing a LinkedList of Loop Control flowgraphs
                    //for loops in the loop nest.
                    LinkedList<CFGraph> Loop_CFG_List = new LinkedList();

                    Loops_in_Nest = LoopTools.calculateInnerLoopNest(outermost_for_loop);
                    
                    boolean discontInner = ContainsDiscontiguousInnerLoops(outermost_for_loop.clone());
                
                    CFGraph OuterLoopCFG = null;
                    

                    for(int i =0; i < Loops_in_Nest.size() ; i++){
                        ForLoop l = (ForLoop)Loops_in_Nest.get(i);
                        CFGraph l_CFG = new CFGraph(l);
                        if(l.equals(outermost_for_loop))
                                OuterLoopCFG = l_CFG;

                        Loop_CFG_List.add(l_CFG);
                        
                    }

                    Expression loop_lb =LoopTools.getLowerBoundExpression(outermost_for_loop);
                    Expression loop_ub = LoopTools.getUpperBoundExpression(outermost_for_loop);
                    Boolean nullBounds = false;
                    if(loop_lb == null || loop_ub == null)
                        nullBounds = true;

            
                    if(!IdentifySubSubLoop(Loops_in_Nest) &&
                        !ContainsUnsafeFunctionCalls(outermost_for_loop) &&
                        !LoopTools.containsBreakStatement(outermost_for_loop) && 
                        !discontInner && !nullBounds
                      )
                    {
                    //Perform the analysis for loops that do not contain subscripted susbcript pattern and
                    //loops that do not contain any function calls

                        normalizeCFG(OuterLoopCFG);                           //Important step after creating a CFGraph
        
                        if(Loops_in_Nest.size() > 1){

                            CFGraph NextLoopCFG = null;

                        
                            for(int i = Loops_in_Nest.size()-1; i >=0; i--){
                    
                                ForLoop innerloop = (ForLoop)Loops_in_Nest.get(i);
                                CFGraph CurrentLoopCFG = Loop_CFG_List.get(i);
                                normalizeCFG(CurrentLoopCFG);
                                
                                //The inner loop nodes are to be collapsed in the CFG of
                                // the next outer loop in the loop nest
                                if(!innerloop.equals(outermost_for_loop)){
                                    NextLoopCFG = Loop_CFG_List.get(i-1);
                                    normalizeCFG(NextLoopCFG);
                                }
                        
                                int loopnestdepth = LoopTools.calculateInnerLoopNest(innerloop).size();
                                //Perform the Subscripted Subscript Analysis
                                SubSubAnalysis(innerloop, CurrentLoopCFG ,
                                RangeValuesBeforeCurrentLoop.get(LoopTools.getLoopName(outermost_for_loop)),
                                loopnestdepth);
                            
                                //Collapse the loop nodes in the appropriate CFGs
                                if(innerloop.equals(outermost_for_loop)){
                                    node = CollapseLoopinCFG(SubroutineGraph , innerloop, node);
                                }
                                else{
                                    CollapseLoopinCFG(NextLoopCFG, innerloop, null);
                                }
                            }

                        }
                        else if(Loops_in_Nest.size() == 1){

                                SubSubAnalysis(outermost_for_loop, OuterLoopCFG ,
                                        RangeValuesBeforeCurrentLoop.get(LoopTools.getLoopName(outermost_for_loop)),
                                        1);
                                node = CollapseLoopinCFG(SubroutineGraph , outermost_for_loop, node);
                        }

                    }
                    else{
                        //System.out.println("collapsing subsub loop\n" + outermost_for_loop + "\n");
                        String Outerloop_ant = (outermost_for_loop.getAnnotation(PragmaAnnotation.class, "name")).toString();
                        Loop_agg_ranges.put(Outerloop_ant, curr_ranges);
                        node = CollapseLoopinCFG(SubroutineGraph, outermost_for_loop, node);
        
                    }
                    
            
                }
            }

    
        // Add initial values from declarations
        RangeAnalysis.enterScope(node, curr_ranges);
       
        RangeDomain prev_ranges = node.getData("ranges");

        // Detected changes trigger propagation
        if (prev_ranges == null || !prev_ranges.equals(curr_ranges)) {
    
            if (node != SubroutineGraph.getEntry()) {
                // Keep the IPA result for the entry node.
                node.putData("ranges", curr_ranges);
            }
   
            if(prev_ranges != null && !prev_ranges.equals(curr_ranges)){
               
                node.putData("ranges", prev_ranges);
 
             }
            RangeAnalysis.updateRanges(node);

            RangeAnalysis.exitScope(node);

        }

        for (DFANode succ : node.getSuccs()) {
            // Do not add successors for infeasible paths
            // System.out.println("succ: " + succ.getData("ir") +"\n");
            if (succ.getPredData(node) != null) {
               
                work_list.put((Integer)succ.getData("top-order"), succ);
            
            }
        }  

        if(node.getData("tag") != null && node.getData("tag").equals("FOREXIT")){
       
             node.putData("ranges", curr_ranges);
            //Update successors or ranges associated with the outgoing control flow edges
            for(DFANode successor : node.getSuccs()){

                successor.putPredData(node, node.getData("ranges"));

            }
           
        
        }

        // if(node.getData("ir") != null)
        // System.out.println("node: " + node.getData("ir") +" , range: " + node.getData("ranges") +"\n");
        // else
        // System.out.println("node: " + node.getData("tag") +" , range: " + node.getData("ranges") +"\n");
       

    }

    
}



    /**
     * Collapses a loop in the CFG. Collapsing refers to removing the nodes corresponding to the loop from the
     * input CFG. Outermost loops in a loop nest are collapsed w.r.t the subroutine flow graph. After collapsing, 
     * the loop nodes are replaced with a node with tag = The pragma loop name (unique annotation) and ranges equal 
     * to the aggregated loop ranges after subscripted subscript analysis.
     * @param input_CFG        - The input control flow graph
     * @param loop_to_collapse - The loop object that is to be collapsed
     * @param OutermostLoopNode - Node corresponding to the outermost loop in a nest
     * @return   - The collapsed node
     */
    private static DFANode CollapseLoopinCFG(CFGraph input_CFG, ForLoop loop_to_collapse, DFANode OutermostLoopNode){

        TreeMap<Integer, DFANode> nodes_list = new TreeMap<Integer, DFANode>();
    
        input_CFG.topologicalSort(input_CFG.getNodeWith("stmt", "ENTRY"));

        DFANode entry = input_CFG.getNodeWith("stmt", "ENTRY");
    
        if(OutermostLoopNode != null){
            nodes_list.put((Integer)OutermostLoopNode.getData("top-order"), OutermostLoopNode);
        }
        else
            nodes_list.put((Integer) entry.getData("top-order"), entry);
            
        //System.out.println("Loop to collap: " + loop_to_collapse +"\n");
        List<DFANode> Nodes_to_Remove = new ArrayList<>();
        Set<DFANode> innerLoopPreds = null;
        Set<DFANode> innerLoopSuccs = null;
        Annotation innerloopannotation = null;

        Integer LoopNestDepth = LoopTools.calculateInnerLoopNest(loop_to_collapse).size();
        Integer NumberForExits = 0;
        
        //Traverse the CFG successor by successor and remove the nodes of the inner loop

        //System.out.println("collap loop: \n" + loop_to_collapse +"\n");
        
        while(nodes_list.size() > 0){
    

            // Get the first node in topological order from the work list.
            Integer node_num = nodes_list.firstKey();
            DFANode node = nodes_list.remove(node_num);

            // if(node.getData("ir") != null)
            // System.out.println(" node: " + node.getData("ir")  +"\n");
            // else
            // System.out.println(" node: " + node.getData("tag") +"\n");

            Integer visits = node.getData("num-visits"); 
            
            if (visits == null) {
                node.putData("num-visits", 1);
                
            } else {
                node.putData("num-visits", visits + 1);
            }

            if(node.getData("stmt") instanceof ForLoop){

                ForLoop innerloop = (ForLoop)node.getData("stmt");
                //System.out.println("inner loop: " + innerloop + " ,collap loop: " + loop_to_collapse +"\n");
                if(innerloop.equals(loop_to_collapse)){
                    innerloopannotation = innerloop.getAnnotation(PragmaAnnotation.class, "name");
                    innerLoopPreds = node.getPreds();
                    Nodes_to_Remove.add(node);
                }
                
                
            }

            if(!Nodes_to_Remove.isEmpty() && !Nodes_to_Remove.contains(node)){
                Nodes_to_Remove.add(node);

            }


            if(node.getData("tag")!= null && node.getData("tag").equals("FOREXIT")){
                NumberForExits++;
                Nodes_to_Remove.add(node);

                if(NumberForExits == LoopNestDepth){
                    innerLoopSuccs = node.getSuccs();
                    break;
                }
            }
        

            if((Integer)node.getData("num-visits") > 1){            
                
                for(DFANode successor : node.getSuccs()){
                    
                    if(successor.getData("num-visits") == null){
                        successor.putPredData(node, node.getData("ranges"));
                        nodes_list.put((Integer)successor.getData("top-order"), successor);
                        
                    }
                    
                }
                continue;

            }
        
        
                for (DFANode succ : node.getSuccs()) {
                    

                    nodes_list.put((Integer)succ.getData("top-order"), succ);
                        
                }
                
            }

            input_CFG.removeNodes(Nodes_to_Remove);

            DFANode collap_node = new DFANode("tag", innerloopannotation);
            RangeDomain innerloop_aggRanges = Loop_agg_ranges.get(innerloopannotation.toString());
            collap_node.putData("ranges", innerloop_aggRanges);
            input_CFG.addNode(collap_node);
        
            for(DFANode pred: innerLoopPreds){
                input_CFG.addEdge(pred, collap_node);
                pred.putSuccData(collap_node, new RangeDomain());
                pred.putData("true", collap_node);
        
            }
        
            for(DFANode succ: innerLoopSuccs){
                input_CFG.addEdge(collap_node, succ);
                collap_node.putData("true", succ);
                //System.out.println("collap node: " + collap_node.getData("tag") +" , succ: " + succ.getData("ir") +"\n");
                succ.putPredData(collap_node, collap_node.getData("ranges"));
                //System.out.println("ranges: " + succ.getPredData(collap_node) +"\n");
            }
            
            if(OutermostLoopNode == null){
                for(int j = 0; j < input_CFG.size(); j++){
                        
                    input_CFG.getNode(j).putData("num-visits", null);
                }
            }

            //System.out.println("Collap node: " + collap_node.getData("tag") + ",ranges: " + collap_node.getData("ranges") +"\n");
            //Kill the Loop Index symbol of the collapsed loop
            Killed_symbols.add(LoopTools.getLoopIndexSymbol(loop_to_collapse));
            return collap_node; 

    }

/**
 * Subscripted subscript analysis begins.
 * @param input_for_loop - The For loop to be analyzed
 * @param RangesBeforeLoop - Range values immediately before the loop
 * @return - The aggregated range expressions
 */


private static void SubSubAnalysis(ForLoop input_for_loop, CFGraph Loop_CFG, 
                                RangeDomain Prior_Ranges,int loopnestdepth){

    Integer visits = 0;

    Map<Symbol, Expression> ArraySubscriptMap = new HashMap<Symbol,Expression>();
    Set<Symbol> LoopLVVs = null;
    Set<Symbol> Initial_syms = null;
    Map<Symbol,Expression> DefSymbolExprs = new HashMap<>();
   
    TreeMap<Integer, DFANode> work_list = new TreeMap<Integer, DFANode>();
    
        Loop_CFG.topologicalSort(Loop_CFG.getNodeWith("stmt", "ENTRY"));

        DFANode entry = Loop_CFG.getNodeWith("stmt", "ENTRY");
        if (entry.getData("ranges") == null)
            entry.putData("ranges", Prior_Ranges);
        
        work_list.put((Integer) entry.getData("top-order"), entry);

        //Traverse the CFG successor by successor

        //System.out.println("Loop CFG: " + Loop_CFG +"\n");
        
    while(work_list.size() > 0){
        

            // Get the first node in topological order from the work list.
        Integer node_num = work_list.firstKey();
        DFANode node = work_list.remove(node_num);

        visits = node.getData("num-visits");     
        if (visits == null) {
            node.putData("num-visits", 1);
            
        } else {
            node.putData("num-visits", visits + 1);
        }


        if((Integer)node.getData("num-visits") > 1){

            node.putData("ranges", null);

            for(DFANode predecessor : node.getPreds()){

                if(predecessor.getData("symbol-exit") != null )
                    node.putData("ranges", predecessor.getData("ranges"));

            }

            
            for(DFANode successor : node.getSuccs()){
                if(successor.getData("num-visits") == null){
                    successor.putPredData(node, node.getData("ranges"));
                    work_list.put((Integer)successor.getData("top-order"), successor);
                    
                }
                
            }
            continue;

        }
    

        // if(node.getData("ir") != null)
        //  System.out.println(" node: " + node.getData("ir"));
        //  else
        //  System.out.println(" node tag: " + node.getData("tag"));

        // Merge incoming states from predecessors.

        RangeDomain curr_ranges = null;

        for (DFANode pred : node.getPreds()) {
            RangeDomain pred_range_out = node.getPredData(pred);
            // Skip BOT-state predecessors that has not been visited.
            //System.out.println("pred: " + pred.getData("ir") +",pred range: " + pred_range_out +"\n");
            if (pred_range_out == null) {
                continue;
            }

            if (curr_ranges == null) {
                //System.out.println("pred range: " + pred_range_out +"\n");
                curr_ranges = new RangeDomain(pred_range_out);
            }                
            else {
                 
                TagExpressionsWithIfCondition(pred_range_out,getIfStatement(node.getPreds()));
                curr_ranges.unionRanges(pred_range_out);
    
            }


        }

    
        if(node.getData("ir") != null && 
                        node.getData("ir").toString().contains(input_for_loop.getCondition().toString()) ){

            LoopLVVs = node.getData("loop-variants"); 
         
            Set<Expression> DefExprs = DataFlowTools.getDefMap(input_for_loop.getBody()).keySet();
                  
               for(Expression expr: DefExprs){
                    //if an LVV is present in the range dictionary of the loop header node, initialize it to lambda     

                    Symbol LVV = SymbolTools.getSymbolOf(expr);

                    if(LVV.equals(LoopTools.getLoopIndexSymbol(input_for_loop)))
                        continue;
                    
                    //Preprocessing for the index expression of an array.
                    //If the array index expression is an unary expression,
                    //it might be an intermittent sequence.
                    if(expr instanceof ArrayAccess){
                        
                        ArrayAccess access_expr = (ArrayAccess)expr;

                        if(access_expr.getIndices().size() == 1){
                            Expression array_index = access_expr.getIndex(0);
                            if(array_index instanceof UnaryExpression &&
                            !IRTools.containsClass(array_index, ArrayAccess.class) ){
                                
                                expr = new StringLiteral("lambda");
                                array_index = ((UnaryExpression)array_index).getExpression();
                                // List<Symbol> Symbol_List = Arrays.asList(LVV,SymbolTools.getSymbolOf(array_index)); 
                                // TagSymbolWithIfCond(Symbol_List,input_for_loop, access_expr);
                                
                            }
                            ArraySubscriptMap.put( LVV , array_index); 
                            curr_ranges.setRange(LVV, expr);
                            DefSymbolExprs.put(LVV, access_expr);
    
                        }
                        else{
                            expr = new StringLiteral("lambda");
                            curr_ranges.setRange(LVV, expr);
                        }
            
                    }
                    
                    else{

                    // if(curr_ranges.getRange(LVV) == null){
                        VariableDeclarator vd = (VariableDeclarator)LVV;
                        IDExpression LVVexpr = vd.getID();
                        curr_ranges.setRange(LVV, LVVexpr);
                    }
                        
                    //}
                }

            Initial_syms = curr_ranges.getSymbols();
        }
        

        // Add initial values from declarations
        RangeAnalysis.enterScope(node, curr_ranges);
        
        RangeDomain prev_ranges = node.getData("ranges");

        // Detected changes trigger propagation
        if (prev_ranges == null || !prev_ranges.equals(curr_ranges)) {
    
            if( curr_ranges != null && prev_ranges != null && !prev_ranges.equals(curr_ranges)){

               curr_ranges.kill(Killed_symbols);
    
                for(Symbol sym : curr_ranges.getSymbols()){
                    Expression exp = prev_ranges.getRange(sym);
                    
                    if(exp != null ){
                        List<ArraySpecifier> sym_specs = sym.getArraySpecifiers();
                        if(!sym_specs.isEmpty() && sym_specs.get(0).getNumDimensions()>1){
                            exp = curr_ranges.substituteForwardRange(exp);
                        }
                       exp = curr_ranges.substituteForwardRange(exp);
                        curr_ranges.setRange(sym, exp);
                    }

                }
                if(!prev_ranges.getMultiDimArrays().isEmpty()){
                    for(ArrayAccess arr: prev_ranges.getMultiDimArrays()){
                        Expression exp =prev_ranges.getRange(arr);
                        if(exp != null){
                            exp = curr_ranges.substituteForwardRange(exp);
                            curr_ranges.setRange(arr, exp);
                        }
                    }
                }
                //System.out.println("curr ranges: " + curr_ranges +"\n");
                node.putData("ranges", curr_ranges);
            
            }
            else if (node != Loop_CFG.getEntry()) {
                // Keep the IPA result for the entry node.
                node.putData("ranges", curr_ranges);
            }
        
          
            RangeAnalysis.updateRanges(node);  
                  
            
            //RangeAnalysis.exitScope(node);

            for (DFANode succ : node.getSuccs()) {
                //System.out.println("node: " + node.getData("tag") + ",suc: " + succ.getData("ir") +"\n");
                // Do not add successors for infeasible paths
               if (succ.getPredData(node) != null) {
                    RangeDomain SuccRange =  succ.getPredData(node);
                    //System.out.println( succ.getData("ir") + ",edge ranges: " + SuccRange +"\n");
                    //Remove entry for Loop index in the range dictionaries of successors
                    //Loop index is a special case and will be considered separately.
                    SuccRange.removeRange(LoopTools.getLoopIndexSymbol(input_for_loop));
                    work_list.put((Integer)succ.getData("top-order"), succ);
                
                }
            }

        }

            //Phase 2 triggered after the final node in the loop is reached
            if(node.getData("tag") != null && node.getData("tag").equals("FOREXIT")){

                node.putData("array-subscripts", ArraySubscriptMap);
              
                RangeDomain Phase1Exprs = node.getData("ranges");
                if(Phase1Exprs == null)
                    return;

                Set<Symbol> Final_symbols = Phase1Exprs.getSymbols();
               
                Killed_symbols = symmetricDifference(Initial_syms, Final_symbols);

                //System.out.println("Subscripted-subscript analysis for Loop: " + LoopName + "\n" );  
                SubSubPhasetwo(node.getData("ranges") , LoopLVVs , input_for_loop , DefSymbolExprs,
                                      Prior_Ranges , node.getData("array-subscripts"));                

                Annotation loop_ant = input_for_loop.getAnnotation(PragmaAnnotation.class, "name");
                
                //Collecting the aggregation information corresponding to each loop
                Loop_agg_ranges.put(loop_ant.toString(), node.getData("ranges"));

                //Update successors or ranges associated with the outgoing control flow edges


            }

                // if(node.getData("ir") != null)
                // System.out.println(" node: " + node.getData("ir")+ ", ranges: " + node.getData("ranges")  +"\n");
                // else
                // System.out.println(" node: " + node.getData("tag")+ ", ranges: " + node.getData("ranges")+"\n");
          
   
        }

        // System.out.println("*************\n");
   
    }
    
    /**
     * Subscripted subscript analysis - Phase 2 begins
     * 1. In Phase 2, the range values from Phase 1 are analyzed and the relevant 
          properties are determined.
     * 2. The Loop index variable is treated as a special SSR or Class 1 variable with it's 
          property being strict monotonicity since the loops are normalized.
     * 3. For now we have two Maps - Mapping of the symbol to its aggregated range and
     *    Mapping of the symbol to a relevant property.
     * @param LoopRangeExpressions - The Phase 1 range expressions
     * @param LoopVariantVars      - The list of Loop variant variables
     * @param input_for_loop              - The loop being analyzed
     * @param RangeValsBeforeLoop  - Range dictionary of the statement immediately prior to the loop
     * @param ArraySubscripts      - Mappings of arrays to their subscript expressions
     * Note: This function might need to return a range dictionary
     */
    

    private static void SubSubPhasetwo(RangeDomain LoopRangeExpressions , Set<Symbol> LoopVariantVars, ForLoop input_for_loop,
                                         Map<Symbol,Expression> DefSymbolExprs,RangeDomain RangeValsBeforeLoop, 
                                         Map<Symbol,Expression>ArraySubscripts)
    {

        Expression aggregate_lb = null;
        Expression aggregate_ub = null;
        Expression re = null;
        Symbol SSR_Var = null;
        List<Symbol> SSR_variables = new ArrayList<Symbol>();
        Expression LoopIdx = LoopTools.getIndexVariable(input_for_loop);

        //System.out.println("LVVs: " + LoopVariantVars +":"+ LoopRangeExpressions +"\n");

        RangeExpression LoopIdxRange = getLoopIndexRange(input_for_loop, RangeValsBeforeLoop);

        Expression LoopIterationCount = Symbolic.add(Symbolic.subtract(LoopIdxRange.getUB() , LoopIdxRange.getLB()), new IntegerLiteral(1));
        Expression ValueBeforeLoop = null;

        Symbol LoopIndexSymbol = SymbolTools.getSymbolOf(LoopIdx);

        if(!LoopTools.isOutermostLoop(input_for_loop)){

            Expression LoopUBVal = RangeValsBeforeLoop.getRange(LoopIndexSymbol);
            LoopRangeExpressions.setRange(LoopIndexSymbol, LoopUBVal);
           
            //RangeValsBeforeLoop = null;

        }

        //Collect information about multi-dimensional subscript arrays
        CollectMultiDimArrayInfo(LoopRangeExpressions, input_for_loop);
    
       // System.out.println("\nResults of Phase 2 Analysis for "+ LoopIndexSymbol +"-loop: " +"\n");

        //Collect info about the loop index symbol - aggregate range and property
        Expression SubstitutedRange = (RangeExpression)LoopRangeExpressions.substituteForwardRange(LoopIdxRange);

        if(!IRTools.containsClass(SubstitutedRange, InfExpression.class))
            LoopRangeExpressions.setRange(LoopIndexSymbol, SubstitutedRange);
        else
        LoopRangeExpressions.setRange(LoopIndexSymbol, LoopIdxRange);

        SSR_variables.add(LoopIndexSymbol);
        variable_property.put(LoopIndexSymbol, "STRICT_MONOTONIC");

        List<Symbol> Symbols_to_analyze = new ArrayList<>(LoopRangeExpressions.getSymbols());

        Symbols_to_analyze = determine_eval_order(Symbols_to_analyze,LoopRangeExpressions);

        Symbols_to_analyze.retainAll(LoopVariantVars);

        Map<Symbol,Expression> TaggedSymbols = new HashMap<>();
        for(Symbol s : LoopRangeExpressions.getSymbols()){
            Expression e = LoopRangeExpressions.getRange(s);
            if(IRTools.IsTagged(e)){
                TaggedSymbols.put(s, IRTools.getTaggedExpression(e));
            }
        }

            //System.out.println("Syms: " + Symbols_to_analyze+"\n");
               for(Symbol sym : Symbols_to_analyze){
            
                        //Check if the symbol is the loop index variable
                       
                        ArraySpecifier arr_specs = null;
                        
                        Expression LVV_Value_expr = LoopRangeExpressions.getRange(sym);

                        if(!sym.getArraySpecifiers().isEmpty())
                            arr_specs = (ArraySpecifier)sym.getArraySpecifiers().get(0);

                     
                        //Identifying the recurrence class of the LVV
                    

                        String recurrence_class = identify_recurrence_class(sym, LVV_Value_expr ,
                                                                            LoopIdx, LoopVariantVars , DefSymbolExprs.get(sym),
                                                                            SSR_variables,RangeValsBeforeLoop,TaggedSymbols);


                        switch(recurrence_class){
                            case "Class 1":
                                SSR_variables.add(sym);
                                variable_property.put(sym, "MONOTONIC");
            
                                //increment expression has been determined in identify_recurrence_class()
                                aggregate_lb = Symbolic.multiply(increment_expression.getLB(), LoopIterationCount);
                                aggregate_ub = Symbolic.multiply(increment_expression.getUB(), LoopIterationCount);

                                //if the loop is outermost loop, add the value before the loop for scalars
                                //System.out.println("inc expr: " + increment_expression + "," + RangeValsBeforeLoop +"\n");
                                
                                if(LoopTools.isOutermostLoop(input_for_loop) &&
                                   (RangeValsBeforeLoop!=null && RangeValsBeforeLoop.getRange(sym) != null) 
                                  ){
                                    ValueBeforeLoop = RangeValsBeforeLoop.getRange(sym);
                                    aggregate_lb = Symbolic.add(aggregate_lb, ValueBeforeLoop);
                                    aggregate_ub = Symbolic.add(aggregate_ub , ValueBeforeLoop);
                                    //re = LoopRangeExpressions.substituteForwardRange(re);
                                }
                                else{
                                    RangeExpression range = (RangeExpression)LVV_Value_expr;
                                    aggregate_lb = Symbolic.add( aggregate_lb, range.getLB());
                                    aggregate_ub = Symbolic.add(aggregate_ub , range.getLB());

                                }
                                
                                re = new RangeExpression(aggregate_lb, aggregate_ub);
                                   
                                LoopRangeExpressions.setRange(sym, re);
                                                                  
                                
                            break;
                            case "Class 2":
                                Expression SSR_expr = Find_SSR_expr(LVV_Value_expr, SSR_variables);
                                String prop_type = Determine_Monotonicity_Type(sym, input_for_loop, 
                                                                SymbolTools.getAccessedSymbols(SSR_expr));
                                //check the property of the SSR variable
        
                                variable_property.put(sym, prop_type);
                                
                                if(sym.getArraySpecifiers().isEmpty())
                                    SSR_variables.add(sym);
                                //Subsitute the range expression of all variables in RHS and simplify
                                re = (RangeExpression)LoopRangeExpressions.substituteForwardRange(LVV_Value_expr);
                                if(LoopTools.isOutermostLoop(input_for_loop)){
                                    LoopRangeExpressions.setRange(sym, re);
                                }
                            break;
                            case "Class 3":
                                //Evaluate the PNN term
                               String property = eval_PNN(Class3_PNN_term);
                               variable_property.put(sym, property);
                               //Conservative estimation of the aggregate value expressions
                               LoopRangeExpressions.setRange(sym, LoopIdxRange.getUB());
                            break;
                            case "Unknown Class":
                             
                            //If none of the classes are recognized and the value expression is not constant, the aggregated value is unknown
                               
                              if(SymbolTools.isArray(sym) && IRTools.containsClass(LVV_Value_expr, ArrayAccess.class)){
                                LoopRangeExpressions.setRange(sym, new StringLiteral("bot"));
                                break;
                              }
                                 //Aggregate list of Value Expressions for a Multi-dimensional array.
                               if(SymbolTools.isArray(sym) && arr_specs.getNumDimensions() > 2){
                                
                                    if(SymbolTools.HasMultipleAccesses(sym))
                                    {
                                        Expression simplified_expr = Unionize_MultiDimArrayExprs(sym, SymbolTools.getAssignedValues(sym),
                                                                    LoopRangeExpressions, SSR_variables,LoopIndexSymbol);
                                                
                                        if(simplified_expr != null){
                                            LVV_Value_expr = simplified_expr;
                                            LVV_Value_expr = LoopRangeExpressions.substituteForwardRange(LVV_Value_expr);
                                            LoopRangeExpressions.multiDimRDclear();
                                            SymbolTools.clearAccesses();
                                        }
                                        LoopRangeExpressions.setRange(sym, LVV_Value_expr);
                                        break;
                                    }
                                    if(LVV_Value_expr instanceof StringLiteral ||
                                        !is_constant(LVV_Value_expr))
                                        LoopRangeExpressions.setRange(sym, new StringLiteral("bot"));
                                    
                               }
                               else if(!is_constant(LVV_Value_expr) && 
                                            RangeValsBeforeLoop!=null &&
                                            RangeValsBeforeLoop.getSymbols().contains(sym)){
                                    
                                    Expression SubValue = LoopRangeExpressions.expandSymbol(LVV_Value_expr, LoopIndexSymbol);

                                     ValueBeforeLoop = RangeValsBeforeLoop.getRange(sym);
                                     if(ValueBeforeLoop != null && 
                                            SubValue instanceof RangeExpression){
                                            
                                                SubValue = UnionValues(SubValue, ValueBeforeLoop);
                                            
                                     }
                                     LoopRangeExpressions.setRange(sym, SubValue);
                                    
                               }
                            //    else
                            //         LoopRangeExpressions.setRange(sym, new StringLiteral("bot"));
                            break;
                            
                               
                        }

                        //For an array, aggregate the subscript expression if it's a simple subscript
                        if(arr_specs!=null || SymbolTools.isPointer(sym)){
                           
                            Expression array_subscript = ArraySubscripts.get(sym);
                            
                            RangeExpression agg_subscript = null;

                            if(SymbolTools.isPointer(sym) ||
                                SymbolTools.isArray(sym) && arr_specs.getNumDimensions()<=1){

                                if(is_simple_subscript(array_subscript,LoopIdx)){
                                  
                                     array_subscript = LoopRangeExpressions.substituteForwardRange(array_subscript);
                                     Loop_agg_subscripts.put(sym, array_subscript);        
                               
                                }
                                //Aggregation of subscript expression of an intermittent sequence
                                else if(IRTools.IsTagged(LVV_Value_expr)){
                                    agg_subscript = new RangeExpression(new IntegerLiteral(0), array_subscript.clone());
                                    Loop_agg_subscripts.put(sym, agg_subscript);

                                }
                                else if(!Loop_agg_subscripts.keySet().contains(sym)){
                                        Loop_agg_subscripts.put(sym, new StringLiteral("bot"));
                                }
                            }
                            else{

                                //Aggregating Multi-Dimensional Array Subscript Expressions
                                List<Expression> agg_sub = (List)Loop_agg_subscripts.get(sym);
            
                                if(agg_sub != null && agg_sub.contains(LoopIdx)){
                                    int idx = agg_sub.indexOf(LoopIdx);
                                    agg_sub.set(idx, LoopIdxRange);
    
                                }
                                
                            }
                        
                        // if(SymbolTools.getAssignedValues(sym)!=null)
                        //    System.out.println("LVV: " + sym + "\nValues:" + SymbolTools.getAssignedValues(sym) + "\nproperty: " +
                        //                  variable_property.get(sym) + "\nsub: " + Loop_agg_subscripts.get(sym) +"\n");

                        // else
                        // System.out.println("LVV: " + sym + "\nValues:" + LoopRangeExpressions.getRange(sym) + "\nproperty: " +
                        //                     variable_property.get(sym) + "\nsub: " + Loop_agg_subscripts.get(sym) +"\n");

                        }

               }


               LoopRangeExpressions.removeRange(LoopIndexSymbol);
          
        }

       

        /**
         * Determining the Recurrence class of the LVV.
         * 1. There are 3 recurrence classes that need to be identified:
         *    (a) Simple Scalar Recurrence (Class 1)
         *    (b) Scalar Recurrence Array Assignment (Class 2)
         *    (c) Array Recurrence (Class 3)
         *    (From the paper - "From the ICS paper - On the automatic parallelization of subscripted
         *                         subscript patterns using array property analysis")
         * 2. Multi-dimensional arrays and arrays with loop invariant subscripts are also supported
         * @param LVV       - The input LVV
         * @param ValueExpr - Phase 1 range expression of the LVV
         * @param LoopIndex - The loop index variable
         * @param ArraySubscriptExprs - Mapping of arrays to their respective subscript expressions
         * @param List_SSR_Vars - List of SSR variables
         * @return          - The recurrence class
         * 
         */
    
    private static String identify_recurrence_class(Symbol LVV, Expression ValueExpr, Expression LoopIndex, 
                                                    Set<Symbol> LoopVariantVars,
                                                    Expression DefExpr,
                                                    List<Symbol>List_SSR_Vars, RangeDomain RangesBeforeLoop,
                                                    Map<Symbol,Expression>TaggedSymbols)
    {

      //System.out.println("LVV: " + LVV + " ,value: " + ValueExpr + " ,arr def expr: " + DefExpr +"\n");
       //Check if the LVV is a scalar or an array. 
       //Scalars can have only Class 1 recurrence

       if(ValueExpr instanceof StringLiteral)
                return "Unknown Class";

        if(!SymbolTools.isPointer(LVV) &&
            !SymbolTools.isArray(LVV)||
           (SymbolTools.isArray(LVV) && 
            IRTools.containsClass(ValueExpr, ArrayAccess.class) &&
             !IRTools.containsExpression(ValueExpr, LoopIndex)))
            {

            Expression idexpr = null;
            if(LVV.getArraySpecifiers().isEmpty()){
                VariableDeclarator vd = (VariableDeclarator)LVV;
                idexpr = (Expression)vd.getID();
            }
            else{
                idexpr = DefExpr;
            }
        
            if(ValueExpr instanceof RangeExpression){
                RangeExpression re = (RangeExpression)ValueExpr;
                Expression inc_expr_lb = Symbolic.subtract(re.getLB(), idexpr);
                Expression inc_expr_ub = Symbolic.subtract(re.getUB() , idexpr);
                increment_expression = new RangeExpression(inc_expr_lb, inc_expr_ub);
                //Check if the increment expression is Positive or Non-negative
        
                if(is_PNN(increment_expression))
                        return "Class 1";
                
                return "Unknown Class";
            }
            else{
                //Monotonic Range Assignment to a scalar variable
                Expression SSR_expr = Find_SSR_expr(ValueExpr, List_SSR_Vars);
                if(SSR_expr!=null && SSR_expr.equals(LoopIndex)){
                    Expression coeff = Symbolic.getCoefficient(ValueExpr,(Identifier)SSR_expr);
                    if(coeff!=null)
                       SSR_expr = Symbolic.multiply(SSR_expr, coeff);

                    return determine_Class2_recurr(ValueExpr, SSR_expr);
                    
                }
                
                return "Unknown Class";
            }
            
        }
        else{

            Expression SSR_expr = Find_SSR_expr(ValueExpr,  List_SSR_Vars);
            
            int NumArrayIndices = 0;
            Expression SubscriptExp = null;
            ArraySpecifier arr_specs = null;

            if(SymbolTools.isPointer(LVV)){
                ArrayAccess Pointer_Array = (ArrayAccess)DefExpr;
                NumArrayIndices = Pointer_Array.getNumIndices();
                if(NumArrayIndices == 1){
                    SubscriptExp = Pointer_Array.getIndex(0);
                }
                
            }
            else{

                arr_specs = (ArraySpecifier)LVV.getArraySpecifiers().get(0);
          
                if(arr_specs.getNumDimensions() == 1){
                    List<Expression> Array_indices = ((ArrayAccess)DefExpr).getIndices();
                    SubscriptExp = Array_indices.remove(0);
                }
                else if(Loop_agg_subscripts.get(LVV) != null){

                    List<Expression> Agg_Subs = (ArrayList)Loop_agg_subscripts.get(LVV);
                    List<Expression> Subscript_Exprs = new ArrayList<>();
                    for(Expression exp: Agg_Subs){
                        if(IRTools.containsExpression(LoopIndex, exp))
                            Subscript_Exprs.add(exp);
                    }

                    if(!(Subscript_Exprs.size() == 1))
                        return "Unknown Class"; 
                    SubscriptExp = Subscript_Exprs.iterator().next();
                }

            }
         
            //If SSR expression is not null, Class 2 recurrence possible, else Class 3
            if(SSR_expr != null){

                //An array can have class 2 recurrence if RHS is an SSR expression
                //Monotonic Range Assignment to an array variable
           
                if(is_simple_subscript(SubscriptExp, LoopIndex)){

                    return determine_Class2_recurr(ValueExpr, SSR_expr);
                }
               
                else{
                  
                
                    //Check if the array is an intermittant sequence and then evaluate for a property
                    if(SubscriptExp instanceof UnaryExpression){
                        UnaryExpression subexpr = (UnaryExpression)SubscriptExp;
                        Expression NormalizedSubscript = subexpr.getExpression();
                        Expression TaggedSubExpr = TaggedSymbols.get(SymbolTools.getSymbolOf(NormalizedSubscript));

                        Expression array_if_tag = IRTools.get_IfConditionTag(ValueExpr);
                        Expression sub_if_tag = IRTools.get_IfConditionTag(TaggedSubExpr);
                        Expression inc_expr = Symbolic.subtract(TaggedSubExpr, NormalizedSubscript);

                            if(SSR_expr!=null && array_if_tag!=null && 
                                sub_if_tag.equals(array_if_tag) && inc_expr.equals(new IntegerLiteral(1)) &&
                                is_LoopVariant(array_if_tag, LoopVariantVars) &&
                                (subexpr.getOperator().equals(UnaryOperator.POST_INCREMENT)|| 
                                subexpr.getOperator().equals(UnaryOperator.PRE_INCREMENT))){
                                
                                return determine_Class2_recurr(ValueExpr, SSR_expr);
                            }
                    }
                }
           

            }
            //Check for Class 3 Recurrence
            else if(SubscriptExp != null){
                VariableDeclarator array_vd = (VariableDeclarator)LVV;
                Expression array_idexpr = array_vd.getID().clone();
                IntegerLiteral ONE = new IntegerLiteral(1);
                ArrayAccess LHS_sub_one = new ArrayAccess(array_idexpr, Symbolic.subtract(SubscriptExp, ONE));

                if( ValueExpr.equals(LHS_sub_one) || ValueExpr.getChildren().contains(LHS_sub_one)
                    && is_simple_subscript(SubscriptExp , LoopIndex)){
        
                        Expression remainder = Symbolic.subtract(ValueExpr, LHS_sub_one);

                        Expression remainder_range = RangesBeforeLoop.getRange(SymbolTools.getSymbolOf(remainder));

                        if(remainder_range == null){
                            IRTools.replaceAll(remainder, LHS_sub_one, new IntegerLiteral(0));
                            remainder_range = Symbolic.simplify(remainder);
                        }
                        
                        if(is_PNN(remainder_range)){
                            Class3_PNN_term = remainder_range;
                            return "Class 3";
                        }
                }

                
            }
            
            return "Unknown Class";

        }


    }

    //Returns the SSR variable in a value expression
    private static Expression Find_SSR_expr(Expression ValueExpression, List<Symbol> SSR_variables){

        DFIterator<Expression> iter = new DFIterator<Expression>(ValueExpression, Expression.class);

        while(iter.hasNext()){
            Expression expr = iter.next();
            if(expr instanceof ArrayAccess){
                break;
            }
            Symbol sym = SymbolTools.getSymbolOf(expr);
            if(SSR_variables.contains(sym)){
                if(expr.getParent()!=null && (!expr.getParent().equals(ValueExpression))){
                    return (Expression)expr.getParent();
                }
                else{
                   
                    return expr;
                }
            }
        }

        return null;

    }

        /**
         * Aggregating Multidimensional Subscript arrays. Analysis to determine an array property
         * is performed only with the outermost loop i.e. w.r.t the leftmost array dimension.
         * @param LoopRangeExpressions - Loop Phase 1 Expressions
         * @param inputForLoop - The input for loop
         */

        private static Expression Unionize_MultiDimArrayExprs(Symbol Defined_Array, List<Expression> ValueExprs, 
                                                    RangeDomain LoopRangeExpressions, List<Symbol> SSR_Vars, Symbol LoopIndexSymbol)
        {

            //System.out.println("def array: " + Defined_Array  + ","+ LoopRangeExpressions +"\n");

                //Simplify/Unionize the value expressions to determine 1 consolidated expression
                Expression simplified_expr = ValueExprs.get(0);
                for(int i=1; i < ValueExprs.size(); i++){
                    simplified_expr = 
                        LoopRangeExpressions.unionRanges(simplified_expr, 
                                                        LoopRangeExpressions, 
                                                        ValueExprs.get(i), LoopRangeExpressions);
                }

                //If the result of unionization is "bottom", go through each array expression
                //and perform substituion of aggreagte values.
                if(simplified_expr == null){
                    //Go through every Array range expression and determine, the integer
                    // and non-integer indices. This is required for the aggregation process.
                    //Also forward substitute for any variables who's range expressions are
                    //available in the SVD.
                    int indx=0;
                   
                    List<ArrayAccess> Multi_DimArrays = new ArrayList<>(LoopRangeExpressions.getMultiDimArrays());
                    
                    for(ArrayAccess arr : Multi_DimArrays){
                        //System.out.println("arr: " + arr +"\n");
                        Expression LVV_Value_Expr = LoopRangeExpressions.getRange(arr);
                        if(ValueExprs.contains(LVV_Value_Expr))
                            indx = ValueExprs.indexOf(LVV_Value_Expr);

                        LVV_Value_Expr = LoopRangeExpressions.substituteForwardRange(LVV_Value_Expr);
                        LoopRangeExpressions.setRange(arr, LVV_Value_Expr);
                        ValueExprs.set(indx, LVV_Value_Expr);

                    }

                }
                return simplified_expr;

        }


    

    //Returns the Range expression of the loop index variable of the input 'for' loop.

    private static RangeExpression getLoopIndexRange(ForLoop InputForLoop, RangeDomain ValuesBeforeLoopHeader){

        Expression LoopUpperbound = LoopTools.getUpperBoundExpression(InputForLoop);
        LoopUpperbound.setParent(null);

        Expression LoopLowerbound = LoopTools.getLowerBoundExpression(InputForLoop);
        LoopLowerbound.setParent(null);

        Expression stride = LoopTools.getIncrementExpression(InputForLoop);
        stride.setParent(null);

        if(ValuesBeforeLoopHeader!= null){
            Expression ForSubUB = ValuesBeforeLoopHeader.substituteForwardRange(LoopUpperbound);
            if(!IRTools.containsClass(ForSubUB, InfExpression.class))
                    LoopUpperbound = ForSubUB;

            Expression ForSubLB = ValuesBeforeLoopHeader.substituteForwardRange(LoopLowerbound);
            if(!IRTools.containsClass(ForSubLB, InfExpression.class))
                    LoopLowerbound = ForSubLB;
            
        }
        
        RangeExpression LoopIndexRange = new RangeExpression(LoopLowerbound,LoopUpperbound, stride);

        return LoopIndexRange;


    }

    
    /**
     * Returns true if a Range Expression is PNN i.e. both lower bound and 
     * upper bound are greater than or equal to zero.
     */
    private static Boolean is_PNN(Expression input_re){
       
        if(input_re instanceof RangeExpression){
            Expression inc_expr_lb = ((RangeExpression)input_re).getLB();
            Expression inc_expr_ub = ((RangeExpression)input_re).getUB();
        
            if(inc_expr_lb instanceof IntegerLiteral && inc_expr_ub instanceof IntegerLiteral){
                if( ((IntegerLiteral)inc_expr_lb).getValue() >= 0 && ((IntegerLiteral)inc_expr_ub).getValue() >= 0 )
                    return true;
            }
            else
                return false;
        }
        
        if(input_re instanceof IntegerLiteral && ((IntegerLiteral)input_re).getValue() >= 0)
            return true;
        
        return false;

    }
    
    /**
     * Evaluates the symbols in an SSR expression in an input value expression and returns the appropriate 
     * Monotonicity type. E.g. If all symbols in a SSR expression are strictly monotonic the resulting property
     * is Strict Monotonicity.
     * @param ValueExpr  - The value expression being evaluated
     * @param SSR_expr   - The SSR expression part of the value expression
     * @return    - Appropriate Monotonicity type
     */

    private static String determine_Class2_recurr(Expression ValueExpr, Expression SSR_expr){
        
        if(is_constant(ValueExpr))
          return "Unknown Class";

        Expression remainder = null;
        if(ValueExpr instanceof RangeExpression){
            Expression lb = ((RangeExpression)ValueExpr).getLB();
            Expression ub = ((RangeExpression)ValueExpr).getUB();
            Expression remainder_lb = Symbolic.subtract(lb, SSR_expr);
            Expression remainder_ub = Symbolic.subtract(ub, SSR_expr);
            remainder = new RangeExpression(remainder_lb, remainder_ub);
           
        }
        else
            remainder = Symbolic.subtract(ValueExpr, SSR_expr);

        List ssr_vars = IRTools.getExpressionsOfType(SSR_expr, IDExpression.class);

        Identifier ssr_var = null;
        if(ssr_vars.size() == 1){
            ssr_var = (Identifier)ssr_vars.iterator().next();
        }

        if(SSR_expr !=null && ssr_var != null && is_PNN(remainder)){
            Expression coeff = Symbolic.getCoefficient(SSR_expr, ssr_var);
            if(remainder instanceof RangeExpression){
                Expression remainder_ub = ((RangeExpression)remainder).getUB();
                Expression remainder_lb = ((RangeExpression)remainder).getLB();
                Expression coeff_plus_lb = Symbolic.add(remainder_lb, coeff);
                
                if(Symbolic.gt(coeff, new IntegerLiteral(1)).equals(new IntegerLiteral(1)) &&
                    Symbolic.ge(coeff_plus_lb, remainder_ub).equals(new IntegerLiteral(1))){
                        return "Class 2";
                }
                else
                return "Unknown Class";
                
            }
            else if(Symbolic.eq(coeff, new IntegerLiteral(1)).equals(new IntegerLiteral(1))){
                return "Class 2";
            }
            else
                return "Unknown Class";
        }
        else
        return "Unknown Class";
        

    }

    /**
     * Returns a property after evaluating a PNN term. Primarily used to
     * evaluate for class 3 recurrences
     * @param epxr - The PNN term
     * @return - A property
     */
    private static String eval_PNN(Expression expr){

        if(expr instanceof RangeExpression){
            RangeExpression range = (RangeExpression)expr;
            Expression lb = range.getLB();
            if(((IntegerLiteral)lb).getValue() > 0)
                return "STRICT_MONOTONIC";
            else if(((IntegerLiteral)lb).getValue() >= 0)
                return "MONOTONIC";
            else
                return null;
            
        }
        else{
            if(((IntegerLiteral)expr).getValue() > 0)
                return "STRICT_MONOTONIC";
            else if(((IntegerLiteral)expr).getValue() >= 0)
                return "MONOTONIC";
            else
                return null;


        }

    }


    /**
     * To check if the value expression for an LVV is a constant expression
     * @param input_re - The input value expression
     * @return         - True if the value is constant, false otherwise
     */

    private static Boolean is_constant(Expression input_re){


        if(input_re instanceof RangeExpression){
            Expression inc_expr_lb = ((RangeExpression)input_re).getLB();
            Expression inc_expr_ub = ((RangeExpression)input_re).getUB();
            if( inc_expr_lb instanceof IntegerLiteral && inc_expr_ub instanceof IntegerLiteral)
                return true;
            else if( inc_expr_lb instanceof FloatLiteral && inc_expr_ub instanceof FloatLiteral)
                return true;
        }
        
        if(input_re instanceof IntegerLiteral)
            return true;
        else if(input_re instanceof FloatLiteral)
            return true;
        
        return false;

    }

    /**
     * Determines if a subscript expression is a simple subscript.
     * Simple subscripts are of the form - "i+k", where "i" is the loop
     * index variable and "k" is a constant.
     * @param SubscriptExpr - The subscript expression to be evaluated
     * @param LoopIndexVar  - Loop index variable
     * @return
     */

    private static boolean is_simple_subscript(Expression SubscriptExpr, Expression LoopIndexVar){

        if(SubscriptExpr == null)
            return false;

        if(SubscriptExpr.getChildren().contains(LoopIndexVar) || 
                                SubscriptExpr.equals(LoopIndexVar)){
            Expression remainder = Symbolic.subtract(SubscriptExpr, LoopIndexVar);

            if(is_constant(remainder))
                return true;
        }
       
       return false;

    }

    /**
     * Determines evaluation order of symbols in Phase 2. For e.g. if d[j]=p is the expression
     * for array 'd' after Phase 1 and 'p' is an LVV, then 'p' should be evaluated before 'd' in
     * Phase 2.
     * @param Symbols_to_analyze - The List of Symbols to analyze in Phase 2
     * @param LoopRangeExprs     - The SVD after Phase 1
     * @return - Sorted SVD
     */
    private static List determine_eval_order(List<Symbol> Symbols_to_analyze, RangeDomain LoopRangeExprs){

        for(int i=0; i < Symbols_to_analyze.size(); i++){
            Symbol set_sym = Symbols_to_analyze.get(i);
            Expression valueExpr = LoopRangeExprs.getRange(set_sym);
            Symbol assigned_Sym = SymbolTools.getSymbolOf(valueExpr);

            if(Symbols_to_analyze.contains(assigned_Sym)){
                int assigned_sym_id = Symbols_to_analyze.indexOf(assigned_Sym);
                if(i < assigned_sym_id){
                    Collections.swap(Symbols_to_analyze, i, assigned_sym_id);
                }
            }
        }

        return Symbols_to_analyze;

    }
    

    private static Set<Symbol> symmetricDifference(Set<Symbol> a, Set<Symbol> b) {
        Set<Symbol> result = new HashSet<Symbol>(a);
        for (Symbol element : b) {
            // .add() returns false if element already exists
            if (!result.add(element)) {
                result.remove(element);
            }
        }
        return result;
    }

    /**
     * Method to identify if the Loop Nest contains subscripted subscript patterns. This is required
     * so as to ensure that subscript array analysis is not performed w.r.t. the to-be parallelized
     * subscripted subscript loop(s).
     * @param LoopNest - Loop nest to analyze
     * @return
     */
    private static boolean IdentifySubSubLoop(LinkedList<Loop> LoopNest){

        //System.out.println("subsub: " + LoopNest +"\n");
        Iterator loop_iter = LoopNest.iterator();

        while(loop_iter.hasNext()){
            ForLoop targetLoop = (ForLoop)loop_iter.next(); 
            Expression loop_ub = LoopTools.getUpperBoundExpression(targetLoop);
          
           if(IRTools.containsClass(loop_ub, ArrayAccess.class)){
                 return true;

              }
            
        }

        loop_iter = LoopNest.iterator();        
        //Identify Subscripted subscript loops in the presence of multi-dimensional subscript arrays.
        while(loop_iter.hasNext()){
            ForLoop loop = (ForLoop)loop_iter.next();
            List<AssignmentExpression> assign_exprs = IRTools.getExpressionsOfType(loop, AssignmentExpression.class);
            Map<Expression,Expression> LHS_RHS_Map = new HashMap<>();
            for(AssignmentExpression assign_expr: assign_exprs){    
                LHS_RHS_Map.put(assign_expr.getLHS(), assign_expr.getRHS());
            }

            Set<Expression> Def_exprs = DataFlowTools.getDefSet(loop);
           
            for(Expression expr : Def_exprs){
                if(expr instanceof ArrayAccess){
                    ArrayAccess arr = (ArrayAccess)expr;
                    List<Expression> indices = arr.getIndices();
                    for(Expression indx : indices){
                        if(LHS_RHS_Map.keySet().contains(indx) && (LHS_RHS_Map.get(indx) instanceof ArrayAccess)){
                            return true;
                        }
                      
                    }
                }
            }
            
            
        }
       
        return false;
    }

    /**
     * Checks if an input loop contains discontiguous inner loops or loops with
     * inner loops that have statements after. It is used as an eligibility test
     * for the analysis. Need to refine it and make it more general.
     * @param loop
     * @return
     */
    private static boolean ContainsDiscontiguousInnerLoops(Loop loop){

        Statement stmt = loop.getBody();
        FlatIterator<Traversable> iter = new FlatIterator<Traversable>(stmt);
        boolean pnest = false;
        Object o = null;
    
       
        if (iter.hasNext()) {
            boolean skip = false;
            do {
                o = (Statement)iter.next(Statement.class);
                if (o instanceof AnnotationStatement) {
                    skip = true;
                } else {
                    skip = false;
                }
            } while ((skip) && (iter.hasNext()));
            if (o instanceof ForLoop) {
                pnest = (ContainsDiscontiguousInnerLoops((Loop)o));
                // The ForLoop contains additional statements after the end
                // of the first ForLoop. This is interpreted as
                // a non-perfect nest for dependence testing
                if (iter.hasNext() &&
                   iter.next() instanceof ForLoop) {
                    pnest = true;
                }
               
            } else {
                while(iter.hasNext()){
                     Object obj = iter.next();
                    if((obj instanceof Loop) && iter.hasNext() &&
                       iter.next() instanceof Statement){
                        pnest = true;
                        break;
                    }
                    else if (obj instanceof IfStatement){
                        IfStatement ifstmt = (IfStatement)obj;
                        if(IRTools.containsClass(ifstmt, ForLoop.class)){
                            pnest = true;
                            break;
                        }
                    }
                }
            }
         
        }

        return pnest;
    }


    //Determining the type of monotonicity with details about the property for a Class 2 LVV depending on if
    // the variable is a scalar, 1D Array or MultiDim Array
    private static String Determine_Monotonicity_Type(Symbol LVV,ForLoop inputForLoop,Set<Symbol>Expression_Symbols){

        Set<String> LVV_Properties = new HashSet<>();
        for(Symbol sym : Expression_Symbols){
            LVV_Properties.add(variable_property.get(sym));
        }

        String property = LVV_Properties.iterator().next();
        if(LVV_Properties.size() == 1 && !LVV.getArraySpecifiers().isEmpty() ){
            ArraySpecifier arr_specs = (ArraySpecifier)LVV.getArraySpecifiers().get(0);
            
            if(arr_specs.getNumDimensions() == 1){
                return property;
            }
            else if(arr_specs.getNumDimensions() > 1 && LoopTools.isOutermostLoop(inputForLoop)){
                return (property+":0");
            }
        }
     
        return property;
       
    }

    /**
     * Collect information about Multi-Dimensional subscript arrays
     */
    
    private static void CollectMultiDimArrayInfo(RangeDomain RangeExpressions, ForLoop loop)
    {


        Set<Integer> Integer_idx = new HashSet<>();
        Set<Expression> defined_exprs = DataFlowTools.getDefSet(loop); 
        Set<Symbol> DefinedArray_Syms = new HashSet<>();

        List<IntegerLiteral> Integer_idx_vals = new ArrayList<>();
        List<Expression> Expr_idx_vals = new ArrayList<>();
        List<ArrayAccess> arrs_to_remove = new ArrayList<>();


        if(!RangeExpressions.getMultiDimArrays().isEmpty()){

              //Perform the analysis if only 1 multi-dimensional array is defined.
            for(Expression e : defined_exprs){
                if(e instanceof ArrayAccess){
                    DefinedArray_Syms.add(SymbolTools.getSymbolOf(e));
                    arrs_to_remove.add((ArrayAccess)e);
                }
            }
            
            if(DefinedArray_Syms.size()>1){
                for(ArrayAccess arr: arrs_to_remove){
                    RangeExpressions.removeMultiDimRange(arr);
                    RangeExpressions.removeRange(SymbolTools.getSymbolOf(arr));
                }
                    return;
            }

            List<Expression> RangeExprs = new ArrayList<>();
          
            for(ArrayAccess arr : RangeExpressions.getMultiDimArrays()){
                RangeExprs.add(RangeExpressions.getRange(arr));

                List<Expression> arr_indices = arr.getIndices();
    
                for(int i=0; i < arr_indices.size(); i++){
                    Expression idx = arr_indices.get(i);
                    if(idx instanceof IntegerLiteral){
                        Integer_idx.add((Integer)i);
                        Integer_idx_vals.add((IntegerLiteral)idx);
                    }
                    else{
                      
                            if(!Expr_idx_vals.contains(idx))
                                Expr_idx_vals.add(idx);
                    }
                }
            }
    
                //Correct aggregation w.r.t. index positions in the presence of integer index
                //expressions.
                if( Integer_idx_vals.size()>1){
                    Expression lb = Integer_idx_vals.get(0).clone();
                    Expression ub = Integer_idx_vals.get(Integer_idx_vals.size()-1).clone();
                    RangeExpression integer_range = new RangeExpression(lb,ub);
                    Expr_idx_vals.add(Integer_idx.iterator().next(), integer_range);
                }


                Symbol Defined_Array = DefinedArray_Syms.iterator().next();   

                if(Loop_agg_subscripts.get(Defined_Array) == null){
                    Loop_agg_subscripts.put(Defined_Array, Expr_idx_vals);
                }

                SymbolTools.CollectArrayAccesses(Defined_Array,RangeExpressions.getMultiDimArrays());
                SymbolTools.CollectAssignedValues(Defined_Array, RangeExprs);
        }

        
        return;
    }

    /**
     * Checks the presence of an infinite expression
     * @param input_expr
     * @return
     */

    private static Expression UnionValues(Expression SubValue, Expression valueBeforeLoop){

        RangeExpression SubRange = (RangeExpression)SubValue;
        if(Symbolic.gt(SubRange.getLB(), valueBeforeLoop).equals(new IntegerLiteral(1)))
           SubRange.setLB(valueBeforeLoop);
        
        if(Symbolic.lt(SubRange.getUB(), valueBeforeLoop).equals(new IntegerLiteral(0)))
            SubRange.setUB(valueBeforeLoop);
        
            return SubRange;
    }


    /**
     * Checks if the input for loop contains function calls with side effects
     * @param input_loop
     * @return
     */
    private static boolean ContainsUnsafeFunctionCalls(Loop input_loop){
        
        DFIterator<FunctionCall> iter =
        new DFIterator<FunctionCall>(input_loop, FunctionCall.class);
         while (iter.hasNext()) {
             FunctionCall fc = iter.next();
             if (!StandardLibrary.isSideEffectFree(fc))
                 return true;
         }

         return false;

    }

    private static IfStatement getIfStatement(Set<DFANode>nodeSet){
        
        for(DFANode node: nodeSet){
            if(node.getData("stmt-ref") != null && 
               node.getData("stmt-ref") instanceof IfStatement ){

               return node.getData("stmt-ref");
            }
            
        }

        return null;
    }

    /**
     * Procedure to tag expressions modified within an If-then Statement with
     * the if-condition. Requried for Intermittent sequence analysis.
     * @param rd - Input Range Domain with Phase 1 expressions
     * @param Ifstmt - The enclosing If-statement
     */

    private static void TagExpressionsWithIfCondition(RangeDomain rd, IfStatement Ifstmt){

        if(Ifstmt == null)
            return;
        Expression IfCond = Ifstmt.getControlExpression();
        Statement ThenStmt = Ifstmt.getThenStatement();
        Set<Symbol> ModifiedSyms = DataFlowTools.getDefSymbol(ThenStmt);
       
        for(Symbol sym : rd.getSymbols()){
            Expression expr = rd.getRange(sym);
            if(ModifiedSyms.contains(sym)){
                IRTools.SetIfConditionTag(expr, IfCond);
            }
        }

    
    }
   

    private static boolean is_LoopVariant(Expression expr, Set<Symbol> Def_Syms){
        
        Set<Symbol> accessed_syms = SymbolTools.getAccessedSymbols(expr);

        for(Symbol acc_sym : accessed_syms){
            if(Def_Syms.contains(acc_sym))
                return true;
        }
        return false;
    }

     /**
    * Normalizes the graph so that each node does not have an assignment
    * expression as a sub expression of another expression. Expressions that
    * contain conditionally evaluated sub expressions -- e.g., short-circuit
    * evaluation -- are not normalized to avoid possibly unsafe expression
    * evaluation.
    */
    private static void normalizeCFG(CFGraph CFG) {
        if (CFG.size() < 1) {
            return;
        }
        DFANode last = CFG.getLast(), node = null;
        int i = 0;
        do {
            node = CFG.getNode(i++);
            Object o = CFG.getIR(node);
            // if(node.getData("ir") != null)
            // System.out.println(" node: " + node.getData("ir") +"\n");
            // else
            // System.out.println(" node: " + node.getData("tag")+"\n");

            if (o instanceof Traversable &&
                IRTools.containsClass((Traversable)o, FunctionCall.class)) {
                    List<FunctionCall> Function_Calls = IRTools.getExpressionsOfType((Traversable)o, FunctionCall.class);
                    boolean ContainsSideEffectFreeFCs = true;
                    for(FunctionCall fc : Function_Calls){
                        if(!StandardLibrary.isSideEffectFree(fc)){
                           ContainsSideEffectFreeFCs = false;
                        }
                    }
                if(!ContainsSideEffectFreeFCs)
                    continue;
            }
            DFAGraph temp = CFG.expandExpression(node);
            if (temp == null) {
                continue;
            }
            // Propagates properties from the unexpanded node.
            Object data = node.getData("stmt");
            if (data != null) {
                temp.getFirst().putData("stmt", data);
                // Mark the reference to the original statement
                for (int j = 0; j < temp.nodes.size(); j++) {
                    temp.nodes.get(j).putData("stmt-ref", data);
                }
            }
            if ((data = node.getData("loop-variants")) != null) {
                temp.getFirst().putData("loop-variants", data);
            }
            if ((data = node.getData("symbol-entry")) != null) {
                temp.getFirst().putData("symbol-entry", data);
            }
            if ((data = node.getData("symbol-exit")) != null) {
                List<SymbolTable> node_symbol_exit =
                        node.getData("symbol-exit");
                List<SymbolTable> symbol_exit =
                        temp.getLast().getData("symbol-exit");
                if (symbol_exit == null) {
                    symbol_exit = new ArrayList<SymbolTable>(4);
                    temp.getLast().putData("symbol-exit", symbol_exit);
                }
                for (int j = 0; j < node_symbol_exit.size(); j++) {
                    symbol_exit.add(node_symbol_exit.get(j));
                }
            }
            // Reconnect edges
            for (DFANode pred : node.getPreds()) {
                if (pred.getData("true") == node) {
                    pred.putData("true", temp.getFirst());
                } else if (pred.getData("false") == node) {
                    pred.putData("false", temp.getFirst());
                }
                CFG.addEdge(pred, temp.getFirst());
            }
            // Reconnect edges
            for (DFANode succ : node.getSuccs()) {
                if (node.getData("true") == succ) {
                    temp.getLast().putData("true", succ);
                } else if (node.getData("false") == succ) {
                    temp.getLast().putData("false", succ);
                }
                CFG.addEdge(temp.getLast(), succ);
            }
            CFG.absorb(temp);
            CFG.removeNode(node);
            --i;
        } while (last != node);
    }
 

}