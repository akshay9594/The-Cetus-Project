package cetus.utils;

import java.util.List;

import cetus.hir.DFIterator;
import cetus.hir.Declaration;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.FunctionCall;
import cetus.hir.IDExpression;
import cetus.hir.Identifier;
import cetus.hir.Initializer;
import cetus.hir.Literal;
import cetus.hir.NameID;
import cetus.hir.Procedure;
import cetus.hir.Specifier;
import cetus.hir.Symbol;
import cetus.hir.SymbolTable;
import cetus.hir.Traversable;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;

public class VariableDeclarationUtils {

    public static SymbolTable getParametersDeclarationSpace(Procedure procedure, IDExpression id) {
        DFIterator<FunctionCall> functionCalls = new DFIterator<>(procedure.getParent(), FunctionCall.class);
        while (functionCalls.hasNext()) {
            FunctionCall functionCall = functionCalls.next();
            if (functionCall.getProcedure() == procedure) {
                SymbolTable syt = getVariableDeclarationSpace(functionCall);

                Declaration declaration = syt.findSymbol(id);
                if (declaration == null) {
                    continue;
                }
                Declarator declarator = null;
                for (Traversable child : declaration.getChildren()) {
                    if (!(child instanceof Declarator)) {
                        continue;
                    }

                    declarator = (Declarator) child;
                    break;
                }
                if (declarator == null) {
                    continue;
                }

                return syt;

            }
        }
        return null;
    }

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

    public static Expression getVariableDeclaredValue(SymbolTable variableDeclarationSpace, IDExpression id) {

        if (variableDeclarationSpace == null) {
            return id;
        }
        if (variableDeclarationSpace instanceof Procedure) {
            return getVariableDeclaredValue(getParametersDeclarationSpace((Procedure) variableDeclarationSpace, id),
                    id);
        }
        Expression declaredValue = id;
        Declaration declaration = variableDeclarationSpace.findSymbol(id);
        if (declaration == null) {
            return declaredValue;
        }
        List<Traversable> children = declaration.getChildren();
        Declarator declarator = null;

        for (Traversable child : children) {
            if (!(child instanceof Declarator)) {
                continue;
            }

            declarator = (Declarator) child;
            break;
        }

        if (declarator.getInitializer() == null || declarator.getInitializer().getChildren() == null) {
            return getVariableDeclaredValue(
                    VariableDeclarationUtils.getVariableDeclarationSpace(variableDeclarationSpace.getParent()), id);

        }
        for (Traversable child : declarator.getInitializer().getChildren()) {
            if (child instanceof Literal) {
                declaredValue = (Literal) child;
            }
        }

        return declaredValue;
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

    public static Identifier getIdentifier(SymbolTable variableDeclarationSpace, String symbolName) {
        for (Symbol symbol : variableDeclarationSpace.getSymbols()) {
            if (symbol.getSymbolName().equals(symbolName)) {
                if (symbol instanceof VariableDeclarator) {
                    // for (Traversable child : ((VariableDeclarator) symbol).getChildren()) {
                    Identifier newId = new Identifier(symbol);
                    return newId;
                    // if (child instanceof NameID) {
                    // return ((NameID) child);
                    // }

                    // }
                }
            }
        }

        return null;
    }
}
