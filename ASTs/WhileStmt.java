package VC.ASTs;

import VC.Scanner.SourcePosition;

public class WhileStmt extends Stmt {

    public Expr E;
    public Stmt S;

    public AST trueSuccessor, falseSuccessor;

    public WhileStmt(Expr eAST, Stmt sAST, SourcePosition Position) {
        super(Position);
        E = eAST;
        S = sAST;
        E.parent = S.parent = this;
    }

    public Object visit(Visitor v, Object o) {
        return v.visitWhileStmt(this, o);
    }

}
