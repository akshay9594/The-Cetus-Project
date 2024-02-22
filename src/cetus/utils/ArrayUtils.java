package cetus.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import cetus.hir.UnaryExpression;
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
     * @throws Exception
     */

    public static final int getTypeSize(ArrayAccess array) {
        int typeSize;
        List<?> types = new ArrayList<>();
        Specifier type;

        // when working with some benchmarks that defines
        // initializations functions we found this kind of expressions
        // after AST is build: (* E)[(800+0)][(900+0)]
        // So, the following expressions got into a class cast exception due to *E is an
        // unary
        // expression and it is not castable as an identifier.
        // types = ((Identifier) array.getArrayName()).getSymbol().getTypeSpecifiers();

        Expression arrayName = array.getArrayName();
        if (arrayName instanceof Identifier) {
            types = ((Identifier) arrayName).getSymbol().getTypeSpecifiers();
        } else if (arrayName instanceof UnaryExpression) {
            types = arrayName.getChildren()
                    .stream()
                    .filter(children -> children instanceof Identifier)
                    .map(identifier -> ((Identifier) identifier).getSymbol())
                    .flatMap(symbol -> ((Symbol) symbol).getTypeSpecifiers().stream())
                    .toList();

        }

        if (types.isEmpty()) {
            return 1;
        }

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

    public static final Expression getFullSizeInBytes(SymbolTable symbols, List<ArrayAccess> arrayAccesses) {
        Expression dataSize = new IntegerLiteral("0");
        for (ArrayAccess arrayAccess : arrayAccesses) {
            Expression typeSize = new IntegerLiteral(getTypeSize(arrayAccess) / 8);
            Expression arraySize = getArraySize(symbols, arrayAccess);
            dataSize = Symbolic.add(dataSize, Symbolic.multiply(typeSize, arraySize));
        }

        return dataSize;
    }

    private static final Expression getArraySize(SymbolTable symbols, ArrayAccess arrayAccess) {

        Expression arrayName = arrayAccess.getArrayName();
        IDExpression arrayID;
        Expression size = new IntegerLiteral("1");

        if (arrayName instanceof IDExpression) {
            arrayID = (IDExpression) arrayName;
        } else {
            List<Traversable> ids = arrayName.getChildren()
                    .stream()
                    .filter(name -> name instanceof Identifier)
                    .toList();
            if (ids.isEmpty()) {
                return size;
            }
            arrayID = (IDExpression) ids.get(0);
        }
        Declaration declaration = symbols.findSymbol(arrayID);

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
                    Expression dimension = arraySpecifier.getDimension(j);
                    if(dimension == null) {
                        dimension = new IntegerLiteral("1");
                    }
                    size = Symbolic.multiply(size, dimension);
                }
            }
        }

        return size;
    }

}
