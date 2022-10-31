package cetus.utils;

import java.util.List;

import cetus.hir.ArrayAccess;
import cetus.hir.Identifier;
import cetus.hir.Specifier;

public class ArrayUtils {

    /**
    * Get the size in bits of an specific type/specifier in an array.
    * Example: if array is a boolean array, the return type will be 1. 32 for
    * Integer
    * 
    * @param array An array with an specific type of specifier
    * @return An integer that represents the size in bits of the type of the
    *         array's specifier passed as a paramteer.
    */

    public static final int getTypeSize(ArrayAccess array) {
       int typeSize;
       List<?> types;
       Specifier type;

       types = ((Identifier) array.getArrayName()).getSymbol().getTypeSpecifiers();
       type = (Specifier) types.get(0);
       
       if (type == Specifier.BOOL)
           typeSize = 1;
       else if (type == Specifier.CHAR)
           typeSize = 8;
       else if (type == Specifier.WCHAR_T)
           typeSize = 32;
       else if (type == Specifier.SHORT)
           typeSize = 16;
       else if (type == Specifier.INT)
           typeSize = 32;
       else if (type == Specifier.LONG)
           typeSize = 32;
       else if (type == Specifier.SIGNED)
           typeSize = 32;
       else if (type == Specifier.UNSIGNED)
           typeSize = 32;
       else if (type == Specifier.FLOAT)
           typeSize = 32;
       else if (type == Specifier.DOUBLE)
           typeSize = 64;
       else if (type == Specifier.VOID)
           typeSize = 8;
       else
           typeSize = 32;
       return typeSize;
   }
}
