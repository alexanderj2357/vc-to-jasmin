package VC.Checker;

import VC.ASTs.*;
import VC.ErrorReporter;
import VC.Scanner.SourcePosition;
import VC.StdEnvironment;

import java.util.Arrays;

public final class Checker implements Visitor {
    private final static SourcePosition dummyPos = new SourcePosition();
    private final static Ident dummyI = new Ident("x", dummyPos);
    private java.util.List<String> errorMessage = Arrays.asList(
            "*0: main function is missing",
            "*1: return type of main is not int",
            "*2: identifier redeclared",
            "*3: identifier declared void",
            "*4: identifier declared void[]",
            "*5: identifier undeclared",
            "*6: incompatible type for =",
            "*7: invalid lvalue in assignment",
            "*8: incompatible type for return",
            "*9: incompatible type for this binary operator",
            "*10: incompatible type for this unary operator",
            "*11: attempt to use an array/function as a scalar",
            "*12: attempt to use a scalar/function as an array",
            "*13: wrong type for element in array initialiser",
            "*14: invalid initialiser: array initialiser for scalar",
            "*15: invalid initialiser: scalar initialiser for array",
            "*16: excess elements in array initialiser",
            "*17: array subscript is not an integer",
            "*18: array size missing",
            "*19: attempt to reference a scalar/array as a function",
            "*20: if conditional is not boolean",
            "*21: for conditional is not boolean",
            "*22: while conditional is not boolean",
            "*23: break must be in a while/for",
            "*24: continue must be in a while/for",
            "*25: too many actual parameters",
            "*26: too few actual parameters",
            "*27: wrong type for actual parameter",
            "*28: main function invoked within program code",
            "*29: no parameters allowed for the main function",
            "*30: statement(s) not reached",
            "*31: missing return statement"
    );
    private SymbolTable idTable;
    private ErrorReporter reporter;
    private boolean mainFound = false;
    private int whileCounter = 0;
    private int forCounter = 0;
    private Type returnType = null;

    public Checker(ErrorReporter reporter) {
        this.reporter = reporter;
        this.idTable = new SymbolTable();
        establishStdEnvironment();
    }

    public void check(AST ast) {
        ast.visit(this, null);
    }

    public Object visitProgram(Program ast, Object o) {
        ast.FL.visit(this, null);
        if (!mainFound) reporter.reportError(errorMessage.get(0), "", dummyPos);
        return null;
    }

    public Object visitDeclList(DeclList ast, Object o) {
        ast.D.visit(this, null);
        ast.DL.visit(this, null);
        return null;
    }

    public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
        return null;
    }

    public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
        Type declType = varDeclTypeCheck(ast);
        declareIdentifier(ast.I, ast);
        Type rType = (Type) ast.E.visit(this, declType);
        if (declType.isArrayType()) {
            if (((ArrayType) declType).E.isEmptyExpr())
                reporter.reportError(errorMessage.get(18), ast.I.spelling, ast.I.position);
            if (!rType.isArrayType() && !ast.E.isEmptyExpr()) {
                reporter.reportError(errorMessage.get(6), "", ast.position);
                reporter.reportError(errorMessage.get(15), ast.I.spelling, ast.E.position);
            }
            return null;
        }
        if (!declType.assignable(rType)) reporter.reportError(errorMessage.get(6), "", ast.position);
        if (declType.isFloatType() && rType.isIntType()) ast.E = createExprI2F(ast.E);
        return null;
    }

    public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
        Type declType = varDeclTypeCheck(ast);
        declareIdentifier(ast.I, ast);
        Type rType = (Type) ast.E.visit(this, declType);
        if (declType.isArrayType()) {
            if (((ArrayType) declType).E.isEmptyExpr())
                reporter.reportError(errorMessage.get(18), ast.I.spelling, ast.I.position);
            if (!rType.isArrayType() && !ast.E.isEmptyExpr())
                reporter.reportError(errorMessage.get(15), ast.I.spelling, ast.E.position);
            return null;
        }
        if (!declType.assignable(rType)) reporter.reportError(errorMessage.get(6), "", ast.position);
        if (declType.isFloatType() && rType.isIntType()) ast.E = createExprI2F(ast.E);
        return null;
    }

    public Object visitFuncDecl(FuncDecl ast, Object o) {
        Type declType = (Type) ast.T.visit(this, null);
        declareIdentifier(ast.I, ast);
        if (ast.I.spelling.equals("main")) {
            mainFound = true;
            if (!ast.T.isIntType()) reporter.reportError(errorMessage.get(1), "", ast.position);
        }
        returnType = declType;
        ast.S.visit(this, ast.PL);
        return null;
    }

    public Object visitParaList(ParaList ast, Object o) {
        ast.P.visit(this, null);
        ast.PL.visit(this, null);
        return null;
    }

    public Object visitParaDecl(ParaDecl ast, Object o) {
        varDeclTypeCheck(ast);
        declareIdentifier(ast.I, ast);
        return null;
    }

    public Object visitEmptyParaList(EmptyParaList ast, Object o) {
        return null;
    }

    public Object visitCallExpr(CallExpr ast, Object o) {
        Decl binding = (Decl) ast.I.visit(this, null);
        if (binding == null) {
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        if (!binding.isFuncDecl()) {
            reporter.reportError(errorMessage.get(19), ast.I.spelling, ast.I.position);
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        ast.type = binding.T;
        ast.AL.visit(this, ((FuncDecl) binding).PL);
        return ast.type;
    }

    public Object visitArgList(ArgList ast, Object o) {
        if (!(o instanceof ParaList)) {
            reporter.reportError(errorMessage.get(25), "", ast.position);
            return null;
        }
        ast.A.visit(this, ((ParaList) o).P);
        ast.AL.visit(this, ((ParaList) o).PL);
        return null;
    }

    public Object visitEmptyArgList(EmptyArgList ast, Object o) {
        if (!(o instanceof EmptyParaList)) reporter.reportError(errorMessage.get(26), "", ast.position);
        return null;
    }

    public Object visitArg(Arg ast, Object o) {
        ParaDecl decl = (ParaDecl) o;
        Type argType = (Type) ast.E.visit(this, dummyI);
        if (decl.T.isArrayType() && argType.isArrayType()) {
            ArrayType declArrType = (ArrayType) decl.T;
            ArrayType argArrType = (ArrayType) argType;
            if (!declArrType.T.assignable(argArrType.T))
                reporter.reportError(errorMessage.get(27), decl.I.spelling, ast.position);
        } else if (!decl.T.assignable(argType))
            reporter.reportError(errorMessage.get(27), decl.I.spelling, ast.position);
        if (decl.T.isFloatType() && argType.isIntType()) ast.E = createExprI2F(ast.E);
        return null;
    }

    public Object visitCompoundStmt(CompoundStmt ast, Object o) {
        idTable.openScope();
        if (o instanceof ParaList) ((ParaList) o).visit(this, null);
        ast.DL.visit(this, null);
        ast.SL.visit(this, null);
        idTable.closeScope();
        return null;
    }

    public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
        idTable.openScope();
        if (o instanceof ParaList) ((ParaList) o).visit(this, null);
        idTable.closeScope();
        return null;
    }

    public Object visitStmtList(StmtList ast, Object o) {
        ast.S.visit(this, o);
        if (ast.S instanceof ReturnStmt && ast.SL instanceof StmtList)
            reporter.reportError(errorMessage.get(30), "", ast.SL.position);
        ast.SL.visit(this, o);
        return null;
    }

    public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
        return null;
    }

    public Object visitExprStmt(ExprStmt ast, Object o) {
        ast.E.visit(this, o);
        return null;
    }

    public Object visitEmptyStmt(EmptyStmt ast, Object o) {
        return null;
    }

    public Object visitReturnStmt(ReturnStmt ast, Object o) {
        Type retType = (Type) ast.E.visit(this, null);
        if (returnType.isVoidType() && ast.E.isEmptyExpr()) return null;
        if (!returnType.isVoidType() && ast.E.isEmptyExpr()) {
            reporter.reportError(errorMessage.get(8), "", ast.position);
            return null;
        }
        if (!returnType.assignable(retType)) {
            reporter.reportError(errorMessage.get(8), "", ast.position);
            return null;
        }
        if (returnType.isFloatType() && retType.isIntType()) ast.E = createExprI2F(ast.E);
        return null;
    }

    public Object visitIfStmt(IfStmt ast, Object o) {
        Type ifCondType = (Type) ast.E.visit(this, null);
        if (!ifCondType.isBooleanType()) reporter.reportError(errorMessage.get(20), "", ast.E.position);
        ast.S1.visit(this, null);
        ast.S2.visit(this, null);
        return null;
    }

    public Object visitForStmt(ForStmt ast, Object o) {
        forCounter++;
        ast.E1.visit(this, null);
        Type forCondType = (Type) ast.E2.visit(this, null);
        if (!forCondType.isBooleanType() && !ast.E2.isEmptyExpr())
            reporter.reportError(errorMessage.get(21), "", ast.E2.position);
        ast.E3.visit(this, null);
        ast.S.visit(this, null);
        forCounter--;
        return null;
    }

    public Object visitWhileStmt(WhileStmt ast, Object o) {
        whileCounter++;
        Type whileCondType = (Type) ast.E.visit(this, null);
        if (!whileCondType.isBooleanType()) reporter.reportError(errorMessage.get(22), "", ast.E.position);
        ast.S.visit(this, null);
        whileCounter--;
        return null;
    }

    public Object visitBreakStmt(BreakStmt ast, Object o) {
        if (forCounter == 0 && whileCounter == 0) reporter.reportError(errorMessage.get(23), "", ast.position);
        return null;
    }

    public Object visitContinueStmt(ContinueStmt ast, Object o) {
        if (forCounter == 0 && whileCounter == 0) reporter.reportError(errorMessage.get(24), "", ast.position);
        return null;
    }

    public Object visitInitExpr(InitExpr ast, Object o) {
        if (!((Type) o).isArrayType()) {
            reporter.reportError(errorMessage.get(14), "", ast.position);
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        ArrayType declArrType = (ArrayType) o;
        Integer initLength = (Integer) ast.IL.visit(this, declArrType.T);
        if (declArrType.E.isEmptyExpr())
            declArrType.E = new IntExpr(new IntLiteral(initLength.toString(), dummyPos), dummyPos);
        else {
            IntExpr sizeExpr = (IntExpr) declArrType.E;
            if (Integer.parseInt(sizeExpr.IL.spelling) < initLength)
                reporter.reportError(errorMessage.get(16), "", ast.position);
        }
        ast.type = declArrType;
        return ast.type;
    }

    public Object visitExprList(ExprList ast, Object o) {
        Type exprType = (Type) ast.E.visit(this, null);
        Type declType = (Type) o;
        reporter.reportError(errorMessage.get(13), "", ast.E.position);
        if (declType.isFloatType() && exprType.isIntType()) ast.E = createExprI2F(ast.E);
        Integer restLength = (Integer) ast.EL.visit(this, o);
        return 1 + restLength;
    }

    public Object visitEmptyExprList(EmptyExprList ast, Object o) {
        return 0;
    }

    public Object visitAssignExpr(AssignExpr ast, Object o) {
        Type lType = (Type) ast.E1.visit(this, null);
        Type rType = (Type) ast.E2.visit(this, null);
        if (!(ast.E1 instanceof VarExpr || ast.E1 instanceof ArrayExpr) || lType.isErrorType()) {
            reporter.reportError(errorMessage.get(7), "", ast.position);
            reporter.reportError(errorMessage.get(6), "", ast.position);
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        if (!lType.assignable(rType) || lType.isErrorType() || rType.isErrorType()) {
            reporter.reportError(errorMessage.get(6), "", ast.position);
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        if (lType.isFloatType() && rType.isIntType()) ast.E2 = createExprI2F(ast.E2);
        ast.type = lType;
        return ast.type;
    }

    public Object visitBinaryExpr(BinaryExpr ast, Object o) {
        Type e1Type = (Type) ast.E1.visit(this, null);
        Type e2Type = (Type) ast.E2.visit(this, null);
        if (e1Type.isErrorType() || e2Type.isErrorType()) {
            reporter.reportError(errorMessage.get(9), ast.O.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        if (!(ast.O.spelling.equals("==") || ast.O.spelling.equals("!=") || ast.O.spelling.equals("&&") || ast.O.spelling.equals("||")) && (e1Type.isBooleanType() || e2Type.isBooleanType())) {
            reporter.reportError(errorMessage.get(9), ast.O.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        if ((ast.O.spelling.equals("&&") || ast.O.spelling.equals("||")) && (!e1Type.isBooleanType() || !e2Type.isBooleanType())) {
            reporter.reportError(errorMessage.get(9), ast.O.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        if (!e1Type.assignable(e2Type) && !e2Type.assignable(e1Type)) {
            reporter.reportError(errorMessage.get(9), ast.O.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
        } else {
            if (e1Type.isFloatType() && e2Type.isIntType()) {
                ast.E2 = createExprI2F(ast.E2);
                ast.type = StdEnvironment.floatType;
            } else if (e1Type.isIntType() && e2Type.isFloatType()) {
                ast.E1 = createExprI2F(ast.E1);
                ast.type = StdEnvironment.floatType;
            } else ast.type = e2Type;
            ast.O.visit(this, ast.type);
            if (!(ast.O.spelling.contains("+") || ast.O.spelling.contains("-") || ast.O.spelling.contains("*") || ast.O.spelling.contains("/")))
                ast.type = StdEnvironment.booleanType;
        }
        return ast.type;
    }

    public Object visitUnaryExpr(UnaryExpr ast, Object o) {
        Type exprType = (Type) ast.E.visit(this, null);
        if (ast.O.spelling.equals("!") && !exprType.isBooleanType()) {
            reporter.reportError(errorMessage.get(10), ast.O.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
        } else if ((ast.O.spelling.equals("+") || ast.O.spelling.equals("-")) && !(exprType.isFloatType() || exprType.isIntType())) {
            reporter.reportError(errorMessage.get(10), ast.O.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
        } else {
            ast.type = exprType;
            ast.O.visit(this, ast.type);
        }
        return ast.type;
    }

    public Object visitOperator(Operator O, Object o) {
        Type opType = (Type) o;
        if (opType.isIntType() || opType.isBooleanType()) O.spelling = "i" + O.spelling;
        else if (opType.isFloatType()) O.spelling = "f" + O.spelling;
        return null;
    }

    public Object visitArrayExpr(ArrayExpr ast, Object o) {
        ast.type = (Type) ast.V.visit(this, null);
        if (ast.type == null) {
            reporter.reportError(errorMessage.get(12), ((SimpleVar) ast.V).I.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
        } else if (ast.type.isArrayType()) ast.type = ((ArrayType) ast.type).T;
        else if (!ast.type.isErrorType()) {
            reporter.reportError(errorMessage.get(12), ((SimpleVar) ast.V).I.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
        }
        Type idxType = (Type) ast.E.visit(this, null);
        if (!idxType.isIntType()) {
            reporter.reportError(errorMessage.get(17), "", ast.position);
            ast.type = StdEnvironment.errorType;
        }
        return ast.type;
    }

    public Object visitVarExpr(VarExpr ast, Object o) {
        ast.type = (Type) ast.V.visit(this, null);
        if (o == dummyI) {
            if (ast.type == null) {
                reporter.reportError(errorMessage.get(11), ((SimpleVar) ast.V).I.spelling, ast.position);
                ast.type = StdEnvironment.errorType;
            }
            return ast.type;
        }
        if (ast.type == null || ast.type.isArrayType()) {
            reporter.reportError(errorMessage.get(11), ((SimpleVar) ast.V).I.spelling, ast.position);
            ast.type = StdEnvironment.errorType;
        }
        return ast.type;
    }

    public Object visitSimpleVar(SimpleVar ast, Object o) {
        Decl binding = (Decl) ast.I.visit(this, null);
        if (binding == null) {
            ast.type = StdEnvironment.errorType;
            return ast.type;
        }
        if (binding.isGlobalVarDecl()) ast.type = ((GlobalVarDecl) binding).T;
        else if (binding.isLocalVarDecl()) ast.type = ((LocalVarDecl) binding).T;
        else if (binding.isParaDecl()) ast.type = ((ParaDecl) binding).T;
        else if (binding.isFuncDecl()) {
            ast.type = StdEnvironment.errorType;
            return null;
        }
        return ast.type;
    }

    public Object visitErrorType(ErrorType ast, Object o) {
        return StdEnvironment.errorType;
    }

    public Object visitBooleanType(BooleanType ast, Object o) {
        return StdEnvironment.booleanType;
    }

    public Object visitIntType(IntType ast, Object o) {
        return StdEnvironment.intType;
    }

    public Object visitFloatType(FloatType ast, Object o) {
        return StdEnvironment.floatType;
    }

    public Object visitStringType(StringType ast, Object o) {
        return StdEnvironment.stringType;
    }

    public Object visitVoidType(VoidType ast, Object o) {
        return StdEnvironment.voidType;
    }

    public Object visitArrayType(ArrayType ast, Object o) {
        return ast;
    }

    public Object visitBooleanLiteral(BooleanLiteral SL, Object o) {
        return StdEnvironment.booleanType;
    }

    public Object visitIntLiteral(IntLiteral IL, Object o) {
        return StdEnvironment.intType;
    }

    public Object visitFloatLiteral(FloatLiteral IL, Object o) {
        return StdEnvironment.floatType;
    }

    public Object visitStringLiteral(StringLiteral IL, Object o) {
        return StdEnvironment.stringType;
    }

    public Object visitEmptyExpr(EmptyExpr ast, Object o) {
        ast.type = StdEnvironment.errorType;
        return ast.type;
    }

    public Object visitBooleanExpr(BooleanExpr ast, Object o) {
        ast.type = StdEnvironment.booleanType;
        return ast.type;
    }

    public Object visitIntExpr(IntExpr ast, Object o) {
        ast.type = StdEnvironment.intType;
        return ast.type;
    }

    public Object visitFloatExpr(FloatExpr ast, Object o) {
        ast.type = StdEnvironment.floatType;
        return ast.type;
    }

    public Object visitStringExpr(StringExpr ast, Object o) {
        ast.type = StdEnvironment.stringType;
        return ast.type;
    }

    public Object visitIdent(Ident I, Object o) {
        Decl binding = idTable.retrieve(I.spelling);
        if (binding == null) reporter.reportError(errorMessage.get(5), I.spelling, I.position);
        I.decl = binding;
        return binding;
    }

    private FuncDecl declareStdFunc(Type resultType, String id, List pl) {
        FuncDecl binding;
        binding = new FuncDecl(resultType, new Ident(id, dummyPos), pl, new EmptyStmt(dummyPos), dummyPos);
        idTable.insert(id, binding);
        return binding;
    }

    private void establishStdEnvironment() {
        StdEnvironment.booleanType = new BooleanType(dummyPos);
        StdEnvironment.intType = new IntType(dummyPos);
        StdEnvironment.floatType = new FloatType(dummyPos);
        StdEnvironment.stringType = new StringType(dummyPos);
        StdEnvironment.voidType = new VoidType(dummyPos);
        StdEnvironment.errorType = new ErrorType(dummyPos);
        StdEnvironment.getIntDecl = declareStdFunc(StdEnvironment.intType, "getInt", new EmptyParaList(dummyPos));
        StdEnvironment.putIntDecl = declareStdFunc(StdEnvironment.voidType, "putInt", new ParaList(new ParaDecl(StdEnvironment.intType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putIntLnDecl = declareStdFunc(StdEnvironment.voidType, "putIntLn", new ParaList(new ParaDecl(StdEnvironment.intType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.getFloatDecl = declareStdFunc(StdEnvironment.floatType, "getFloat", new EmptyParaList(dummyPos));
        StdEnvironment.putFloatDecl = declareStdFunc(StdEnvironment.voidType, "putFloat", new ParaList(new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putFloatLnDecl = declareStdFunc(StdEnvironment.voidType, "putFloatLn", new ParaList(new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putBoolDecl = declareStdFunc(StdEnvironment.voidType, "putBool", new ParaList(new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putBoolLnDecl = declareStdFunc(StdEnvironment.voidType, "putBoolLn", new ParaList(new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putStringLnDecl = declareStdFunc(StdEnvironment.voidType, "putStringLn", new ParaList(new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putStringDecl = declareStdFunc(StdEnvironment.voidType, "putString", new ParaList(new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos), new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putLnDecl = declareStdFunc(StdEnvironment.voidType, "putLn", new EmptyParaList(dummyPos));
    }

    private Type varDeclTypeCheck(Decl ast) {
        Type declType = (Type) ast.T.visit(this, null);
        if (declType.isVoidType()) reporter.reportError(errorMessage.get(3), ast.I.spelling, ast.I.position);
        else if (declType.isArrayType()) {
            if (((ArrayType) declType).T.isVoidType())
                reporter.reportError(errorMessage.get(4), ast.I.spelling, ast.I.position);
        }
        return declType;
    }

    private void declareIdentifier(Ident ident, Decl decl) {
        IdEntry entry = idTable.retrieveOneLevel(ident.spelling);
        if (entry != null) reporter.reportError(errorMessage.get(2), ident.spelling, ident.position);
        idTable.insert(ident.spelling, decl);
        ident.decl = decl;
    }

    private Expr createExprI2F(Expr oldE) {
        Operator op = new Operator("i2f", dummyPos);
        UnaryExpr eAST = new UnaryExpr(op, oldE, dummyPos);
        eAST.type = StdEnvironment.floatType;
        return eAST;
    }
}
