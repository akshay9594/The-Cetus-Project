
package cetus.transforms;

import cetus.analysis.AliasAnalysis;
import cetus.analysis.AnalysisPass;
import cetus.analysis.ArrayPrivatization;
import cetus.analysis.DDGraph;
import cetus.analysis.DDTDriver;
import cetus.analysis.DependenceVector;
import cetus.analysis.LoopTools;
import cetus.analysis.RangeAnalysis;
import cetus.analysis.RangeDomain;
import cetus.hir.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Exchange loops if they are perfect nested loop.
 */


public class LoopInterchange extends TransformPass
{

    //    protected Program program;

    List Loop_ref_cost = new ArrayList<>();

    Boolean SymbolicIter;

    public LoopInterchange(Program program)
    {
        super(program);
    }

    
    /** 
     * @return String
     */
    public String getPassName()
    {
        return new String("[LoopInterchange]");
    }

    public void start()
    {
    
        LinkedList<Loop> loops = new LinkedList<Loop>();
        List<Statement> outer_loops = new ArrayList<Statement>();
        DepthFirstIterator iter = new DepthFirstIterator(program);
        List<Expression> expList = new LinkedList<Expression>();
        List<AssignmentExpression> LoopAssnExprs = new ArrayList<AssignmentExpression>();


        int i;
        int target_loops = 0;
        int num_single = 0, num_non_perfect = 0, num_contain_func = 0, num_loop_interchange=0;
             
        HashMap<ForLoop,String> loopMap = new HashMap<ForLoop,String>();

        while(iter.hasNext()) {
            Object o = iter.next();
            if(o instanceof ForLoop)
                outer_loops.add((Statement)o);
        }


        for(i = 0 ; i < outer_loops.size(); i++){

            ForLoop l = (ForLoop)outer_loops.get(i);

            //Exclude loops with negative stride
            if(LoopTools.getIncrementExpression(l).toString().equals("-1") ){
                loopMap.put(l, "NegativeIncrement");
                outer_loops.remove(l);
            }
          
        }

        
        for(i = outer_loops.size()-1; i >= 0; i--)
        {
            ForLoop program_loop = (ForLoop)outer_loops.get(i);
            if(!LoopTools.isOutermostLoop(program_loop))
            {
                outer_loops.remove(i);
            }
            
        }

       
     
        for(i = outer_loops.size()-1; i >= 0; i--)
        {
           
            iter = new DepthFirstIterator(outer_loops.get(i));
            loops.clear();
            LoopAssnExprs.clear();
            List<Expression> OriginalLoopOrder = new ArrayList<>();
            while(iter.hasNext()) {
                Object o = iter.next();
                if(o instanceof ForLoop){

                    OriginalLoopOrder.add(LoopTools.getIndexVariable((ForLoop)o));
                    loops.add((Loop)o);
                }
            }
            if(loops.size() < 2) {
                num_single++;
            }else if(!LoopTools.isPerfectNest((ForLoop)loops.get(0))) {
                num_non_perfect++;
                loopMap.put((ForLoop)loops.get(0), "non-perfect");
            }else if(LoopTools.containsFunctionCall((ForLoop)loops.get(0))) {
                num_contain_func++;
                loopMap.put((ForLoop)loops.get(0), "Function-call");
            } else {
                target_loops++;
                Statement stm = ((ForLoop)loops.get(loops.size()-1)).getBody();
                List<ArrayAccess> arrays = new ArrayList<ArrayAccess>();  // Arrays in loop body
                DepthFirstIterator iter2 = new DepthFirstIterator(stm);
                List<Expression> PermutedLoopOrder = new ArrayList<>();
                

                while(iter2.hasNext())
                {
                    Object child = iter2.next();
                    if(child instanceof ArrayAccess)
                    {

                        ArrayAccess array = (ArrayAccess)child;
                        arrays.add(array);
                    }

                    if(child instanceof AssignmentExpression)
                    {
                        LoopAssnExprs.add((AssignmentExpression)child);
                    }

                }

                   
                /*              
                    Begin reusability test to determine Reusability for each loop in the Nest
                    Result of reusability test is the Order of the loops in the nest for max reusability
                    TODO: Implementing the Nearby Permutation alogirthm in the scenario where the 
                          MemoryOrder cannot be reached.

                */


                HashMap LoopNestIterMap = LoopIterationMap(loops.get(0));

                if(HasSymbolicBounds(LoopNestIterMap)){
                    SymbolicIter = true;
                 }
                 else if(HasNonSymbolicBounds(LoopNestIterMap))
                 {
                    SymbolicIter = false;
          
                 }
                 else{
                     loopMap.put((ForLoop)loops.get(0), "ComplexBounds");
                     continue;
                 }

            
                List<Expression> MemoryOrder = ReusabilityAnalysis(program , loops.get(0), LoopAssnExprs , arrays , loops);


                //If the Original Nest is already in the desired order, no need for further analysis. 
             
                if(OriginalLoopOrder.equals(MemoryOrder)){

                    loopMap.put((ForLoop)loops.get(0), "AlreadyInOrder");
                    continue;
                }

                int r = 0,j,until = loops.size();
                int target_index = 0;
                boolean icFlag = true;
                List<Integer> rank;
                int rankSize;


OuterWhileLoop:                
                while(icFlag)
                {
                    Expression exp;
                    expList.clear();
                    for(j = 0; j < until; j++)
                    {
                        exp = LoopTools.getIndexVariable((ForLoop)loops.get(j));
                        if(exp != null)
                            expList.add(exp);
                    }


                    rank = getRank(arrays, expList, target_index);

                    rankSize = rank.size();

                    for(j = 0; j < rankSize; j++) 
                    {
                        r = getRank2(rank, expList, loops);
    
                        rank.remove(rank.indexOf(r));
                
                        if(expList.size() < until) until = expList.size();


                        for(int k = r+1; k < until; k++)
                        {

                            ForLoop l = (ForLoop)loops.get(0);
                            ForLoop outermostLoop = (ForLoop)LoopTools.getOutermostLoop(l);
                            LinkedList innerLoops = LoopTools.calculateInnerLoopNest(outermostLoop);

                            if(isLegal(loops, r, k))
                            {
                               
                                    ForLoop loop1 = (ForLoop)loops.get(r);
                                    ForLoop loop2 = (ForLoop)loops.get(k);

                                    swapLoop( loop1 , loop2);
                                    num_loop_interchange++;
                                    Collections.swap(expList, r, k);
                                    r = k;

                                     PermutedLoopOrder = new ArrayList<>();

                                    for(int q = 0 ; q < loops.size(); q++){

                                        PermutedLoopOrder.add(LoopTools.getIndexVariable(loops.get(q)));

                                    }


                                    if(PermutedLoopOrder.equals(MemoryOrder)){
                                        loopMap.put(l, "Permuted"); 
                                        icFlag = false;
                                        break OuterWhileLoop;
                                    
                                    }            
                                      
                            }


                            
                        }        
                        until = r;
                    }
                    target_index++;
                    if(until == 0) icFlag = false;
             
                }

                if(PermutedLoopOrder.equals(OriginalLoopOrder) ||
                   PermutedLoopOrder.isEmpty())
                loopMap.put((ForLoop)loops.get(0), "Non-Permuted");
            }


        }
      
        System.out.print("\n");

       DFIterator<ForLoop> ForLoopIter =
                        new DFIterator<ForLoop>(program, ForLoop.class);

       while(ForLoopIter.hasNext()){

        ForLoop forloop = ForLoopIter.next();

            if(loopMap.containsKey(forloop)){      
                
                if((loopMap.get(forloop)).equals("Permuted")){

                    System.out.println("[LoopInterchange] Loops in nest: " + LoopTools.getLoopName(forloop) +
                                    " have been Interchanged"+"\n");

                }

                else if( (loopMap.get(forloop)).equals("Non-Permuted") )
                    System.out.println("[LoopInterchange] Loops in nest: " + LoopTools.getLoopName(forloop) +
                                    " cannot be Interchanged"+"\n");

                else if((loopMap.get(forloop)).equals("AlreadyInOrder")){

                    System.out.println("[LoopInterchange] Loops in nest: " + LoopTools.getLoopName(forloop) + 
                    " Already in desired order\n");

                }

                else if((loopMap.get(forloop)).equals("NegativeIncrement")){

                    System.out.println("[LoopInterchange] Loops in nest: " + LoopTools.getLoopName(forloop) + 
                    " Have negative increment expression;Cannot perform Interchange\n");

                }

                else if((loopMap.get(forloop)).equals("ComplexBounds")){

                    System.out.println("[LoopInterchange] Loops in nest: " + LoopTools.getLoopName(forloop) + 
                    " have complex bound expressions;Cannot perform Interchange\n");

                }
                else if((loopMap.get(forloop)).equals("non-perfect")){

                    System.out.println("[LoopInterchange] Loops in nest: " + LoopTools.getLoopName(forloop) + 
                    " are imprefectly nested;Cannot perform Interchange\n");

                }
                
                else if((loopMap.get(forloop)).equals("Function-call")){

                    System.out.println("[LoopInterchange] Loops in nest: " + LoopTools.getLoopName(forloop) + 
                    " contain function call;Cannot perform Interchange\n");

                }

            }

           

        }


        if(loopMap.isEmpty())
        System.out.println("[LoopInterchange] No loops have been interchanged\n");


        return;
    }

    
    /** 
     * @param rank
     * @param loops
     * @return List<Integer>
     */
    // Find out which loops could legally be interchanged with innermost loop.
    protected List<Integer> rankByMaxInterchange(List<Integer> rank, List<Loop> loops)
    {
        int i, j, legal, max = 0, cur;
        boolean legality;
        List<Integer> result = new LinkedList<Integer>();

        for(i=0; i<rank.size(); i++)
        {
            legal = rank.get(i);
            cur = rank.get(i);
            if(cur+1 == loops.size()) {
                if(legal > max) result.clear();
                result.add(rank.get(i));
                max = legal;
            }

            for(j=cur+1; j<loops.size(); j++)
            {
                legality = isLegal((LinkedList<Loop>)loops, cur, j);
                if(legality) legal = j;
                if(!legality || j == loops.size()-1) {
                    if(legal > max)
                    {
                        result.clear();
                        result.add(rank.get(i));
                        max = legal;
                    } else if (legal == max) {
                        result.add(rank.get(i));
                    }
                    break;
                }
            }
        }
        if(result.size() == 0) return rank;
        return result;
    }


    /**
     * Reusability Test to determine the innermost loop in the nest for Max reusability.
     *         1. Loop which accesses the least number of cache lines should be the innermost loop.
     *         2. To find reusability score , for a loop:
     *             (a) Form Reference groups with array accesses
     *             (b) Array accesses can be of following types:
     *                 - Accesses with loop carried or non-loop carried dependencies
     *                 - Accesses with no loop dependencies
     *                 - Loop invariant accesses
     *             * Refer to K.S. McKinley's paper - 'Optimizing for parallelism and locality' on how the ref groups are formed
     *         3. Add the costs in terms of cache lines for each reference group
     *            - For an array access w.r.t. candidate innermost loop
     *              (a) The array access requires 'trip/Cache Line size' no. of cache lines if the loop index of the
     *                  candidate innermost loop appears on the rightmost dimension of the access(row-major access)
     *              (b) The array access requires 1 cache line if it is loop invariant and
     *              (c) The array access requires 'trip' no. of cache lines if the loop index of the candidate innermost loop
     *                  appears on the leftmost dimension of the access(Column-major access)
     *             *trip : Number of iterations of the candidate innermost loop.
     *         4. Multiply the Reference group costs with the number of iterations of loops other than the candidate innnermost loop
     *         5. Here Cache line size is assumed to be 64.
     *         6.Symbolic loop bounds are also supported, though extensive testing needs to be done to ensure accuracy.
     * @param OriginalProgram - The program
     * @param LoopNest    - The loop nest to be analyzed
     * @param LoopExprs   - The expressions in the loop body
     * @param LoopArrays  - All array accesses in the loop body
     * @param LoopNestList - List of loops in the loop nest 
     * @return    - - Order of the loops in the nest for max reusability
     */


    public List ReusabilityAnalysis(Program OriginalProgram, Loop LoopNest ,
                            List<AssignmentExpression> LoopExprs, List<ArrayAccess> LoopArrays, LinkedList<Loop> LoopNestList ){

        int i , j ,k , l;

        long count;
    
       List<Expression> LoopNestOrder = new ArrayList<Expression>();
       HashMap<Expression,Long> LoopCostMap = new HashMap<Expression,Long>();
       HashMap<Expression,Expression> Symbolic_LoopCostMap = new HashMap<Expression,Expression>();                            

       List<Long> scores = new ArrayList<>(); 
       List<Expression> Symbolic_scores = new ArrayList<>();
       Expression IndexOfInnerLoop = null; 
     
       DDGraph programDDG = program.getDDGraph();
       ArrayList<DependenceVector> Loopdpv = new ArrayList<>();

       Loopdpv = programDDG.getDirectionMatrix(LoopNestList);

       DepthFirstIterator LoopNestiter = new DepthFirstIterator(LoopNest);

       while(LoopNestiter.hasNext()){

            Object LoopObj = LoopNestiter.next();

            if(LoopObj instanceof ForLoop){

            LoopNestOrder.add(LoopTools.getIndexVariable((ForLoop)LoopObj));

            }

       }

       //System.out.println("loop nest: " + LoopNest +"\n");

       Boolean SymbolicLoopStride = false;

       HashMap LoopNestIterationMap = LoopIterationMap(LoopNest);

      
      
       //Getting reusability score

       DFIterator<ForLoop> forloopiter = new DFIterator<>(LoopNest, ForLoop.class);

       ArrayList<Long> RefGroupCost = new ArrayList<Long>();
       ArrayList<Expression> Symbolic_RefGRoupCost = new ArrayList<Expression>();
       ArrayList<Expression> LHSDim = new ArrayList<Expression>();
       ArrayList<Expression> RHSDim = new ArrayList<Expression>();
       long LoopstrideValue = 0;
       long trip_currentLoop = 0;
       long TotalLoopCost = 0;

       IntegerLiteral initalValue = new IntegerLiteral(0);
       FloatLiteral cls = new FloatLiteral(0.015625);
       IntegerLiteral Identity = new IntegerLiteral(1);
       
       Expression Symbolic_LoopTripCount = null;
       Expression Symbolic_count = null;
       Expression Symbolic_TotalLoopCost = null;

            while(forloopiter.hasNext()){

                ForLoop loop = forloopiter.next();

                //Collecting Loop Information
                Expression LoopIdx = LoopTools.getIndexVariable(loop);

                Expression LoopIncExpr = LoopTools.getIncrementExpression(loop);

                if(LoopIncExpr instanceof IntegerLiteral)
                 LoopstrideValue = ((IntegerLiteral)LoopIncExpr).getValue();
                
                else
                 SymbolicLoopStride = true;
                

                List ReferenceGroups =  RefGroup( loop , LoopArrays , LoopNestOrder);
 
                if(!SymbolicIter)
                 trip_currentLoop = (long)LoopNestIterationMap.get(LoopIdx);
                
                 else
                 Symbolic_LoopTripCount = (Expression)LoopNestIterationMap.get(LoopIdx);

                RefGroupCost = new ArrayList<>();

                Symbolic_RefGRoupCost = new ArrayList<>();

                ArrayList RepresentativeGroup = new ArrayList<>();

                //Taking one Array Access from each Ref Group to form a representative group

                for( i = 0 ; i < ReferenceGroups.size(); i++){

                  ArrayList Group  = (ArrayList)ReferenceGroups.get(i);

                  RepresentativeGroup.add(Group.get(0));
                
                }

        
                    for( j = 0; j < RepresentativeGroup.size() ; j++){

                         count = 0;

                         Symbolic_count = (Expression)initalValue;

                        ArrayAccess Array = (ArrayAccess)RepresentativeGroup.get(j);

                        List<Expression> ArrayDims = Array.getIndices();

                        LHSDim = new ArrayList<>();

                        RHSDim = new ArrayList<>();

                        for( k = 0 ; k < ArrayDims.size() ;k++){

                            if( k == 0)
                              LHSDim.add(ArrayDims.get(k));
                            else
                              RHSDim.add(ArrayDims.get(k));

                        }
                    

                        /*
                         Cost = (trip)/cache line size if:
                         (a) Loop index variable is present in the right hand side dimension of the array access
                         (b) The Dimension has a unit stride and
                         (c) The loop also has a unit stride

                        */
                        
                        Expression RHSExprWithLoopID = null;

                        for(l =0 ; l < RHSDim.size();l++){

                            Expression Expr = RHSDim.get(l);
                                           
                            if(Expr.toString().contains(LoopIdx.toString())){

                                RHSExprWithLoopID = Expr;
                                break;

                            }
      

                        }
                      
            
                        Expression LHSExpr = LHSDim.get(0);

                        // If Loop Trip count is not Symbolic

                        if(Symbolic_LoopTripCount == null){

                            if(RHSExprWithLoopID!= null && UnitStride(RHSExprWithLoopID, LoopIdx) && LoopstrideValue == 1)
                                count += (trip_currentLoop/64);

                            /*

                            Cost = 1 if:
                            Array access is loop invariant                    

                            */

                            else if(!LHSExpr.toString().contains(LoopIdx.toString()) && RHSExprWithLoopID == null ){
                                count += 1;
                            }
        

                            /*
                            Cost = (trip) if:
                            (a) Loop index variable is present in the left hand side dimension of the array access
                            (b) The Dimension has a non- unit stride or
                            (c) The loop candidate loop has a non-unit stride

                            */

                            else 
                                count += trip_currentLoop;


                            RefGroupCost.add(count);
                        }

                        // If Loop Trip Count is Symbolic

                        else{

                        

                            if(RHSExprWithLoopID!= null && UnitStride(RHSExprWithLoopID, LoopIdx) && LoopstrideValue == 1){
                                   Expression result = Symbolic.multiply(Symbolic_LoopTripCount , (Expression)cls);
                                   Symbolic_count =  Symbolic.add(Symbolic_count, result);
                            }
                                   
                            
                            else if(!LHSExpr.toString().contains(LoopIdx.toString()) && RHSExprWithLoopID == null )
                                Symbolic_count = Symbolic.add(Symbolic_count , (Expression)Identity);

                            else{

                                Symbolic_count = Symbolic.add(Symbolic_count , Symbolic_LoopTripCount);

                                
                            }
                                

                            Symbolic_RefGRoupCost.add(Symbolic_count);

                        }


                    }


                if(!SymbolicIter){
                    TotalLoopCost =  LoopCost(RefGroupCost, LoopNestIterationMap , LoopIdx);

                    scores.add(TotalLoopCost);

                    LoopCostMap.put(LoopIdx, TotalLoopCost);
                }

                else{

                    Symbolic_TotalLoopCost = SymbolicLoopCost(Symbolic_RefGRoupCost, LoopNestIterationMap , LoopIdx);

                    Symbolic_scores.add(Symbolic_TotalLoopCost);
                    Symbolic_LoopCostMap.put(LoopIdx , Symbolic_TotalLoopCost);

                }

              

             }
        
             //System.out.println("Map: " + Symbolic_LoopCostMap +"\n");

            if(!LoopCostMap.isEmpty()){
                Collections.sort(scores, Collections.reverseOrder());

                long MinScore = Collections.min(scores);

            
                for(Expression key : LoopCostMap.keySet()){

                    long Cost = LoopCostMap.get(key);

                    if(Cost == MinScore ){

                        IndexOfInnerLoop = key;

                    }

                    /*
                        Order of loops in the loop nest according to Cost analysis
                        Loop accessing the least number of cache lines is innermost while
                        the one accessing the most number of cache lines is outermost.

                    */

                    LoopNestOrder.set(scores.indexOf(Cost), key);

                }
            }

            else{

                //For Symbolic Loop Costs

                int counter = 0;

                List <Expression> DominantTerms = new ArrayList<>();

                Expression variable = null;

                List<Expression> ListOfExpressionVariables = new ArrayList<>();

                for( k = 0 ; k < Symbolic_scores.size() ; k++){

                        Expression se1 = (Expression)Symbolic_scores.get(k);

                         variable = Symbolic.getVariables(se1).get(0);

                         ListOfExpressionVariables.add(variable);

                        List <Expression> Terms = Symbolic.getTerms(se1);

                        List<Integer> VariableCount = new ArrayList<>();

                        for(i = 0 ; i < Terms.size(); i++){

                            Expression childterm = Terms.get(i);

                            counter = 0;

                            if(childterm.getChildren().contains(variable)){

                                List entries = childterm.getChildren();

                                for( j = 0 ; j < entries.size() ; j++){

                                    if(entries.get(j).equals(variable))
                                    counter++;

                                }

                                VariableCount.add(counter);
                            }

                            else
                            VariableCount.add(counter);

                        }

                        int MaxCount = Collections.max(VariableCount);

                        int indx = VariableCount.indexOf(MaxCount);

                        DominantTerms.add(Terms.get(indx));

                       


            }


             List Coeffs = new ArrayList<>();

             for( i = 0 ; i < DominantTerms.size(); i++){

                Expression term = DominantTerms.get(i);

                Identifier VarID = (Identifier)ListOfExpressionVariables.get(i);

                Coeffs.add(Symbolic.getCoefficient(term, VarID));

             }
               
             
             Collections.sort(Coeffs, Collections.reverseOrder());


             List<Expression> Sortedscores = new ArrayList<>();

             for(i = 0 ; i < Coeffs.size() ; i++){

                Expression coefficient = (Expression)Coeffs.get(i);


                for( j = 0 ; j < DominantTerms.size(); j++){

                    if(DominantTerms.get(j).getChildren().contains(coefficient)){

                        Sortedscores.add(Symbolic_scores.get(j));

                    }

                }


             }

            //System.out.println( "LNO: " + Symbolic_LoopCostMap + " \nScores sorted: " + Sortedscores +"\n");

                for(Expression key : Symbolic_LoopCostMap.keySet() ){

                    Expression Symbolic_cost = Symbolic_LoopCostMap.get(key);

                    if(Sortedscores.indexOf(Symbolic_cost) != -1)
                        LoopNestOrder.set(Sortedscores.indexOf(Symbolic_cost) , key);

                }


            }
   

        return  LoopNestOrder;


    }

    /** 
     * Following test determines if in the candidate loop permutation, the loop with max reuse is at the
     * innermost position. If that's the case, the loop permutation is profitable.
     * @param loop1     - 1st loop in the permutation
     * @param loop2     - 2nd loop in the permutation
     * @param LoopNest  - The loop nest with the 2 loops
     * @param InnerLoopidx  - Loop index of the inner loop in the permutation
     * @return              - true if profitable, false otherwise
     */
    

    private boolean isprofitable(ForLoop loop1 , ForLoop loop2 , Loop LoopNest , Expression InnerLoopidx){

        int i;
      
        List<Expression> CandidateReverseorder = new ArrayList<Expression>();
        List<Expression> modifiedLoopNest = new ArrayList<Expression>();
        DepthFirstIterator Nestiter = new DepthFirstIterator(LoopNest);
        List<Expression> NestOrder = new ArrayList<Expression>();

        CandidateReverseorder.add(LoopTools.getIndexVariable(loop1));
        CandidateReverseorder.add(LoopTools.getIndexVariable(loop2));

        Collections.reverse(CandidateReverseorder); 
        
        
        while(Nestiter.hasNext()){

            Object LoopObj = Nestiter.next();

            if(LoopObj instanceof ForLoop){

            NestOrder.add(LoopTools.getIndexVariable((ForLoop)LoopObj));

            }

       }
        
        for(i = 0 ; i < CandidateReverseorder.size() ;i++){

            modifiedLoopNest.add(CandidateReverseorder.get(i));
        }
        
       for(i = 0 ; i < NestOrder.size(); i++){

        Expression loopidx = NestOrder.get(i);

        if(!CandidateReverseorder.contains(loopidx)){

                int index = NestOrder.indexOf(loopidx);
                modifiedLoopNest.add(index, loopidx);

        }

       }

       Expression Idx = modifiedLoopNest.get(modifiedLoopNest.size()-1);


       if(Idx.equals(InnerLoopidx)){

        return true;

       }
        else
        return false;


    }


    /**
     * Determines the cost in terms of cache lines for the loop. 
     * Add the costs of all the reference groups and multiply with the iteration count
     * of the loops other than the candidate innermost loop.
     * @param ReferenceCosts    - Cost of Ref groups
     * @param LoopNestIterCount - Number of iterations of each loop in the nest
     * @param CurrentLoop       - Loop index of current loop
     * @return                  - Loop cost in terms of cache lines accessed
     */

    protected long LoopCost(ArrayList ReferenceCosts , HashMap LoopNestIterCount , Expression CurrentLoop){

      
        long RestOfLoops_Iterations = 1 , SumOfRefCosts = 0;
        
        int i;

     
            for(Object key : LoopNestIterCount.keySet()){

                if(!CurrentLoop.equals(key))
                RestOfLoops_Iterations *= (long)LoopNestIterCount.get(key);

            }

            for(i =0 ; i < ReferenceCosts.size(); i++){

                SumOfRefCosts += (long)ReferenceCosts.get(i);

            }
            
        return (SumOfRefCosts * RestOfLoops_Iterations);

    }


    
    /** 
     * Symbolic loop cost. Same as the routine to calculate loop cost with 
     * long loop bounds.
     */
    
    protected Expression SymbolicLoopCost(ArrayList ReferenceCosts , HashMap LoopNestIterCount , Expression CurrentLoop){

        int i;
        IntegerLiteral InitialVal = new IntegerLiteral(1);
        IntegerLiteral IntialValForSum = new IntegerLiteral(0);
        Expression Symbolic_RestOfLoopIterations = (Expression)InitialVal;
        Expression Symbolic_SumOfRefCosts = (Expression)IntialValForSum;

        for(Object key : LoopNestIterCount.keySet()){

            if(!CurrentLoop.equals(key))
             Symbolic_RestOfLoopIterations = Symbolic.multiply(Symbolic_RestOfLoopIterations , (Expression)LoopNestIterCount.get(key));

        }

        for(i =0 ; i < ReferenceCosts.size(); i++){

            Symbolic_SumOfRefCosts = Symbolic.add(Symbolic_SumOfRefCosts, (Expression)ReferenceCosts.get(i));

        }

        Loop_ref_cost.add(Symbolic_SumOfRefCosts);

        Expression result = Symbolic_SumOfRefCosts;                                                          


        return result;


    }


    
    /** 
     * Following method forms reference groups of Array Accesses for each loop in the Nest.
     * 1. Criteria for 2 references to be in the same group depend on whether they have dependencies (Loop carrried and Non Loop Carried).
     * 2. Criteria for forming groups have been derived from the paper - "Optimizing for Parallelism and Data Locality" - K.S Mckinley
     * @param CandidateLoop   - The loop with the array accesses
     * @param LoopBodyArrays  - Arrays in the loop body
     * @param OriginalLoopNestOrder - Original order of the loops
     * @return    - list of reference groups
     */
    

    private static List RefGroup( Loop CandidateLoop, List<ArrayAccess> LoopBodyArrays , List<Expression> OriginalLoopNestOrder){
            
        int i , j ,k;
    
        List FinalRefGroups = new ArrayList<>();
        ArrayList<Expression> ParentArrays = new ArrayList<Expression>();

     
            for( i = 0 ; i < LoopBodyArrays.size();i++){

                ParentArrays.add(LoopBodyArrays.get(i).getArrayName());

            }

           // System.out.println("Program DDG: " + ProgramDDG +"\n");


            LinkedHashSet<Expression> hashSet = new LinkedHashSet<>(ParentArrays);

            ParentArrays = new ArrayList<>(hashSet);

            
            List<Expression> LoopIndexExpressions = new ArrayList<Expression>();
            Expression CandidateLoopid = LoopTools.getIndexVariable(CandidateLoop);

            

                for(i = 0 ; i < LoopBodyArrays.size(); i++){

                    ArrayAccess a = LoopBodyArrays.get(i);

                    List<Expression> IndicesExprs =  a.getIndices();

                    for( j = 0 ; j < IndicesExprs.size(); j++){

                        Expression e = IndicesExprs.get(j);

                        for( k = 0 ; k < OriginalLoopNestOrder.size() ;k++){

                            Expression id = OriginalLoopNestOrder.get(k);

                            if(e.getChildren().contains(id) && 
                            !id.equals(CandidateLoopid)){
                                LoopIndexExpressions.add(e);
                            }

                            else if(e.getChildren().isEmpty() &&
                                    !e.equals(CandidateLoopid) &&
                                    !LoopIndexExpressions.contains(e)){
                                    LoopIndexExpressions.add(e);
                                }

                        }
                    }

                }


                //System.out.println("Loop: " + CandidateLoopid + " LoopIndexExprs: " + LoopIndexExpressions +"\n");

         
                ArrayList<ArrayAccess> References = new ArrayList<ArrayAccess>();

                ArrayList<ArrayAccess> ArraysAssignedToGroups = new ArrayList<ArrayAccess>();

                for(i = 0 ; i < ParentArrays.size() ;i++){

                    Expression expr = ParentArrays.get(i);


                    for(j = 0 ; j < LoopIndexExpressions.size() ; j++){

                        Expression IdxExpr = LoopIndexExpressions.get(j);

                        References = new ArrayList<>();
                            for(k =0 ; k < LoopBodyArrays.size(); k++ ){

                                ArrayAccess loopArray = LoopBodyArrays.get(k);

                                if(loopArray.getIndices().contains(IdxExpr) &&
                                   loopArray.getArrayName().equals(expr)   &&
                                   !ArraysAssignedToGroups.contains(loopArray)){

                                    References.add(loopArray);

                                    ArraysAssignedToGroups.add(loopArray);

                                }


                            }

                            //System.out.println("References: " + References +"\n");

                            if(!References.isEmpty())
                            FinalRefGroups.add(References);

                    }



                }
            

            LinkedHashSet tempHashSet = new LinkedHashSet<>(FinalRefGroups);

            FinalRefGroups = new ArrayList<>(tempHashSet);

         
            // Uncomment the following for testing

            // System.out.println("Loop: " + CandidateLoopid +"\n");
            // System.out.println("Ref groups: " + FinalRefGroups +"\n");
      

        return FinalRefGroups;

    }


 
    private HashMap LoopIterationMap(Loop LoopNest)

    {

        HashMap<Expression, Long> LoopIterationCount = new HashMap<Expression , Long>();
        HashMap<Expression , Expression> SymbolicIterationCount = new HashMap<Expression,Expression>();

        DFIterator<ForLoop> forloopiter = new DFIterator<>(LoopNest, ForLoop.class);
        
        long LoopUpperBound = 0;
        long LoopLowerBound = 0;
        long Loopstride = 0;

        Boolean UpperboundisSymbolic = false;
        Boolean LowerboundisSymbolic = false;
        Boolean StrideboundisSymbolic = false;

        Expression Symbolic_Iter = null;

        long numLoopiter = 0;

      
            while(forloopiter.hasNext()){

                ForLoop loop = forloopiter.next();

                    //Using custom method to find loop upper bound as Range analysis
                    // gives probles when substituting the value range of a symbolic
                    //loop upperbound.

                     Expression upperbound = LoopUpperBoundExpression(loop);
 
                     Expression lowerbound = LoopTools.getLowerBoundExpression(loop);
 
                     Expression incExpr = LoopTools.getIncrementExpression(loop);
 
                     if(upperbound instanceof IntegerLiteral)
                      LoopUpperBound = ((IntegerLiteral)upperbound).getValue();   
                     else
                        UpperboundisSymbolic = true;

                     if(lowerbound instanceof IntegerLiteral)
                      LoopLowerBound = ((IntegerLiteral)lowerbound).getValue();
                     else
                        LowerboundisSymbolic = true;
 
                    if(incExpr instanceof IntegerLiteral)
                      Loopstride = ((IntegerLiteral)incExpr).getValue();
                     else
                      StrideboundisSymbolic = true;


                    if(!UpperboundisSymbolic && !LowerboundisSymbolic && !StrideboundisSymbolic){
                        numLoopiter = ((LoopUpperBound - LoopLowerBound + Loopstride) / Loopstride);
                        
                        LoopIterationCount.put(LoopTools.getIndexVariable(loop), numLoopiter);
                    }

                     else{

                        Expression Bound_Difference = Symbolic.subtract(upperbound , lowerbound);

                        Expression Numerator = Symbolic.add(Bound_Difference , incExpr);

                        Symbolic_Iter = Symbolic.divide(Numerator , incExpr);

                        SymbolicIterationCount.put(LoopTools.getIndexVariable(loop) , Symbolic_Iter);


                     }


            }


            if(!LoopIterationCount.isEmpty())
             return LoopIterationCount;

            else
             return SymbolicIterationCount;


    }


    protected List<Integer> rankByNumOfIteration(List<Integer> rank, List<Loop> loops)
    {
        int i, rankSize;
        boolean flag = true;
        Expression lBound, uBound, inc;
        long lb, ub, in, max = 0;
        long count[] = new long[rank.size()];
        List<Integer> result = new LinkedList<Integer>();

        for(i = 0; i < rank.size(); i++)
        {
            if(LoopTools.isUpperBoundConstant(loops.get(rank.get(i))) && LoopTools.isLowerBoundConstant(loops.get(rank.get(i)))
                    && LoopTools.isIncrementConstant(loops.get(rank.get(i))))
            {
                lBound = LoopTools.getLowerBoundExpression(loops.get(rank.get(i)));
                uBound = LoopTools.getUpperBoundExpression(loops.get(rank.get(i)));
                inc = LoopTools.getIncrementExpression(loops.get(rank.get(i)));
                lb = ((IntegerLiteral)lBound).getValue();
                ub = ((IntegerLiteral)uBound).getValue();
                in = ((IntegerLiteral)inc).getValue();
                count[i] = (ub-lb)/in;
            } else {
                flag = false;
                break;
            }
        }

        // check which one has more loop count
        if(flag) {
            for(i = 0; i < rank.size(); i++)
            {
                if(count[i] > max) {
                    result.clear();
                    result.add(rank.get(i));
                    max = count[i];
                } else if (count[i] == max) {
                    result.add(rank.get(i));
                }
            }
        }
        if(result.size() == 0) return rank;
        return result;
    }

    
    protected int getRank2(List<Integer> rank, List<Expression> expList, List<Loop> loops)
    {
        int i;
        List<Integer> result;

        if(rank.size() == 1) {
            return rank.get(0);
        }
     
        result = rankByMaxInterchange(rank, loops);
        if(result.size() == 1) return result.get(0);

        result = rankByNumOfIteration(result, loops);
        if(result.size() == 1) return result.get(0);

        return result.get(result.size()-1);
    }

    
    protected List<Integer> getRank(List<ArrayAccess> array , List<Expression> expList, int n)
    {
        int i, j, max = 0, cur_exp;
        ArrayList<Integer> result = new ArrayList<Integer>();
        List<Expression> temp = new LinkedList<Expression>();
        Traversable parentTemp;
        Expression lhs, rhs;

    

        for(i = 0; i < expList.size(); i++)
        {
            Expression e = expList.get(i);
            cur_exp = 0;

            for(j = 0; j < array.size(); j++)
            {
                ArrayAccess f = array.get(j);
                if(f.getNumIndices() >= n) {
                    temp = f.getIndex(f.getNumIndices()-1-n).findExpression(e);

                    if(temp.size() >= 1) {
                        cur_exp+=2;
                        parentTemp = (temp.get(0)).getParent();
                        if(parentTemp instanceof BinaryExpression)
                        {
                            if((((BinaryExpression)parentTemp).getOperator()).toString() == "*")
                            {
                                lhs = ((BinaryExpression)parentTemp).getLHS();
                                rhs = ((BinaryExpression)parentTemp).getRHS();

                                if(lhs.equals((Object)e) || rhs.equals((Object)e)) {
                                    cur_exp--;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if(cur_exp > max) {
                max = cur_exp;
                result.clear();
                result.add(i);
            } else if (cur_exp == max)
                result.add(i);
        }


        return result;
    }

   

    
    /** 
     * Determines if the access is a stride 1 access
     * @param Expr - Input Expression
     * @param Var - LoopIndex
     * @return    -  boolean
     */
    //Determines if an access is a stride 1 access
    private boolean UnitStride(Expression Expr , Expression Var)

    {

        boolean IsStrideOneAccess = false;
        
        Expression LHS = null;

        Expression RHS = null;

        if(Expr.equals(Var)){

            return true;
           
        }


        if( Expr instanceof BinaryExpression ){

          BinaryExpression BinaryExpr = (BinaryExpression) Expr;

          LHS = BinaryExpr.getLHS();

          RHS = BinaryExpr.getRHS();

          if(BinaryExpr.getOperator().toString().equals("*"))
             IsStrideOneAccess = false;

          else if(LHS.getChildren().contains(Var) && LHS.toString().contains("*"))
              IsStrideOneAccess = false;

          else if(RHS.getChildren().contains(Var) && RHS.toString().contains("*"))
              IsStrideOneAccess = false;                

          else
              IsStrideOneAccess = true;
             

        }

        return IsStrideOneAccess;


    }

    
  
  /** 
   * Performs the actual swapping of loops
   * @param loop1 - Input loop to be swapped
   * @param loop2 - Input loop to be swapped
   */

    public void swapLoop(ForLoop loop1, ForLoop loop2) 
    {

      
        loop1.getInitialStatement().swapWith(loop2.getInitialStatement());
        loop1.getCondition().swapWith(loop2.getCondition());
        loop1.getStep().swapWith(loop2.getStep());


        return;
    }

   /**
    * Check legality of loop interchange between src and target. Both src and target are in the nest and src is outer than target
    * @param nest   - The input loop nest
    * @param src    - Rank of the source loop
    * @param target - Rank of the target loop
    * @return       - If two loops can be interchanged or not
    */

    public boolean isLegal(LinkedList<Loop> nest, int src, int target)
    {
        int i, j, next;
        DDGraph ddg;
        String str;
        ArrayList<DependenceVector> dpv;
        DependenceVector dd;
        ddg = program.getDDGraph();
        dpv = ddg.getDirectionMatrix(nest);
    
        if(src == target) return true;

        if(src > target) {
            i = src;
            src = target;
            target = i;
        }

        for(i = 0; i < dpv.size(); i++)
        {
            dd = dpv.get(i);
            str = dd.toString();
            for(j = 0; j < str.length(); j++)
            {
                if(j == src) next = target;
                else if(j == target) next = src;
                else next = j;

                if(next < str.length()) {

                    if(str.charAt(next) == '>' ||
                        str.charAt(next) == '*') {
                        return false;
                    }
                    if(str.charAt(next) == '<') break;
                }
            }
        }

        return true;
    }
    

  private boolean HasSymbolicBounds(HashMap LoopIterMap){

    Set<Expression> LoopIndices = LoopIterMap.keySet();

    for(Expression e : LoopIndices){

        Object o = LoopIterMap.get(e);

        if(o instanceof IntegerLiteral || 
           o instanceof Long || o instanceof ArrayAccess){
            
              return false;
        }

        if(o instanceof Expression){
            Expression ubexp = (Expression)o;

            if(Symbolic.getVariables(ubexp) == null)
               return false;
           
        }    
     }

     return true;

  }

  private boolean HasNonSymbolicBounds(HashMap LoopIterMap){

    Set<Expression> LoopIndices = LoopIterMap.keySet();

    for(Expression e : LoopIndices){

        if(!(LoopIterMap.get(e) instanceof IntegerLiteral || 
            LoopIterMap.get(e) instanceof Long)){
           
              return false;
        }
     }

  
     return true;

  }




private static Expression LoopUpperBoundExpression(Loop loop) {
        Expression ub = null;
        if (loop instanceof ForLoop) {
            ForLoop for_loop = (ForLoop)loop;
            // determine upper bound for index variable of this loop
            BinaryExpression cond_expr =
                    (BinaryExpression)for_loop.getCondition();
            Expression rhs = cond_expr.getRHS();
            Expression step_size = LoopTools.getIncrementExpression(loop);
            BinaryOperator cond_op = cond_expr.getOperator();
            if (cond_op.equals(BinaryOperator.COMPARE_LT)) {
                ub = Symbolic.subtract(rhs, step_size);
            } else if ((cond_op.equals(BinaryOperator.COMPARE_LE)) ||
                       (cond_op.equals(BinaryOperator.COMPARE_GE))) {
                ub = Symbolic.simplify(rhs);
            } else if (cond_op.equals(BinaryOperator.COMPARE_GT)) {
                ub = Symbolic.add(rhs, step_size);
            }
        }
      
        return ub;
    }

    
}



