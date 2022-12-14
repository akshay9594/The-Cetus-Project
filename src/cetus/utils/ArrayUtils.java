package cetus.utils;

import java.util.List;

import cetus.hir.ArrayAccess;
import cetus.hir.ArraySpecifier;
import cetus.hir.Declaration;
import cetus.hir.Expression;
import cetus.hir.ForLoop;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.IntegerLiteral;
import cetus.hir.Specifier;
import cetus.hir.Symbol;
import cetus.hir.SymbolTable;
import cetus.hir.Symbolic;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

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
        else if (type == Specifier.VOID)
            typeSize = 8;
        else if (type == Specifier.WCHAR_T)
            typeSize = 32;
        else if (type == Specifier.SHORT)
            typeSize = 16;
        else if (type == Specifier.INT)
            typeSize = 32;
        else if (type == Specifier.SIGNED)
            typeSize = 32;
        else if (type == Specifier.UNSIGNED)
            typeSize = 32;
        else if (type == Specifier.FLOAT)
            typeSize = 32;
        else if (type == Specifier.LONG)
            typeSize = 64;
        else if (type == Specifier.DOUBLE)
            typeSize = 64;
        else
            typeSize = 32;
        return typeSize;
    }

    public static final Expression getFullSize(SymbolTable symbols, List<ArrayAccess> arrayAccesses) {
        Expression dataSize = new IntegerLiteral("0");
        for (ArrayAccess arrayAccess : arrayAccesses) {
            Expression typeSize = new IntegerLiteral(getTypeSize(arrayAccess));
            Expression arraySize = getArraySize(symbols, arrayAccess);
            dataSize = Symbolic.add(dataSize, Symbolic.multiply(typeSize, arraySize));
        }

        return dataSize;
    }

    private static final Expression getArraySize(SymbolTable symbols, ArrayAccess arrayAccess) {

        IDExpression arrayName = (IDExpression) arrayAccess.getArrayName();
        Declaration declaration = symbols.findSymbol(arrayName);

        Expression size = new IntegerLiteral("1");
        List<Traversable> children = declaration.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Traversable childObj = children.get(i);
            if (!(childObj instanceof VariableDeclarator)) {
                continue;
            }
            VariableDeclarator child = (VariableDeclarator) childObj;
            if (!child.getSymbolName().equals(arrayName.toString())) {
                continue;
            }
            List<ArraySpecifier> specs = child.getArraySpecifiers();
            for (ArraySpecifier arraySpecifier : specs) {
                int dimensions = arraySpecifier.getNumDimensions();
                for (int j = 0; j < dimensions; j++) {
                    size = Symbolic.multiply(size, arraySpecifier.getDimension(j));
                }
            }
        }

        return size;
    }

}
