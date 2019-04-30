package VC.ASTs;

import VC.Scanner.SourcePosition;

public abstract class Type extends AST {

    public Type(SourcePosition Position) {
        super(Position);
    }

    public abstract boolean equals(Object obj);

    public abstract boolean assignable(Object obj);

    public boolean isVoidType() {
        return (this instanceof VoidType);
    }

    public boolean isIntType() {
        return (this instanceof IntType);
    }

    public boolean isFloatType() {
        return (this instanceof FloatType);
    }

    public boolean isStringType() {
        return (this instanceof StringType);
    }

    public boolean isBooleanType() {
        return (this instanceof BooleanType);
    }

    public boolean isArrayType() {
        return (this instanceof ArrayType);
    }

    public boolean isErrorType() {
        return (this instanceof ErrorType);
    }

}
