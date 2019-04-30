package VC;

import VC.ASTs.FuncDecl;
import VC.ASTs.Type;

public final class StdEnvironment {

    public static Type booleanType, intType, floatType, stringType, voidType, errorType;

    public static FuncDecl
            putBoolDecl, putBoolLnDecl,
            getIntDecl, putIntDecl, putIntLnDecl,
            getFloatDecl, putFloatDecl, putFloatLnDecl,
            putStringDecl, putStringLnDecl, putLnDecl;
}
