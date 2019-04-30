package VC.ASTs;

public interface Visitor {

    public abstract Object visitProgram(Program ast, Object o);

    public abstract Object visitEmptyDeclList(EmptyDeclList ast, Object o);

    public abstract Object visitEmptyStmtList(EmptyStmtList ast, Object o);

    public abstract Object visitEmptyExprList(EmptyExprList ast, Object o);

    public abstract Object visitEmptyParaList(EmptyParaList ast, Object o);

    public abstract Object visitEmptyArgList(EmptyArgList ast, Object o);

    public abstract Object visitDeclList(DeclList ast, Object o);

    public abstract Object visitFuncDecl(FuncDecl ast, Object o);

    public abstract Object visitGlobalVarDecl(GlobalVarDecl ast, Object o);

    public abstract Object visitLocalVarDecl(LocalVarDecl ast, Object o);

    public abstract Object visitStmtList(StmtList ast, Object o);

    public abstract Object visitIfStmt(IfStmt ast, Object o);

    public abstract Object visitWhileStmt(WhileStmt ast, Object o);

    public abstract Object visitForStmt(ForStmt ast, Object o);

    public abstract Object visitBreakStmt(BreakStmt ast, Object o);

    public abstract Object visitContinueStmt(ContinueStmt ast, Object o);

    public abstract Object visitReturnStmt(ReturnStmt ast, Object o);

    public abstract Object visitCompoundStmt(CompoundStmt ast, Object o);

    public abstract Object visitExprStmt(ExprStmt ast, Object o);

    public abstract Object visitEmptyCompStmt(EmptyCompStmt ast, Object o);

    public abstract Object visitEmptyStmt(EmptyStmt ast, Object o);

    public abstract Object visitIntExpr(IntExpr ast, Object o);

    public abstract Object visitFloatExpr(FloatExpr ast, Object o);

    public abstract Object visitBooleanExpr(BooleanExpr ast, Object o);

    public abstract Object visitStringExpr(StringExpr ast, Object o);

    public abstract Object visitUnaryExpr(UnaryExpr ast, Object o);

    public abstract Object visitBinaryExpr(BinaryExpr ast, Object o);

    public abstract Object visitInitExpr(InitExpr ast, Object o);

    public abstract Object visitExprList(ExprList ast, Object o);

    public abstract Object visitArrayExpr(ArrayExpr ast, Object o);

    public abstract Object visitVarExpr(VarExpr ast, Object o);

    public abstract Object visitCallExpr(CallExpr ast, Object o);

    public abstract Object visitAssignExpr(AssignExpr ast, Object o);

    public abstract Object visitEmptyExpr(EmptyExpr ast, Object o);

    public abstract Object visitIntLiteral(IntLiteral ast, Object o);

    public abstract Object visitFloatLiteral(FloatLiteral ast, Object o);

    public abstract Object visitBooleanLiteral(BooleanLiteral ast, Object o);

    public abstract Object visitStringLiteral(StringLiteral ast, Object o);

    public abstract Object visitIdent(Ident ast, Object o);

    public abstract Object visitOperator(Operator ast, Object o);

    public abstract Object visitParaList(ParaList ast, Object o);

    public abstract Object visitParaDecl(ParaDecl ast, Object o);

    public abstract Object visitArgList(ArgList ast, Object o);

    public abstract Object visitArg(Arg ast, Object o);

    public abstract Object visitVoidType(VoidType ast, Object o);

    public abstract Object visitBooleanType(BooleanType ast, Object o);

    public abstract Object visitIntType(IntType ast, Object o);

    public abstract Object visitFloatType(FloatType ast, Object o);

    public abstract Object visitStringType(StringType ast, Object o);

    public abstract Object visitArrayType(ArrayType ast, Object o);

    public abstract Object visitErrorType(ErrorType ast, Object o);

    public abstract Object visitSimpleVar(SimpleVar ast, Object o);

}
