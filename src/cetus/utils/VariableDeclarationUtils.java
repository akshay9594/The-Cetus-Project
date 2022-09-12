package cetus.utils;

import java.util.List;

import cetus.hir.Declaration;
import cetus.hir.Expression;
import cetus.hir.Identifier;
import cetus.hir.Initializer;
import cetus.hir.NameID;
import cetus.hir.Specifier;
import cetus.hir.SymbolTable;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

public class VariableDeclarationUtils {

    public static SymbolTable getVariableDeclarationSpace(Traversable traversable) {

        if (traversable instanceof SymbolTable) {
            return (SymbolTable) traversable;
        }
        Traversable auxTraversable = traversable.getParent();
        while (auxTraversable != null && auxTraversable.getParent() != null
                && !(auxTraversable instanceof SymbolTable)) {
            auxTraversable = auxTraversable.getParent();
        }

        return (SymbolTable) auxTraversable;
    }

    public static Identifier declareVariable(SymbolTable variableDeclarationSpace, String varName) {

        return declareVariable(variableDeclarationSpace, varName, null);
    }

    public static Identifier declareVariable(SymbolTable variableDeclarationSpace, String varName,
            Expression value) {

        NameID variableNameID = new NameID(varName);

        VariableDeclarator varDeclarator = new VariableDeclarator(variableNameID);

        if (value != null) {
            Initializer initializer = new Initializer(value);
            varDeclarator.setInitializer(initializer);
        }
        Declaration varDeclaration = new VariableDeclaration(Specifier.INT, varDeclarator);

        if (variableDeclarationSpace.findSymbol(variableNameID) == null) {
            variableDeclarationSpace.addDeclaration(varDeclaration);
        }

        Identifier varIdentifier = new Identifier(varDeclarator);

        return varIdentifier;
    }

    public static void addNewVariableDeclarations(Traversable target, List<Declaration> variableDeclarations) {

        SymbolTable variableDeclarationSpace = VariableDeclarationUtils.getVariableDeclarationSpace(target);
        for (Declaration declaration : variableDeclarations) {
            variableDeclarationSpace.addDeclaration(declaration.clone());
        }

    }
}
