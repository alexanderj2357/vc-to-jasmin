package VC.ASTs;

import VC.Scanner.SourcePosition;

public abstract class Decl extends AST {

  public Type T;
  public Ident I;

  public int index; 

  public Decl(SourcePosition Position) {
    super (Position);
  }

  public boolean isFuncDecl() {
    return this instanceof FuncDecl;
  }

  public boolean isGlobalVarDecl() {
    return this instanceof GlobalVarDecl;
  }

  public boolean isLocalVarDecl() {
    return this instanceof LocalVarDecl;
  }

  public boolean isParaDecl() {
    return this instanceof ParaDecl;
  }

}
