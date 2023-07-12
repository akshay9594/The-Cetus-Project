package cetus.transforms;
import cetus.analysis.CFGraph;
import cetus.analysis.DFANode;
import cetus.analysis.LoopTools;
import cetus.analysis.RangeDomain;
import cetus.exec.Driver;
import cetus.hir.*;

import java.util.*;

public class TransformConsecParallelLoopNests extends TransformPass {

     /**
     * Pass name
     */
    private static final String pass_name = "[TransformConsecParallelLoopNests]";

    public TransformConsecParallelLoopNests(Program program){
        super(program);
    }

     /**
     * Starts the transformation.
     */
    public void start() {
        DFIterator<Procedure> iter =
                new DFIterator<Procedure>(program, Procedure.class);
        iter.pruneOn(Procedure.class);
        while (iter.hasNext()) {
           transformProcedure(iter.next());
        }
    }

    /**
     * Returns the pass name.
     */
    public String getPassName() {
        return pass_name;
    }

    private static void transformProcedure(Procedure proc){

        CFGraph ProcedureGraph = new CFGraph(proc);

        ProcedureGraph.topologicalSort(ProcedureGraph.getNodeWith("stmt", "ENTRY"));

        TreeMap<Integer, DFANode> work_list = new TreeMap<Integer, DFANode> ();

        DFANode entry = ProcedureGraph.getNodeWith("stmt", "ENTRY");
        work_list.put((Integer) entry.getData("top-order"), entry);

        // CompoundStatement parent =
        // IRTools.getAncestorOfType(loop, CompoundStatement.class);
        // parallel_region: container of the transformed parallel region
        CompoundStatement parallel_region = new CompoundStatement();
        parallel_region.annotate(new CetusAnnotation("parallel", ""));


        while(work_list.size()>0){
               // Get the first node in topological order from the work list.
            Integer node_num = work_list.firstKey();
            DFANode node = work_list.remove(node_num);

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
                ForLoop CurrentLoop = (ForLoop)node.getData("stmt");
                if(LoopTools.isOutermostLoop(CurrentLoop)){
                    //System.out.println("Loop: " + LoopTools.getLoopName(loop) +"\n");
                    for(DFANode FORPred : node.getPreds()){
                        if(FORPred.getData("tag") != null && FORPred.getData("tag").equals("FOREXIT")){
                            ForLoop PredecessorLoop = (ForLoop)FORPred.getData("stmt-exit");
                        
                        }
                       
                    }

                }
            }


            if((Integer)node.getData("num-visits") > 1){            
                
                for(DFANode successor : node.getSuccs()){

                    if(successor.getData("num-visits") == null){
                        work_list.put((Integer)successor.getData("top-order"), successor);
                        
                    }
                    
                }
                continue;

            }

               for (DFANode succ : node.getSuccs()) {
    
                        work_list.put((Integer)succ.getData("top-order"), succ);
                    
                }  


        }

    }

    
}
