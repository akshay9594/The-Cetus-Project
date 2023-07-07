package cetus.analysis;

import cetus.hir.*;

import java.util.*;

public class ProcInfo {

    private static Map<IDExpression, Map<Symbol, String>> Procedure_Props_Info;
    private static Map<IDExpression, Map<Symbol,Object>> Procedure_AggSubs_Info;
    private static Map<IDExpression, Map<String, RangeDomain>> Procedure_AggRange_Info;

    public ProcInfo(){
        Procedure_Props_Info=new HashMap<>();
        Procedure_AggSubs_Info = new HashMap<>();
        Procedure_AggRange_Info = new HashMap<>();

    }
    
    public static void setProcedureProps(Procedure proc,Map<Symbol,String> variable_properties){
        Procedure_Props_Info.put(proc.getName(), variable_properties);
    }

    public static void setProcedureSubRanges(Procedure proc,Map<Symbol,Object> Loop_agg_subscripts){
        Procedure_AggSubs_Info.put(proc.getName(), Loop_agg_subscripts);
    }

    public static void setProcedureAggRangeVals(Procedure proc,Map<String, RangeDomain> Loop_agg_ranges){
        Procedure_AggRange_Info.put(proc.getName(),Loop_agg_ranges);
    }

    public static Map<Symbol, String> getProcedureProps(Procedure proc){
        
        if(Procedure_Props_Info == null)
            return new HashMap<>();
        else
        return Procedure_Props_Info.get(proc.getName());
    }

    public static Map<Symbol, Object> getProcedureSubRanges(Procedure proc){
        if(Procedure_AggSubs_Info == null)
        return new HashMap<>();
        else
        return Procedure_AggSubs_Info.get(proc.getName());
    }

    public static Map<String, RangeDomain> getProcedureAggRangeVals(Procedure proc){
        if(Procedure_AggRange_Info == null)
        return new HashMap<>();
        return Procedure_AggRange_Info.get(proc.getName());
    }
    
}
