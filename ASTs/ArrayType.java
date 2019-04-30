package VC.ASTs;

import VC.Scanner.SourcePosition;

public class ArrayType extends Type {

  public Type T;
  public Expr E;

  public ArrayType (Type tAST, Expr dAST, SourcePosition Position) {
    super (Position);
    T = tAST;
    E = dAST;
    T.parent = E.parent = this;
  }

  public Object visit (Visitor v, Object o) {
    return v.visitArrayType(this, o);
  }

  public boolean equals(Object obj) {
    return false;
  }

  public boolean assignable(Object obj) {
    return false;
  }

  public String toString() {
    if (T instanceof IntType)
      return "[I";
    else if (T instanceof FloatType)
      return "[F";
    else if (T instanceof BooleanType)
      return "[B";
    else if (T instanceof VoidType)
      return "[V";
    else
      return "[*";
  }

}
