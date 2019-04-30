package VC.ASTs;

import VC.Scanner.SourcePosition;

public class ExprList extends List {
    public Expr E;
    public List EL;

    public int index;

    public ExprList(Expr eAST, List elAST, SourcePosition thePosition) {
        super(thePosition);
        E = eAST;
        EL = elAST;
        E.parent = EL.parent = this;
    }

    public Object visit(Visitor v, Object o) {
        return v.visitExprList(this, o);
    }

}
